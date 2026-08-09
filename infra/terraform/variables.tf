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

variable "availability_zones" {
  description = "Optional explicit AZs for private subnets. If empty, the first N available AZs are used"
  type        = list(string)
  default     = []

  validation {
    condition = length(var.availability_zones) == 0 || (
      length(var.availability_zones) >= 2 &&
      length(var.availability_zones) == length(var.private_subnet_cidrs)
    )
    error_message = "If availability_zones is set, provide at least two AZs and one AZ per private subnet CIDR."
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

variable "db_host_placeholder" {
  description = "Placeholder host value until the RDS instance is provisioned"
  type        = string
  default     = "set-by-rds-story"
}

variable "spring_profile" {
  description = "Spring profile contract for AWS runtime"
  type        = string
  default     = "aws-dev"
}
