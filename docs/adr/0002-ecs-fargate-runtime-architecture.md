# ADR-0002: ECS Fargate Runtime Architecture for Development

- Status: Accepted
- Date: 2026-08-15
- Supersedes: Runtime portion of ADR-0001 (App Runner runtime decision)

## Context

ADR-0001 selected AWS App Runner as the initial backend runtime for HomeOps development.

After ADR-0001, AWS App Runner availability and service-direction constraints changed for this AWS account. Continuing new HomeOps runtime planning around App Runner is no longer appropriate.

Issue #64 therefore re-evaluated runtime options focused on:

- Amazon ECS Express Mode
- conventional Amazon ECS with AWS Fargate

HomeOps is both:

- a real application that must remain practical, secure, and cost-aware for development, and
- a modernization learning and portfolio project where explicit platform experience matters.

Conventional ECS/Fargate was selected because it provides deeper, transferable experience with:

- ECS clusters and services
- task definitions and task lifecycle
- Fargate networking and security groups
- ALB target health and deployment behavior
- IAM execution and task roles
- ECR image consumption and release traceability
- CloudWatch logging and operational visibility

## Decision

Adopt conventional Amazon ECS using AWS Fargate as the selected backend runtime architecture for HomeOps development.

Approved development runtime topology:

```text
Internet
        ↓ HTTPS
CloudFront default domain
        ↓ /api/* over HTTP (temporary development limitation)
Public Application Load Balancer
        ↓ port 8080, SG-to-SG only
ECS/Fargate Spring Boot task
   - public subnet
   - public IP
   - no direct Internet inbound SG rule
   ↓ port 5432, SG-to-SG only
Private RDS PostgreSQL
```

Component responsibilities:

- ECR stores immutable backend container artifacts.
- ECS cluster provides the control-plane boundary for services and tasks.
- ECS service keeps the desired task count and manages rolling deployments.
- Task definition declares image reference, resource sizing, networking mode, environment, logging, and secret wiring.
- Fargate task runs the Spring Boot container.
- Application Load Balancer terminates public HTTPS and forwards to tasks.
- Security groups enforce least-privilege traffic flow between ALB, task, and RDS.
- Secrets Manager stores database credentials (RDS-managed secret).
- CloudWatch stores runtime logs and supports operational monitoring.
- Private RDS PostgreSQL remains non-public and reachable only from allowed security groups.
- CloudFront serves the React frontend from a private S3 origin through OAC and routes relative `/api/*` requests to the ALB.

CloudFront enforces HTTPS for viewer traffic using its default certificate. The current ALB listener is HTTP-only, so CloudFront uses HTTP for the ALB origin connection in this development slice. That is an explicit temporary limitation, not a production transport model; ALB HTTPS, ACM, Route 53, and a custom domain remain deferred.

## Networking

For the development environment, Fargate tasks initially run in public subnets with `assign_public_ip = true`.

This is an intentional development cost optimization and simplification:

- avoids NAT Gateway recurring baseline cost,
- avoids introducing a broad initial fleet of paid interface VPC endpoints,
- keeps Terraform runtime wiring smaller for the first ECS/Fargate deployment slice.

Security behavior remains explicit:

- task security group does not allow direct Internet inbound traffic,
- application inbound traffic is restricted to ALB security-group sources only,
- RDS remains private,
- PostgreSQL 5432 access is SG-to-SG only from ECS task SG to RDS SG.

A public IP on a Fargate task provides outbound routing capability but does not create unrestricted inbound access without matching inbound security-group rules.

## Security Tradeoff

Public-addressed Fargate tasks are not the intended final production topology.

This development posture is accepted with compensating controls:

- restrictive SG-to-SG inbound rules,
- no direct Internet inbound rule to the task,
- least-privilege IAM separation,
- private RDS boundary,
- no plaintext credential storage in source or image artifacts.

## Production Evolution

Target evolution path:

```text
Development
Public-subnet Fargate task
        ↓
Production hardening
        ↓
Private-subnet Fargate task
        ↓
Controlled outbound connectivity
        ↓
Private RDS
```

The final controlled-egress mechanism for production (for example NAT Gateway, VPC endpoints, or another controlled design) is intentionally deferred to a future architecture decision.

## Cost Notes

For development operations:

- Fargate compute can be reduced by setting desired task count to zero.
- RDS can be stopped during development within AWS RDS stop constraints.
- ALB remains a fixed recurring cost while provisioned.
- Runtime resources can be destroyed and recreated with Terraform for deeper idle periods.
- NAT Gateway and a fleet of paid interface endpoints are intentionally deferred because their fixed development cost is not currently justified.

## Configuration and Secrets

- Non-secret runtime configuration may be injected directly into ECS task environment variables where practical.
- Database credentials remain in the existing RDS-managed Secrets Manager secret.
- Credentials must not appear in Terraform source, Git, Docker images, logs, or documentation.
- SSM Parameter Store does not need to be an application runtime dependency solely for non-secret values when ECS task environment injection is sufficient.

## Health and Startup

Expected startup and readiness flow:

```text
Fargate starts task
        ↓
Spring Boot starts
        ↓
connects to private RDS
        ↓
Flyway checks/applies migrations
        ↓
Hibernate validates schema
        ↓
Actuator health becomes healthy
        ↓
ALB marks target healthy
```

`/actuator/health` is the intended load-balancer health-check concept. Operational detail exposure should remain controlled so detailed health internals are not unnecessarily public.

## Authentication Caveat

HomeOps does not yet implement production-ready authentication and authorization controls.

Any Internet-accessible backend exposed through this development topology is development-only until authentication and authorization are implemented.

## Related Documents

- ADR-0001: Initial HomeOps AWS Development Architecture
- docs/architecture/overview.md
- docs/finops/aws-cost-guardrails.md
- docs/security/threat-model.md
