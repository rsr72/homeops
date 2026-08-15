#!/usr/bin/env bash
set -euo pipefail

readonly SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly REPOSITORY_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
readonly TERRAFORM_DIR="$REPOSITORY_ROOT/infra/terraform"
readonly TFVARS_FILE="${HOMEOPS_TFVARS:-$TERRAFORM_DIR/environments/dev.tfvars}"
readonly RDS_STOP_POLL_INTERVAL_SECONDS="${HOMEOPS_RDS_STOP_POLL_INTERVAL_SECONDS:-10}"
readonly RDS_STOP_MAX_ATTEMPTS="${HOMEOPS_RDS_STOP_MAX_ATTEMPTS:-60}"

AWS_REGION=""
RDS_IDENTIFIER=""
ECS_CLUSTER=""
ECS_SERVICE=""
ALB_ARN=""
TARGET_GROUP_ARN=""
FRONTEND_URL=""
RDS_STATUS=""
ECS_DESIRED_COUNT=""
ECS_RUNNING_COUNT=""
ECS_PENDING_COUNT=""
ECS_STATUS=""
ALB_STATUS=""
HEALTHY_TARGET_COUNT=""
CURRENT_STATE=""

usage() {
  cat <<'EOF'
Usage: homeops-dev-lifecycle.sh <status|awake|sleep|deep-sleep>

The command uses Terraform to manage ECS desired count and AWS APIs only for
the RDS operational state. It never deletes Terraform-managed resources.
EOF
}

fail() {
  printf 'ERROR: %s\n' "$*" >&2
  exit 1
}

require_command() {
  command -v "$1" >/dev/null 2>&1 || fail "Required command is not available: $1"
}

terraform_command() {
  terraform -chdir="$TERRAFORM_DIR" "$@"
}

terraform_output() {
  terraform_command output -raw "$1"
}

load_identifiers() {
  [[ -f "$TFVARS_FILE" ]] || fail "Terraform variable file is missing: $TFVARS_FILE"

  AWS_REGION="$(terraform_output aws_region)"
  RDS_IDENTIFIER="$(terraform_output rds_instance_identifier)"
  ECS_CLUSTER="$(terraform_output backend_ecs_cluster_arn)"
  ECS_SERVICE="$(terraform_output backend_ecs_service_name)"
  ALB_ARN="$(terraform_output backend_alb_arn)"
  TARGET_GROUP_ARN="$(terraform_output backend_target_group_arn)"
  FRONTEND_URL="$(terraform_output frontend_cloudfront_url)"
}

collect_live_state() {
  RDS_STATUS="$(aws rds describe-db-instances \
    --region "$AWS_REGION" \
    --db-instance-identifier "$RDS_IDENTIFIER" \
    --query 'DBInstances[0].DBInstanceStatus' \
    --output text)"

  read -r ECS_DESIRED_COUNT ECS_RUNNING_COUNT ECS_PENDING_COUNT ECS_STATUS <<<"$(aws ecs describe-services \
    --region "$AWS_REGION" \
    --cluster "$ECS_CLUSTER" \
    --services "$ECS_SERVICE" \
    --query 'services[0].[desiredCount,runningCount,pendingCount,status]' \
    --output text)"

  if ! ALB_STATUS="$(aws elbv2 describe-load-balancers \
    --region "$AWS_REGION" \
    --load-balancer-arns "$ALB_ARN" \
    --query 'LoadBalancers[0].State.Code' \
    --output text 2>/dev/null)"; then
    ALB_STATUS="absent"
    HEALTHY_TARGET_COUNT="0"
  else
    HEALTHY_TARGET_COUNT="$(aws elbv2 describe-target-health \
      --region "$AWS_REGION" \
      --target-group-arn "$TARGET_GROUP_ARN" \
      --query 'length(TargetHealthDescriptions[?TargetHealth.State==`healthy`])' \
      --output text)"
  fi

  if [[ "$ALB_STATUS" != "active" ]]; then
    CURRENT_STATE="RECONCILIATION_REQUIRED"
  elif [[ "$RDS_STATUS" == "stopped" && "$ECS_RUNNING_COUNT" != "0" ]]; then
    CURRENT_STATE="ERROR"
  elif [[ "$RDS_STATUS" == "available" && "$ECS_DESIRED_COUNT" == "1" && "$ECS_RUNNING_COUNT" == "1" && "$ECS_PENDING_COUNT" == "0" && "$HEALTHY_TARGET_COUNT" -ge 1 ]]; then
    CURRENT_STATE="AWAKE"
  elif [[ "$RDS_STATUS" == "available" && "$ECS_DESIRED_COUNT" == "0" && "$ECS_RUNNING_COUNT" == "0" && "$ECS_PENDING_COUNT" == "0" && "$HEALTHY_TARGET_COUNT" == "0" ]]; then
    CURRENT_STATE="SLEEP"
  elif [[ "$RDS_STATUS" == "stopped" && "$ECS_DESIRED_COUNT" == "0" && "$ECS_RUNNING_COUNT" == "0" && "$ECS_PENDING_COUNT" == "0" ]]; then
    CURRENT_STATE="DEEP_SLEEP"
  else
    CURRENT_STATE="TRANSITIONING"
  fi
}

print_status() {
  printf 'state=%s\n' "$CURRENT_STATE"
  printf 'rds_status=%s\n' "$RDS_STATUS"
  printf 'ecs_desired_count=%s\n' "$ECS_DESIRED_COUNT"
  printf 'ecs_running_count=%s\n' "$ECS_RUNNING_COUNT"
  printf 'ecs_pending_count=%s\n' "$ECS_PENDING_COUNT"
  printf 'ecs_status=%s\n' "$ECS_STATUS"
  printf 'alb_status=%s\n' "$ALB_STATUS"
  printf 'healthy_target_count=%s\n' "$HEALTHY_TARGET_COUNT"
}

status_exit_code() {
  case "$CURRENT_STATE" in
    AWAKE|SLEEP|DEEP_SLEEP) return 0 ;;
    TRANSITIONING) return 1 ;;
    RECONCILIATION_REQUIRED|ERROR) return 2 ;;
    *) return 2 ;;
  esac
}

ensure_reconcilable_runtime() {
  if [[ "$CURRENT_STATE" == "RECONCILIATION_REQUIRED" ]]; then
    fail "A Terraform-managed ALB is absent or inactive. Reconcile it with Terraform, then rerun this command."
  fi

  if [[ "$CURRENT_STATE" == "ERROR" ]]; then
    fail "ECS has running tasks while RDS is stopped. Restore a consistent state before continuing."
  fi
}

set_desired_count_in_tfvars() {
  local desired_count="$1"
  local assignment_count
  local temporary_file

  assignment_count="$(grep -Ec '^[[:space:]]*ecs_desired_count[[:space:]]*=' "$TFVARS_FILE")"
  [[ "$assignment_count" == "1" ]] || fail "Expected exactly one ecs_desired_count assignment in $TFVARS_FILE"

  temporary_file="$(mktemp "${TFVARS_FILE}.XXXXXX")"
  awk -v desired_count="$desired_count" '
    /^[[:space:]]*ecs_desired_count[[:space:]]*=/ {
      print "ecs_desired_count = " desired_count
      next
    }
    { print }
  ' "$TFVARS_FILE" >"$temporary_file"
  mv "$temporary_file" "$TFVARS_FILE"
}

apply_desired_count() {
  local desired_count="$1"
  local original_file
  local plan_file
  local plan_exit_code

  original_file="$(mktemp "${TFVARS_FILE}.original.XXXXXX")"
  plan_file="$(mktemp "${TMPDIR:-/tmp}/homeops-lifecycle.XXXXXX.tfplan")"
  cp "$TFVARS_FILE" "$original_file"

  cleanup_transition_files() {
    rm -f "$original_file" "$plan_file"
  }
  restore_tfvars_on_failure() {
    mv "$original_file" "$TFVARS_FILE"
    rm -f "$plan_file"
  }

  set_desired_count_in_tfvars "$desired_count"

  set +e
  terraform_command plan -detailed-exitcode -out="$plan_file" -var-file="$TFVARS_FILE"
  plan_exit_code=$?
  set -e

  if [[ "$plan_exit_code" -ne 2 ]]; then
    restore_tfvars_on_failure
    if [[ "$plan_exit_code" -eq 0 ]]; then
      fail "Terraform reported no desired-count change; refusing an unnecessary apply."
    fi
    fail "Terraform plan failed; restored the previous desired count."
  fi

  if ! terraform_command show -json "$plan_file" | jq -e --argjson desired_count "$desired_count" '
    (
      [
        .resource_changes[]?
        | select(.change.actions != ["no-op"])
        | select(
            .address != "aws_ecs_service.backend"
            or .change.actions != ["update"]
            or .change.before.desired_count == .change.after.desired_count
          )
      ] | length
    ) == 0
    and (
      [
        .resource_changes[]?
        | select(
            .address == "aws_ecs_service.backend"
            and .change.actions == ["update"]
            and .change.before.desired_count != .change.after.desired_count
            and .change.after.desired_count == $desired_count
          )
      ] | length
    ) == 1
  ' >/dev/null; then
    restore_tfvars_on_failure
    fail "Terraform plan includes changes outside aws_ecs_service.backend desired_count; restored the previous desired count."
  fi

  if ! terraform_command apply "$plan_file"; then
    restore_tfvars_on_failure
    fail "Terraform apply failed; restored the previous desired count."
  fi

  cleanup_transition_files
}

wait_for_ecs_state() {
  local desired_count="$1"

  aws ecs wait services-stable \
    --region "$AWS_REGION" \
    --cluster "$ECS_CLUSTER" \
    --services "$ECS_SERVICE"

  collect_live_state
  [[ "$ECS_DESIRED_COUNT" == "$desired_count" && "$ECS_RUNNING_COUNT" == "$desired_count" && "$ECS_PENDING_COUNT" == "0" ]] || fail "ECS did not reach desired_count=$desired_count."
}

verify_application() {
  aws elbv2 wait target-in-service \
    --region "$AWS_REGION" \
    --target-group-arn "$TARGET_GROUP_ARN"

  curl --fail --silent --show-error --max-time 30 "$FRONTEND_URL/api/households" >/dev/null || fail "API health verification failed through CloudFront."
}

wait_for_rds_stopped() {
  local attempt
  local last_status="unknown"

  for ((attempt = 1; attempt <= RDS_STOP_MAX_ATTEMPTS; attempt++)); do
    last_status="$(aws rds describe-db-instances \
      --region "$AWS_REGION" \
      --db-instance-identifier "$RDS_IDENTIFIER" \
      --query 'DBInstances[0].DBInstanceStatus' \
      --output text)"

    case "$last_status" in
      stopped) return ;;
      stopping)
        if ((attempt < RDS_STOP_MAX_ATTEMPTS)); then
          command sleep "$RDS_STOP_POLL_INTERVAL_SECONDS"
        fi
        ;;
      *) fail "RDS entered unexpected status '$last_status' while waiting to stop." ;;
    esac
  done

  fail "Timed out waiting for RDS to stop after $RDS_STOP_MAX_ATTEMPTS attempts; last observed status: $last_status."
}

awake() {
  collect_live_state
  ensure_reconcilable_runtime

  if [[ "$CURRENT_STATE" == "AWAKE" ]]; then
    verify_application
    printf 'HomeOps development environment is already awake.\n'
    return
  fi

  [[ "$CURRENT_STATE" == "SLEEP" || "$CURRENT_STATE" == "DEEP_SLEEP" ]] || fail "Cannot awake while state is $CURRENT_STATE. Run status and resolve the intermediate state first."

  if [[ "$RDS_STATUS" == "stopped" ]]; then
    aws rds start-db-instance --region "$AWS_REGION" --db-instance-identifier "$RDS_IDENTIFIER" >/dev/null
  fi
  aws rds wait db-instance-available --region "$AWS_REGION" --db-instance-identifier "$RDS_IDENTIFIER"

  collect_live_state
  ensure_reconcilable_runtime
  [[ "$RDS_STATUS" == "available" ]] || fail "RDS did not become available."

  apply_desired_count 1
  wait_for_ecs_state 1
  verify_application
  printf 'HomeOps development environment is awake.\n'
}

sleep() {
  collect_live_state
  ensure_reconcilable_runtime

  if [[ "$CURRENT_STATE" == "SLEEP" ]]; then
    printf 'HomeOps development environment is already sleeping.\n'
    return
  fi

  [[ "$CURRENT_STATE" == "AWAKE" ]] || fail "Cannot sleep while state is $CURRENT_STATE. Run status and resolve the intermediate state first."

  apply_desired_count 0
  wait_for_ecs_state 0
  [[ "$HEALTHY_TARGET_COUNT" == "0" ]] || fail "ALB still reports healthy targets after ECS stopped."
  printf 'HomeOps development environment is sleeping; RDS and ALB remain available.\n'
}

deep_sleep() {
  collect_live_state
  ensure_reconcilable_runtime

  if [[ "$CURRENT_STATE" == "DEEP_SLEEP" ]]; then
    printf 'HomeOps development environment is already in deep sleep.\n'
    return
  fi

  if [[ "$CURRENT_STATE" == "AWAKE" ]]; then
    sleep
    collect_live_state
  fi

  [[ "$CURRENT_STATE" == "SLEEP" ]] || fail "Cannot enter deep sleep while state is $CURRENT_STATE. Run status and resolve the intermediate state first."
  [[ "$ECS_DESIRED_COUNT" == "0" && "$ECS_RUNNING_COUNT" == "0" && "$ECS_PENDING_COUNT" == "0" ]] || fail "ECS must be fully stopped before RDS is stopped."

  aws rds stop-db-instance --region "$AWS_REGION" --db-instance-identifier "$RDS_IDENTIFIER" >/dev/null
  wait_for_rds_stopped

  collect_live_state
  [[ "$CURRENT_STATE" == "DEEP_SLEEP" ]] || fail "RDS did not reach the expected deep-sleep state."
  printf 'HomeOps development environment is in deep sleep; ALB remains Terraform-managed and provisioned.\n'
}

main() {
  [[ $# -eq 1 ]] || {
    usage
    exit 2
  }

  require_command aws
  require_command curl
  require_command jq
  require_command terraform
  load_identifiers

  case "$1" in
    status)
      collect_live_state
      print_status
      status_exit_code
      ;;
    awake) awake ;;
    sleep) sleep ;;
    deep-sleep) deep_sleep ;;
    *)
      usage
      exit 2
      ;;
  esac
}

main "$@"