resource "aws_security_group" "apprunner_vpc_connector" {
  name        = "${local.name_prefix}-apprunner-vpc-connector-sg"
  description = "Security group contract for future App Runner VPC connector"
  vpc_id      = aws_vpc.dev.id

  tags = {
    Name = "${local.name_prefix}-apprunner-vpc-connector-sg"
  }
}

resource "aws_security_group" "rds" {
  name        = "${local.name_prefix}-rds-sg"
  description = "Restricts PostgreSQL access to the App Runner connector SG"
  vpc_id      = aws_vpc.dev.id

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

resource "aws_security_group" "alb" {
  name        = "${local.name_prefix}-alb-sg"
  description = "Security group for public backend ALB"
  vpc_id      = aws_vpc.dev.id

  tags = {
    Name = "${local.name_prefix}-alb-sg"
  }
}

resource "aws_vpc_security_group_ingress_rule" "alb_http_from_internet" {
  security_group_id = aws_security_group.alb.id
  ip_protocol       = "tcp"
  from_port         = 80
  to_port           = 80
  cidr_ipv4         = "0.0.0.0/0"

  description = "Allow public HTTP ingress for development ALB"
}

resource "aws_vpc_security_group_egress_rule" "alb_to_backend" {
  security_group_id            = aws_security_group.alb.id
  referenced_security_group_id = aws_security_group.apprunner_vpc_connector.id
  ip_protocol                  = "tcp"
  from_port                    = 8080
  to_port                      = 8080

  description = "Allow ALB egress to backend task SG on application port"
}

resource "aws_vpc_security_group_ingress_rule" "backend_from_alb" {
  security_group_id            = aws_security_group.apprunner_vpc_connector.id
  referenced_security_group_id = aws_security_group.alb.id
  ip_protocol                  = "tcp"
  from_port                    = 8080
  to_port                      = 8080

  description = "Allow backend task ingress only from ALB SG"
}

resource "aws_vpc_security_group_egress_rule" "backend_to_internet_https" {
  security_group_id = aws_security_group.apprunner_vpc_connector.id
  ip_protocol       = "tcp"
  from_port         = 443
  to_port           = 443
  cidr_ipv4         = "0.0.0.0/0"

  description = "Allow backend task outbound HTTPS for AWS service access"
}
