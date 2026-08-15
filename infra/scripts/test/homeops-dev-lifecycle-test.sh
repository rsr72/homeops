#!/usr/bin/env bash
set -euo pipefail

readonly TEST_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly REPOSITORY_ROOT="$(cd "$TEST_DIR/../../.." && pwd)"
readonly COMMAND="$REPOSITORY_ROOT/infra/scripts/homeops-dev-lifecycle.sh"

TEMPORARY_DIRECTORY=""
MOCK_DIRECTORY=""
STATE_FILE=""
LOG_FILE=""
TFVARS_FILE=""

cleanup() {
  [[ -n "$TEMPORARY_DIRECTORY" ]] && rm -rf "$TEMPORARY_DIRECTORY"
}
trap cleanup EXIT

fail() {
  printf 'FAIL: %s\n' "$*" >&2
  exit 1
}

assert_contains() {
  local expected="$1"
  local actual="$2"
  [[ "$actual" == *"$expected"* ]] || fail "Expected '$expected' in '$actual'"
}

assert_file_contains() {
  local expected="$1"
  local file="$2"
  grep -Fqx "$expected" "$file" || fail "Expected '$expected' in $file"
}

assert_before() {
  local first="$1"
  local second="$2"
  local first_line
  local second_line

  first_line="$(grep -n -m1 -F "$first" "$LOG_FILE" | cut -d: -f1)"
  second_line="$(grep -n -m1 -F "$second" "$LOG_FILE" | cut -d: -f1)"
  [[ -n "$first_line" && -n "$second_line" && "$first_line" -lt "$second_line" ]] || fail "Expected '$first' before '$second'"
}

write_state() {
  cat >"$STATE_FILE" <<EOF
rds=$1
desired=$2
running=$3
pending=$4
alb=$5
rds_sequence=
EOF
}

setup_test() {
  TEMPORARY_DIRECTORY="$(mktemp -d)"
  MOCK_DIRECTORY="$TEMPORARY_DIRECTORY/mock-bin"
  STATE_FILE="$TEMPORARY_DIRECTORY/state"
  LOG_FILE="$TEMPORARY_DIRECTORY/calls.log"
  TFVARS_FILE="$TEMPORARY_DIRECTORY/dev.tfvars"
  mkdir -p "$MOCK_DIRECTORY"

  cat >"$TFVARS_FILE" <<'EOF'
aws_region = "us-east-2"
ecs_desired_count = 1
EOF

  cat >"$MOCK_DIRECTORY/terraform" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail

log() { printf 'terraform %s\n' "$*" >>"$MOCK_LOG"; }
command_name=""
for argument in "$@"; do
  case "$argument" in
    output|plan|show|apply) command_name="$argument"; break ;;
  esac
done

case "$command_name" in
  output)
    case "${!#}" in
      aws_region) printf 'us-east-2\n' ;;
      rds_instance_identifier) printf 'homeops-dev-postgres\n' ;;
      backend_ecs_cluster_arn) printf 'homeops-dev-backend-cluster\n' ;;
      backend_ecs_service_name) printf 'homeops-dev-backend-service\n' ;;
      backend_alb_arn) printf 'arn:aws:elasticloadbalancing:alb\n' ;;
      backend_target_group_arn) printf 'arn:aws:elasticloadbalancing:target-group\n' ;;
      frontend_cloudfront_url) printf 'https://example.cloudfront.net\n' ;;
      *) exit 1 ;;
    esac
    ;;
  plan)
    plan_file="$(printf '%s\n' "$@" | sed -n 's/^-out=//p')"
    [[ -n "$plan_file" ]] || exit 1
    if [[ "${MOCK_UNEXPECTED_PLAN:-false}" == "true" ]]; then
      printf '{"resource_changes":[{"address":"aws_s3_bucket.frontend","change":{"actions":["create"],"before":null,"after":{}}}]}' >"$plan_file"
    else
      source "$MOCK_STATE"
      target_desired="$(awk -F= '/^[[:space:]]*ecs_desired_count[[:space:]]*=/{gsub(/[[:space:]]/, "", $2); print $2}' "$HOMEOPS_TFVARS")"
      printf '{"resource_changes":[{"address":"aws_ecs_service.backend","change":{"actions":["update"],"before":{"desired_count":%s},"after":{"desired_count":%s}}}]}' "$desired" "$target_desired" >"$plan_file"
    fi
    log plan
    exit 2
    ;;
  show)
    cat "${!#}"
    ;;
  apply)
    source "$MOCK_STATE"
    desired="$(awk -F= '/^[[:space:]]*ecs_desired_count[[:space:]]*=/{gsub(/[[:space:]]/, "", $2); print $2}' "$HOMEOPS_TFVARS")"
    cat >"$MOCK_STATE" <<EOF_STATE
rds=$rds
desired=$desired
running=$desired
pending=0
alb=$alb
rds_sequence=${rds_sequence:-}
EOF_STATE
    log apply
    ;;
  *) exit 1 ;;
esac
EOF

  cat >"$MOCK_DIRECTORY/aws" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail

source "$MOCK_STATE"
log() { printf 'aws %s\n' "$*" >>"$MOCK_LOG"; }
write_state() {
  cat >"$MOCK_STATE" <<EOF_STATE
rds=$rds
desired=$desired
running=$running
pending=$pending
alb=$alb
rds_sequence=$rds_sequence
EOF_STATE
}

case "$1 $2" in
  "rds describe-db-instances")
    if [[ -n "$rds_sequence" ]]; then
      rds="${rds_sequence%%,*}"
      if [[ "$rds_sequence" == *,* ]]; then
        rds_sequence="${rds_sequence#*,}"
      else
        rds_sequence=""
      fi
      write_state
    fi
    log "rds-describe-$rds"
    printf '%s\n' "$rds"
    ;;
  "rds start-db-instance") rds=available; write_state; log rds-start ;;
  "rds stop-db-instance") rds=stopping; rds_sequence="${MOCK_RDS_STOP_SEQUENCE:-stopped}"; write_state; log rds-stop ;;
  "rds wait") log "rds-wait-${3}" ;;
  "ecs describe-services") printf '%s\t%s\t%s\tACTIVE\n' "$desired" "$running" "$pending" ;;
  "ecs wait") log ecs-wait ;;
  "elbv2 describe-load-balancers")
    if [[ "${MOCK_ALB_MISSING:-false}" == "true" ]]; then exit 255; fi
    printf '%s\n' "$alb"
    ;;
  "elbv2 describe-target-health")
    if [[ "$desired" == "1" ]]; then printf '1\n'; else printf '0\n'; fi
    ;;
  "elbv2 wait") log target-wait ;;
  *) exit 1 ;;
esac
EOF

  cat >"$MOCK_DIRECTORY/curl" <<'EOF'
#!/usr/bin/env bash
printf 'curl %s\n' "$*" >>"$MOCK_LOG"
EOF

  chmod +x "$MOCK_DIRECTORY/terraform" "$MOCK_DIRECTORY/aws" "$MOCK_DIRECTORY/curl"
}

run_command() {
  PATH="$MOCK_DIRECTORY:$PATH" \
    HOMEOPS_TFVARS="$TFVARS_FILE" \
    MOCK_STATE="$STATE_FILE" \
    MOCK_LOG="$LOG_FILE" \
    "$COMMAND" "$@"
}

test_status_reports_awake() {
  setup_test
  write_state available 1 1 0 active
  local output
  output="$(run_command status)"
  assert_contains 'state=AWAKE' "$output"
  assert_contains 'healthy_target_count=1' "$output"
  cleanup
  TEMPORARY_DIRECTORY=""
}

test_sleep_is_terraform_owned() {
  setup_test
  write_state available 1 1 0 active
  run_command sleep >/dev/null
  assert_file_contains 'ecs_desired_count = 0' "$TFVARS_FILE"
  assert_file_contains 'terraform plan' "$LOG_FILE"
  assert_file_contains 'terraform apply' "$LOG_FILE"
  cleanup
  TEMPORARY_DIRECTORY=""
}

test_deep_sleep_stops_ecs_before_rds() {
  setup_test
  write_state available 0 0 0 active
  PATH="$MOCK_DIRECTORY:$PATH" \
    HOMEOPS_TFVARS="$TFVARS_FILE" \
    MOCK_STATE="$STATE_FILE" \
    MOCK_LOG="$LOG_FILE" \
    MOCK_RDS_STOP_SEQUENCE="stopping,stopped" \
    HOMEOPS_RDS_STOP_POLL_INTERVAL_SECONDS=0 \
    "$COMMAND" deep-sleep >/dev/null
  assert_file_contains 'rds=stopped' "$STATE_FILE"
    assert_file_contains 'desired=0' "$STATE_FILE"
    assert_file_contains 'running=0' "$STATE_FILE"
  assert_before 'aws rds-stop' 'aws rds-describe-stopping'
  assert_before 'aws rds-describe-stopping' 'aws rds-describe-stopped'
  if grep -F 'aws rds-wait' "$LOG_FILE" >/dev/null; then
    fail 'The invalid AWS RDS stopped waiter was invoked.'
  fi
  cleanup
  TEMPORARY_DIRECTORY=""
}

test_deep_sleep_timeout_reports_last_rds_status() {
  setup_test
  write_state available 0 0 0 active
  set +e
  local output
  output="$(PATH="$MOCK_DIRECTORY:$PATH" \
    HOMEOPS_TFVARS="$TFVARS_FILE" \
    MOCK_STATE="$STATE_FILE" \
    MOCK_LOG="$LOG_FILE" \
    MOCK_RDS_STOP_SEQUENCE="stopping,stopping" \
    HOMEOPS_RDS_STOP_POLL_INTERVAL_SECONDS=0 \
    HOMEOPS_RDS_STOP_MAX_ATTEMPTS=2 \
    "$COMMAND" deep-sleep 2>&1)"
  local exit_code=$?
  set -e
  [[ "$exit_code" -ne 0 ]] || fail 'Expected RDS stop timeout to fail.'
  assert_contains 'Timed out waiting for RDS to stop after 2 attempts; last observed status: stopping.' "$output"
  assert_before 'aws rds-stop' 'aws rds-describe-stopping'
  cleanup
  TEMPORARY_DIRECTORY=""
}

test_awake_waits_for_rds_before_scaling_ecs() {
  setup_test
  write_state stopped 0 0 0 active
  sed -i.bak 's/ecs_desired_count = 1/ecs_desired_count = 0/' "$TFVARS_FILE"
  rm "$TFVARS_FILE.bak"
  run_command awake >/dev/null
  assert_file_contains 'ecs_desired_count = 1' "$TFVARS_FILE"
  assert_before 'aws rds-start' 'terraform apply'
  assert_before 'terraform apply' 'aws target-wait'
  assert_file_contains 'curl --fail --silent --show-error --max-time 30 https://example.cloudfront.net/api/households' "$LOG_FILE"
  cleanup
  TEMPORARY_DIRECTORY=""
}

test_unexpected_plan_restores_tfvars() {
  setup_test
  write_state available 1 1 0 active
  set +e
  PATH="$MOCK_DIRECTORY:$PATH" HOMEOPS_TFVARS="$TFVARS_FILE" MOCK_STATE="$STATE_FILE" MOCK_LOG="$LOG_FILE" MOCK_UNEXPECTED_PLAN=true "$COMMAND" sleep >/dev/null 2>&1
  local exit_code=$?
  set -e
  [[ "$exit_code" -ne 0 ]] || fail 'Expected unexpected Terraform plan to fail.'
  assert_file_contains 'ecs_desired_count = 1' "$TFVARS_FILE"
  if grep -Fqx 'terraform apply' "$LOG_FILE"; then
    fail 'Terraform apply ran for an unexpected plan.'
  fi
  cleanup
  TEMPORARY_DIRECTORY=""
}

test_missing_alb_requires_reconciliation() {
  setup_test
  write_state available 0 0 0 active
  set +e
  local output
  output="$(PATH="$MOCK_DIRECTORY:$PATH" HOMEOPS_TFVARS="$TFVARS_FILE" MOCK_STATE="$STATE_FILE" MOCK_LOG="$LOG_FILE" MOCK_ALB_MISSING=true "$COMMAND" status)"
  local exit_code=$?
  set -e
  [[ "$exit_code" == "2" ]] || fail "Expected reconciliation status exit code 2, got $exit_code"
  assert_contains 'state=RECONCILIATION_REQUIRED' "$output"
  cleanup
  TEMPORARY_DIRECTORY=""
}

test_status_reports_awake
test_sleep_is_terraform_owned
test_deep_sleep_stops_ecs_before_rds
test_deep_sleep_timeout_reports_last_rds_status
test_awake_waits_for_rds_before_scaling_ecs
test_unexpected_plan_restores_tfvars
test_missing_alb_requires_reconciliation

printf 'PASS: homeops development lifecycle tests\n'