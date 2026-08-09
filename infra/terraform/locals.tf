locals {
  name_prefix = "${var.project}-${var.environment}"

  required_tags = merge(
    var.extra_tags,
    {
      Project     = var.project
      Environment = var.environment
      Owner       = var.owner
      Purpose     = var.purpose
      ManagedBy   = "Terraform"
    }
  )
}
