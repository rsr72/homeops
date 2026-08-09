# AWS Cost Guardrails for HomeOps AI MVP

## Purpose

This document defines lightweight but professional AWS cost guardrails for the HomeOps AI MVP. The goal is to keep the project affordable, easy to operate, and aligned with the MVP’s small-scope product goals without introducing enterprise FinOps overhead.

The guardrails below are intended for a personal or portfolio-style SaaS project that is still validating product value and architecture.

## 1. Product Cost Constraints

The MVP should remain affordable to operate at early-stage usage levels.

- Target a monthly development cost of approximately $25-$50 for the MVP environment.
- Treat $50 as an escalation and review threshold rather than an automatically enforced hard spending cap.
- Keep the system small enough that experimentation and iteration remain inexpensive.
- Avoid unnecessary infrastructure complexity until the core product value is proven.
- Defer expensive capabilities such as large-scale AI/document processing, multi-region deployment, or production-grade redundancy until the product scope justifies them.

## 2. Operational FinOps Guardrails

### AWS Budget and Alerting

- Create an AWS Budget for the project and set warnings at roughly 50% and 80-90% of the target budget.
- Use alerts to flag unexpected spend growth rather than to create a rigid approval process for every small cost.
- Review budget alerts promptly when they trigger, especially if the increase is tied to a new deployment, test environment, or data-transfer spike.

### Required Resource Tags

All major AWS resources should be tagged consistently. Recommended tags include:

- Project: HomeOps
- Environment: dev, staging, prod, or temporary
- Owner: the person or team responsible
- Purpose or CostCenter: MVP, experimentation, or other clear purpose
- ManagedBy: Terraform, manual, or other tooling

### Ephemeral Resource Expectations

- Non-production and temporary environments should be treated as disposable unless a clear need exists to keep them running.
- Development, staging, and experimental resources should be created only when necessary and destroyed when their purpose is complete.
- Terraform-managed resources should be cleaned up or destroyed when no longer needed.

### Cleanup and Destroy Expectations

- Temporary environments, proof-of-concepts, and experiments should be destroyed after use unless they are explicitly needed for ongoing validation.
- Any change that creates a new AWS resource should include a cleanup plan or a clear reason for keeping it running.
- Before closing work or moving on from an experiment, verify whether the resources created are still necessary.

### Local-First Development

- Favor local-first development where practical for day-to-day coding, testing, and validation.
- Use cloud resources primarily for integration testing, demonstration, or deployment rather than as an always-on development environment.
- Keep runtime footprints small and avoid leaving unnecessary services running in the cloud.

### Post-Deployment Verification

After deployment or environment changes, verify:

- what AWS resources were actually created,
- whether they are still necessary,
- and what recurring cost each one is expected to add.

This should be a lightweight review step, not a heavy process.

## 3. Major Cost-Risk Areas

The following areas deserve extra attention because they can create ongoing cost unexpectedly:

- NAT Gateway: avoid unless clearly necessary; it can become a recurring cost point.
- EKS: avoid for the MVP unless there is a strong product or operational reason; a simpler runtime model is more cost-effective.
- RDS: use the smallest practical instance and storage size; avoid over-provisioning.
- Load balancers: do not add them unless there is a real need for traffic distribution or routing.
- Data transfer: watch outbound transfer, document access, and large file movement carefully.
- AI/document-processing services: keep these deferred or strictly limited; do not introduce them without clear value and clear budget rationale.
- Storage and log retention: use conservative retention policies and avoid keeping unnecessary logs or snapshots.
- Idle resources: stop or destroy unused development, test, or experiment resources.

## 4. Cost Considerations in ADRs and Architecture Decisions

Cost should be an explicit factor in architecture decisions and ADRs.

- Any architecture choice that materially changes operating cost, runtime complexity, or long-term maintenance should include a brief cost note.
- ADRs should mention whether the choice increases recurring cost, introduces avoidable operational overhead, or helps keep the MVP affordable.
- The project should prefer simple, low-cost choices unless a more expensive approach is clearly justified by product needs.

## 5. Implementation Decisions to Defer

These decisions are not required for the MVP and should remain implementation-neutral for now:

- specific AWS runtime or container platform choice
- production-grade multi-AZ, multi-region, or HA architecture
- advanced edge protection or large-scale traffic-routing patterns
- detailed secrets-management implementation choices
- future AI/document-processing integration decisions

The project should avoid premature production-scale infrastructure while the MVP is still being validated.

## 6. Terraform Cleanup and Destroy Expectations

If Terraform is used for infrastructure, the following expectations should apply:

- temporary environments should be planned for teardown when their purpose ends,
- destroy or cleanup steps should be part of the workflow for experiments and non-production validation,
- and infrastructure should not be left running simply because it was easy to create.

This supports a lightweight but disciplined approach to cloud cost control.

## 7. Lightweight Policy Summary

A concise policy statement for the project could be:

> HomeOps AI will keep AWS spend intentionally small for the MVP. The default posture is local-first development, low-cost managed services, minimal redundancy, and disciplined cleanup of temporary resources. Any choice that materially increases recurring cost should be reviewed and justified.
