# BookNest

BookNest is a Java portfolio project for managing a personal library. It is being built incrementally; the current checkpoint is **Phase 0: project foundation**. No library-management features have been implemented yet.

## Stack

- Java 17
- Spring Boot 4.0.8, Spring MVC, Spring Data JPA, and Actuator
- PostgreSQL 17
- Maven 3.9.16 via Maven Wrapper 3.3.4
- Docker Compose

## Run locally with Docker

Requirements: Git and Docker Desktop with Docker Compose.

```powershell
docker compose up --build
```

Open <http://localhost:8080/actuator/health> for the health endpoint. The application is bound to loopback only. PostgreSQL is not published as a host port.

The defaults in Compose are public, local-development-only values for convenience. They are not secrets and must not be used for a public deployment. You can copy `.env.example` to `.env` and change the values for local use; `.env` is ignored by Git.

Stop the BookNest services while preserving database data:

```powershell
docker compose down
```

To intentionally reset BookNest's local database, first stop the stack and then remove only the named `booknest-postgres-data` volume using Docker Desktop. This permanently deletes BookNest's local database data. Do not remove unrelated volumes.

## Run tests with Maven Wrapper

The test profile uses an in-memory H2 database for the Phase 0 application/health smoke tests. PostgreSQL-specific behavior is not tested yet.

On Windows:

```powershell
.\mvnw.cmd clean verify
```

On macOS/Linux:

```bash
./mvnw clean verify
```

## Current scope

Phase 0 establishes the application skeleton, database connection settings, container setup, and health checks only. Authentication and library workflows are planned for later phases and are not available yet.

See [PHASE_CHECKLIST.md](PHASE_CHECKLIST.md) (English) or [PHASE_CHECKLIST.vi.md](PHASE_CHECKLIST.vi.md) (Tiếng Việt) for acceptance checklists and verification status. The shared AI-agent context and full project roadmap are in [AGENTS.md](AGENTS.md).
