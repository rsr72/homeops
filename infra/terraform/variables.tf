variable "aws_region" {
  description = "AWS region for HomeOps development infrastructure"
  type        = string
  default     = "us-east-2"
}

variable "project" {
  description = "Project identifier used for naming and tagging"
  type        = string
  default     = "homeops"
}

variable "environment" {
  description = "Environment identifier used for naming and tagging"
  type        = string
  default     = "dev"
}

variable "owner" {
  description = "Owner tag value for accountability"
  type        = string
  default     = "homeops"
}

variable "purpose" {
  description = "Purpose or cost center tag value"
  type        = string
  default     = "mvp"
}

variable "extra_tags" {
  description = "Optional additional tags merged with required tags"
  type        = map(string)
  default     = {}
}

variable "vpc_cidr" {
  description = "CIDR block for the development VPC"
  type        = string
  default     = "10.42.0.0/20"
}

variable "private_subnet_cidrs" {
  description = "CIDR blocks for private subnets. At least two are required"
  type        = list(string)
  default     = ["10.42.0.0/24", "10.42.1.0/24"]

  validation {
    condition     = length(var.private_subnet_cidrs) >= 2
    error_message = "At least two private subnet CIDRs are required."
  }
}

variable "public_subnet_cidrs" {
  description = "CIDR blocks for public subnets. At least two are required"
  type        = list(string)
  default     = ["10.42.2.0/24", "10.42.3.0/24"]

  validation {
    condition     = length(var.public_subnet_cidrs) >= 2
    error_message = "At least two public subnet CIDRs are required."
  }
}

variable "availability_zones" {
  description = "Optional explicit AZs for subnet placement. If empty, the first N available AZs are used"
  type        = list(string)
  default     = []

  validation {
    condition = length(var.availability_zones) == 0 || (
      length(var.availability_zones) >= 2 &&
      length(var.availability_zones) >= max(length(var.private_subnet_cidrs), length(var.public_subnet_cidrs))
    )
    error_message = "If availability_zones is set, provide at least two AZs and at least one AZ per configured private/public subnet CIDR."
  }
}

variable "db_name" {
  description = "Default database name contract for future RDS provisioning"
  type        = string
  default     = "homeops"
}

variable "db_port" {
  description = "PostgreSQL port contract for future RDS provisioning"
  type        = number
  default     = 5432
}

variable "db_engine_version" {
  description = "PostgreSQL engine version for the RDS development instance"
  type        = string
  default     = "16.14"
}

variable "db_instance_class" {
  description = "RDS instance class for the development database"
  type        = string
  default     = "db.t4g.micro"
}

variable "db_storage_type" {
  description = "RDS storage type for the development database"
  type        = string
  default     = "gp3"
}

variable "db_allocated_storage" {
  description = "Initial allocated RDS storage in GiB"
  type        = number
  default     = 20
}

variable "db_max_allocated_storage" {
  description = "Maximum autoscaled RDS storage in GiB"
  type        = number
  default     = 40
}

variable "db_master_username" {
  description = "Stable non-personal master username for the development database"
  type        = string
  default     = "homeops_master"
}

variable "db_backup_retention_period" {
  description = "Backup retention period in days for the development database"
  type        = number
  default     = 3
}

variable "db_backup_window" {
  description = "Preferred daily backup window in UTC (hh24:mi-hh24:mi)"
  type        = string
  default     = "08:00-08:30"
}

variable "db_maintenance_window" {
  description = "Preferred weekly maintenance window in UTC (ddd:hh24:mi-ddd:hh24:mi)"
  type        = string
  default     = "sun:09:00-sun:09:30"
}

variable "db_deletion_protection" {
  description = "Whether deletion protection is enabled for the development database"
  type        = bool
  default     = false
}

variable "db_skip_final_snapshot" {
  description = "Whether to skip final snapshot on destroy for the development database"
  type        = bool
  default     = true
}

variable "spring_profile" {
  description = "Spring profile contract for AWS runtime"
  type        = string
  default     = "aws-dev"
}

variable "backend_image_tag" {
  description = "Immutable backend ECR image tag to deploy"
  type        = string
  default     = "sha-659a315"
}

variable "backend_container_port" {
  description = "Application container port for the backend service"
  type        = number
  default     = 8080
}

variable "ecs_desired_count" {
  description = "Desired running task count for backend ECS service"
  type        = number
  default     = 1
}

variable "ecs_task_cpu" {
  description = "Fargate task CPU units for backend service"
  type        = number
  default     = 256
}

variable "ecs_task_memory" {
  description = "Fargate task memory in MiB for backend service"
  type        = number
  default     = 512
}

variable "ecs_health_check_grace_period_seconds" {
  description = "ECS service grace period before evaluating ALB health checks"
  type        = number
  default     = 180
}

variable "alb_health_check_path" {
  description = "ALB target group health check path"
  type        = string
  default     = "/actuator/health"
}

variable "cloudwatch_log_retention_days" {
  description = "CloudWatch log retention for backend runtime logs"
  type        = number
  default     = 14
}
