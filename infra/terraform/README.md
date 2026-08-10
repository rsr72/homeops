# HomeOps Terraform Foundation (Issues #61 and #62)

This directory defines the Terraform foundation and initial private RDS development database for HomeOps AWS infrastructure.

Issue #61 established infrastructure definitions only. Issue #62 extends that baseline by defining the first private RDS PostgreSQL development instance.

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

## Local State Decision

This first slice intentionally uses local Terraform state for learning and scope control.

Remote state and locking are intentionally deferred until Terraform usage becomes shared or deployment-oriented. At that point, introduce an S3 backend with locking in a dedicated follow-up story.

## Security and Cost Guardrails

- Do not commit secrets or real credentials.
- The RDS password is generated and stored by AWS RDS in Secrets Manager.
- Terraform does not generate or store a plaintext DB password value in source or tfvars.
- Keep `.tfvars` local and uncommitted, except example files.
- Keep infrastructure cost-conscious for dev. RDS is now the main recurring cost driver in this Terraform scope.

## Local Workflow (No Apply)

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
