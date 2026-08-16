# The HomeOps repository was created after 2026-07-15, so GitHub issues immutable
# owner/repo-ID subject claims by default (verified via GET /repos/rsr72/homeops).
# thumbprint_list is intentionally omitted: AWS validates this provider against its
# own trusted root CA library rather than a configured certificate thumbprint.
resource "aws_iam_openid_connect_provider" "github_actions" {
  url            = "https://token.actions.githubusercontent.com"
  client_id_list = ["sts.amazonaws.com"]

  tags = {
    Name = "${local.name_prefix}-github-actions-oidc"
  }
}

data "aws_iam_policy_document" "github_actions_assume_role" {
  statement {
    sid     = "GitHubActionsAssumeRoleWithWebIdentity"
    actions = ["sts:AssumeRoleWithWebIdentity"]

    principals {
      type        = "Federated"
      identifiers = [aws_iam_openid_connect_provider.github_actions.arn]
    }

    condition {
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:aud"
      values   = ["sts.amazonaws.com"]
    }

    condition {
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:sub"
      values = [
        "repo:${var.github_actions_owner}@${var.github_actions_owner_id}/${var.github_actions_repository}@${var.github_actions_repository_id}:ref:refs/heads/${var.github_actions_trusted_branch}"
      ]
    }
  }
}

resource "aws_iam_role" "github_actions" {
  name               = "${local.name_prefix}-github-actions"
  description        = "Assumed by GitHub Actions in rsr72/homeops via OIDC; no permission policy attached yet"
  assume_role_policy = data.aws_iam_policy_document.github_actions_assume_role.json

  tags = {
    Name = "${local.name_prefix}-github-actions"
  }
}
