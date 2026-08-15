resource "aws_lb" "backend" {
  count = var.runtime_present ? 1 : 0

  name               = "${local.name_prefix}-backend-alb"
  internal           = false
  load_balancer_type = "application"
  security_groups    = [aws_security_group.alb.id]
  subnets            = [for subnet in aws_subnet.public : subnet.id]

  tags = {
    Name = "${local.name_prefix}-backend-alb"
  }
}

resource "aws_lb_target_group" "backend" {
  count = var.runtime_present ? 1 : 0

  name        = "${local.name_prefix}-backend-tg"
  port        = var.backend_container_port
  protocol    = "HTTP"
  target_type = "ip"
  vpc_id      = aws_vpc.dev.id

  health_check {
    enabled             = true
    interval            = 30
    timeout             = 5
    healthy_threshold   = 2
    unhealthy_threshold = 2
    matcher             = "200"
    path                = var.alb_health_check_path
    protocol            = "HTTP"
  }

  tags = {
    Name = "${local.name_prefix}-backend-tg"
  }
}

resource "aws_lb_listener" "backend_http" {
  count = var.runtime_present ? 1 : 0

  load_balancer_arn = aws_lb.backend[0].arn
  port              = 80
  protocol          = "HTTP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.backend[0].arn
  }
}
