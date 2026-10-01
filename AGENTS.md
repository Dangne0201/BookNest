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
- No application code exists; build and tests have not been run.
- This file records the agreed project context and plan; it is not authorization to start implementation.
- Do not start coding until the user explicitly asks to begin a phase or implement a specific task.
- The current planned starting point is **Phase 0: project foundation**.

Keep this section accurate as work progresses. After a phase is accepted or completed, update the status and checklist here with what actually changed, what was tested, the exact results, and any remaining work. Never mark work complete based only on intention.

## Goal and mentoring approach

Build a credible junior-level Java portfolio with practical library workflows, secure account-specific data, tests, and a reviewer-friendly local setup. Favor correctness, readable code, and design choices the user can explain over adding technologies for appearance. This is a learning portfolio, not a claim of production readiness.

The user knows Java, REST APIs, Docker, HTML, and CSS. Keep the stack familiar and implement incrementally. Explain important trade-offs plainly when useful; do not hide complexity behind unnecessary abstractions.

## Agreed stack and scope boundaries

- Java 21 LTS, Spring Boot, Spring MVC/REST.
- Spring Security with username/password, server-side session and HTTP-only session cookie; BCrypt password hashing.
- Spring Data JPA/Hibernate and PostgreSQL.
- Maven Wrapper, JUnit, Spring Boot Test, and Mockito when appropriate.
- Plain HTML, CSS, and JavaScript served by Spring Boot from the same origin as the API.
- Dockerfile and Docker Compose for local reviewer setup. A reviewer should not need Java, Maven, or PostgreSQL installed on the host to run the app.
- Flyway is optional only if it keeps schema changes clear and simple.
- Swagger/OpenAPI is not required.

Do not add cloud services, OAuth/Google login, JWT, frontend frameworks, microservices, email, payments, or elaborate role systems. Avoid extra dependencies unless they solve a concrete need and the user agrees where appropriate.

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

## Work protocol

1. Read this file, inspect the current repository, Git status, and relevant existing files before changing anything. Preserve user changes; never overwrite an existing `AGENTS.md` or other file without inspecting and resolving the conflict first.
2. Keep all project work inside `D:\NTGiang\BookNest`; do not inspect or operate on paths or repositories outside this authorized workspace.
3. Implement only the phase/task the user has explicitly authorized. The phase plan below is a roadmap, not permission to do all phases at once.
4. Keep each change set focused. Build/test the smallest relevant scope, fix regressions caused by the change, then report actual outcomes.
5. Do not run destructive commands against containers, volumes, databases, or files. Compose project/resource names must be specific to BookNest; never stop or delete unrelated Docker resources.
6. Do not commit or push unless the user explicitly asks. Do not claim a build, test, Docker run, or manual flow passed unless it was actually run and passed.
7. Ask before materially changing product behavior or expanding scope. For small unspecified details, choose the simplest conventional behavior and document it.
8. Report incomplete work, environmental blockers, and unverified behavior honestly.

## Incremental roadmap and acceptance checks

Complete one phase at a time. A phase is ready for the next only when its acceptance checks have actually been verified. Adjust implementation details to the repository as it grows, but do not silently drop agreed requirements.

### Phase 0 — Project foundation

Create the Spring Boot/Maven Wrapper skeleton, Java 21 configuration, PostgreSQL connectivity, Dockerfile/Compose foundation, basic health check, and a minimal project README. No library workflows yet.

**Accept when:** Maven build passes; Compose configuration validates; app and database start together when Docker is available; app can connect to PostgreSQL; no real secrets are committed; Compose resources are BookNest-specific and scoped safely.

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

Add opt-in local/demo idempotent seed data. Complete README with scope, architecture/data model, run/stop/reset instructions and warning, demo login, principal API examples, tests (including Docker requirements), design trade-offs, and honest limitations.

**Accept when:** a repeat startup does not duplicate seed data; default/demo behavior matches configuration; a reviewer can follow the documented local workflow.

### Phase 10 — End-to-end verification

Run Maven `clean verify`, validate Compose, build/run the stack if Docker is available, inspect health/static page, try demo login and a checkout/return flow, and verify persistence over restart without deleting existing volumes.

**Accept when:** report exact commands and outcomes. If Docker/dependency access prevents a check, identify it as unverified instead of claiming success.

## Current checkpoint

- **Completed:** standalone Git repository initialized; public GitHub repository created; local `origin` configured; branch `main`; this project guide records the agreed context and roadmap.
- **In progress:** none.
- **Next authorized implementation step:** none yet. User asked to prepare a phased plan but has not authorized coding. Wait for an explicit request to begin Phase 0 or another specific task.
- **Tests/build:** none; there is no application code yet.
