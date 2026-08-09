resource "aws_db_subnet_group" "main" {
  name        = "${local.name_prefix}-db-subnet-group"
  description = "Private DB subnet group contract for future RDS instance"
  subnet_ids  = [for subnet in aws_subnet.private : subnet.id]

  tags = {
    Name = "${local.name_prefix}-db-subnet-group"
  }
}

resource "aws_secretsmanager_secret" "db_credentials" {
  name                    = "${local.name_prefix}/database/credentials"
  description             = "Metadata-only secret contract for HomeOps DB credentials. Secret value is intentionally not created in Issue #61"
  recovery_window_in_days = 7

  tags = {
    Name = "${local.name_prefix}-db-credentials"
  }
}

resource "aws_ssm_parameter" "db_name" {
  name        = "/${var.project}/${var.environment}/database/name"
  description = "Database name for the HomeOps backend"
  type        = "String"
  tier        = "Standard"
  value       = var.db_name
}

resource "aws_ssm_parameter" "db_port" {
  name        = "/${var.project}/${var.environment}/database/port"
  description = "Database port for the HomeOps backend"
  type        = "String"
  tier        = "Standard"
  value       = tostring(var.db_port)
}

resource "aws_ssm_parameter" "db_host_placeholder" {
  name        = "/${var.project}/${var.environment}/database/host"
  description = "Placeholder database host until the RDS instance is provisioned"
  type        = "String"
  tier        = "Standard"
  value       = var.db_host_placeholder
}

resource "aws_ssm_parameter" "spring_profile" {
  name        = "/${var.project}/${var.environment}/application/spring-profile"
  description = "Spring profile contract for AWS runtime"
  type        = "String"
  tier        = "Standard"
  value       = var.spring_profile
}

resource "aws_ssm_parameter" "db_credentials_secret_arn" {
  name        = "/${var.project}/${var.environment}/database/credentials-secret-arn"
  description = "Reference to the Secrets Manager ARN for DB credentials"
  type        = "String"
  tier        = "Standard"
  value       = aws_secretsmanager_secret.db_credentials.arn
}
