# Java CI Learning Notes

## GitHub Actions and Maven

- A GitHub Actions workflow can act as an automated quality gate for every pull request and push to `main`.
- HomeOps uses Java 21 and the Maven Wrapper so CI runs the same project-controlled Maven version as local development.
- `./mvnw verify` is a useful CI lifecycle command because it compiles, tests, packages, and leaves room for later verify-phase quality gates.
- `actions/setup-java` can provide Maven dependency caching without a separate cache action.
- Explicit workflow permissions such as `contents: read` demonstrate least-privilege design.
- Stable workflow/job names matter because branch protection can require those exact checks before merge.

## Local PostgreSQL and Docker

- Docker Compose provides a reproducible local dependency without installing and managing PostgreSQL directly on the host OS.
- Pinning PostgreSQL to a specific version (`postgres:16.14`) makes the development environment repeatable.
- A named Docker volume preserves database data across normal container stop/start cycles; `docker compose down -v` is an intentional destructive reset.
- Container health (`pg_isready`) verifies PostgreSQL readiness separately from application-level connectivity.
- Spring Boot's `DataSource` plus HikariCP/JDBC establishes the application-to-database connection without requiring JPA/Hibernate.
- A dedicated `local-postgres` Spring profile keeps local infrastructure configuration separate from the default application/CI path.
- Environment variables and an ignored `.env.postgres` file keep real local credentials out of source control; an example env file documents the required contract.
- Actuator database health proved the full Spring Boot -> HikariCP -> JDBC -> PostgreSQL path. The database reported `UP`, failed when PostgreSQL was stopped, and recovered after restart.
- `./mvnw verify` still passed with PostgreSQL stopped: 11 tests, 0 failures, 0 errors. This demonstrates that adding a local external dependency does not require coupling the normal CI/unit-test path to that dependency.
- Green unit/CI tests alone do not prove an external integration works. Runtime positive, failure, and recovery checks are separate evidence.

## JPA / Hibernate and Persisted Domain Modeling

- Spring Data JPA provides the repository abstraction used by the persisted Household domain; Hibernate is the JPA ORM implementation underneath it.
- A JPA entity maps a Java domain object to a relational table while keeping HTTP request/response DTOs separate from persistence details.
- HomeOps uses a real Java `UUID` mapped to PostgreSQL's native `uuid` type instead of treating identifiers as arbitrary strings.
- Transaction boundaries belong in the service layer. Read operations can use `@Transactional(readOnly = true)` while create/update/delete operations execute in normal transactions.
- HikariCP remains the connection-pool layer beneath Hibernate/JPA, and JDBC remains the underlying Java/database communication layer.
- ORM convenience does not eliminate the need to understand SQL, constraints, transaction boundaries, and relational design.

## Flyway and Schema Ownership

- Flyway is database schema version control: migrations define how a blank or older database reaches the required schema version.
- `V1__create_households.sql` established the first HomeOps domain table.
- Flyway is the sole schema owner. Hibernate is configured with `ddl-auto: validate`, so Hibernate checks that entity mappings match the schema but does not create or alter tables.
- A PostgreSQL-specific Flyway module was required when the initial Flyway runtime reported PostgreSQL 16.14 as unsupported. Adding the correct database module resolved the integration without changing the domain design.
- Versioned SQL migrations make schema changes explicit, reviewable, repeatable, and suitable for future local/CI/AWS environments.

## Household Persistence Lessons

- The first durable Household resource proved the full path: HTTP -> Controller -> Service -> Spring Data JPA -> Hibernate -> JDBC/HikariCP -> PostgreSQL.
- Household uses UUID identity, required name, optional notes, and server-managed UTC timestamps.
- The persisted CRUD path was validated against PostgreSQL 16.14, including create, read/list, update, delete, and not-found behavior.
- Persistence was proven across a backend restart: the same Household remained available after Spring Boot stopped and restarted.
- Database runtime health was validated separately from unit/controller tests.
- After Household persistence was added, the ordinary Maven verification path still passed with PostgreSQL stopped: 26 tests, 0 failures, 0 errors.

## Environment Boundaries and Test Design

- Application/domain code should not be annotated as `local-postgres` simply because the first database environment is local. The same Household service/repository should be usable later against managed PostgreSQL.
- Environment-specific behavior belongs in datasource/infrastructure configuration rather than domain/service classes.
- The initial profile-gated Household service/repository worked technically but represented the wrong abstraction; reviewing the design before commit caught and corrected it.
- Controller tests can use test-scoped service mocks and service tests can mock the JPA repository, keeping CI fast without creating a second fake persistence implementation.
- Test-scoped mocks are different from maintaining an alternate in-memory application repository: production persistence still has one implementation path.
- Database-independent CI and real PostgreSQL runtime validation are complementary rather than interchangeable.

## Architecture Boundaries Learned

- JDBC connectivity and ORM/domain persistence are different concerns. Issue #27 deliberately stopped after proving connectivity; Issue #28 added the ORM, migration, and durable domain layers.
- In-memory -> database connectivity -> schema migrations/ORM -> persisted domain is a useful incremental modernization path because each step can be independently tested and reviewed.
- Local development credentials and configuration should not become production/cloud configuration by accident.
- Household is becoming the data-isolation root for future Vehicle, Document, Maintenance, and authorization behavior; future resource queries should make that boundary explicit.
- Avoid speculative frameworks: a concrete Household/Vehicle model is preferable to prematurely introducing a generic tenant or asset hierarchy.

## FastAPI / Existing Experience Mental Mapping

- Spring controller roughly maps to FastAPI route handling.
- Bean Validation DTOs play a similar role to validated request models.
- Spring service/repository layering separates HTTP behavior, business logic, and storage concerns.
- Spring `DataSource`/HikariCP/JDBC is the Java-side database connection infrastructure.
- Spring Data JPA/Hibernate fills a role similar to an ORM layer such as SQLAlchemy: Java entities map to relational tables and repositories provide data access while the ORM generates/executes SQL.
- Flyway is analogous to a dedicated database migration/versioning workflow: schema history is explicit rather than inferred from ORM models at startup.

## AI-Assisted Engineering Workflow

- Use Plan mode to inspect repository standards, scope, dependencies, and risks before changing files.
- Review and tighten the plan before Agent implementation.
- Use Agent mode for the bounded implementation and validation work.
- Independently inspect diffs, tests, runtime evidence, Git branch state, and scope before committing.
- Architecture review matters even when tests are green: the `@Profile("local-postgres")` correction is an example where code worked but the abstraction was wrong for future environments.
- AI can accelerate implementation, but engineering ownership remains in defining constraints, reviewing decisions, validating behavior, and maintaining repository history.

## Current Modern Application Development Position

Implemented so far:

- Git/GitHub feature branches, pull requests, branch protection, and CI quality gates
- Java 21 / Spring Boot backend
- REST APIs, DTOs, validation, consistent errors
- automated controller/service/application tests
- Docker Compose local infrastructure
- PostgreSQL 16.14
- JDBC / HikariCP
- Spring Data JPA / Hibernate
- Flyway schema migrations
- persisted Household CRUD
- Actuator health and runtime failure/recovery validation
- environment-variable/secrets separation

Not implemented yet:

- persisted Vehicle + Household relationship
- frontend/UI
- backend container image
- Terraform/AWS infrastructure
- cloud deployment and CD
- managed cloud PostgreSQL
- authentication/authorization
- documents/S3/OCR
- maintenance/reminders
- production observability/hardening

Near-term sequence:

1. Persist Vehicle and reconcile the temporary Asset/Vehicle model.
2. Build the first responsive Household/Vehicle UI.
3. Add a small useful end-to-end workflow beyond basic CRUD.
4. Containerize the backend.
5. Start Terraform/AWS foundation, image publishing, deployment, and managed database work.

## Portfolio / Interview Framing

HomeOps demonstrates incremental modernization rather than a one-shot generated application: Java 21/Spring Boot API development, automated GitHub Actions CI, least-privilege workflow permissions, Docker Compose, PostgreSQL, JDBC/HikariCP, Spring Data JPA/Hibernate ORM, Flyway migrations, native UUID persistence, environment-based secret/configuration handling, Actuator health validation, persistence-after-restart testing, database-independent CI, and deliberate human review of AI-generated architecture and implementation choices.
