moved {
  from = aws_lb.backend
  to   = aws_lb.backend[0]
}

moved {
  from = aws_lb_target_group.backend
  to   = aws_lb_target_group.backend[0]
}

moved {
  from = aws_lb_listener.backend_http
  to   = aws_lb_listener.backend_http[0]
}

moved {
  from = aws_ecs_service.backend
  to   = aws_ecs_service.backend[0]
}

check "runtime_absent_requires_zero_desired_count" {
  assert {
    condition     = var.runtime_present || var.ecs_desired_count == 0
    error_message = "ecs_desired_count must be 0 when runtime_present is false."
  }
}