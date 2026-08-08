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

### Follow-Up

Future journal entries should capture meaningful architecture decisions, AWS implementations, Java and Kubernetes learning, CI/CD development, security controls, observability, FinOps decisions, AI integrations, production incidents, performance improvements, and lessons learned from operating HomeOps AI.