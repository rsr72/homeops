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
