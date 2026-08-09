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

## Architecture Boundaries Learned

- JDBC connectivity and ORM/domain persistence are different concerns. Issue #27 deliberately stopped after proving connectivity instead of prematurely adding JPA entities, repositories, migrations, or domain tables.
- In-memory -> database connectivity -> domain persistence is a useful incremental modernization path: each step is independently testable and reviewable.
- Local development credentials and configuration should not become production/cloud configuration by accident.

## FastAPI / Existing Experience Mental Mapping

- Spring controller roughly maps to FastAPI route handling.
- Bean Validation DTOs play a similar role to validated request models.
- Spring service/repository layering separates HTTP behavior, business logic, and storage concerns.
- Spring `DataSource`/HikariCP/JDBC is the Java-side database connection infrastructure that sits below future persistence/domain layers.

## AI-Assisted Engineering Workflow

- Use Plan mode to inspect repository standards, scope, dependencies, and risks before changing files.
- Review and tighten the plan before Agent implementation.
- Use Agent mode for the bounded implementation and validation work.
- Independently inspect diffs, tests, runtime evidence, and scope before committing.
- AI can accelerate implementation, but engineering ownership remains in defining constraints, reviewing decisions, validating behavior, and maintaining repository history.

## Portfolio / Interview Framing

HomeOps demonstrates incremental modernization rather than a one-shot generated application: Java 21/Spring Boot API development, automated GitHub Actions CI, least-privilege workflow permissions, Docker Compose, PostgreSQL, JDBC/HikariCP, Spring profiles, environment-based secret/configuration handling, Actuator health validation, runtime failure/recovery testing, and deliberate separation between infrastructure connectivity and domain persistence.
