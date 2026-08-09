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
