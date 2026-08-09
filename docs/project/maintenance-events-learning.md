# Vehicle Maintenance Events Learning Notes

## Purpose

This note captures the engineering lessons from the first full-stack Vehicle Maintenance Events slice completed in PR #53.

The feature extended the existing Household -> Vehicle foundation into a real lifecycle workflow and exercised database design, JPA/Hibernate mapping, Flyway migration discipline, scoped REST APIs, React UI integration, automated tests, CI compatibility, and real PostgreSQL/browser validation.

## Delivered Slice

HomeOps now supports full CRUD for Vehicle Maintenance Events through the browser and backend API.

Domain path:

`Household -> Vehicle -> Maintenance Event`

Technical path:

`Browser -> React/TypeScript -> Spring Boot REST -> service/domain -> Spring Data JPA/Hibernate -> Flyway -> PostgreSQL 16.14`

Maintenance Event fields include:

- UUID id
- required service date
- required description
- optional mileage
- optional cost
- optional notes
- server-managed created/updated timestamps

## Persistence Lessons

### JPA relationship ownership

`MaintenanceEvent -> Vehicle` is modeled as a required, unidirectional, lazy relationship.

That keeps the persistence model simple: MaintenanceEvent knows which Vehicle owns it, but Vehicle does not need to maintain a large bidirectional collection merely to support the API.

The same ownership boundary appears consistently across layers:

- Household- and Vehicle-scoped REST paths
- service-level scope validation
- repository queries
- JPA relationship
- PostgreSQL foreign key

This reinforces an important design principle: ownership and authorization boundaries are easier to reason about when every layer tells the same story.

### Flyway remains the schema owner

Flyway V3 created the `maintenance_events` table. Hibernate remains configured to validate the schema rather than generate or mutate it.

This keeps schema evolution explicit, versioned, reviewable, and repeatable.

### Application validation and database constraints complement each other

Bean Validation and service rules provide useful request feedback, while PostgreSQL constraints protect persisted data integrity.

Mileage and cost are required to be non-negative when present. Enforcing this both at the application boundary and at the database layer protects against invalid data entering through future code paths.

### Indexes should match access patterns

Maintenance Events are listed per Vehicle in service-date order. The migration therefore includes an index designed around that real query pattern rather than adding indexes speculatively.

### Cascade delete is a lifecycle decision

The Vehicle foreign key uses `ON DELETE CASCADE`, so deleting a Vehicle removes dependent Maintenance Events.

This behavior was validated explicitly against PostgreSQL rather than being assumed from the mapping.

## API and Scope Lessons

The maintenance API is nested under Household and Vehicle context.

Wrong Household, Vehicle, or Event scope behaves as not found. This is useful even before authentication exists because the application avoids creating unscoped data-access patterns that would later undermine authorization.

## Full CRUD Decision

The original plan considered create/list only. Full CRUD was chosen because Maintenance Events represent user-owned historical records.

Users need to correct mistakes such as mileage, date, cost, or description and remove erroneous records. Once a resource represents durable user history, update/delete become part of a practical MVP rather than unnecessary scope.

## Testing Strategy

The default backend CI path remains database-free and passed after the slice:

- `./mvnw verify`
- 50 tests
- 0 failures
- 0 errors

Frontend verification also remained fast and deterministic:

- `npm ci`
- `npm test`
- 2 passing frontend tests
- `npm run build`

The real persistence path was validated separately against PostgreSQL 16.14:

- Flyway migrated schema from V2 to V3
- Hibernate schema validation succeeded
- create/list/get/update/delete worked
- list ordering was validated
- wrong Household scope returned 404
- Vehicle deletion cascade behavior was validated

This reinforces the distinction between fast CI feedback and real-runtime integration proof. Both are valuable and solve different problems.

## Frontend Lessons

The React UI reused the existing frontend architecture rather than adding a new state-management pattern:

- TanStack Query manages server state and cache invalidation
- React Hook Form manages forms
- MSW supports realistic API behavior in frontend tests
- existing loading, empty, validation, and error-state conventions were reused

After create/update/delete, the UI refreshes server state while preserving Household and Vehicle context.

## Vertical-Slice Lesson

This story is a good example of vertical slicing:

`story -> migration -> entity -> repository -> service -> controller -> tests -> API client -> React UI -> frontend tests -> PostgreSQL validation -> browser validation -> PR`

The user capability was completed across the whole system rather than leaving disconnected database, API, or UI work unfinished.

## AI-Assisted Engineering Lesson

AI accelerated implementation and repetitive validation, but engineering judgment remained necessary for:

- deciding that full CRUD belonged in the first slice
- keeping Household/Vehicle/Event scoping consistent
- selecting the JPA relationship shape
- defining Flyway ownership and database constraints
- preserving database-free CI
- verifying actual PostgreSQL behavior
- reviewing browser behavior and Git changes before merge

The working model remains:

`requirements/story -> plan/review -> Agent implementation -> automated tests -> real runtime validation -> Git review -> PR -> merge -> learning update`

## Next Learning Step

The Household -> Vehicle -> Maintenance Event browser flow is now stable and valuable enough to automate as a true browser end-to-end test.

The next recommended quality slice is Playwright E2E automation, followed by backend containerization and the first AWS deployment phase.