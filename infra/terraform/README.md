# HomeOps Terraform Foundation (Issues #61, #62, and #63)

This directory defines the Terraform foundation and initial private RDS development database for HomeOps AWS infrastructure.

Issue #61 established infrastructure definitions only. Issue #62 extends that baseline by defining the first private RDS PostgreSQL development instance. Issue #63 adds the first backend container registry slice with Amazon ECR.

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

## Local State Decision

This first slice intentionally uses local Terraform state for learning and scope control.

Remote state and locking are intentionally deferred until Terraform usage becomes shared or deployment-oriented. At that point, introduce an S3 backend with locking in a dedicated follow-up story.

## Security and Cost Guardrails

- Do not commit secrets or real credentials.
- The RDS password is generated and stored by AWS RDS in Secrets Manager.
- Terraform does not generate or store a plaintext DB password value in source or tfvars.
- Keep `.tfvars` local and uncommitted, except example files.
- Keep infrastructure cost-conscious for dev. RDS is now the main recurring cost driver in this Terraform scope.

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
