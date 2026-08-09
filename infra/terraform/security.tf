resource "aws_security_group" "apprunner_vpc_connector" {
  name        = "${local.name_prefix}-apprunner-vpc-connector-sg"
  description = "Security group contract for future App Runner VPC connector"
  vpc_id      = aws_vpc.dev.id

  ingress = []
  egress  = []

  tags = {
    Name = "${local.name_prefix}-apprunner-vpc-connector-sg"
  }
}

resource "aws_security_group" "rds" {
  name        = "${local.name_prefix}-rds-sg"
  description = "Restricts PostgreSQL access to the App Runner connector SG"
  vpc_id      = aws_vpc.dev.id

  ingress = []
  egress  = []

  tags = {
    Name = "${local.name_prefix}-rds-sg"
  }
}

resource "aws_vpc_security_group_ingress_rule" "rds_from_apprunner" {
  security_group_id            = aws_security_group.rds.id
  referenced_security_group_id = aws_security_group.apprunner_vpc_connector.id
  ip_protocol                  = "tcp"
  from_port                    = 5432
  to_port                      = 5432

  description = "Allow PostgreSQL only from App Runner connector SG"
}

resource "aws_vpc_security_group_egress_rule" "apprunner_to_rds" {
  security_group_id            = aws_security_group.apprunner_vpc_connector.id
  referenced_security_group_id = aws_security_group.rds.id
  ip_protocol                  = "tcp"
  from_port                    = 5432
  to_port                      = 5432

  description = "Allow connector egress only to PostgreSQL on RDS SG"
}
