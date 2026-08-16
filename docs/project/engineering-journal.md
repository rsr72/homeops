# HomeOps AI Engineering Journal

## Purpose

This journal records significant engineering decisions, SDLC practices, lessons learned, and concrete examples from building HomeOps AI.

It is intended to support:

- engineering retrospectives
- architecture and implementation analysis
- interview preparation
- resume and portfolio development
- documentation of lessons learned while using modern cloud and AI-assisted engineering practices

Entries should focus on what was done, why it was done, what was learned, and what engineering skill or principle the experience demonstrates.

## AI-Assisted Engineering Operating Model

HomeOps uses a responsibility model that mirrors enterprise engineering practice while making the AI roles explicit.

- ChatGPT is the architecture, reasoning, review, governance, security/cost analysis, and decision-support layer.
- Copilot Agent is the codebase-aware implementation agent.
- The human engineer owns scope, approvals, validation, Git, credentials, consequential actions, and release.

"AI-assisted software engineering with human-controlled architecture, governance, validation, and delivery."

Enterprise responsibility model:

| Area | Primary owner | What it covers |
| --- | --- | --- |
| Engineer | Human engineer | Owns change and final decisions |
| Reasoning / review | ChatGPT or approved enterprise AI assistant | Architecture, alternatives, security, cost, review |
| Implementation | Copilot Agent or repo-connected coding agent | Codebase-aware implementation and tests |
| Work management | GitHub Issues, Jira, GitHub, or Azure DevOps | Requirements and audit trail |
| Architecture | ADRs and design docs | Decision rationale |
| Validation | Maven, Vitest, Playwright, Terraform, CI quality gates | Verifies implementation |
| Approval | Human + PR, peer, owner, security | Human accountability |
| Deployment | Terraform and AWS, controlled CD | Consequential infrastructure changes |

The working sequence is intentionally ordered:

1. ChatGPT architecture and review.
2. Copilot plan generation.
3. Human review of scope, tradeoffs, and risks.
4. Copilot Agent implementation.
5. Terraform and test execution.
6. ChatGPT review of the evidence and outcome.
7. Human approval.
8. Apply, commit, and pull request.

Traditional Cloud Foundry:

`FastAPI → Python → Cloud Foundry → service binding → PostgreSQL`

HomeOps modernization:

`Spring Boot → Java → Docker → ECR → ECS/Fargate → RDS PostgreSQL`

AI-assisted engineering across either model:

`reasoning/review AI → human engineering decision → repo-connected implementation agent → Git/CI validation → runtime platform`

This is the same basic responsibility split a team would expect in enterprise Python/FastAPI/Cloud Foundry work: the AI can accelerate analysis and implementation, but the human still owns authorization, operational risk, environment access, release judgment, and final accountability. ChatGPT functions like an enterprise architecture and governance reviewer, Copilot Agent like a fast implementation pair programmer with repository context, and the human engineer like the release manager and system owner.

The model is useful when documenting cloud and platform engineering because it preserves decision provenance, makes validation explicit, and keeps security and cost decisions tied to named human approval rather than implied AI authority.

Enterprise use requires employer-approved AI tooling, and proprietary, restricted, customer, credential, or other protected data must not be placed into unapproved consumer AI services.

---

## 2026-08-15 — Issue #66 ECS/Fargate Backend Deployment Completion

### Context

Issue #66 completed the first working backend deployment on the approved ECS/Fargate runtime path from ADR-0002. The goal was not just to start containers in AWS, but to prove the full deployment chain from immutable image artifact to live application traffic.

The early ECS slice exposed several real integration failures that were not visible from local compilation or Terraform resource creation alone:

- the application initially failed at startup because the backend wiring was not actually connected to the AWS database runtime,
- the Testcontainers-based integration path had to be modernized so the repository wiring test became a real integration gate instead of a skipped safety net,
- the locally built Docker artifact had to match the AWS runtime architecture contract (`linux/amd64` rather than the local Apple Silicon default),
- and Terraform security-group ownership had to be corrected so standalone VPC security-group rule resources were the exclusive rule owners.

### What Was Completed

The backend was deployed as an ECS/Fargate service behind an ALB using the immutable ECR artifact published in the prior slice.

The deployment work validated the full runtime chain:

```text
Terraform
	↓
AWS resources
	↓
ECS task definition and service
	↓
running container
	↓
secret injection
	↓
Spring Boot startup
	↓
RDS PostgreSQL connection
	↓
Flyway migrations
	↓
Hibernate schema validation
	↓
Actuator health
	↓
ALB target health
	↓
client request
```

Key implementation and validation lessons:

- Terraform apply success did not prove the application worked.
- `BUILD SUCCESS` with a skipped critical integration test was not sufficient evidence of runtime health.
- Compile-time and dependency-resolution success did not prove Docker or ECS runtime compatibility.
- Post-apply Terraform reconciliation mattered because the AWS provider state still had to settle cleanly after SG ownership changes.
- The ALB had to wait for a genuinely healthy application target; startup timing was part of the deployment contract, not a cosmetic detail.
- Immutable artifact traceability mattered because the deployed container could be tied back to a specific Git SHA and image digest rather than a mutable tag.

### Resolution Summary

- Spring backend wiring was corrected so the AWS runtime used the real database-backed configuration path.
- Testcontainers was modernized to the 2.0.5 line and used as a hard repository-wiring integration gate.
- The CI path was separated so the integration test could not silently disappear behind unit-only success.
- The Docker artifact was aligned with AWS runtime expectations so the container image matched ECS/Fargate architecture requirements.
- Terraform security-group rule ownership was cleaned up so standalone `aws_vpc_security_group_ingress_rule` and `aws_vpc_security_group_egress_rule` resources owned the actual rules.
- The service eventually reached healthy state behind the ALB and was validated end to end.

### Engineering Lesson

This issue reinforced the difference between infrastructure provisioned successfully and software operating successfully. Real deployment confidence comes from a validated chain that reaches the client, not from isolated success in Terraform, Maven, or a Docker build.

The work also reinforced why HomeOps keeps immutable artifacts, explicit runtime validation, and post-apply reconciliation in the process. Those controls make it possible to detect architecture mismatches, startup failures, and infrastructure ownership drift before they become invisible production assumptions.

### Skills Demonstrated

- ECS/Fargate deployment validation
- Spring Boot runtime wiring against AWS RDS
- Testcontainers integration-gate modernization
- Docker architecture compatibility management
- Terraform security-group rule ownership design
- ALB health and startup timing analysis
- immutable artifact traceability
- end-to-end deployment reasoning

### Historical Note

ADR-0002 superseded the runtime direction from ADR-0001, but the earlier App Runner-era entries remain part of the project history. This retrospective records the completed ECS/Fargate implementation without rewriting that history.

## 2026-08-15 — Issue #69 Development Lifecycle Command Implementation (Pre-Issue-#70 Behavior)

### Context

The ECS/Fargate deployment created a real development environment with meaningful idle cost. The prior lifecycle notes identified the need to pause ECS and RDS, but manual AWS CLI operations could leave Terraform desired state out of sync or hide an incomplete startup behind an apparently successful command.

### What Was Implemented

Issue #69 adds a local operator command with `status`, `awake`, `sleep`, and `deep-sleep` actions. The command keeps Terraform responsible for the ECS service by changing the ignored local Terraform input for `ecs_desired_count` and applying only an inspected plan that contains the expected desired-count update.

Before Issue #70, the operational states were:

| State | ECS | RDS | ALB | Approximate cost behavior |
| --- | --- | --- | --- | --- |
| Awake | desired/running 1 | available | retained and healthy | Existing active-development baseline |
| Sleep | desired/running 0 | available | retained | Removes Fargate compute while retaining RDS and ALB |
| Deep Sleep | desired/running 0 | stopped | retained | Removes Fargate and RDS instance compute; storage and ALB costs remain |

`awake` follows the required dependency order: RDS start, RDS availability wait, runtime presence check, Terraform-managed ECS scale-up, target health wait, then CloudFront API verification. `deep-sleep` first proves ECS has no desired, running, or pending tasks before it stops RDS. The command reports inconsistent and transitional states explicitly and fails when the API is not healthy.

### Ownership Boundary and Deferred Work

No `ignore_changes` rule was added. No Terraform-managed ALB, target group, listener, ECS service, or CloudFront dependency is deleted through the AWS CLI. If the ALB is missing, the command reports a Terraform reconciliation requirement and refuses to proceed until a reviewed Terraform plan and apply restore the managed runtime path.

The original Deep Sleep ambition to eliminate ALB cost is deferred. Removing the ALB safely requires a declarative Terraform runtime-layer design that coordinates ECS service lifecycle, ALB and target-group resources, and CloudFront `/api/*` origin behavior. That is a separate follow-up infrastructure issue, not an out-of-band lifecycle operation.

### CloudFront Default Certificate Reconciliation

The CloudFront default `*.cloudfront.net` certificate fixes the viewer TLS policy at `TLSv1`, regardless of an explicit Terraform `minimum_protocol_version`. Terraform therefore omits that unsupported setting while the distribution uses `cloudfront_default_certificate = true`. This aligns Terraform desired state with AWS behavior and does not change the currently deployed TLS behavior.

The repeated S3 bucket-policy update was not actual policy drift. It cascaded from Terraform deferring the policy document because it depends on the CloudFront distribution ARN while the unresolved CloudFront update was planned. The live OAC policy already restricted reads to the intended distribution and account. A future custom domain with an ACM certificate in `us-east-1` can use an explicit viewer policy such as `TLSv1.2_2021` or newer.

### CI/CD Promotion Learning Decision

HomeOps intentionally operates one persistent AWS runtime environment, DEV, while the MVP is validated. TEST may initially be automated or ephemeral, and STAGE and PROD are initially CI/CD promotion gates rather than permanently hosted copies of DEV.

The delivery model is still designed as an enterprise promotion path: build one immutable image, run unit tests, deploy that exact image to DEV, run integration and E2E tests, then use stricter test, Terraform-plan, dependency/image-scan, and approval gates before later production deployment validation. No environment rebuilds its own Docker image; environment-specific configuration and secrets remain outside the artifact.

This cost-conscious model allows future isolated environments to replace the simulated gates without redesigning delivery: DEV -> TEST -> STAGE -> PROD. True Stage and Prod environments are required before real users, payments, or production data are introduced.

### Validation Status

Local mocked lifecycle tests cover ordering, idempotency, guarded Terraform-plan rejection, desired-count restoration on failure, missing-ALB reporting, `stopping -> stopped` RDS polling, the absence of an invalid RDS stopped waiter, and bounded timeout reporting with the last observed status.

Live validation completed on 2026-08-15 without deleting Terraform-managed runtime resources:

1. **Awake -> Sleep:** Terraform changed only ECS desired count from one to zero. RDS remained `available`, the ALB remained `active`, and the final lifecycle state was `SLEEP`.
2. **Sleep -> Deep Sleep:** ECS desired and running counts reached zero before the RDS stop action. Validation discovered that the AWS CLI has no `aws rds wait db-instance-stopped` waiter. The command was corrected to use bounded `DBInstanceStatus` polling, the mock regression tests passed, and the final lifecycle state was `DEEP_SLEEP` with RDS `stopped`.
3. **Deep Sleep -> Awake:** RDS started and reached `available` before Terraform changed only ECS desired count from zero to one. ECS became running, the ALB target became healthy, and the final state was `AWAKE` with RDS `available`, ECS desired/running/pending counts of `1/1/0`, ALB `active`, and one healthy target.

The complete cold Deep Sleep -> Awake recovery took approximately nine minutes. Issue #70 tracks the next FinOps step: a Terraform-safe declarative design for optional ALB/runtime removal and later restoration, including CloudFront `/api/*` reconciliation.

## 2026-08-15 — Issue #70 Declarative Runtime Layer Implementation

### Context

Issue #69 reduced compute cost by scaling ECS to zero and stopping RDS, but retained the ALB because deleting Terraform-managed runtime resources imperatively would break Terraform ownership and CloudFront API routing. ALBs cannot be stopped; eliminating their idle cost requires an explicit declarative destroy/recreate boundary.

### Design

Issue #70 introduces `runtime_present` as a Terraform-controlled lifecycle input. When false, Terraform removes only the ephemeral runtime layer: ECS service, ALB listener, target group, ALB, and CloudFront `/api/*` origin behavior. When true, Terraform recreates that same layer. Explicit Terraform moved blocks preserve existing resource addresses when the newly conditional resources are introduced.

Current Issue #70 Deep Sleep removes the ECS service, ALB listener, target group, ALB, and CloudFront API routing while RDS remains available; only then does it stop RDS.

RDS storage/backups, ECR, S3 frontend, CloudFront distribution, Secrets Manager, SSM, networking, security groups, IAM roles, ECS cluster/task definition, and CloudWatch log group remain durable. This keeps state, data, identity, and network contracts stable while removing the principal idle runtime costs.

### Dependency and Recovery Model

Deep Sleep now follows: ECS desired count zero -> verify `0/0/0` -> Terraform runtime absent -> verify runtime absence while RDS remains available -> RDS stop -> bounded status polling -> Deep Sleep. Awake follows: RDS available -> Terraform runtime present with ECS desired count one -> ECS stable -> target healthy -> CloudFront API verification -> Awake.

The lifecycle plan guard uses exact resource/action allowlists for normal Sleep, runtime removal, and runtime recreation. Unexpected infrastructure changes abort the operation and restore local lifecycle inputs. A partial Terraform failure leaves a declaratively recoverable state; rerunning the same requested lifecycle action reviews a new plan rather than bypassing Terraform.

### Validation Status

Mocked local tests cover runtime present/absent transitions, strict destroy/create plan allowlists, Deep Sleep and Awake ordering, idempotent Deep Sleep and Awake, unexpected plan rejection, apply-failure input restoration and recovery, and API verification only after runtime recreation, ECS readiness, and target health. No live Issue #70 runtime lifecycle transition has been run yet.

## 2026-08-15 — Historical FinOps / Development Environment Lifecycle for ECS/Fargate

### Context

Issue #66 was successfully completed and merged after end-to-end AWS validation. That made the development environment a real operational system rather than a one-way deployment target, so the environment needed explicit cost-control states instead of remaining continuously running.

### Operating States

At the time, HomeOps treated the development environment as having three operational states:

| State | Definition | Approximate cost | When to use |
| --- | --- | --- | --- |
| Running | ECS desired count 1, RDS running, ALB provisioned | about $50-60/month | Active development, testing, or validation |
| Sleeping | ECS desired count 0, RDS stopped, ALB retained | about $22-30/month | Hours, overnight, or a single day of inactivity |
| Deep Sleep | RDS stopped and ECS/ALB runtime layer destroyed through Terraform while retaining inexpensive foundation resources | about $3-5/month | Multi-day inactivity or planned idle periods |

Issue #69 supersedes this operational model. The current lifecycle command retains the ALB in Deep Sleep and defers ALB-cost elimination until a follow-up issue provides a declarative Terraform runtime-layer design that also reconciles CloudFront `/api/*` routing.

### FinOps Lesson

Cloud cost management is part of engineering. A development environment should have an explicit lifecycle so the team can reduce waste without losing the ability to restore a known-good runtime path.

Terraform remains the durable desired-state definition, even when temporary operational sleep intentionally differs from that state. During sleep, the live environment may drift from Terraform by design, but the wake procedure must intentionally restore the runtime and then reconcile Terraform and live state.

### Tested ECS Sleep Procedure

The ECS sleep procedure was tested against the live AWS environment using authoritative resource discovery before mutation:

1. Discover the live ECS cluster with `aws ecs list-clusters`.
2. Discover the live ECS service with `aws ecs list-services`.
3. Set ECS desired count to 0.
4. Verify `Desired=0`, `Running=0`, and `Pending=0`.

The discovered development runtime identifiers used for the sleep procedure were:

- cluster `homeops-dev-backend-cluster`
- service `homeops-dev-backend-service`

An initial `ClusterNotFoundException` occurred because an assumed cluster name was used. The lesson is to query AWS and Terraform for authoritative physical resource identifiers before making operational changes instead of assuming Terraform logical names always match deployed AWS names.

### RDS Stop and Verification

The development PostgreSQL instance `homeops-dev-postgres` was stopped and verified to reach the `stopped` state.

Sleeping RDS still incurs storage and backup-related charges, and AWS may automatically restart a stopped RDS instance after the permitted stop period. The stopped database therefore reduces cost but does not eliminate it.

### Cost Behavior While Sleeping

- The ALB continues to incur charges while retained.
- ECS desired count 0 eliminates the running Fargate task compute and task public IPv4 usage while asleep.
- Manually changing ECS desired count from Terraform's declared 1 to AWS runtime 0 creates intentional temporary Terraform drift.

### Wake and Reconciliation Procedure

The wake procedure must deliberately restore RDS and ECS before normal development or Terraform work resumes.

Concise wake runbook:

1. Restore the RDS instance to `available`.
2. Restore the ECS service desired count to 1.
3. Verify the task, container, and ALB target become healthy again.
4. Reconcile Terraform and live state so the durable desired-state definition again matches the active runtime.

Concise sleep runbook:

1. Confirm the environment is not needed for the current work window.
2. Set ECS desired count to 0 and verify `Desired=0`, `Running=0`, `Pending=0`.
3. Stop the RDS instance and verify `stopped`.
4. Retain the ALB if the environment should wake quickly, or move to Deep Sleep for longer idle periods.
5. Record the active state so the next wake step is deliberate.

### Engineering Lesson

The operational lifecycle is part of the system design, not an afterthought. Terraform defines the durable desired state, but the team can temporarily sleep the environment to control cost as long as the wake procedure restores the runtime deliberately and reconciles the state afterward.

### Skills Demonstrated

- FinOps-aware development operations
- ECS service lifecycle management
- RDS stop and restart awareness
- authoritative AWS resource discovery
- temporary Terraform drift management
- durable desired-state reasoning
- reusable operational runbook design

## 2026-08-15 — Issue #64 Runtime Architecture Decision Update (Documentation Only)

### Context

HomeOps previously selected App Runner in ADR-0001 for the initial backend runtime direction. After that decision, App Runner availability and service-direction constraints changed for this AWS account.

Issue #64 therefore performed a runtime re-evaluation and cost-optimization review before implementation.

### What Was Decided

HomeOps accepted ADR-0002 and selected conventional ECS on Fargate as the backend runtime architecture.

Why conventional ECS/Fargate was selected:

- stronger transferable AWS modernization learning value,
- explicit experience with ECS clusters, services, task definitions, IAM roles, ALB integration, and deployment lifecycle,
- clearer portfolio signal than higher-abstraction alternatives.

Development network topology selected for first runtime implementation:

- public ALB for HTTPS entry,
- Fargate tasks in public subnets with `assign_public_ip = true`,
- no direct Internet inbound rule to task security group,
- inbound application traffic restricted to ALB security group source,
- private RDS retained, with PostgreSQL 5432 SG-to-SG only from ECS task SG,
- no NAT Gateway initially,
- no initial interface VPC endpoint fleet.

Why this development topology was selected:

- reduces fixed recurring cost,
- reduces Terraform complexity in first ECS runtime slice,
- preserves a clean future hardening path to private-subnet tasks with controlled egress.

### Scope Boundary

This entry documents an architecture decision update only.

- No Terraform runtime resources were applied.
- No ECS service, ALB, public subnets, or Internet Gateway were created in this step.
- No backend code, Dockerfile, or CI deployment automation changes were made in this step.

### Security Caveat

Any Internet-accessible backend introduced under this development topology remains development-only until production authentication and authorization controls are implemented.

## 2026-08-16 — GitHub Actions AWS Authentication via OIDC (ADR-0003)

### Context

Future CI/CD stages need GitHub Actions to call AWS APIs (image publishing, ECS deployment, Terraform automation). Storing long-lived `AWS_ACCESS_KEY_ID`/`AWS_SECRET_ACCESS_KEY` values as GitHub secrets was rejected in favor of OIDC federation, which issues short-lived, per-run credentials with no standing secrets.

### What Was Done

- Verified via `terraform.tfstate` that no `aws_iam_openid_connect_provider` for `token.actions.githubusercontent.com` already existed, avoiding a duplicate provider.
- Verified GitHub's OIDC `sub` claim format for this specific repository rather than assuming the legacy format: `GET /repos/rsr72/homeops` showed `created_at: 2026-08-08`, which is after GitHub's 2026-07-15 immutable-subject-claim rollout cutoff. This means the repository's OIDC tokens use `repo:OWNER@OWNER-ID/REPO@REPO-ID:ref:refs/heads/BRANCH`, not the legacy `repo:OWNER/REPO:ref:refs/heads/BRANCH` format. Assuming the legacy format would have produced a trust policy that could never actually match.
- Added Terraform-managed `aws_iam_openid_connect_provider.github_actions` and an IAM role (`github-oidc.tf`) whose trust policy uses `StringEquals` (not `StringLike`) on both the `aud` and `sub` claims, pinned to the exact immutable subject for `main`.
- The role has **no permission policy** — `sts:GetCallerIdentity` requires zero IAM permissions, so the role starts at true least privilege.
- Added `.github/workflows/aws-oidc-auth.yml`: `workflow_dispatch`-only, minimal `permissions: { id-token: write, contents: read }`, using `aws-actions/configure-aws-credentials@v6` (current stable major) to assume the role and run `aws sts get-caller-identity` as proof.
- Documented the decision, trust-policy rationale, and OIDC mental model in [ADR-0003](../adr/0003-github-actions-oidc-authentication.md).

### What Was Learned

- Never assume a cloud IdP subject-claim format; verify it against the actual repository's metadata. GitHub's 2026-07-15 immutable-subject rollout means identical-looking trust policies can be silently wrong depending on a repository's creation date.
- `StringEquals` is the correct condition operator when every claim value is a fixed, known string; `StringLike` should be reserved for wildcard matching, which was explicitly not needed here.
- The tightest practical trust scope for this stage was branch-level (`ref:refs/heads/main`), not environment-level, because environment-scoped trust requires a manually configured GitHub Environment outside Terraform's authority — deferred as a documented future hardening step.

### Scope Boundary

This entry establishes the OIDC identity foundation only.

- No permission policy was attached to the IAM role.
- No ECR push, ECS deployment, S3/CloudFront frontend deployment, or Terraform apply from GitHub Actions was implemented.
- `terraform apply` was not run as part of this change; the plan is reviewed for an additive-only resource delta and held for explicit human approval before any AWS resources are created.

### Skills Demonstrated

- architecture supersession governance through ADRs
- cloud cost and security tradeoff analysis
- ECS platform decision framing for real-world and learning objectives
- scope-controlled documentation updates before infrastructure implementation

---

## 2026-08-10 — Issue #63 Backend ECR Repository and Manual Image Publish Slice

### Context

HomeOps added the first container registry runtime surface so the existing Spring Boot backend image can move from local/CI build validation into a controlled AWS registry flow, while keeping deployment runtime work (App Runner) out of scope.

### What Was Delivered

- Added Terraform resources for one private Amazon ECR backend repository.
- Preserved naming and tagging conventions from the existing Terraform foundation.
- Enabled immutable image tags for traceable, non-overwritable image references.
- Enabled ECR scan-on-push for baseline vulnerability visibility.
- Kept encryption at rest with AWS-managed ECR encryption (`AES256`).
- Added lifecycle policy rules to expire untagged images after 7 days and retain the most recent 20 `sha-` tagged backend images.
- Added Terraform outputs needed by downstream runtime stories: ECR repository name, URL, ARN, and registry ID.
- Added manual publish runbook steps: ECR auth via AWS CLI, `linux/amd64` backend image build, immutable Git SHA tag push, ECR digest verification, and post-publish Terraform no-drift check.

### Actual Execution Results

- Terraform apply completed successfully with `2 added, 0 changed, 0 destroyed`.
- Provisioned ECR repository:
	- name: `homeops-dev-backend`
	- URI: `564001313291.dkr.ecr.us-east-2.amazonaws.com/homeops-dev-backend`
	- tag mutability: `IMMUTABLE`
	- scan on push: enabled
	- encryption at rest: `AES256` (AWS-managed)
- Lifecycle policy confirmed in AWS:
	- untagged images expire after 7 days
	- retain the most recent 20 images tagged with the `sha-` prefix
- Published backend image tag: `sha-a9592b5a6702`
- Published image digest: `sha256:9e6a102239c46bfe308769dde8bc7eb7621b5d6d1730aa02e0c7ba8b387b8237`
- Image pushed timestamp: `2026-08-10T19:08:08.018000-05:00`
- Image size reported by ECR: `144,936,220` bytes
- ECR verification confirmed the pushed image exists.
- Image scan verification lookup returned `ScanNotFound` for this pushed artifact at verification time.
- This should not be interpreted as a clean vulnerability scan result.
- Post-apply reconciliation check completed with detailed exit code `0` and no drift.
- No App Runner, ECS, EKS, S3 frontend hosting, CloudFront, NAT Gateway, or deployment/CD infrastructure was introduced in this story.
- GitHub Actions image publishing remains deferred.

### ECR and Artifact Flow

Amazon Elastic Container Registry (ECR) is AWS's container-image registry. It stores Docker/OCI images but does not execute them.

```text
Spring Boot source
	↓
Maven/JAR
	↓
Docker build on Mac
	↓
linux/amd64 Docker image
	↓
ECR
	↓
future App Runner
```

Current state clarification:

- The backend image now stored in ECR was built locally on an Apple Silicon Mac.
- The image is not currently running in AWS.
- Building with `--platform linux/amd64` intentionally produced an image for the planned AWS runtime instead of defaulting to local ARM architecture.

### Runtime Building Blocks

- Dockerfile: recipe used to construct the backend image.
- Docker image: packaged application/runtime artifact.
- ECR: repository that stores the image artifact.
- Container: running instance of an image.
- App Runner: AWS service that will run the ECR image in the next story.

### Immutable Git SHA Tagging

```text
Git commit
    ↓
sha-a9592b5a6702
    ↓
exact Docker image
    ↓
future deployment
```

- A tag is a human-readable reference to an image.
- A SHA256 image digest identifies the actual image content and provides stronger artifact identity.

### ECR Authentication Path

```text
User
  ↓
AWS IAM Identity Center / SSO
  ↓
temporary AWS CLI credentials
  ↓
ECR authorization token
  ↓
Docker push
```

No long-lived AWS access keys were created.

### Lifecycle and Cost Control

- Untagged images expire after 7 days.
- Only the latest 20 `sha-` images are retained.
- This limits unnecessary registry-storage accumulation.

### Scanning Note

- Scan-on-push is enabled on the repository.
- Verification-time scan lookup returned `ScanNotFound` for the pushed artifact.
- The result means no findings were available at verification time and should not be reported as a clean scan.

### Terraform Lifecycle Used in This Story

```text
Terraform configuration
	↓
plan
	↓
apply
	↓
AWS ECR created
	↓
verify AWS
	↓
terraform plan -detailed-exitcode
	↓
exit code 0 / zero drift
```

### Current HomeOps AWS Checkpoint

```text
BUILT/PROVISIONED

Terraform
   ├── VPC
   ├── private subnets
   ├── security groups
   ├── RDS PostgreSQL
   ├── Secrets Manager
   ├── SSM Parameter Store
   └── ECR
	    └── Spring Boot Docker image

NEXT — Issue #64

ECR
 ↓
App Runner
 ↓
VPC Connector
 ↓
RDS PostgreSQL

LATER

React build
 ↓
S3
 ↓
CloudFront
 ↓
/api/*
 ↓
App Runner
```

Issue #64 is the milestone where the backend image already stored in ECR becomes a running Spring Boot container in AWS.

### Key Concepts Captured During the Work

- What ECR is:
	- Amazon ECR is a private container image registry. It stores versioned image artifacts, metadata, and policies.
- Docker image vs running container:
	- An image is an immutable package template (filesystem + metadata).
	- A container is a running process instance created from that image.
- Why ECR stores but does not run:
	- ECR is a registry service only. It has no runtime scheduler. Compute services such as App Runner or ECS pull and run images.
- Why App Runner is next:
	- This story created the trusted image source. The next runtime story can consume immutable SHA-tagged images from ECR without changing image build contracts.
- Immutable Git SHA tagging:
	- `sha-<12-char-git-sha>` gives traceability from deployed artifact back to exact source commit and avoids mutable release references.
- Image digest vs image tag:
	- Tag: human-friendly pointer (for example `sha-a9592b5a6702`).
	- Digest: cryptographic content address (for example `sha256:...`) that uniquely identifies image content.
- ECR authentication model:
	- Docker authenticated with short-lived AWS CLI login tokens (`aws ecr get-login-password`), with no long-lived access keys introduced.
- Why `linux/amd64` was explicit on Apple Silicon:
	- The local machine is ARM-based. The target runtime contract for upcoming App Runner usage is `linux/amd64`, so build platform was pinned to guarantee compatibility.
- Lifecycle policy and cost control:
	- Expiring untagged images and retaining only recent SHA-tagged images prevents unbounded registry growth and aligns with MVP cost guardrails.
- Vulnerability scanning behavior:
	- Scan-on-push is enabled at the repository.
	- The pushed artifact media type is `application/vnd.oci.image.index.v1+json`; immediate scan findings were not available in this run for that artifact type.
	- The security baseline remains improved because scanning is configured; follow-up hardening can refine artifact format and scan-gating strategy if needed.
- Terraform zero-drift verification:
	- Running `terraform plan -var-file=environments/dev.tfvars.example -detailed-exitcode` after publish returned exit code `0`, confirming no infrastructure drift.

### Scope Boundaries

- No App Runner resources were added.
- No GitHub Actions publishing automation was added.
- No ECS/EKS/ALB/CloudFront/S3 delivery resources were added.
- Existing backend Dockerfile and Java 21 runtime contract were preserved.

### Security and Cost Notes

- Repository remains private and does not introduce public registry access.
- Immutable tags improve artifact integrity and release traceability.
- Scan-on-push creates a minimal security feedback loop without changing release gates yet.
- Lifecycle policy caps image retention to prevent avoidable storage drift and aligns with MVP cost guardrails.

### Validation Boundary

Issue #63 follows a controlled checkpoint: Terraform static validation and plan review first, then `terraform apply` only after explicit approval. Manual image publish and digest verification occur after apply. Final closure requires a Terraform no-drift plan check.

### Skills Demonstrated

- incremental Terraform extension for container registry infrastructure
- secure artifact lifecycle configuration (immutability, scanning, retention)
- architecture-aware container publishing (`linux/amd64` target for AWS runtime compatibility)
- controlled apply and post-change drift verification discipline

---

## 2026-08-09 — Issue #62 Private RDS PostgreSQL Development Database

### Context

HomeOps progressed from Terraform foundation contracts to the first concrete database runtime definition for AWS development: a private RDS PostgreSQL instance aligned to ADR-0001 security and cost constraints.

### What Was Delivered

- Added Terraform configuration for one private single-AZ RDS PostgreSQL development instance.
- Kept networking and access model private-only using the existing DB subnet group and RDS security group.
- Preserved SG-to-SG PostgreSQL access from the future App Runner connector security group only.
- Configured storage and lifecycle defaults for a low-cost development profile.
- Switched DB host SSM parameter from placeholder to the provisioned RDS endpoint contract.
- Exposed RDS endpoint and identifier outputs needed by future App Runner integration work.

### Security and Credential Model

- Enabled RDS-managed master password storage in Secrets Manager.
- Avoided Terraform password generation and avoided committing credentials in source or tfvars.
- Kept the database non-public (`publicly_accessible = false`) with no CIDR-based PostgreSQL ingress.

### Cost and Lifecycle Notes

- Selected a cost-conscious baseline (`db.t4g.micro`, gp3, 20 GiB with capped autoscaling).
- Kept deletion protection disabled and `skip_final_snapshot = true` for intentional development teardown.
- Maintained backup retention and fixed maintenance/backup windows for practical recoverability.

### Validation Boundary

Issue #62 follows a strict checkpoint workflow: `terraform fmt`, `init`, `validate`, identity/region confirmation, and `terraform plan` only. No `terraform apply` is performed without explicit approval.

### Skills Demonstrated

- incremental Terraform delivery from contracts to first managed data service
- secure credential handling with managed AWS secrets
- pragmatic dev-environment lifecycle and cost tradeoff management

---

## 2026-08-09 — Issue #61 Terraform AWS Development Foundation

### Context

HomeOps began its first infrastructure-as-code slice after ADR-0001 was accepted. The goal was to define a minimal Terraform baseline for AWS development without provisioning runtime services or introducing avoidable recurring cost.

### What Was Delivered

- Added a single-root Terraform foundation under `infra/terraform/`.
- Defined version constraints, provider constraints, and a reusable naming and tagging contract.
- Defined development VPC and private subnet topology across at least two availability zones.
- Defined private-only networking with no public subnets, Internet Gateway, or NAT Gateway.
- Added security-group contracts for future App Runner connector and private PostgreSQL access.
- Added RDS DB subnet-group contract.
- Added a metadata-only Secrets Manager secret contract for DB credentials with no secret value.
- Added SSM Parameter Store Standard parameters for non-secret runtime configuration.
- Added outputs to unblock later RDS, ECR, and App Runner Terraform stories.
- Added lightweight CI validation (`fmt`, `init -backend=false`, `validate`) without AWS credentials, `plan`, or `apply`.

### Scope Boundaries

- No `terraform apply` was run.
- No AWS resources were provisioned in this story.
- No RDS instance, ECR repository, App Runner service, S3, or CloudFront resources were created.
- Remote state and locking were intentionally deferred.

### Engineering Lessons (Building on Issue #59)

- ADR-first architecture decisions reduce rework before IaC implementation begins.
- Foundation-first Terraform slices keep cost and risk low while creating clear contracts for later stories.
- App Runner-to-RDS security should be codified as explicit SG-to-SG rules from the start.
- Secrets handling should begin with metadata contracts and strict no-secrets-in-repo discipline.
- The most meaningful early cost control is deferring stateful runtime resources until they are truly needed.

### Skills Demonstrated

- Terraform foundation design and scope control
- cost-aware cloud architecture implementation
- network and security boundary codification
- CI validation integration for IaC without deployment access

---

## 2026-08-09 — Issue #55 Playwright Browser E2E First Slice

### Context

HomeOps added its first browser end-to-end test layer to validate the real stack path from browser through React/Vite and Spring Boot into PostgreSQL 16.14 persistence.

### What Was Delivered

Playwright was added to the existing `frontend/` workspace as a development-only browser test dependency, with a Chromium-only configuration and one focused E2E spec. The new workflow exercises:

- household creation
- vehicle creation
- maintenance event creation
- browser refresh persistence confirmation
- maintenance event edit
- maintenance event delete with confirmation
- final empty maintenance state confirmation

The E2E slice uses real frontend and backend processes with the `local-postgres` backend profile and the existing Vite `/api` proxy model. PostgreSQL remains an explicit prerequisite managed through the repository's existing Docker Compose flow.

### Test-State and Cleanup Strategy

The first slice avoids destructive database resets and instead uses isolated synthetic test data with unique household naming per run. Cleanup is handled by deleting the created Household through the real API, relying on relational cascade behavior to remove child Vehicle and Maintenance Event records.

### Engineering Lesson

The most effective first E2E layer is a single high-value journey, not a broad suite. It complements unit/service/controller and frontend component/integration tests by proving runtime wiring and persistence behavior without replacing lower-level automated safety nets.

### Skills Demonstrated

- Playwright test architecture
- browser-level workflow automation
- runtime orchestration of frontend/backend services
- deterministic test-data isolation and cleanup
- practical test-pyramid layering and scope control

---

## 2026-08-09 — Issue #59 Initial AWS Development Architecture Decision

### Context

HomeOps required a first AWS runtime and hosting architecture decision that balances security, simplicity, and low recurring cost for a small controlled MVP development environment.

### What Was Decided

ADR-0001 was accepted to establish the initial AWS development architecture:

- frontend on private S3 origin behind CloudFront
- CloudFront path routing using `/*` for frontend assets and `/api/*` for backend requests to preserve same-origin browser behavior
- backend runtime on AWS App Runner using the existing backend Docker image in Amazon ECR
- private single-AZ RDS PostgreSQL
- App Runner VPC connector for private database access
- Secrets Manager for database credentials and SSM Parameter Store for non-secret runtime configuration
- CloudWatch logs and basic alarms
- no NAT Gateway in the first slice

### Scope Boundaries

- no AWS resources were provisioned as part of this decision
- no Terraform was added in this story
- no application code, test, CI behavior, or product functionality changed

### Cost and Risk Notes

The accepted architecture targets a generally available development-environment cost band around $30-65/month, with lower totals possible when resources are paused or stopped where supported. RDS is expected to be the largest baseline cost driver.

Because authentication and authorization are not yet implemented, this environment is not treated as production-ready for broad public exposure.

### Skills Demonstrated

- architecture option analysis and tradeoff documentation
- cost-aware AWS runtime selection
- secure boundary design for private database connectivity
- ADR-based technical governance

---

## 2026-08-09 — Backend Containerization First Slice

### Context

HomeOps introduced a minimal professional containerization baseline for the Spring Boot backend to produce a reproducible deployable image while preserving current local development and test flows.

### What Was Delivered

- Added `backend/Dockerfile` with a multi-stage build.
- Built the Spring Boot executable JAR inside Docker using the Maven Wrapper.
- Used Java 21 runtime image for the final container stage.
- Added a dedicated non-root runtime user.
- Set an explicit application working directory and exposed port `8080`.
- Kept JVM and Spring runtime configuration environment-driven.
- Added Docker `HEALTHCHECK` using `/actuator/health` with an explicit HTTP probe utility available in the runtime image.
- Added `backend/.dockerignore` to reduce build context and avoid local-secret leakage.
- Added CI build-only verification so GitHub Actions confirms the backend image builds successfully without push/deploy or cloud credentials.

### Scope and Safety Boundaries

- No frontend containerization.
- No AWS resources, registry push, deployment, or Terraform.
- No schema or product behavior changes.
- Flyway remains the schema owner and Hibernate remains `ddl-auto: validate` under `local-postgres`.

### Engineering Lesson

The best first containerization slice is image reproducibility and runtime contract clarity, not orchestration expansion. By validating a standalone backend container against the existing PostgreSQL local model, HomeOps gains deployable-image confidence while avoiding premature complexity.

### Skills Demonstrated

- Spring Boot containerization design
- multi-stage Docker build optimization
- secure runtime defaults (non-root and env-driven secrets)
- CI container build verification
- controlled scope delivery aligned to MVP architecture and FinOps constraints

---

## 2026-08-09 — GitHub Actions Frontend Verification Added

### Context

The HomeOps CI workflow was extended to verify both the Spring Boot backend and the React/TypeScript frontend on pull requests and pushes to `main`.

### What Was Changed

The existing `Java CI` workflow was kept as the single CI entry point. A new independent frontend job now runs `npm ci`, `npm test`, and `npm run build` from the separate `frontend/` workspace using Node 22 and npm caching keyed to `frontend/package-lock.json`.

The backend Maven verification job remained unchanged and still runs `./mvnw verify` from `backend/` on Java 21. No PostgreSQL, Docker, AWS, deployment, or Playwright steps were added to CI.

### Engineering Lesson

For a small MVP, the smallest professional CI improvement is usually to extend the existing workflow rather than create another one. That keeps the repository easier to understand, preserves one place to look for verification status, and makes it obvious that backend and frontend are both first-class parts of the same local product slice.

### Skills Demonstrated

- GitHub Actions workflow design
- backend and frontend CI separation
- Node.js and Maven verification strategy
- repository-local cache configuration
- documentation of CI status changes

## 2026-08-09 — Issue #43 First Responsive HomeOps Web UI

### Context

Issue #43 delivered the first browser-based HomeOps workflow and was merged after end-to-end validation against PostgreSQL 16.14.

### What Was Delivered

The new frontend is a separate `frontend/` workspace built with React, TypeScript, Vite, npm, TanStack Query, React Hook Form, Vitest, React Testing Library, and MSW. The browser UI now supports URL-driven household selection, household creation from an empty database, household summary display, and household-scoped vehicle create, edit, delete, and refresh flows.

The browser talks to the Spring Boot backend through the Vite `/api` proxy, so local development did not require a backend CORS change. Generated frontend artifacts remain excluded from Git, while `package-lock.json` is committed for reproducible dependency resolution.

### Verified End-to-End Path

Browser -> React / TypeScript -> Vite development proxy -> Spring Boot REST API -> service/domain layer -> Spring Data JPA / Hibernate -> Flyway-managed PostgreSQL 16.14

### Validation Evidence

- frontend npm tests passed
- frontend production build passed
- backend `./mvnw verify` passed with PostgreSQL stopped
- PostgreSQL 16.14 local runtime was validated
- the browser created a Household from an empty database
- Household selection persisted through the URL across refresh
- the browser created, edited, and deleted a Vehicle
- the backend API confirmed Vehicle deletion
- the browser returned to the correct empty Vehicle state after refresh
- the frontend, backend, and PostgreSQL were stopped cleanly

### Engineering Lessons

This issue reinforced that the first useful frontend slice should be a browser-to-database vertical slice rather than a decorative shell. URL-driven application state makes important context reload-safe and shareable, TanStack Query keeps remote state out of ad hoc component state, React Hook Form keeps forms lightweight while backend Bean Validation stays authoritative, and MSW plus Vitest provide realistic browser-level feedback without depending on the live backend for every test.

The work also reinforced the broader SDLC pattern HomeOps should continue using: requirements or story -> plan and review -> implementation -> automated tests -> real runtime validation -> Git review -> pull request -> merge -> documentation and learning update. AI can accelerate implementation, but architecture, scope boundaries, validation, security, cost awareness, review, and acceptance remain engineering responsibilities.

### Skills Demonstrated

- browser-first product slicing
- React and TypeScript frontend engineering
- Vite-based local development and build tooling
- API proxy-based local integration
- browser workflow validation against a real database
- frontend testing with Vitest, React Testing Library, and MSW
- reusable UI state management and form handling
- documentation of a merged vertical slice

## 2026-08-08 — AI-Assisted MVP Requirements Workflow

### Context

HomeOps AI began with a documented product vision and a GitHub Projects backlog. GitHub Issue #2, **Define MVP requirements**, was selected as the first active requirements story and worked on an isolated feature branch.

### Workflow Practiced

The requirements workflow followed a modern SDLC pattern:

1. Define the product vision.
2. Convert planned work into GitHub Issues with acceptance criteria.
3. Manage work through GitHub Projects and an iteration.
4. Select Issue #2 for active work.
5. Create a dedicated feature branch.
6. Use GitHub Copilot Agent to analyze the issue and product vision and propose MVP requirements.
7. Review the AI-generated proposal as the human product owner/engineer.
8. Refine the scope and acceptance criteria before accepting repository changes.
9. Inspect Git working-tree changes before staging, committing, and opening a pull request.

### Product Decision

The initial MVP was narrowed to a vehicle-first vertical slice while preserving a generic asset domain model for future expansion.

The MVP focuses on a user being able to create a household, add a vehicle, attach documents, record maintenance, configure basic maintenance schedules, and see upcoming or overdue maintenance.

AI/OCR, mobile applications, subscription payments, VIN decoding, advanced collaboration, and other higher-complexity capabilities were deliberately deferred.

### AI Agent Governance Lesson

During the requirements workflow, GitHub Copilot Agent modified `docs/product/mvp-requirements.md` even though it had explicitly been instructed to show the revised requirements in chat first and not modify files.

The unexpected change was detected by running `git status` before staging or committing anything. Because the work was isolated on a feature branch and subject to human review, the change could be inspected safely before entering version history.

This demonstrated an important principle for AI-assisted software engineering: AI agents can accelerate analysis and implementation, but their output and actions should be treated as untrusted changes until reviewed.

Useful controls include:

- isolated feature branches
- explicit agent instructions and scope boundaries
- Git working-tree inspection
- diff review before staging and committing
- testing and automated quality gates
- pull-request review
- human approval before merge

### Interview / Portfolio Example

A concise way to describe the experience:

> I used GitHub Projects, Issues, feature branching, and GitHub Copilot Agent to develop the MVP requirements for a cloud-native SaaS project. I treated the AI agent as an engineering accelerator rather than an authority: I reviewed its initial proposal, narrowed the product scope, and established measurable success criteria. When the agent modified a repository file despite an explicit instruction not to make changes, I detected the action through Git working-tree review before anything was staged or committed. The experience reinforced the importance of feature-branch isolation, change review, automated controls, and human approval when incorporating autonomous AI agents into an SDLC.

### Skills Demonstrated

- modern software development lifecycle (SDLC)
- Agile backlog and iteration management
- requirements engineering
- product scope and MVP definition
- vertical-slice product design
- Git and GitHub workflow
- pull-request-based change management
- AI-assisted software engineering
- human-in-the-loop AI governance
- engineering risk management

---

## 2026-08-08 — Establishing a Definition of Ready

### Context

A formal Definition of Ready was established for HomeOps AI so backlog items have a consistent minimum quality bar before entering active development.

### What Was Established

A work item is expected to have clear business value, bounded scope, testable acceptance criteria, identified dependencies and assumptions, relevant security and data considerations, sufficient design or architecture context, a known test approach, documentation expectations, appropriate sizing, and no major unresolved blocker before it is considered Ready.

### Engineering Lesson

A backlog is not simply a list of things to code. Moving an item into development creates cost and risk. Establishing an entrance quality gate reduces ambiguity and rework by forcing important questions to be answered before implementation begins.

The Definition of Ready is intentionally lightweight: it provides enough governance to improve delivery quality without creating unnecessary process for a small project.

### Interview / Portfolio Example

> I established a lightweight Definition of Ready for HomeOps AI so backlog items do not enter development until business value, scope, acceptance criteria, dependencies, security and data implications, architecture considerations, testability, and sizing are sufficiently understood. This created an explicit quality gate between backlog refinement and implementation and helped reduce ambiguity and rework while keeping the SDLC practical for a small product team.

### Resume Evidence

Potential resume language after the project has enough implementation evidence to support a broader bullet:

> Applied modern SDLC and Agile engineering practices to a cloud-native SaaS product, establishing backlog quality gates, measurable acceptance criteria, feature-branch workflows, pull-request review, and human oversight of AI-assisted development.

### Skills Demonstrated

- Agile backlog refinement
- Definition of Ready
- requirements quality management
- acceptance-criteria design
- dependency and risk identification
- security-by-design awareness
- architecture readiness
- test planning
- work decomposition and sizing
- SDLC governance

---

## 2026-08-08 — Copilot Agent Learning Track

### Goal

Use the HomeOps AI project to deliberately learn professional GitHub Copilot Agent usage alongside product, cloud, and software engineering skills. The objective is not simply to use an AI coding assistant, but to learn how to incorporate an autonomous engineering agent safely and effectively into a modern SDLC.

### Working Model

The preferred workflow is:

1. Human defines the intent, business outcome, constraints, and acceptance criteria.
2. Copilot investigates the repository and proposes a plan when planning is needed.
3. Human reviews important product and architecture decisions before implementation.
4. Copilot implements an approved, bounded change.
5. Tests and quality gates validate the implementation.
6. Human reviews the actual Git diff rather than relying only on the agent's summary.
7. The change proceeds through pull-request review and merge.

The level of agent autonomy should increase gradually as repository instructions, automated tests, CI controls, and engineering confidence improve.

### Copilot Skills to Practice

During HomeOps development, deliberately practice:

- choosing appropriately between conversational/Ask, planning, and Agent workflows
- writing prompts that specify outcome, context, constraints, acceptance criteria, and stopping conditions
- providing useful repository context without overloading the prompt
- creating repository-level Copilot instructions so engineering standards do not need to be repeated manually
- controlling tool permissions and approval boundaries
- decomposing large stories into agent-sized implementation tasks
- asking the agent to inspect existing code and conventions before changing them
- using tests as executable acceptance criteria
- requiring the agent to run relevant tests and quality checks after implementation
- reviewing diffs, changed files, and command output independently of the agent's narrative summary
- using feature branches and pull requests as safety boundaries
- having the agent diagnose failures before attempting additional changes
- recognizing when a decision requires human product, security, or architecture judgment
- allowing larger multi-file implementations only after adequate automated guardrails exist

### Prompting Pattern

For implementation work, prompts should increasingly resemble engineering assignments rather than line-by-line coding instructions. A useful pattern is:

> Implement the referenced issue according to the repository instructions and acceptance criteria. Inspect the existing implementation and conventions first. Create or update appropriate tests, run the relevant quality gates, and stop before committing. Report architectural decisions, files changed, tests run, and unresolved concerns.

The human engineer remains responsible for determining whether the resulting implementation is correct, maintainable, secure, and appropriate for the product.

### Lesson Learned So Far

Copilot Agent should be treated as an actor with repository capabilities, not merely as a conversational assistant. An instruction such as "do not modify files" is useful but is not itself a sufficient safety control. Branch isolation, permissions, diff inspection, automated testing, CI, pull-request review, and human approval provide stronger controls.

### Interview / Portfolio Example

> I incorporated GitHub Copilot Agent into the SDLC for a cloud-native SaaS project using a human-in-the-loop model. I defined requirements and acceptance criteria, used the agent for bounded investigation and implementation, reviewed repository diffs and test results independently, and used feature branches, CI quality gates, and pull requests as controls. As the project matured, I increased agent autonomy only as repository instructions and automated guardrails improved.

### Skills Demonstrated

- AI-assisted software engineering
- agentic development workflows
- prompt and context engineering
- human-in-the-loop governance
- Git-based change control
- automated quality gates
- test-driven validation
- engineering judgment and review
- AI risk management

### Follow-Up

Future journal entries should capture meaningful architecture decisions, AWS implementations, Java and Kubernetes learning, CI/CD development, security controls, observability, FinOps decisions, AI integrations, production incidents, performance improvements, Copilot Agent techniques, agent failures and corrections, and lessons learned from operating HomeOps AI.

---

## 2026-08-08 — Architecture, ADRs, and AI Engineering Guardrails

### Context

As HomeOps AI moved from product requirements into architecture planning, the project benefited from a more explicit engineering operating model. The work focused on establishing reusable quality gates, repository standards, and an architecture approach that stayed intentionally simple for the MVP.

### What Was Established

A lightweight Definition of Done was introduced as an exit quality gate so work would not be considered complete without reviewable scope, evidence of testing or validation, and clear documentation expectations.

Repository standards were also formalized to support safe collaboration and review. These included a branch-based workflow, pull-request-based changes, protecting the main branch, and keeping repository changes reviewable before merge.

Repository-level Copilot instructions were added to create persistent AI engineering guardrails so future work would follow the project’s intended standards without repeatedly re-explaining the same expectations.

An ADR process was established to capture durable architecture decisions in a lightweight, reviewable format rather than leaving important choices implicit.

The initial architecture for HomeOps was framed as a deliberately simple modular single-application MVP rather than prematurely adopting microservices or other distributed complexity. This kept the design aligned with the product’s immediate value proposition while leaving room for future growth.

The work also clarified the difference between product requirements and architecture or implementation decisions. Product requirements defined what the system should accomplish; architecture decisions defined how it should be structured and operated at a higher level.

Security and trust boundaries were made explicit around the public application edge, the private database, and private document storage so the architecture reflected the product’s data sensitivity and operational expectations.

Finally, implementation-specific AWS choices such as App Runner versus ECS/Fargate were intentionally deferred until an ADR could evaluate the tradeoffs in a more structured way.

### Engineering Lesson

Small products benefit from lightweight governance that is strong enough to reduce risk but simple enough to remain practical. A Definition of Done, repository standards, Copilot guardrails, and ADRs help create reviewable workstreams without over-engineering the process. They also help prevent premature architecture decisions and encourage a deliberate, evidence-based approach to implementation choices.

### Copilot and Git Lessons

Several practical lessons were reinforced during this work:

- `Keep` accepts Agent changes into the local working tree; it does not commit or push them.
- Untracked files do not appear in normal `git diff`.
- `git diff --cached` is useful after staging new files.
- Always verify the active branch with Git rather than relying on the agent's narrative.
- A pushed branch is not the same as a branch merged into `main`.
- Later branches do not automatically contain work from an earlier unmerged branch.
- AI agents may perform actions beyond the conversational intent, so Git state and diffs are authoritative.

### Interview / Portfolio Example

> I helped establish a lightweight engineering operating model for HomeOps AI by defining a Definition of Done, repository standards, persistent Copilot instructions, and an ADR process. I also shaped the initial architecture as a deliberately simple modular MVP with clear trust boundaries between the public application edge, private database, and private document storage. When working with an AI coding agent, I verified actual Git state and diffs rather than relying on narrative summaries, which helped ensure that changes stayed within scope and remained reviewable.

### Skills Demonstrated

- engineering process design
- Definition of Done and Definition of Ready
- repository governance
- Git workflow discipline
- architecture decision documentation
- secure-by-design system thinking
- AI-assisted engineering governance
- technical communication and documentation
- change review and risk management

---

## 2026-08-08 — Backlog Hygiene, Story Readiness, and Project Governance

### Context

Before beginning implementation, the HomeOps backlog and recently merged foundation work were reviewed against actual GitHub issue state rather than assuming merged pull requests had automatically completed their corresponding work items.

### What Was Improved

Several completed foundation issues were still open because their pull requests had been merged without issue-closing references. Those issues were explicitly reconciled and closed as completed. The review also found that the ADR-template issue had been merged but did not fully satisfy its original acceptance criteria: the template was stored under `docs/architecture/` while the issue required a dedicated `docs/adr/` location and usage guidance. Rather than quietly changing the acceptance criteria after implementation, the issue was left open and updated to show the remaining gap.

The initial product backlog was then converted from broad ideas into dependency-aware implementation stories. Sprint 1 stories were written with business value, bounded scope, testable acceptance criteria, security/data notes, architecture context, testing expectations, documentation expectations, and completed Definition of Ready checks.

The first implementation sequence was established as:

1. Scaffold the Java 21 / Spring Boot backend.
2. Add GitHub Actions Java CI.
3. Add a local PostgreSQL development environment.
4. Implement the Household domain foundation.
5. Implement the Vehicle domain foundation.

Later work was explicitly sequenced behind prerequisites such as threat modeling before sensitive document/auth flows and AWS cost guardrails before recurring cloud infrastructure.

### Engineering Lessons

- A merged pull request and a completed backlog item are related but not identical states; issue and project hygiene should be reconciled deliberately.
- Acceptance criteria should not be rewritten after implementation merely to make completed work appear compliant. Gaps should remain visible and be finished or consciously re-scoped.
- A professional backlog expresses dependencies and risk sequencing, not just priority order.
- Definition of Ready checks are most useful when they are applied to real implementation stories before coding begins.
- Security and FinOps work can be sequencing constraints: threat modeling should precede sensitive data paths, and cost guardrails should precede recurring cloud spend.
- AI-assisted development benefits from the same backlog discipline as human-only development; clearer stories produce safer and more bounded agent tasks.

### Interview / Portfolio Example

> I reconciled GitHub issue state against merged pull requests, identified an ADR acceptance-criteria gap rather than masking it, and converted the product roadmap into a dependency-aware implementation backlog. I used a Definition of Ready to make Sprint 1 stories implementation-ready, including measurable acceptance criteria, security considerations, test expectations, and explicit dependencies. I also sequenced threat modeling and AWS cost controls ahead of higher-risk implementation work.

### Skills Demonstrated

- Agile backlog refinement and prioritization
- story decomposition
- dependency mapping
- Definition of Ready application
- GitHub issue / pull-request governance
- acceptance-criteria integrity
- security-by-design sequencing
- FinOps-aware planning
- AI-agent task design
- professional SDLC hygiene

---

## 2026-08-08 — Threat Modeling and AI Capacity as Engineering Constraints

### Context

Before implementing authentication, document upload, or sensitive household-data paths, HomeOps completed an initial MVP threat model. During the same work, GitHub Copilot Agent exhausted its included usage allowance and became temporarily unavailable until the development plan was upgraded.

### Security Work Completed

The threat model identified the system's key assets, actors, and trust boundaries and rated major threats using a lightweight High / Medium / Low approach. Broken household authorization and cross-household data access were treated as High risk. The model also covered document-upload abuse, API and input-validation risks, secrets/configuration exposure, privacy-safe logging, backup/recovery, accidental exposure of internal health/debug endpoints, and future AI/OCR processing risks.

The highest-risk areas were connected to explicit test expectations, especially denial of cross-household access and safe handling of uploaded content. Durable security choices such as authentication provider, document scanning, edge/rate-limiting controls, and secrets management were intentionally left for later ADRs rather than being silently embedded in the threat model.

### AI Engineering / FinOps Lesson

AI-agent capacity is an engineering resource with cost and quota constraints. When the Copilot quota was exhausted, the project did not need to stop: planning, review, Git verification, and manual repository work remained viable. The experience reinforced a model-routing approach in which expensive agentic execution is used where repository-local automation creates real value, while planning and review can happen outside the coding agent.

Upgrading the Copilot plan restored capacity, but the important lesson was not simply to buy more quota. AI usage should be treated similarly to other engineering resources: understand the cost model, choose the least expensive capability that reliably completes the task, bound agent work tightly, and preserve a graceful human-controlled fallback path.

### Engineering Lessons

- Threat modeling is most valuable before sensitive features are implemented, not after production exposure exists.
- Multi-tenant authorization should be treated as a primary security boundary and tested explicitly.
- Security requirements and implementation choices are different artifacts; durable implementation decisions may deserve ADRs.
- AI agents should not become a single point of failure for the development process.
- Model selection, context size, agent autonomy, and quota consumption are practical AI FinOps concerns.
- Human-controlled Git workflows provide continuity and governance when an AI tool is unavailable or behaves unexpectedly.

### Interview / Portfolio Example

> Before implementing sensitive SaaS workflows, I created a lightweight threat model that identified trust boundaries, ranked risks, and made cross-tenant authorization a high-risk testable control. During the same project, an AI coding-agent quota was exhausted, so I treated agent capacity as an engineering/FinOps constraint rather than a blocker: the workflow fell back to human-controlled planning and Git operations, and I refined model-routing and agent-use practices to reserve expensive inference for work where repository-local automation added value.

### Skills Demonstrated

- application threat modeling
- multi-tenant security design
- secure file-upload planning
- security test planning
- privacy-aware observability
- architecture decision governance
- AI-assisted engineering operations
- AI FinOps / usage governance
- graceful degradation of engineering tooling
- human-in-the-loop change control
