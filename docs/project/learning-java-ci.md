# Java CI Learning Notes

## 2026-08-09 — First GitHub Actions Quality Gate

### Context

HomeOps moved from local-only validation to repository-enforced continuous integration by completing Issue #26 and merging PR #44.

The backend already had a Java 21 / Spring Boot application, Maven Wrapper, Actuator health check, and an Asset REST API with automated tests. The next step was to ensure every pull request and every push to `main` repeated that validation in a clean GitHub-hosted environment.

### What Was Implemented

A single workflow was added at `.github/workflows/java-ci.yml`.

The workflow intentionally stays small:

- triggers on all pull requests
- triggers on pushes to `main`
- runs one job on `ubuntu-latest`
- checks out the repository with `actions/checkout@v4`
- installs Temurin Java 21 with `actions/setup-java@v4`
- enables Maven dependency caching through `setup-java`
- uses explicit least-privilege `contents: read` permissions
- runs `./mvnw verify` from the `backend/` Maven project

No deployment, Docker, AWS credentials, artifact publishing, scanners, version matrices, or path filters were added.

### Local-to-CI Mental Model

The important pattern is that local development and hosted CI use the same quality gate:

```text
Developer machine
    |
    |  ./mvnw verify
    v
compile + tests + package
    |
    v
feature branch / pull request
    |
    v
GitHub Actions clean runner
    |
    |  ./mvnw verify
    v
same build and test gate
    |
    v
merge to protected main
```

This reduces the chance that a change works only because of local machine state.

### Why `verify` Instead of Only `test`

Maven lifecycle phases build on earlier phases. `test` compiles and executes tests, while `verify` proceeds through the later verification/package lifecycle stages as well.

For the current HomeOps backend, both validate the existing tests, but `verify` is a better long-term CI command because future integration or quality checks can attach to later Maven phases without changing the fundamental CI entry point.

### Dependency Caching

`actions/setup-java` provides built-in Maven caching. This avoids adding a separate cache action while still reducing repeated dependency downloads on hosted runners.

The lesson is to prefer the smallest supported mechanism that solves the problem rather than adding more workflow machinery than the project needs.

### Least-Privilege Workflow Permissions

The workflow explicitly sets:

```yaml
permissions:
  contents: read
```

The CI job only needs to read repository content and execute the build. It does not need write access, cloud credentials, deployment permissions, or repository mutation privileges.

This is a practical example of least privilege applied to CI/CD rather than only to application runtime identities.

### Branch Protection / Quality-Gate Lesson

CI becomes more valuable when it is connected to repository governance.

The intended `main` branch ruleset is:

- changes arrive through pull requests
- force pushes are blocked
- branch deletion is blocked
- the Java CI status check is required before merge

That changes automated testing from informative feedback into an actual merge gate.

### Failure Behavior

GitHub Actions uses process exit status to determine whether a step succeeds. If Maven compilation or a test fails, `./mvnw verify` exits non-zero and the `backend-verify` job fails automatically.

This means the application test suite becomes executable acceptance evidence rather than a manual convention.

### AI-Assisted Engineering Lesson

This story reinforced the Plan -> Review -> Agent -> Verify -> PR workflow.

Plan mode inspected the repository and produced the smallest CI design before files were changed. Human review adjusted the plan to use `./mvnw verify` and explicit read-only permissions. Agent mode then created exactly one workflow file and ran the local equivalent command before the change was committed.

The AI agent accelerated implementation, but scope, lifecycle-command choice, permissions, test evidence, Git state, and merge remained human-controlled.

### FastAPI / Cloud Foundry Mental Mapping

For an engineer coming from Python/FastAPI and Cloud Foundry, the concepts map cleanly:

| Familiar concept | HomeOps Java/GitHub implementation |
| --- | --- |
| Python CI test command | Maven Wrapper `./mvnw verify` |
| pytest suite in CI | JUnit / Spring tests run by Maven |
| dependency cache | Maven `~/.m2` cache managed by `setup-java` |
| pipeline build stage | GitHub Actions `backend-verify` job |
| clean CI worker | GitHub-hosted `ubuntu-latest` runner |
| deployment gate | future CD stage, deliberately not part of this story |
| protected production branch | GitHub ruleset protecting `main` |

The toolchain is different, but the engineering principle is the same: reproducible build + automated tests + clean runner + merge gate.

### Validation Evidence

Before merge, the local CI-equivalent command completed successfully:

```text
cd backend
./mvnw verify

Tests run: 11
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS
```

The Maven package and Spring Boot repackage phases also completed successfully.

### Skills Demonstrated

- GitHub Actions
- continuous integration
- Java 21 CI configuration
- Maven lifecycle and Maven Wrapper
- JUnit / Spring Boot automated testing
- dependency caching
- least-privilege CI permissions
- pull-request quality gates
- branch protection concepts
- local/CI build parity
- AI-assisted Plan/Agent workflow
- human-in-the-loop engineering governance

### Interview / Portfolio Example

> I implemented the first CI quality gate for a Java 21 / Spring Boot SaaS project using GitHub Actions. I kept the workflow intentionally small: a single Ubuntu job installs Temurin 21, caches Maven dependencies, and runs the same Maven Wrapper `verify` command used locally. I explicitly reduced workflow permissions to read-only and kept deployment credentials and cloud concerns out of the CI stage. I then connected the workflow design to protected-branch controls so automated build and test results can become a required merge gate rather than optional feedback.

### Next Learning Step

Issue #27 introduces local PostgreSQL. That will add the next major layer of learning: relational persistence, application configuration, environment-based credentials, repeatable local database startup/cleanup, database connectivity validation, and eventually JPA/schema migration patterns.