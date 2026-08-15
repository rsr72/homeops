output "aws_region" {
  description = "Region used for this Terraform foundation"
  value       = var.aws_region
}

output "name_prefix" {
  description = "Resource naming prefix shared across foundation resources"
  value       = local.name_prefix
}

output "vpc_id" {
  description = "VPC ID for future App Runner and RDS resources"
  value       = aws_vpc.dev.id
}

output "private_subnet_ids" {
  description = "Private subnet IDs for future App Runner VPC connector and RDS"
  value       = [for subnet in aws_subnet.private : subnet.id]
}

output "public_subnet_ids" {
  description = "Public subnet IDs for ALB and ECS task networking"
  value       = [for subnet in aws_subnet.public : subnet.id]
}

output "private_subnet_azs" {
  description = "Availability zones used by the private subnets"
  value       = [for subnet in aws_subnet.private : subnet.availability_zone]
}

output "private_route_table_id" {
  description = "Private route table ID associated with all private subnets"
  value       = aws_route_table.private.id
}

output "public_route_table_id" {
  description = "Public route table ID associated with all public subnets"
  value       = aws_route_table.public.id
}

output "internet_gateway_id" {
  description = "Internet gateway ID attached to the development VPC"
  value       = aws_internet_gateway.dev.id
}

output "apprunner_vpc_connector_security_group_id" {
  description = "Legacy-named backend task security group ID"
  value       = aws_security_group.apprunner_vpc_connector.id
}

output "alb_security_group_id" {
  description = "Security group ID for the backend ALB"
  value       = aws_security_group.alb.id
}

output "rds_security_group_id" {
  description = "Security group ID for the RDS instance"
  value       = aws_security_group.rds.id
}

output "db_subnet_group_name" {
  description = "RDS DB subnet group name for future RDS instance provisioning"
  value       = aws_db_subnet_group.main.name
}

output "db_credentials_secret_arn" {
  description = "RDS-managed Secrets Manager ARN for DB credentials"
  value       = aws_db_instance.main.master_user_secret[0].secret_arn
}

output "rds_instance_identifier" {
  description = "RDS instance identifier"
  value       = aws_db_instance.main.identifier
}

output "rds_instance_arn" {
  description = "RDS instance ARN"
  value       = aws_db_instance.main.arn
}

output "rds_endpoint" {
  description = "RDS endpoint address and port"
  value       = aws_db_instance.main.endpoint
}

output "rds_endpoint_address" {
  description = "RDS endpoint address for application connectivity"
  value       = aws_db_instance.main.address
}

output "rds_endpoint_port" {
  description = "RDS endpoint port for application connectivity"
  value       = aws_db_instance.main.port
}

output "rds_hosted_zone_id" {
  description = "RDS hosted zone ID"
  value       = aws_db_instance.main.hosted_zone_id
}

output "ssm_parameter_names" {
  description = "SSM parameter names for non-secret runtime configuration"
  value = {
    db_name                   = aws_ssm_parameter.db_name.name
    db_port                   = aws_ssm_parameter.db_port.name
    db_host                   = aws_ssm_parameter.db_host_placeholder.name
    spring_profile            = aws_ssm_parameter.spring_profile.name
    db_credentials_secret_arn = aws_ssm_parameter.db_credentials_secret_arn.name
  }
}

output "ecr_backend_repository_name" {
  description = "ECR repository name for the backend image"
  value       = aws_ecr_repository.backend.name
}

output "ecr_backend_repository_url" {
  description = "ECR repository URL for backend image push and pull"
  value       = aws_ecr_repository.backend.repository_url
}

output "ecr_backend_repository_arn" {
  description = "ECR repository ARN for future App Runner IAM policies"
  value       = aws_ecr_repository.backend.arn
}

output "ecr_registry_id" {
  description = "AWS account registry ID that owns the backend ECR repository"
  value       = aws_ecr_repository.backend.registry_id
}

output "backend_alb_arn" {
  description = "Backend application load balancer ARN"
  value       = aws_lb.backend.arn
}

output "backend_alb_dns_name" {
  description = "Backend application load balancer DNS name"
  value       = aws_lb.backend.dns_name
}

output "backend_target_group_arn" {
  description = "Backend ALB target group ARN"
  value       = aws_lb_target_group.backend.arn
}

output "backend_ecs_cluster_arn" {
  description = "Backend ECS cluster ARN"
  value       = aws_ecs_cluster.backend.arn
}

output "backend_ecs_service_name" {
  description = "Backend ECS service name"
  value       = aws_ecs_service.backend.name
}

output "backend_ecs_task_definition_arn" {
  description = "Backend ECS task definition ARN"
  value       = aws_ecs_task_definition.backend.arn
}

output "backend_cloudwatch_log_group_name" {
  description = "CloudWatch log group for backend container logs"
  value       = aws_cloudwatch_log_group.backend.name
}

output "backend_task_execution_role_arn" {
  description = "ECS task execution IAM role ARN"
  value       = aws_iam_role.ecs_task_execution.arn
}

output "backend_task_role_arn" {
  description = "ECS task IAM role ARN"
  value       = aws_iam_role.ecs_task.arn
}

output "frontend_s3_bucket_name" {
  description = "Private S3 bucket name used as the frontend CloudFront origin"
  value       = aws_s3_bucket.frontend.bucket
}

output "frontend_s3_bucket_arn" {
  description = "Private S3 bucket ARN used as the frontend CloudFront origin"
  value       = aws_s3_bucket.frontend.arn
}

output "frontend_cloudfront_distribution_id" {
  description = "CloudFront distribution ID for frontend cache invalidations"
  value       = aws_cloudfront_distribution.frontend.id
}

output "frontend_cloudfront_domain_name" {
  description = "CloudFront default domain name for the frontend"
  value       = aws_cloudfront_distribution.frontend.domain_name
}

output "frontend_cloudfront_url" {
  description = "HTTPS URL for the public frontend CloudFront distribution"
  value       = "https://${aws_cloudfront_distribution.frontend.domain_name}"
}
