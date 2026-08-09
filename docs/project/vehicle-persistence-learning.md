# HomeOps Vehicle Persistence Learning Notes

## Purpose

Capture the engineering lessons from Issue #29, which moved HomeOps from a temporary in-memory Asset/Vehicle slice to a canonical persisted Household-scoped Vehicle domain.

## What Was Built

Issue #29 established the durable Vehicle path:

`HTTP -> Household-scoped REST API -> controller -> service -> Spring Data JPA repository -> Hibernate -> Flyway-managed PostgreSQL 16.14`

The implementation uses one canonical Vehicle model and one real JPA repository. The earlier in-memory Asset/Vehicle implementation was deliberately retired once the persisted Vehicle model replaced it.

## Relational Modeling: Household -> Vehicle

Every Vehicle belongs to a Household. This relationship is represented at several layers:

- REST routes are nested under `/api/households/{householdId}/vehicles`.
- Repository lookups include both Vehicle ID and Household ID.
- The Vehicle JPA entity has a required `ManyToOne` relationship to Household.
- PostgreSQL stores a real `household_id` foreign key.
- The migration uses `ON DELETE CASCADE` for the current MVP ownership semantics.

The important lesson is that a data-ownership boundary should be designed before authentication is added. Authentication will later establish who the caller is; authorization will decide which Household the caller may access. By already scoping Vehicle access through Household identity, the persistence/API design will not need to be redesigned just to add authorization.

## JPA / Hibernate Lesson

Spring Data JPA provides the repository abstraction used by the application. Hibernate is the ORM implementation that maps the Java entity model to relational database operations.

For Vehicle, the relationship is intentionally unidirectional: Vehicle knows its Household, but Household does not need to maintain an in-memory collection of Vehicles merely to satisfy the ORM. The relationship is lazy so loading a Vehicle does not create unnecessary object-graph work beyond what the use case requires.

Application/domain services and repositories are not environment-specific. A previous Household implementation briefly used `@Profile("local-postgres")` on domain/persistence beans to keep database-free tests working. That was corrected: environment-specific database enablement belongs in configuration/infrastructure, while tests use scoped mocks where appropriate.

## Flyway and Schema Ownership

Flyway remains the sole owner of database schema creation and evolution.

- `V1__create_households.sql` created the Household table.
- `V2__create_vehicles.sql` added the Vehicle table and Household foreign key.
- Hibernate uses `ddl-auto: validate` rather than creating or changing tables.

This separation is important. Flyway makes schema changes explicit, versioned, reviewable, and reproducible. Hibernate validates that the Java mappings agree with the schema but does not silently mutate production structure.

## UUID and Vehicle Field Decisions

Vehicle uses native UUID identity, consistent with Household and PostgreSQL.

VIN remains optional in the MVP. When supplied it is trimmed, normalized to uppercase, and limited to 17 characters. VIN uniqueness was deliberately deferred because it is not required to establish the first persisted Vehicle workflow.

Other optional fields remain simple MVP data rather than triggering additional domains prematurely: notes, purchase date, purchase cost, and current mileage.

## Household-Scoped Querying as a Security Foundation

A key repository pattern is conceptually:

`findByIdAndHouseholdId(vehicleId, householdId)`

rather than an unscoped Vehicle lookup followed by an ownership check somewhere else.

The cross-household negative test proved that asking for Household A's route with a Vehicle owned by Household B does not retrieve the Vehicle. Authentication/authorization is still future work, but the resource and query model now reflects the security boundary identified in the threat model.

## Testing Strategy

The normal Maven/GitHub Actions path remains practical and database-independent. Controller and service behavior are tested with test-scoped mocks rather than a fake application persistence implementation.

After #29:

- `./mvnw verify` passes with PostgreSQL stopped.
- 33 tests run with 0 failures and 0 errors.
- Real PostgreSQL 16.14 is used separately for local runtime validation of the actual persistence stack.

The runtime validation proved:

- PostgreSQL becomes healthy.
- Flyway V2 applies.
- Hibernate schema validation succeeds.
- Actuator database health is `UP`.
- Household-scoped Vehicle CRUD works.
- VIN normalization works.
- Vehicle data survives backend restart.
- Cross-household Vehicle access returns 404.
- Backend and database can be stopped cleanly.

This demonstrates the difference between fast CI-oriented automated tests and real infrastructure/runtime validation. Both provide value, but they answer different questions.

## Transitional Code and Technical Debt

The earlier `/api/assets` implementation was intentionally temporary. It established the first controller/service/repository/API testing patterns before persistence existed, but it modeled Vehicle through a generic in-memory Asset abstraction.

Once #29 created the canonical persisted Vehicle domain, leaving the old Asset/Vehicle implementation in place would have produced two competing models and APIs. The old implementation and tests were therefore retired deliberately.

The application-wide `GlobalExceptionHandler` was also moved out of the retiring Asset package into a neutral API package. This reinforced a useful refactoring rule: when a transitional module is removed, shared infrastructure should not remain conceptually owned by that obsolete domain.

## Vertical Slice Lesson

A vertical slice is one small capability implemented through every layer it actually needs. Vehicle persistence was not "build the whole Vehicle product." It was the bounded path required to make persisted Household-owned Vehicles real:

`REST -> validation -> service -> repository -> ORM -> migration -> PostgreSQL -> tests`

UI, AWS, documents, maintenance, authentication, and other capabilities stayed outside the slice. This keeps work reviewable and reduces the risk of an AI agent or developer expanding a story into unrelated architecture.

## Current Modern Application Position

After #29, HomeOps has:

- professional Git/PR/branch workflow
- GitHub Actions CI
- Java 21 / Spring Boot
- REST APIs and validation/error handling
- Docker Compose local infrastructure
- PostgreSQL 16.14
- Spring Data JPA / Hibernate
- Flyway migrations
- persisted Household and Vehicle domains
- explicit Household -> Vehicle relational ownership
- 33-test database-independent CI baseline
- real local persistence/restart validation

The next major learning area is the first responsive TypeScript web UI using the real Household/Vehicle APIs. AWS/Terraform/container deployment follows after a useful local end-to-end application exists.

## Interview / Portfolio Example

> I evolved a Spring Boot MVP from a temporary in-memory API into a durable PostgreSQL domain model using Spring Data JPA/Hibernate and Flyway. I modeled Household-to-Vehicle ownership with native UUIDs and a real foreign key, scoped repository/API operations by Household to establish the future authorization boundary, kept Flyway as the sole schema owner with Hibernate validation, and preserved a fast database-independent CI path while separately validating the real PostgreSQL runtime. I also retired the transitional Asset/Vehicle model once its replacement became canonical rather than allowing duplicate domain abstractions to accumulate.

## Skills Demonstrated

- relational data modeling and foreign keys
- Spring Data JPA and Hibernate ORM
- `ManyToOne` relationship mapping
- Flyway versioned migrations
- PostgreSQL UUID and timestamp modeling
- service-layer transactions and normalization
- household-scoped API/repository design
- security-boundary-aware domain design
- automated controller/service testing
- database-independent CI design
- real PostgreSQL runtime validation
- persistence/restart testing
- negative-path/cross-household testing
- technical-debt retirement
- vertical-slice delivery
- AI-assisted plan/review/agent/verify workflow
