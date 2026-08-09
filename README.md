# HomeOps AI

HomeOps AI is a local-first household and vehicle ownership platform. It helps a household keep a structured record of vehicles, supporting data, and related lifecycle information in a small, professional full-stack application.

Current status: HomeOps is a working local browser-to-database application. Issue #43 delivered the first responsive browser UI, household selection is URL-driven, and the browser can create a Household from an empty database and create, edit, and delete Vehicles against PostgreSQL 16.14.

## Current Architecture

Browser
-> React / TypeScript
-> Vite development proxy
-> Spring Boot REST API
-> service/domain layer
-> Spring Data JPA / Hibernate
-> Flyway-managed PostgreSQL 16.14

The Vite `/api` proxy is used for local development, so no backend CORS change is required for this slice.

## Implemented Capabilities

- Separate `frontend/` workspace for the browser UI.
- React + TypeScript frontend with Vite development and production build tooling.
- npm-managed frontend dependencies with committed `package-lock.json`.
- TanStack Query for server state.
- React Hook Form for household and vehicle forms.
- URL-driven Household selection and summary state.
- Household creation from an empty database.
- Household-scoped Vehicle list, create, edit, and delete workflows.
- Responsive UI with lightweight CSS, CSS Modules, and CSS variables.
- Loading, empty, validation, and API error states.
- Vitest, React Testing Library, and MSW frontend tests.
- Playwright Chromium E2E coverage for one real browser journey: Household -> Vehicle -> Maintenance Event create/edit/delete with refresh persistence.
- Generated frontend artifacts such as `node_modules`, `dist`, and TypeScript build output are excluded from Git.

## Technology Stack

- Backend: Java 21, Spring Boot 3.3.x, Spring Web, Spring Validation, Spring Boot Actuator
- Data: PostgreSQL 16.14, Flyway, Spring Data JPA / Hibernate
- Frontend: React 18, TypeScript, Vite, React Router, TanStack Query, React Hook Form
- Testing: JUnit / Spring Boot Test on the backend, Vitest / React Testing Library / MSW on the frontend
- Tooling: Maven Wrapper, npm, Docker Compose for local PostgreSQL

## Repository Structure

- `backend/` Spring Boot application, database migrations, and backend tests
- `frontend/` React + TypeScript Vite application, frontend tests, and package-lock
- `docs/` product, architecture, development, project, and learning documentation
- `.github/workflows/` GitHub Actions CI

## Local Prerequisites

- Java 21
- Node.js 22+ and npm
- Docker with Docker Compose v2

## Local Setup

### 1. Start PostgreSQL

From `backend/`:

```bash
cp .env.postgres.example .env.postgres
docker compose --env-file .env.postgres -f docker-compose.postgres.yml up -d
```

### 2. Start the Spring Boot backend

From `backend/`:

```bash
set -a
. ./.env.postgres
set +a
SPRING_PROFILES_ACTIVE=local-postgres ./mvnw spring-boot:run
```

### 3. Start the React/Vite frontend

From `frontend/`:

```bash
npm install
npm run dev
```

Open the Vite local URL shown in the terminal.

## Verification Commands

Backend container image:

```bash
cd backend
docker build -t homeops-backend:local .
```

Run backend container against existing local PostgreSQL Compose setup:

```bash
docker run --rm --name homeops-backend-local \
	-p 8080:8080 \
	-e SPRING_PROFILES_ACTIVE=local-postgres \
	-e HOMEOPS_DB_HOST=host.docker.internal \
	-e HOMEOPS_DB_PORT=5432 \
	-e HOMEOPS_DB_NAME=homeops \
	-e HOMEOPS_DB_USER=homeops \
	-e HOMEOPS_DB_PASSWORD=homeops_dev_password \
	homeops-backend:local
```

Container health checks:

```bash
curl -s http://localhost:8080/actuator/health
docker ps --filter name=homeops-backend-local
```

This story does not containerize the frontend and does not introduce AWS runtime resources, image publishing, or deployment automation.

Frontend:

```bash
cd frontend
npm test
npm run build
npm run test:e2e
```

Playwright first-run browser install:

```bash
cd frontend
npx playwright install chromium
```

Playwright E2E prerequisites:

- PostgreSQL 16.14 must be running through the existing `backend/docker-compose.postgres.yml` flow.
- `backend/.env.postgres` must exist and contain local development values.
- `npm run test:e2e` manages Spring Boot (`local-postgres`) and Vite startup and shutdown.
- Stop manually running backend/frontend processes before using managed E2E startup.
- The E2E test uses uniquely named synthetic Household data and attempts cleanup by deleting that Household through the real backend API in `finally` behavior.
- Playwright failure artifacts are generated under `frontend/test-results/` (trace + screenshot on failure), and the HTML report is generated under `frontend/playwright-report/`.

Run Playwright unchanged against an already-running backend container:

```bash
cd frontend
PLAYWRIGHT_REUSE_EXISTING_BACKEND=true npm run test:e2e
```

Backend:

```bash
cd backend
./mvnw verify
```

## Current CI Status

GitHub Actions currently runs backend and frontend verification on pull requests and pushes to `main`. The current workflow executes `backend ./mvnw verify` on Java 21 and `frontend npm ci`, `frontend npm test`, and `frontend npm run build` on Node 22.

Playwright E2E is intentionally not a required CI check in this first slice. It is currently positioned as a focused local browser-integration confidence layer while startup/runtime characteristics stabilize.

## AWS Architecture Status

Issue #59 selected the initial AWS development architecture and documented it in:

- [docs/adr/0001-initial-aws-development-architecture.md](docs/adr/0001-initial-aws-development-architecture.md)

This is a documentation and decision milestone only. No AWS resources have been provisioned yet.

Issue #61 adds the first Terraform foundation under `infra/terraform/` for VPC, private subnet, security-group, DB subnet-group, and runtime configuration contracts.

- The Terraform workflow is local-state only for this initial slice.
- Remote state and locking are intentionally deferred.
- This repository change defines infrastructure only and does not provision resources.

## Documentation

- Product vision: [docs/product/vision.md](docs/product/vision.md)
- MVP requirements: [docs/product/mvp-requirements.md](docs/product/mvp-requirements.md)
- Architecture overview: [docs/architecture/overview.md](docs/architecture/overview.md)
- ADRs: [docs/adr/README.md](docs/adr/README.md)
- Initial AWS development architecture decision: [docs/adr/0001-initial-aws-development-architecture.md](docs/adr/0001-initial-aws-development-architecture.md)
- Engineering journal: [docs/project/engineering-journal.md](docs/project/engineering-journal.md)
- Modernization skills checklist: [docs/project/modernization-skills-checklist.md](docs/project/modernization-skills-checklist.md)
- Backend run instructions: [backend/README.md](backend/README.md)

## Roadmap

Completed foundation:

- Product and engineering documentation
- Spring Boot backend and REST API
- PostgreSQL local development with Flyway migrations
- Spring Data JPA / Hibernate persistence
- persisted Household domain
- household-scoped persisted Vehicle domain
- backend automated tests
- GitHub CI foundation
- React + TypeScript browser frontend
- frontend automated tests
- first full-stack browser-to-PostgreSQL workflow

Next major slices:

- authentication and authorization
- documents and OCR
- maintenance reminders
- AWS deployment and production operations
- backend containerization

## Notes

This repository is intentionally documenting a small, professional local application slice first. AWS deployment, production operations, and other later-stage capabilities are planned but not yet complete.
