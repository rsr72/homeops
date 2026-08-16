# ADR-0003: GitHub Actions AWS Authentication via OIDC

- Status: Accepted
- Date: 2026-08-16

## Context

Future HomeOps CI/CD stages (image publishing, ECS deployment, Terraform plan/apply automation) will need GitHub Actions workflows to call AWS APIs. The naive approach — storing long-lived `AWS_ACCESS_KEY_ID`/`AWS_SECRET_ACCESS_KEY` values as GitHub secrets — creates standing credentials that can leak, are hard to rotate, and are not scoped to individual workflow runs.

This issue establishes the identity foundation only: prove that a GitHub Actions workflow in `rsr72/homeops` can authenticate to AWS with no stored long-lived credentials. No deployment permissions are granted yet.

## Mental Model

```text
GitHub Actions
      |
      | short-lived OIDC token
      v
AWS IAM trust policy
      |
      | AssumeRoleWithWebIdentity
      v
HomeOps deployment role
      |
      v
temporary AWS credentials
```

- **Authentication** answers "who is this workflow run?" — GitHub's OIDC provider issues a short-lived signed JWT identifying the calling repository, branch, and workflow.
- **Authorization** answers "what is this identity allowed to do?" — the IAM role's *permission policy* (not its trust policy) controls that, and this role currently has none.
- OIDC eliminates long-lived GitHub-held AWS keys: no secret ever sits in GitHub. Each workflow run requests a fresh token from `token.actions.githubusercontent.com`, exchanges it for temporary AWS credentials via AWS STS `AssumeRoleWithWebIdentity`, and those credentials expire with the job.

## Decision

Provision, via Terraform:

- An `aws_iam_openid_connect_provider` for `https://token.actions.githubusercontent.com` (audience `sts.amazonaws.com`). No such provider previously existed in this AWS account/state.
- An IAM role (`${name_prefix}-github-actions`) with a **trust policy only** — no permission policy — since this issue only needs to prove `sts:GetCallerIdentity`, which requires no IAM permissions at all.

### Trust policy restrictions

- `Principal`: the OIDC provider ARN only (`Federated`).
- `Condition` uses `StringEquals` (exact match) on both claims, not `StringLike`, because both values are known and fixed:
  - `token.actions.githubusercontent.com:aud` = `sts.amazonaws.com`
  - `token.actions.githubusercontent.com:sub` = `repo:rsr72@6588282/homeops@1328002338:ref:refs/heads/main`

The `sub` value uses GitHub's **immutable subject claim format** (`OWNER@OWNER-ID/REPO@REPO-ID`), not the legacy `repo:rsr72/homeops:ref:refs/heads/main` format. This was verified, not assumed: the repository was created 2026-08-08 via the GitHub REST API (`GET /repos/rsr72/homeops` → `created_at`), which is after GitHub's 2026-07-15 cutoff for automatic immutable-claim rollout, so new repositories (including this one) receive `owner_id`/`repo_id` in `sub` by default. Owner ID (`6588282`) and repository ID (`1328002338`) were read from the same API response. Using the legacy format here would have produced a trust policy that could never match this repository's actual tokens.

Restricting `sub` to `ref:refs/heads/main` (rather than a wildcard or all-branches match) means only workflow runs triggered from `main` can assume the role — no other branch, fork, or pull request context can.

## GitHub Actions workflow

New `.github/workflows/aws-oidc-auth.yml`:

- `permissions: { id-token: write, contents: read }` at the workflow level — the minimum needed to request an OIDC token and check out the repo.
- Trigger: `workflow_dispatch` only. No automatic push-triggered AWS calls are introduced by this issue.
- Single job using `aws-actions/configure-aws-credentials@v6` (current stable major) with `role-to-assume` set to the role ARN (read from a repository variable, not a secret, since a role ARN is not sensitive) and `aws-region`.
- Final step runs `aws sts get-caller-identity` to prove the assumed identity, with no other AWS calls.

## Alternatives Considered

- **Legacy (non-immutable) `sub` format**: rejected once repository creation date confirmed GitHub issues the immutable format for this repository by default; using the legacy format would never match and the role could never be assumed.
- **`StringLike` with wildcard `sub`** (e.g., matching any branch or PR): rejected as broader than necessary; `StringEquals` on the exact main-branch subject is tighter and sufficient for this issue's scope.
- **GitHub Environment-scoped trust** (`environment:<name>` in `sub`): would be marginally tighter and pairs well with GitHub's environment protection rules, but requires manual, non-Terraform-managed repository configuration (creating the environment in GitHub settings). Deferred as a future hardening option once real deployment permissions are added to this role.
- **Attaching a permissions policy now** (e.g., read-only AWS access): rejected — `sts:GetCallerIdentity` needs zero IAM permissions, so attaching anything now would grant more access than this issue requires.

## Consequences

- Future deployment workflows (ECR push, ECS deploy, Terraform apply) will extend this same role's permission policy incrementally, rather than introducing a new identity mechanism.
- No AWS access is granted by this change beyond identity verification; a follow-up issue must explicitly add scoped permissions before any real deployment automation can use this role.
- The OIDC provider is a singleton per AWS account; later stories must reuse `aws_iam_openid_connect_provider.github_actions` rather than creating a second provider for the same issuer URL.

## Security/Privacy

- No `AWS_ACCESS_KEY_ID`/`AWS_SECRET_ACCESS_KEY` are stored anywhere; only a role ARN (not sensitive) is referenced from the workflow.
- `aud` and `sub` are both pinned with exact-match conditions; no wildcard repository or org trust is permitted.
- `aws sts get-caller-identity` output includes the AWS account ID in workflow logs; this is expected and unavoidable for an identity-proof step, not a credential or secret leak.

## Related Issues/Docs

- [ADR-0002](0002-ecs-fargate-runtime-architecture.md) (backend runtime this identity will eventually deploy)
- [infra/terraform/github-oidc.tf](../../infra/terraform/github-oidc.tf)
- [.github/workflows/aws-oidc-auth.yml](../../.github/workflows/aws-oidc-auth.yml)
