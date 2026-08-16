data "aws_iam_policy_document" "github_actions_backend_deploy" {
  statement {
    sid       = "GetEcrAuthorizationToken"
    actions   = ["ecr:GetAuthorizationToken"]
    resources = ["*"]
  }

  statement {
    sid = "PushBackendImage"
    actions = [
      "ecr:BatchCheckLayerAvailability",
      "ecr:CompleteLayerUpload",
      "ecr:InitiateLayerUpload",
      "ecr:PutImage",
      "ecr:UploadLayerPart"
    ]
    resources = [aws_ecr_repository.backend.arn]
  }

  statement {
    sid       = "DescribeHomeOpsBackendService"
    actions   = ["ecs:DescribeServices"]
    resources = ["arn:aws:ecs:us-east-2:564001313291:service/homeops-dev-backend-cluster/homeops-dev-backend-service"]
  }

  statement {
    sid       = "DescribeHomeOpsBackendTaskDefinitions"
    actions   = ["ecs:DescribeTaskDefinition"]
    resources = ["arn:aws:ecs:us-east-2:564001313291:task-definition/homeops-dev-backend:*"]
  }

  statement {
    sid       = "RegisterTaskDefinitionRevision"
    actions   = ["ecs:RegisterTaskDefinition"]
    resources = ["*"]
  }

  statement {
    sid       = "DeployHomeOpsBackendService"
    actions   = ["ecs:UpdateService"]
    resources = ["arn:aws:ecs:us-east-2:564001313291:service/homeops-dev-backend-cluster/homeops-dev-backend-service"]
  }

  statement {
    sid     = "PassExistingEcsTaskRoles"
    actions = ["iam:PassRole"]
    resources = [
      aws_iam_role.ecs_task.arn,
      aws_iam_role.ecs_task_execution.arn
    ]

    condition {
      test     = "StringEquals"
      variable = "iam:PassedToService"
      values   = ["ecs-tasks.amazonaws.com"]
    }
  }
}

resource "aws_iam_role_policy" "github_actions_backend_deploy" {
  name   = "${local.name_prefix}-github-actions-backend-deploy"
  role   = aws_iam_role.github_actions.name
  policy = data.aws_iam_policy_document.github_actions_backend_deploy.json
}
