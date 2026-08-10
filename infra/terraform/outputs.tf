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

output "private_subnet_azs" {
  description = "Availability zones used by the private subnets"
  value       = [for subnet in aws_subnet.private : subnet.availability_zone]
}

output "private_route_table_id" {
  description = "Private route table ID associated with all private subnets"
  value       = aws_route_table.private.id
}

output "apprunner_vpc_connector_security_group_id" {
  description = "Security group ID for future App Runner VPC connector"
  value       = aws_security_group.apprunner_vpc_connector.id
}

output "rds_security_group_id" {
  description = "Security group ID for future RDS instance"
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
