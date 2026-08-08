# Definition of Done

This Definition of Done describes what must be true before a HomeOps AI story or task can be considered complete.

## Required for every story or task

A story or task is not done until all of the following are true:

- Implementation is complete.
  - The work is fully built and integrated into the intended workflow.
- Acceptance criteria are satisfied.
  - The change delivers the intended user or product outcome.
- Automated tests are added or updated and passing where applicable.
  - Tests protect core workflows and help prevent regressions.
- CI is passing.
  - The change does not introduce broken builds, failing checks, or regressions.
- The pull request has been reviewed.
  - Review provides shared ownership and catches issues early.
- No unresolved critical defects remain.
  - Critical issues do not remain open for a story that is being completed.
- The change is merged to main.
  - The work is integrated into the shared delivery path.

## Apply when relevant

The following criteria apply when the change warrants them:

- Security review is completed.
  - This is required for changes involving authentication, authorization, sensitive data, document storage, or other security-sensitive behavior.
- Documentation is updated.
  - Product, engineering, or operational documentation should be updated when behavior or workflow changes.
- Observability and operational impact are considered.
  - Changes that affect reminders, background jobs, integrations, or other reliability-sensitive flows should be observable and supportable in operation.

## Short form

A story or task is Done when it is fully implemented, meets its acceptance criteria, is verified by appropriate tests and checks, is safe and documented where needed, has passed review and CI, has no unresolved critical defects, and has been merged to main.
