# HomeOps Terraform Foundation (Issues #61, #62, #63, #66, and frontend hosting)

This directory defines the Terraform foundation, private RDS development database, backend ECR registry, ECS/Fargate development runtime, and CloudFront-delivered frontend for HomeOps AWS infrastructure.

Issue #61 established infrastructure definitions only. Issue #62 extends that baseline by defining the first private RDS PostgreSQL development instance. Issue #63 adds the first backend container registry slice with Amazon ECR. Issue #66 adds the first conventional ECS/Fargate runtime and public ingress path for development.

Note: references to App Runner in this document are historical to Issues #61-#63 and ADR-0001 context at the time. Current backend runtime direction is ADR-0002 (conventional ECS/Fargate).

## Scope in Issue #61

Included:
- versioned Terraform root and AWS provider constraints
- naming and required tagging convention
- development VPC and private subnets across at least two AZs
- no public subnets, no internet gateway, no NAT gateway
- private route table associations only
- security group contract for future App Runner VPC connector
- RDS security group allowing PostgreSQL (5432) only from the App Runner connector SG
- RDS DB subnet group contract
- Secrets Manager metadata-only secret for DB credentials contract
- SSM Parameter Store Standard parameters for non-secret runtime configuration
- variables and outputs for later RDS, ECR, and App Runner stories

Excluded:
- RDS instance
- ECR repository
- App Runner service
- S3, CloudFront, ALB, ECS, EKS
- remote state backend infrastructure
- CI/CD deployment resources

## Scope Added in Issue #62

Included:
- one private single-AZ RDS PostgreSQL instance in the existing private DB subnet group
- db.t4g.micro instance class
- PostgreSQL major version 16 track
- gp3 storage with 20 GiB initial allocation and 40 GiB max autoscaling
- storage encryption at rest using AWS-managed RDS encryption
- RDS-managed master password in Secrets Manager (`manage_master_user_password = true`)
- fixed non-overlapping backup and maintenance windows
- 3-day automated backup retention
- automatic minor version upgrades enabled
- development lifecycle settings: deletion protection disabled and `skip_final_snapshot = true`
- SSM DB host parameter wired to the RDS endpoint
- outputs for endpoint, port, identifier, ARN, and RDS-managed secret ARN

Excluded:
- NAT Gateway, Internet Gateway, public subnet changes
- App Runner, ECR, CloudFront, S3, ALB, ECS, EKS
- Terraform apply as part of repository validation

## Scope Added in Issue #63

Included:
- one private Amazon ECR repository for backend container images
- repository naming via existing prefix convention (`${project}-${environment}-backend`)
- immutable image tags (`image_tag_mutability = IMMUTABLE`)
- ECR basic scan-on-push enabled
- AWS-managed ECR encryption at rest (AES256)
- lifecycle policy to expire untagged images older than 7 days
- lifecycle policy to retain only the most recent 20 `sha-` tagged backend images
- Terraform outputs for ECR repository name, URL, ARN, and registry ID

Excluded:
- App Runner service definition and runtime deployment wiring
- GitHub Actions image publishing automation
- ECS, EKS, ALB, CloudFront, and S3 delivery resources
- application code changes

## Scope Added in Issue #66

Included:
- public subnets across two AZs, internet gateway, public route table, and associations
- public ALB with HTTP listener on port 80 and `/actuator/health` target group checks
- ECS cluster, Fargate task definition, and ECS service with `assign_public_ip = true`
- CloudWatch log group and awslogs wiring for backend container logs
- split IAM roles for execution vs application task responsibilities
- backend task runtime env + secret injection for DB connectivity with `SPRING_PROFILES_ACTIVE=aws-dev`
- security group flow constrained to ALB -> backend task (8080) and backend task -> RDS (5432)

Excluded:
- HTTPS/ACM/Route53
- NAT gateways and VPC interface endpoints
- Terraform apply in CI or repository validation

## Scope Added for Frontend Hosting

Included:
- one private, versioned, AES256-encrypted S3 bucket for immutable frontend release artifacts
- S3 bucket ownership enforcement and all four public-access-block controls
- CloudFront Origin Access Control (OAC) with SigV4 signing for the S3 origin
- an S3 bucket policy allowing reads only from the intended CloudFront distribution
- CloudFront default-domain HTTPS delivery with `PriceClass_100` for development cost control
- CloudFront default behavior for the S3 origin and a viewer-request function for React SPA routes
- CloudFront `/api/*` behavior forwarding to the existing backend ALB with caching disabled
- Terraform outputs required for manual frontend publishing and cache invalidation

Excluded:
- public S3 website hosting or public bucket access
- Cognito/authentication, Route 53, ACM, custom domains, WAF, and GitHub Actions deployment
- ALB HTTPS configuration; CloudFront-to-ALB origin traffic remains HTTP for this temporary development slice
- remote Terraform state and frontend build artifacts managed by Terraform

## Frontend Architecture and Security

Browser requests use the CloudFront default HTTPS domain:

```text
Browser
	-> HTTPS -> CloudFront
			-> /*     -> private S3 frontend origin through OAC
			-> /api/* -> existing public ALB over HTTP (temporary development limitation)
									-> ECS/Fargate backend -> private RDS
```

The frontend keeps its existing relative `/api` requests. CloudFront path routing makes API requests same-origin with the SPA, so no frontend API base URL or backend CORS policy is needed for this slice. The SPA-routing CloudFront Function is associated only with the default S3 behavior; `/api/*` is not rewritten and backend error responses remain intact.

S3 is never configured as a public website origin. All public access is blocked, object ownership is bucket-owner-enforced, default encryption is enabled, and the bucket policy allows `s3:GetObject` only to the CloudFront service principal for this distribution and account. The policy also denies insecure transport.

## Local State Decision

This first slice intentionally uses local Terraform state for learning and scope control.

Remote state and locking are intentionally deferred until Terraform usage becomes shared or deployment-oriented. At that point, introduce an S3 backend with locking in a dedicated follow-up story.

## Security and Cost Guardrails

- Do not commit secrets or real credentials.
- The RDS password is generated and stored by AWS RDS in Secrets Manager.
- Terraform does not generate or store a plaintext DB password value in source or tfvars.
- Keep `.tfvars` local and uncommitted, except example files.
- Keep infrastructure cost-conscious for dev. RDS is now the main recurring cost driver in this Terraform scope.
- Use the CloudFront `PriceClass_100` default, cache hashed frontend assets, and invalidate only after frontend releases.

## Local Workflow (Pre-Apply Gate)

From the repository root:

```bash
cd infra/terraform
terraform fmt -check
terraform init
terraform validate
aws sts get-caller-identity
aws configure get region
terraform plan -var-file=environments/dev.tfvars
```

Before running `terraform plan`, verify the account and region from the AWS commands above to avoid planning against the wrong environment.

Stop at `terraform plan` for the go/no-go checkpoint. Do not run `terraform apply` until explicitly approved.

## Development Lifecycle Commands

The development lifecycle command keeps Terraform as the owner of the ECS service. It updates the ignored local `environments/dev.tfvars` desired-count input and applies only a Terraform plan that changes `aws_ecs_service.backend.desired_count`. It does not call `aws ecs update-service` and does not use `ignore_changes`.

From the repository root:

```bash
./infra/scripts/homeops-dev-lifecycle.sh status
./infra/scripts/homeops-dev-lifecycle.sh awake
./infra/scripts/homeops-dev-lifecycle.sh sleep
./infra/scripts/homeops-dev-lifecycle.sh deep-sleep
```

The command requires `aws`, `terraform`, `jq`, and `curl`, plus an initialized local Terraform state and `environments/dev.tfvars`. It fails instead of applying when the Terraform plan includes any change outside the single ECS desired-count update.

| State | ECS | RDS | ALB | Intended use |
| --- | --- | --- | --- | --- |
| Awake | desired/running 1 | available | retained, target healthy | Active development and validation |
| Sleep | desired/running 0 | available | retained | Short idle periods; Fargate compute paused |
| Deep Sleep | desired/running 0 | stopped | retained | Multi-day idle periods; Fargate and RDS instance compute paused |

`awake` starts RDS, waits for `available`, confirms the Terraform-managed ALB and ECS service exist, applies the guarded desired-count change to one, waits for an ALB healthy target, and verifies `/api/households` through CloudFront. `sleep` applies the guarded desired-count change to zero and waits for no running or pending tasks. `deep-sleep` performs the sleep sequence first, then stops RDS and polls `DBInstanceStatus` until `stopped`; the poll is bounded and reports its last observed status on timeout because the AWS CLI has no `db-instance-stopped` waiter.

Live validation on 2026-08-15 confirmed the complete Awake -> Sleep -> Deep Sleep -> Awake path. A cold Deep Sleep -> Awake recovery took approximately nine minutes, so operators should account for several minutes of RDS startup before the ECS and ALB readiness stages begin.

`status` returns `0` for Awake, Sleep, and Deep Sleep; `1` for a transitional state; and `2` for an error or `RECONCILIATION_REQUIRED` state. An absent or inactive ALB is `RECONCILIATION_REQUIRED`. Do not delete the ALB, target group, listener, ECS service, or CloudFront dependencies with AWS CLI. Reconcile a missing Terraform-managed runtime resource with a reviewed Terraform plan and apply before rerunning `awake`:

```bash
cd infra/terraform
terraform plan -var-file=environments/dev.tfvars
terraform apply -var-file=environments/dev.tfvars
```

Issue #69 deliberately retains the ALB in Sleep and Deep Sleep. Issue #70 tracks eliminating ALB cost through a declarative Terraform runtime-layer design that coordinates the ECS service, ALB, target group, listener, and CloudFront `/api/*` origin behavior.

## Controlled Apply and Manual ECR Publish Workflow

This sequence is used only after explicit apply approval.

From the repository root:

```bash
cd infra/terraform
terraform apply -var-file=environments/dev.tfvars
```

Capture outputs needed for image publishing:

```bash
terraform output -raw aws_region
terraform output -raw ecr_backend_repository_url
terraform output -raw ecr_backend_repository_name
```

Authenticate Docker to ECR:

```bash
AWS_REGION="$(terraform output -raw aws_region)"
ECR_REPOSITORY_URL="$(terraform output -raw ecr_backend_repository_url)"

aws ecr get-login-password --region "$AWS_REGION" \
	| docker login --username AWS --password-stdin "${ECR_REPOSITORY_URL%/*}"
```

Build the existing backend image for AWS runtime-compatible architecture (`linux/amd64`) and tag with immutable Git SHA:

```bash
GIT_SHA="$(git rev-parse --short=12 HEAD)"
IMAGE_TAG="sha-${GIT_SHA}"

docker build \
	--platform linux/amd64 \
	-f backend/Dockerfile \
	-t "$ECR_REPOSITORY_URL:$IMAGE_TAG" \
	backend
```

Push and verify image tag and digest in ECR:

```bash
docker push "$ECR_REPOSITORY_URL:$IMAGE_TAG"

aws ecr describe-images \
	--region "$AWS_REGION" \
	--repository-name "$(terraform output -raw ecr_backend_repository_name)" \
	--image-ids imageTag="$IMAGE_TAG" \
	--query 'imageDetails[0].{tags:imageTags,digest:imageDigest,pushedAt:imagePushedAt,sizeBytes:imageSizeInBytes}'
```

Post-publish Terraform no-drift check:

```bash
cd infra/terraform
terraform plan -detailed-exitcode -var-file=environments/dev.tfvars
```

Expected no-drift result is exit code `0`. Exit code `2` means drift or pending changes and should be reviewed before closing the issue.

## Controlled Frontend Publish Workflow

This sequence is used only after Terraform apply approval and after the CloudFront distribution has deployed. Terraform owns the bucket and distribution; it does not manage generated `frontend/dist` artifacts.

Build and verify the frontend:

```bash
cd frontend
npm ci
npm test
npm run build
```

Upload immutable hashed assets with long-lived caching, then upload the SPA entry point without caching:

```bash
cd infra/terraform
AWS_REGION="$(terraform output -raw aws_region)"
FRONTEND_BUCKET="$(terraform output -raw frontend_s3_bucket_name)"

aws s3 sync ../../frontend/dist "s3://${FRONTEND_BUCKET}" \
	--region "$AWS_REGION" \
	--delete \
	--exclude "index.html" \
	--cache-control "public,max-age=31536000,immutable"

aws s3 cp ../../frontend/dist/index.html "s3://${FRONTEND_BUCKET}/index.html" \
	--region "$AWS_REGION" \
	--cache-control "no-cache, no-store, must-revalidate" \
	--content-type "text/html"
```

Invalidate the distribution after upload and wait for completion before testing the new release:

```bash
FRONTEND_DISTRIBUTION_ID="$(terraform output -raw frontend_cloudfront_distribution_id)"
INVALIDATION_ID="$(aws cloudfront create-invalidation \
	--distribution-id "$FRONTEND_DISTRIBUTION_ID" \
	--paths '/*' \
	--query 'Invalidation.Id' \
	--output text)"

aws cloudfront wait invalidation-completed \
	--distribution-id "$FRONTEND_DISTRIBUTION_ID" \
	--id "$INVALIDATION_ID"
```

## Frontend Post-Deployment Validation

Verify the static artifact inventory and that all S3 public-access blocks remain enabled:

```bash
aws s3 ls "s3://$(terraform output -raw frontend_s3_bucket_name)" --recursive
aws s3api get-public-access-block \
	--bucket "$(terraform output -raw frontend_s3_bucket_name)"
```

Verify anonymous S3 object access is denied, then verify the public CloudFront endpoint and SPA deep links return the application over HTTPS:

```bash
FRONTEND_URL="$(terraform output -raw frontend_cloudfront_url)"
curl --fail --silent --show-error --head "$FRONTEND_URL"
curl --fail --silent --show-error --head "$FRONTEND_URL/vehicles"
```

Use the deployed CloudFront URL to run the existing household, vehicle, and maintenance workflow. Confirm browser requests remain on the CloudFront origin at `/api/*`, succeed without CORS errors, and cleanup uses the same backend API. Finally, run the no-drift check:

```bash
terraform plan -detailed-exitcode -var-file=environments/dev.tfvars
```

For Deep Sleep or permanent cleanup, destroy the distribution and empty versioned frontend bucket through the approved Terraform teardown workflow. CloudFront request/data-transfer and S3 storage/request usage are incremental costs; at low development traffic they should be small compared with the existing ALB, RDS, and Fargate baseline.
