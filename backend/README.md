# HomeOps Backend

## Prerequisites

- Java 21
- Docker with Docker Compose v2

## Build and test

From this directory:

```bash
./mvnw test
./mvnw package
```

## Run locally

```bash
./mvnw spring-boot:run
```

Then open:

```text
http://localhost:8080/actuator/health
```

## Backend container image (local validation)

Build the backend image from this directory:

```bash
docker build -t homeops-backend:local .
```

Start PostgreSQL first using the existing local Compose setup and `.env.postgres`, then run the backend container with the existing database environment-variable contract.

For Docker Desktop on macOS, use `HOMEOPS_DB_HOST=host.docker.internal` because `localhost` inside the backend container does not resolve to the host PostgreSQL container.

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

Container health check (Dockerfile `HEALTHCHECK`) probes:

```text
http://127.0.0.1:8080/actuator/health
```

Host-side checks:

```bash
curl -s http://localhost:8080/actuator/health
docker ps --filter name=homeops-backend-local
docker logs homeops-backend-local
docker exec homeops-backend-local id
```

Notes:

- Keep credentials out of images and source control. Provide secrets only at runtime.
- Logging remains stdout/stderr for container runtime compatibility.
- Flyway remains the schema owner and Hibernate remains `ddl-auto: validate` through the `local-postgres` profile.

## Local PostgreSQL development

Only Docker Compose is supported for local PostgreSQL setup in this repository.

### 1) Configure local environment variables

From this directory, create a local environment file from the example template:

```bash
cp .env.postgres.example .env.postgres
```

Edit `.env.postgres` and set a local-only development password.

Environment variables used by both Docker Compose and the backend local profile:

- `HOMEOPS_DB_HOST` (default: `localhost`)
- `HOMEOPS_DB_PORT` (default: `5432`)
- `HOMEOPS_DB_NAME` (default: `homeops`)
- `HOMEOPS_DB_USER` (default: `homeops`)
- `HOMEOPS_DB_PASSWORD` (required for real local use; do not commit)

### 2) Start PostgreSQL

```bash
docker compose --env-file .env.postgres -f docker-compose.postgres.yml up -d
```

### 3) Confirm PostgreSQL readiness

```bash
docker compose --env-file .env.postgres -f docker-compose.postgres.yml ps
```

The `postgres` service should report healthy.

### 4) Start backend with local PostgreSQL profile

```bash
set -a
. ./.env.postgres
set +a
SPRING_PROFILES_ACTIVE=local-postgres ./mvnw spring-boot:run
```

### 5) Verify database connectivity through Actuator

```bash
curl -s http://localhost:8080/actuator/health/db
```

Expected result includes `"status":"UP"`.

### 6) Verify failure and recovery behavior

In a second terminal, stop PostgreSQL and verify health is no longer UP:

```bash
docker compose --env-file .env.postgres -f docker-compose.postgres.yml stop postgres
curl -s http://localhost:8080/actuator/health/db
```

Restart PostgreSQL and verify health returns to UP:

```bash
docker compose --env-file .env.postgres -f docker-compose.postgres.yml start postgres
docker compose --env-file .env.postgres -f docker-compose.postgres.yml ps
curl -s http://localhost:8080/actuator/health/db
```

### 7) Normal shutdown

Stop the backend process with Ctrl+C, then stop PostgreSQL:

```bash
docker compose --env-file .env.postgres -f docker-compose.postgres.yml stop
```

### 8) Destructive reset (container + volume)

This removes local PostgreSQL data permanently.

```bash
docker compose --env-file .env.postgres -f docker-compose.postgres.yml down -v
```
