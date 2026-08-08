# GitHub Copilot Instructions for HomeOps AI

HomeOps AI is an intelligent ownership and lifecycle platform for households. The current MVP is vehicle-first and should stay small, useful, and testable. Preserve a generic asset model so the product can expand later without redesign.

Use the repository documentation as the source of truth, especially the product vision, MVP requirements, development definitions, repository standards, and engineering journal. Keep changes small, reviewable, and aligned with the MVP. Avoid over-engineering and prefer simple, maintainable solutions.

Before changing anything, inspect the existing code, patterns, and conventions in the repository. Prefer technologies and patterns already approved for HomeOps. Do not introduce new frameworks, services, libraries, or architectural patterns unless the change clearly requires them and the tradeoff is explained.

Preserve approved architecture decisions and avoid introducing unnecessary new abstractions. Treat security and privacy as first-class concerns. Protect household, asset, document, and financial data; avoid unnecessary exposure of sensitive information.

For behavior changes, add or update tests that validate the real user workflow. Update documentation when behavior, workflow, or operational expectations change. Follow the project’s Definition of Ready and Definition of Done: make scope and acceptance criteria clear, confirm testability, and do not treat work as done without evidence.

Follow repository workflow rules: use branch names like feature/<issue-number>-short-description, use pull requests for changes, and do not commit, push, merge, or modify protected branches without human review and approval.

When introducing unfamiliar Java, Spring, AWS, Terraform, Kubernetes, security, or architecture concepts, briefly explain the reasoning so the implementation remains understandable. When a change has meaningful tradeoffs, risks, or unresolved assumptions, explain them clearly rather than silently making design choices.
