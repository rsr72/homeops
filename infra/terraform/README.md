# HomeOps Terraform Foundation (Issue #61)

This directory defines the first Terraform foundation for HomeOps AWS development infrastructure.

Issue #61 establishes infrastructure definitions only. It does not provision resources as part of this repository change, and you must not run `terraform apply` for this story.

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

## Local State Decision

This first slice intentionally uses local Terraform state for learning and scope control.

Remote state and locking are intentionally deferred until Terraform usage becomes shared or deployment-oriented. At that point, introduce an S3 backend with locking in a dedicated follow-up story.

## Security and Cost Guardrails

- Do not commit secrets or real credentials.
- The Terraform code creates only a Secrets Manager secret container, not a secret value.
- Keep `.tfvars` local and uncommitted, except example files.
- Avoid recurring-cost resources in this foundation slice. The largest expected recurring cost in future stories will be the RDS instance, which is intentionally deferred.

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
