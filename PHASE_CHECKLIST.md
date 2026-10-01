# Phase acceptance checklists

Use these checklists to review each phase. A box should be marked complete only after the item has been checked against the implementation or command output. If an item cannot be verified, leave it unchecked and explain why in `AGENTS.md`.

## Phase 0 — Project foundation

- [x] Spring Boot skeleton is configured for Java 21.
- [x] Maven Wrapper scripts and wrapper configuration are present.
- [x] PostgreSQL connection settings are externalized through environment variables.
- [x] Docker Compose defines the app and PostgreSQL with a BookNest-specific named volume.
- [x] PostgreSQL has a health check and the app waits for it to become healthy.
- [x] App port is bound to loopback; PostgreSQL is not published to the host.
- [x] A multi-stage Dockerfile builds with Java 21 and runs as a non-root user.
- [x] Actuator readiness health includes the database health indicator.
- [x] A README explains local startup, health URL, shutdown, test command, and current limitations.
- [x] Local-only environment values are documented; `.env` is ignored and `.env.example` contains no real secrets.
- [x] `.\mvnw.cmd --no-transfer-progress clean verify` passes on the host (Temurin Java 25; project compiles for Java 21).
- [x] `docker compose config --quiet` passes.
- [x] Docker image builds and the app/database start successfully (`docker compose up -d --build`).
- [x] Running readiness health check reports both app and database as healthy (`status=UP`, `components.db.status=UP`).
- [x] Changes are limited to the BookNest repository.

Docker Hub image pulls initially failed because of a local registry authentication error. The same official image tags were pulled from the public Amazon ECR mirror and tagged in the local Docker cache for verification; project files still reference the standard official image names.

### Phase 0 file ledger

These files were created or changed during Phase 0. The ledger is retrospective; the Phase 0 checklist itself was added after implementation began, which did not follow the user's requested process. For all future phases, prepare that phase's checklist before implementation and update this ledger as each file is completed.

- [x] `pom.xml` — Spring Boot dependencies and Java 21 build configuration.
- [x] `.gitignore` — ignore build artifacts and local environment files.
- [x] `.gitattributes` — line-ending attributes.
- [x] `.env.example` — documented local-only Compose configuration.
- [x] `.mvn/wrapper/maven-wrapper.properties` — Maven Wrapper distribution configuration.
- [x] `mvnw` and `mvnw.cmd` — Unix and Windows Maven Wrapper launchers.
- [x] `src/main/java/com/booknest/BookNestApplication.java` — Spring Boot application entry point.
- [x] `src/main/resources/application.yml` — PostgreSQL and health configuration.
- [x] `src/test/java/com/booknest/BookNestApplicationTests.java` — context and readiness smoke tests.
- [x] `src/test/resources/application-test.yml` — isolated H2 test profile.
- [x] `Dockerfile` — multi-stage Java 21 image build and non-root runtime.
- [x] `compose.yaml` — app/database services, health checks, loopback binding, and dedicated volume.
- [x] `README.md` — Phase 0 scope and local run/test instructions.
- [x] `AGENTS.md` — shared project context, phase status, and agent workflow rules.
- [x] `PHASE_CHECKLIST.md` — phase acceptance checks and file ledger.

## Later phases

### Phase 1 — Authentication and account isolation (prepared; implementation not started)

#### Acceptance checks

- [ ] Agree and document whether each registered account automatically owns exactly one personal library, or whether those concepts are represented by one account-scoped owner model.
- [ ] Registration creates an account using a unique username and BCrypt-hashed password; password is never returned.
- [ ] Login/logout use Spring Security server-side sessions and cookies; no JWT or custom authentication implementation.
- [ ] CSRF protection remains enabled; unauthenticated clients can obtain the token and the API rejects unsafe requests without a valid token.
- [ ] Unauthenticated requests cannot access protected business endpoints.
- [ ] Current account identity is resolved from Spring Security, never from user/owner/library IDs supplied by a client.
- [ ] Account-owned queries are consistently scoped; tests prove account A cannot read, modify, or delete account B's records.
- [ ] Relevant schema constraints and error responses are implemented and tested.
- [ ] `.\mvnw.cmd --no-transfer-progress clean verify` passes; Docker/PostgreSQL integration checks are recorded if run.
- [ ] The per-file ledger below is updated as each implementation file is completed.

#### Planned file ledger (revise with confirmed design before implementation)

- [ ] `pom.xml` — add Spring Security dependencies if not already present.
- [ ] `src/main/java/com/booknest/...` — account/library persistence model, repositories, service, DTOs, and API endpoints, organized to fit the existing project.
- [ ] `src/main/java/com/booknest/...` — Spring Security session, CSRF, password encoder, and current-account configuration.
- [ ] `src/main/resources/application.yml` — only necessary security/session configuration updates; no secrets.
- [ ] `src/test/java/com/booknest/...` — authentication, CSRF, and cross-account isolation tests.
- [ ] `src/test/resources/application-test.yml` — test configuration updates only if required.
- [ ] `PHASE_CHECKLIST.md` — update this checklist and ledger as each actual file is completed.
- [ ] `README.md` — document verified Phase 1 behavior and any relevant local usage changes.

**State:** checklist prepared before Phase 1 implementation. Phase 1 has not started and requires an explicit user request.
