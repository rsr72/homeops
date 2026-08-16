resource "aws_ecs_cluster" "backend" {
  name = "${local.name_prefix}-backend-cluster"

  tags = {
    Name = "${local.name_prefix}-backend-cluster"
  }
}

resource "aws_cloudwatch_log_group" "backend" {
  name              = "/ecs/${local.name_prefix}-backend"
  retention_in_days = var.cloudwatch_log_retention_days

  tags = {
    Name = "${local.name_prefix}-backend-log-group"
  }
}

resource "aws_ecs_task_definition" "backend" {
  family                   = "${local.name_prefix}-backend"
  cpu                      = tostring(var.ecs_task_cpu)
  memory                   = tostring(var.ecs_task_memory)
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  execution_role_arn       = aws_iam_role.ecs_task_execution.arn
  task_role_arn            = aws_iam_role.ecs_task.arn

  container_definitions = jsonencode([
    {
      name      = "backend"
      image     = "${aws_ecr_repository.backend.repository_url}:${var.backend_image_tag}"
      essential = true

      portMappings = [
        {
          containerPort = var.backend_container_port
          hostPort      = var.backend_container_port
          protocol      = "tcp"
        }
      ]

      environment = [
        {
          name  = "SPRING_PROFILES_ACTIVE"
          value = var.spring_profile
        },
        {
          name  = "APP_DB_HOST"
          value = aws_db_instance.main.address
        },
        {
          name  = "APP_DB_PORT"
          value = tostring(aws_db_instance.main.port)
        },
        {
          name  = "APP_DB_NAME"
          value = aws_db_instance.main.db_name
        },
        {
          name  = "APP_DB_USER"
          value = aws_db_instance.main.username
        }
      ]

      secrets = [
        {
          name      = "APP_DB_PASSWORD"
          valueFrom = "${aws_db_instance.main.master_user_secret[0].secret_arn}:password::"
        }
      ]

      logConfiguration = {
        logDriver = "awslogs"
        options = {
          awslogs-group         = aws_cloudwatch_log_group.backend.name
          awslogs-region        = var.aws_region
          awslogs-stream-prefix = "backend"
        }
      }

      healthCheck = {
        command     = ["CMD-SHELL", "curl -f http://localhost:${var.backend_container_port}/actuator/health || exit 1"]
        interval    = 30
        timeout     = 5
        retries     = 3
        startPeriod = 45
      }
    }
  ])

  tags = {
    Name = "${local.name_prefix}-backend-task-def"
  }
}

resource "aws_ecs_service" "backend" {
  count = var.runtime_present ? 1 : 0

  name                              = "${local.name_prefix}-backend-service"
  cluster                           = aws_ecs_cluster.backend.id
  task_definition                   = aws_ecs_task_definition.backend.arn
  desired_count                     = var.ecs_desired_count
  launch_type                       = "FARGATE"
  health_check_grace_period_seconds = var.ecs_health_check_grace_period_seconds
  enable_execute_command            = false
  force_new_deployment              = false

  network_configuration {
    subnets          = [for subnet in aws_subnet.public : subnet.id]
    security_groups  = [aws_security_group.apprunner_vpc_connector.id]
    assign_public_ip = true
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.backend[0].arn
    container_name   = "backend"
    container_port   = var.backend_container_port
  }

  deployment_controller {
    type = "ECS"
  }

  deployment_minimum_healthy_percent = 100
  deployment_maximum_percent         = 200

  lifecycle {
    ignore_changes = [task_definition]
  }

  depends_on = [aws_lb_listener.backend_http]

  tags = {
    Name = "${local.name_prefix}-backend-service"
  }
}
