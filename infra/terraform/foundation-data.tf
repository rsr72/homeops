resource "aws_db_subnet_group" "main" {
  name        = "${local.name_prefix}-db-subnet-group"
  description = "Private DB subnet group contract for future RDS instance"
  subnet_ids  = [for subnet in aws_subnet.private : subnet.id]

  tags = {
    Name = "${local.name_prefix}-db-subnet-group"
  }
}

resource "aws_db_instance" "main" {
  identifier                   = "${local.name_prefix}-postgres"
  engine                       = "postgres"
  engine_version               = var.db_engine_version
  instance_class               = var.db_instance_class
  allocated_storage            = var.db_allocated_storage
  max_allocated_storage        = var.db_max_allocated_storage
  storage_type                 = var.db_storage_type
  storage_encrypted            = true
  db_name                      = var.db_name
  username                     = var.db_master_username
  manage_master_user_password  = true
  port                         = var.db_port
  publicly_accessible          = false
  multi_az                     = false
  db_subnet_group_name         = aws_db_subnet_group.main.name
  vpc_security_group_ids       = [aws_security_group.rds.id]
  parameter_group_name         = "default.postgres16"
  backup_retention_period      = var.db_backup_retention_period
  backup_window                = var.db_backup_window
  maintenance_window           = var.db_maintenance_window
  auto_minor_version_upgrade   = true
  allow_major_version_upgrade  = false
  deletion_protection          = var.db_deletion_protection
  skip_final_snapshot          = var.db_skip_final_snapshot
  performance_insights_enabled = false
  monitoring_interval          = 0
  copy_tags_to_snapshot        = true

  tags = {
    Name = "${local.name_prefix}-postgres"
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
  description = "Database host endpoint for the HomeOps backend"
  type        = "String"
  tier        = "Standard"
  value       = aws_db_instance.main.address
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
  description = "Reference to the RDS-managed Secrets Manager ARN for DB credentials"
  type        = "String"
  tier        = "Standard"
  value       = aws_db_instance.main.master_user_secret[0].secret_arn
}
