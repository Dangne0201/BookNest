# BookNest — Project Guide for AI Agents

## Project identity and workspace boundary

- Project: **BookNest**, a personal-library management portfolio application.
- The only authorized project folder and Git repository for this work is `D:\NTGiang\BookNest`.
- Keep every project file, change, and project-specific command strictly within this folder/repository. Do not create the project in a parent, sibling, nested, or different repository folder.
- Do not inspect or operate on files, repositories, or workspaces outside `D:\NTGiang\BookNest`. This scope rule applies to all project work, not just source-code edits.
- GitHub repository: `https://github.com/Dangne0201/BookNest` (public).
- Do not create the old proposed `booknest-java/` subdirectory; the repository root itself is the project root.

## Current status

- Git has been initialized on branch `main`, and `origin` points to the GitHub repository above.
- Phase 0 application foundation is implemented: Spring Boot 4.0.8/Java 17 Maven Wrapper skeleton, PostgreSQL configuration, Dockerfile/Compose, Actuator health probes, minimal README, and phase checklist.
- After the Java 17 correction, `.\mvnw.cmd --no-transfer-progress clean verify` passed on host Temurin Java 25; Maven compiled main and test sources with `release 17`, and both H2 smoke tests passed.
- `docker compose config --quiet` passed after the Java 17 correction.
- `docker compose up -d --build` passed using Java 17 Maven build and Java 17 runtime images. Both `booknest-app-1` and `booknest-db-1` became healthy; `docker compose exec -T app java -version` reported Temurin 17.0.20.1; readiness returned `status=UP` and `components.db.status=UP`.
- Docker Hub image pulls initially returned a local authentication error. For runtime verification only, the official Maven, Eclipse Temurin, and PostgreSQL images were pulled from the public Amazon ECR mirror and tagged in the local Docker cache; project configuration was not changed for this workaround.
- After the user corrected the baseline to Java 17, removed the specifically tagged Java 21 Maven/JRE images that had been downloaded for the initial build and removed the generated `target/` output with Maven `clean`. Did not run Docker prune or touch unrelated images, containers, or volumes.
- Phase 0 acceptance checks are complete; see `PHASE_CHECKLIST.md`.
- No authentication or library-management functionality has been started.

Keep this section accurate as work progresses. After a phase is accepted or completed, update the status and checklist here with what actually changed, what was tested, the exact results, and any remaining work. Never mark work complete based only on intention.

## Goal and mentoring approach

Build a credible junior-level Java portfolio with practical library workflows, secure account-specific data, tests, and a reviewer-friendly local setup. Favor correctness, readable code, and design choices the user can explain over adding technologies for appearance. This is a learning portfolio, not a claim of production readiness.

The idea started as a request for a Java portfolio with a different concept from the user's JavaScript Task Manager learning project. The user chose BookNest, a personal-library manager, to practice Java while building something with real business rules rather than a basic CRUD showcase.

The user knows Java, REST APIs, Docker, HTML, and CSS, and wants to limit the amount of unfamiliar technology. They are a junior developer and want to build in small, controlled increments so each part can be understood, checked, and explained in an interview. The portfolio should demonstrate the user's learning and decisions, not look like a pile of technologies or something they cannot explain as AI-generated.

Communicate in Vietnamese unless the user asks otherwise. Explain important trade-offs plainly and at a junior-friendly level when useful. Do not hide complexity behind unnecessary abstractions. Avoid rushing ahead: the user previously emphasized that discussion/planning was not permission to code, and was frustrated when they thought implementation had started prematurely. Treat roadmap phases as plans only; implement only after a clear, explicit request.

The user wants to continue from another computer using GitHub. Treat this file as the durable shared project context: read it first on every new machine/session, and update the current checkpoint after real progress so another agent can resume without relying on chat history. Tell the user to sync/pull the repository on the other computer before expecting this file to contain the latest status.

## Agreed stack and scope boundaries

- Java 17 LTS, Spring Boot 4.0.8, Spring MVC/REST.
- Spring Security with username/password, server-side session and HTTP-only session cookie; BCrypt password hashing.
- Spring Data JPA/Hibernate and PostgreSQL 17.
- Maven 3.9.16 via Maven Wrapper 3.3.4, JUnit, Spring Boot Test, and Mockito when appropriate.
- Plain HTML, CSS, and JavaScript served by Spring Boot from the same origin as the API.
- Dockerfile and Docker Compose for local reviewer setup. A reviewer should not need Java, Maven, or PostgreSQL installed on the host to run the app.
- Flyway is optional only if it keeps schema changes clear and simple.
- Swagger/OpenAPI is not required.

Do not add cloud services, OAuth/Google login, JWT, frontend frameworks, microservices, email, payments, or elaborate role systems. Avoid extra dependencies unless they solve a concrete need and the user agrees where appropriate.

The goal is practical and appropriately scoped for a junior portfolio, not production-grade completeness. In particular, Swagger/OpenAPI is optional and intentionally omitted for this version; no cloud deployment is required.

### Confirmed version baseline

The user explicitly confirmed this version set on 2026-10-01. Keep it stable; ask before changing any of these versions:

- Java: 17 (compiler target and Docker build/runtime).
- Spring Boot: 4.0.8.
- PostgreSQL: 17.
- Maven: 3.9.16.
- Maven Wrapper: 3.3.4.

## Product and domain requirements

Each account owns an isolated personal library. Business records include books, individual copies, members, loans, reservations, and activity history.

### Accounts and ownership

- Support registration, login, logout, and current-account information.
- Obtain the current account from Spring Security's authenticated context/session, never from client-supplied `userId`, `ownerId`, or library ID.
- Scope every business read, update, and delete query to the authenticated account on the backend.
- Do not rely on UI hiding for authorization. Cross-account access must fail safely and consistently without revealing another account's data.

### Books, copies, members, loans

- A `Book` represents a title (for example title, author, genre, and relevant bibliographic details).
- A `BookCopy` represents an individual physical copy with an availability/loan/reservation status.
- A `Member` is managed by a library owner and is not a login account.
- A `Loan` records its copy, member, checkout date, due date, return date, renewal state, and history after return.
- Do not allow borrowing when no copy is available, borrowing a copy/member from another account, double lending, or returning a missing/already-returned loan.
- Validate due dates. Derive overdue status from due date and current time rather than persisting a status that can become stale.
- Allow at most one renewal. Reject renewal for overdue loans or when someone is waiting for that title.
- Preserve referential integrity and useful loan history when a member or book is referenced by activity.

### Reservations and waiting queue

- When no copy is available, allow a member to join a FIFO queue for the title.
- On return, reserve the copy for the first eligible person in the queue; it must not be borrowable by another member while held.
- The queue leader can cancel; transfer the hold to the next eligible person when appropriate.
- Keep this workflow intentionally simple: no email and no automatic reservation-expiry scheduler.
- Use transactions and a suitable locking/update strategy for changes to loans, copies, and reservations. Prevent two concurrent requests from lending the final available copy twice.

### Activity and discovery

- Keep ordinary business history for important events such as checkout, return, renewal, and reservation; do not implement event sourcing.
- Provide backend search/filter, database-backed pagination, and sorting for books and loans.
- Allowlist sort fields; validate page and size; never concatenate arbitrary client input into SQL.
- Provide a simple account-scoped dashboard with useful counts such as available, loaned, overdue, and queued items.

### User interface

- Use plain HTML/CSS/JavaScript, with a clean, readable, reasonably responsive layout.
- Cover authentication, overview, books/copies, members, loans/returns/renewals, reservations/queue, and list search/filter/pagination/sorting.
- Include loading, empty, success, error, and important-action confirmation states.
- Use safe DOM APIs such as `textContent`; do not render untrusted user input as HTML.
- Keep frontend and API same-origin. JavaScript must obtain and send the CSRF token correctly for state-changing requests.

### Demo data

- Provide one demo account and sample books/copies only in explicitly enabled local/demo configuration.
- Seed idempotently; restarting must not create duplicates.
- Do not populate every newly registered personal library with demo books.
- If demo mode is disabled, do not create a default account or password.
- Make clear in the README that public demo credentials are local/demo-only and unsuitable for a public deployment.
- Never commit real secrets. Use environment variables and a sample configuration without secret values.

## API and reviewer experience

Provide REST endpoints for equivalent capabilities for registration, login/logout, current account, book/copy management, members, checkout/return/renewal, reservation/queue management, and dashboard counts. Endpoint paths can follow consistent Spring conventions; document the implemented endpoints and representative request/response examples in the project README or concise API documentation.

The reviewer should be able to clone the BookNest repository and run the application and PostgreSQL locally using Docker Compose, without installing Java, Maven, or PostgreSQL on the host. Prefer a multi-stage Docker build using Maven Wrapper in the build stage and a reasonably small runtime image, running as a non-root user when feasible. Compose must wait for a healthy database, use a BookNest-specific project/resource/volume name, and bind the app to loopback (for example `127.0.0.1`) rather than presenting itself as a public deployment. Do not publish PostgreSQL to the host unless there is a clear local-development need.

Document the one-command startup, local URL, demo credentials and their limitations, shutdown, and a clearly warned reset procedure. Never execute volume/container deletion or affect Docker resources belonging to anything else.

## Security, correctness, and implementation expectations

- Protect business endpoints with Spring Security; permit only registration, login, static resources, and explicitly safe health resources as appropriate.
- Use session-cookie authentication and CSRF protection; do not disable CSRF for convenience.
- Never log passwords, session IDs, CSRF tokens, or secrets.
- Validate requests with Bean Validation and enforce business rules in service/domain code.
- Use centralized API error handling with suitable HTTP status codes and clear, safe messages; never expose stack traces.
- Do not swallow exceptions or report success after failed operations.
- Use database constraints (foreign keys, required fields, and suitable uniqueness) alongside application validation.
- Use transactions and locking/atomic updates where consistency requires them. Add indexes only for actual access patterns.
- Avoid N+1 queries and in-memory pagination of unbounded result sets.
- Prefer DTOs over exposing persistence entities when that protects internal details or avoids coupling/serialization problems.
- Use constructor injection, clear names, and a small, understandable package structure (web/controllers, services, repositories, entities, DTOs, configuration/security as useful).
- Do not add layers, generic frameworks, or TODO/placeholder endpoints and buttons without a concrete need.
- Do not claim production readiness; document meaningful limitations and trade-offs honestly.

## Work protocol

1. Read this file, inspect the current repository, Git status, and relevant existing files before changing anything. Preserve user changes; never overwrite an existing `AGENTS.md` or other file without inspecting and resolving the conflict first.
2. Keep all project work inside `D:\NTGiang\BookNest`; do not inspect or operate on paths or repositories outside this authorized workspace.
3. Implement only the phase/task the user has explicitly authorized. The phase plan below is a roadmap, not permission to do all phases at once.
4. Keep each change set focused. Build/test the smallest relevant scope, fix regressions caused by the change, then report actual outcomes.
5. Do not run destructive commands against containers, volumes, databases, or files. Compose project/resource names must be specific to BookNest; never stop or delete unrelated Docker resources.
6. Do not commit or push unless the user explicitly asks. Do not claim a build, test, Docker run, or manual flow passed unless it was actually run and passed.
7. Ask before materially changing product behavior or expanding scope. For small unspecified details, choose the simplest conventional behavior and document it.
8. Report incomplete work, environmental blockers, and unverified behavior honestly.
9. At the start of a new session, briefly acknowledge the current checkpoint from this file before doing work. If the request is only a question or planning discussion, answer without editing or implementing.
10. After a phase has actually been completed and accepted, update the checkpoint below with changed functionality/files, verification commands and outcomes, known limitations, and the next proposed phase. Keep project state in this file factual and concise.
11. Before starting any new phase, create that phase's checklists in both `PHASE_CHECKLIST.md` (English) and `PHASE_CHECKLIST.vi.md` (Vietnamese), including matching acceptance criteria, planned checks, and an anticipated file list where practical. Do not begin phase implementation until both checklists exist.
12. Update both checklist files as each file is completed, not only at the end of the phase. Keep matching per-file ledgers with purpose and status; if implementation requires an unplanned file, add it to both checklists before or as it is introduced. At phase end, ensure both acceptance checklists show the same, verifiable outcomes.

## Incremental roadmap and acceptance checks

Complete one phase at a time. A phase is ready for the next only when its acceptance checks have actually been verified. Adjust implementation details to the repository as it grows, but do not silently drop agreed requirements.

### Phase 0 — Project foundation

Create the Spring Boot/Maven Wrapper skeleton, Java 17 configuration, PostgreSQL connectivity, Dockerfile/Compose foundation, basic health check, `.gitignore`, and a minimal project README. No library workflows yet.

**Accept when:** Maven build passes; Compose configuration validates; app and database start together when Docker is available; app can connect to PostgreSQL; health behavior is verified; no real secrets are committed; Compose resources are BookNest-specific and scoped safely.

### Phase 1 — Authentication and account isolation

Implement registration, login/logout, BCrypt, session cookie, CSRF, current-account lookup, and account/library ownership foundations.

**Accept when:** authentication behavior is tested; unauthenticated users cannot call business APIs; passwords are never stored/returned in clear text; tests prove account A cannot read/update/delete account B's data.

### Phase 2 — Books and individual copies

Implement account-scoped book and copy management, statuses, validation, persistence constraints, and focused tests.

**Accept when:** create/list/update/delete and copy status behavior work; ownership is enforced in backend queries; invalid data and referenced records are handled safely.

### Phase 3 — Members

Implement account-scoped member management and rules for deletion when loans or history reference a member.

**Accept when:** a member from another account cannot be used; referential integrity/history is preserved; relevant behavior is tested.

### Phase 4 — Checkout and return

Implement borrowing, due dates, return, copy-status transitions, and activity records. Address concurrent attempts to borrow the last available copy.

**Accept when:** happy paths and rejection cases are tested; no copy can have two active loans; concurrency safety is verified against PostgreSQL where feasible, or its unverified status and reason are explicitly documented.

### Phase 5 — Renewal and reservation queue

Implement the one-renewal policy, eligibility rules, FIFO reservations, copy holds on return, and cancellation/queue advancement.

**Accept when:** eligible/ineligible renewal and queue ordering/cancellation are tested; a held copy cannot be lent to another member; transaction boundaries keep state consistent.

### Phase 6 — History and backend list features

Complete account-scoped activity/history, search/filter, database-backed pagination, and allowlisted sorting for books and loans.

**Accept when:** history follows the lifecycle accurately; pagination/search/sort tests pass; invalid sort fields and invalid paging inputs are safely handled.

### Phase 7 — Dashboard

Implement simple account-scoped inventory, loan, overdue, and reservation counts.

**Accept when:** dashboard values match stored business data and cannot expose another account's counts.

### Phase 8 — Plain web interface

Build the same-origin HTML/CSS/JavaScript screens for authentication and the main workflows, with CSRF-aware API calls and usable error/empty/loading states.

**Accept when:** no committed control is a fake/placeholder; the main user flows work against the API; untrusted text is rendered safely; layout works on a phone-sized viewport.

### Phase 9 — Demo data and reviewer documentation

Add opt-in local/demo idempotent seed data. Complete README with scope, architecture/data model diagrams as useful, run/stop/reset instructions and warning, demo login, principal API examples, tests (including Docker requirements), design trade-offs, and honest limitations.

**Accept when:** a repeat startup does not duplicate seed data; default/demo behavior matches configuration; a reviewer can follow the documented local workflow.

### Phase 10 — End-to-end verification

Run Maven `clean verify`, validate Compose, build/run the stack if Docker is available, inspect health/static page, try demo login and a checkout/return flow, and verify persistence over restart without deleting existing volumes.

**Accept when:** report exact commands and outcomes. If Docker/dependency access prevents a check, identify it as unverified instead of claiming success.

## Current checkpoint

- **Completed:** standalone Git repository initialized; public GitHub repository created; local `origin` configured; branch `main`; this project guide records the agreed context and roadmap.
- **Completed this turn:** Phase 0 implementation, verification, and acceptance checklist; see `PHASE_CHECKLIST.md`.
- **Git history:** `f1143f0` contains the first Phase 0 implementation and is pushed. Java 17 correction and confirmed version-baseline documentation are currently uncommitted/unpushed. Do not commit/push unless the user asks.
- **Next step:** Phase 0 is complete. Wait for the user to explicitly request Phase 1 before implementation.
- **Verification:** After switching to the user-confirmed Java 17 baseline: Maven `clean verify` passed (2 tests, no failures; compiled with release 17); Compose config validation passed; Java 17 container image built and ran; app and PostgreSQL became healthy; readiness reported application and DB UP.
