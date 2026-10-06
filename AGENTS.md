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
- Phase 1 authentication is implemented, verified, and accepted by the user. The user confirmed one shared library per running application/database; login accounts are staff, all registered staff accounts have equal basic permissions, and `Member` records are borrowers. Both Phase 1 checklists were revised before implementation and updated with verified results.
- Phase 1 verification: `.\mvnw.cmd --no-transfer-progress clean verify` passed (10 tests, 0 failures/errors/skips); `docker compose config --quiet` and `docker compose up -d --build` passed. PostgreSQL 17.11 applied Flyway V1, both services became healthy, readiness reported app and DB UP, the session cookie had HttpOnly/SameSite=Lax, CSRF bootstrap returned 200, anonymous `/api/auth/me` returned 401, and a temporary account completed registration/login/current-account/logout (201/200/200/204) and was removed.
- Phase 2 books and individual copies are implemented, verified, and accepted by the user. All authenticated staff share the same books and copies. Titles have required title/author and optional normalized unique ISBN, genre, publication year, and description; copies have `AVAILABLE`, `MAINTENANCE`, or `RETIRED` status. Available-copy counts are derived, and a title cannot be deleted while it has copies.
- Phase 2 verification: `.\mvnw.cmd --no-transfer-progress clean verify` passed (16 tests, 0 failures/errors/skips); `docker compose config --quiet` passed; `docker compose up -d --build` passed without deleting/resetting the existing volume. PostgreSQL 17.11 applied Flyway V2, app and DB were healthy, readiness was UP/UP, and a live two-account flow verified registration/login, book/copy creation, shared visibility, cross-account status update, and updated availability counts. Temporary accounts/book/copy were removed; a follow-up query confirmed no temporary accounts/books/orphan copies remained.
- The account/books/copies UI slice is implemented, verified, and accepted by the user. It uses same-origin static HTML/CSS/JavaScript, session-cookie authentication and CSRF. It provides registration/login/logout, shared title list/details/forms and per-title copy management, with safe text rendering and responsive layout.
- UI verification: JavaScript syntax checks and `.\mvnw.cmd --no-transfer-progress clean verify` passed (16 tests). Docker Compose rebuilt and served `/`; browser checks exercised registration/login and visible login errors, title create/edit/delete, copy add/status/delete, logout, and malicious-looking title/description strings (rendered as text; no injected elements). At 375px the page had no horizontal overflow. Temporary UI test account and title/copy data were removed; a targeted database query confirmed none remained.
- Phase 3 member backend is implemented, verified, and accepted; user-approved scope includes required full name and optional email/phone/notes, normalized blank optionals, shared CRUD, authentication/CSRF, and deletion of unreferenced records.
- Phase 3 verification: `.\mvnw.cmd --no-transfer-progress clean verify` passed (20 tests, 0 failures/errors/skips); `docker compose config --quiet` passed; Compose rebuilt with existing PostgreSQL volume, applied Flyway V3, and reported app/DB healthy and readiness UP. A live authenticated API flow passed member create/list/update/delete and normalization; temporary member/account records were removed and confirmed absent. Referenced-member deletion cannot yet be exercised because loan/reservation/history tables do not exist; verify restrictive foreign-key behavior when adding those phases.
- The member UI slice is implemented, verified, and accepted by the user. It adds accessible Books/Members tabs and responsive member listing/forms/CRUD using the existing same-origin API and CSRF helper; member-provided content is rendered with safe DOM APIs.
- Member UI verification: all static JS syntax checks passed; full `clean verify` passed (20 tests); `docker compose config --quiet` and `docker compose up -d --build` passed. Browser tests exercised login, tab switching, member create/edit/delete, browser email validation, blank optional fields, a simulated 409 conflict message, safe rendering (0 injected `img`/`script` elements), and 375px layout (document width 360px). Temporary browser-test account/member records were removed and verified absent.
- Phase 4 checkout/return backend is implemented under the approved 14-calendar-day policy. It records checkout/return dates and authenticated staff, derives active/overdue state, transitions copy status, preserves loan history with restrictive references, and serializes checkout on a locked copy with a database uniqueness guard. Renewal and reservation workflows are implemented; dashboard and loan-list search/pagination remain future work.
- Phase 4 verification: `.\mvnw.cmd --no-transfer-progress clean verify` passed (25 tests, 0 failures/errors/skips); `docker compose config --quiet` passed; Compose rebuilt without resetting the PostgreSQL volume, Flyway V4 was confirmed applied, and app/DB/readiness were healthy. Live PostgreSQL API checks confirmed checkout/return dates and staff attribution, a 14-day due date, and parallel checkout yielding exactly one 201 and one 409. Temporary test accounts/member/book/copies/loans were removed and targeted SQL counts confirmed no fixture rows remained. Loan test fixtures now delete dependent loans before referenced staff/member records to honor restrictive foreign keys.

- The user approved the incremental checkout/return UI slice. It adds a third accessible workspace tab, a loan list with active/overdue/returned display, checkout selection limited to available copies, and confirmed return; checkout/return refresh inventory and loan records. Empty-state guidance explains how to create test data in Books and Members; no seed was added.
- Loan UI verification: all static JavaScript syntax checks and `.\mvnw.cmd --no-transfer-progress clean verify` passed (25 tests); `docker compose config --quiet`, rebuild without resetting the database, and app/DB readiness passed. Browser checks registered/logged in a temporary account, created a member/book/available copy through the UI, checked out and returned it (14-day due date; active then returned row; copy availability changed), confirmed non-available copies are excluded/submit disabled, simulated a 409 and network error and checked localized messages, rendered overdue state, checked keyboard tab navigation and no page overflow at a 360px viewport. All temporary account/member/book/copy/loan rows were removed and targeted SQL counts confirmed zero fixtures. UI slice is awaiting user acceptance.
- The user noted the password-change option was missing, approved the bilingual change-password checklist, and chose to end the current session after a successful change and require sign-in again. The feature is implemented and verified, awaiting user acceptance; other active sessions are not tracked/revoked.
- The user then confirmed a product-model change: patrons should self-register and use borrowing/reservations in the shared public library; staff manage the library, with a highest-level admin. The user accepted no-email account recovery by admin verification and a one-time temporary password, plus host/server operational recovery for admin; never use fixed `admin/admin`. A bilingual proposed checklist is appended to `PHASE_CHECKLIST.md` and `PHASE_CHECKLIST.vi.md`; detailed product decisions and user approval are still pending. Do not implement this redesign until the detailed checklist is agreed.
- Password-change verification: `.\mvnw.cmd --no-transfer-progress clean verify` passed (29 tests, 0 failures/errors/skips); all static JavaScript syntax checks, Compose config, rebuild without resetting PostgreSQL, and app/DB readiness passed. Browser tests exercised password-change UI, mismatch and incorrect-current-password errors, successful change with current-session invalidation, new-password sign-in and old-password rejection, and 360px layout without page overflow. Tests also confirmed BCrypt hash update affects only the current account; anonymous/CSRF/input-policy behavior and session invalidation are covered by automated tests. No password/hash browser storage or request-body logging was added. Temporary browser accounts were removed and confirmed absent.
- Phase 5 renewal is implemented under the user-approved policy of one renewal adding 14 calendar days to the current due date. Patrons can renew only their own eligible loans; staff/admin can renew operationally. Renewal is blocked for returned, overdue, already-renewed, or actively queued/held titles. The API stores renewal timestamp/actor and reports `renewalEligible`, which hides the UI action when known to be unavailable. Loan/book locking serializes renewal against queue changes.
- Phase 5 verification: `.\mvnw.cmd --no-transfer-progress clean verify` passed 51 tests (0 failures/errors/skips); JavaScript syntax checks, `git diff --check`, and `docker compose config --quiet` passed. Compose rebuilt against the existing PostgreSQL volume; Flyway V8 succeeded and readiness returned HTTP 200 with app/DB UP. Database counts after the UI flow: 6 accounts, 4 books, 9 copies, 4 members, 5 loans, 6 reservations, and exactly 1 renewed loan. Browser confirmed renewal from 2026-10-17 to 2026-10-31, actor/status persisted after app restart and re-login, and no renewal action remained. Automated H2 concurrency tests cover simultaneous renewals and renewal versus queue insertion; those specific races were not separately run against PostgreSQL. The previously observed 360px account-bar overflow was fixed in Phase 8; loan tables remain horizontally scrollable inside their wrappers. The user accepted Phase 5 on 2026-10-06; commit and push are authorized.
- Phase 6 activity history and database-backed list discovery were accepted by the user on 2026-10-06 and published in commit `a100990`. Books support case-insensitive title/author/ISBN search, genre/availability filters, allowlisted sort, and bounded paging; loans support search/status filters/sort/page while preserving patron ownership; activity history records loan checkout/return/renewal and reservation placement/hold/cancel/fulfill with server-derived actors and safe snapshots. History starts at V9; existing loan records retain older loan history, and past reservation events were not fabricated.
- Phase 6 verification: `.\mvnw.cmd --no-transfer-progress clean verify` passed 59 tests (0 failures/errors/skips). JavaScript syntax checks, `git diff --check`, and `docker compose config --quiet` passed. Compose rebuilt against the existing PostgreSQL volume, Flyway V9 applied, and readiness returned HTTP 200 with app/DB UP. PostgreSQL catalog/search, loan paging, and activity queries passed; the existing row counts were preserved at 6 accounts, 4 books, 9 copies, 4 members, 5 loans, and 6 reservations. Browser checks exercised the public catalog, staff book/loan filters, patron-only loan scope and activity access, and empty history state. The previously observed overall 360px overflow was fixed in Phase 8. Phase 6 was accepted and pushed as `a100990`.
- Phase 7 dashboard was accepted by the user and published on 2026-10-06. It exposes shared counts for available copies, active loans, overdue loans, and active reservations to STAFF/ADMIN only; patrons retain their personal loan/reservation views. The API uses database count queries, and the UI refreshes after relevant operations. `.\mvnw.cmd --no-transfer-progress clean verify` passed 62 tests (0 failures/errors/skips); JavaScript syntax checks, `git diff --check`, and `docker compose config --quiet` passed. Compose rebuilt without resetting the existing PostgreSQL volume; readiness returned HTTP 200 with app/DB UP, and V9 remains the latest migration. Browser STAFF values and the API response (2 available copies, 3 active loans, 1 overdue loan, 3 active reservations) matched direct PostgreSQL counts; anonymous access returned 401 and PATRON access returned 403 with the dashboard hidden.
- Phase 8 UI polish was accepted by the user on 2026-10-06 and published to GitHub, with explicit acceptance of the remaining verification limitations. The responsive header now wraps account controls on mobile, and workspace tabs wrap at narrow widths; the existing book search/reset/pagination handlers are registered once rather than on every list fetch. Full `.\mvnw.cmd --no-transfer-progress clean verify` passed 62 tests (0 failures/errors/skips), every static JS file passed `node --check`, `git diff --check` and Compose config passed, Compose rebuilt, and app/DB readiness returned UP. Browser checks measured no document overflow at 360/375/768/1280px for STAFF and 360px for PATRON; wide tables stayed within internal scroll wrappers. STAFF and PATRON navigation/visibility, profile dialog, search (one request), a simulated 503 error state, and staff denial from `/api/admin/accounts` (403) were checked. PostgreSQL counts remained 6/4/9/4/5/6; no rows or volume were changed. A live ADMIN UI journey and comprehensive keyboard-only dialog/focus/confirmation behavior were not verified. Demo intentionally has no admin and no admin credential was rotated.

Keep this section accurate as work progresses. After a phase is accepted or completed, update the status and checklist here with what actually changed, what was tested, the exact results, and any remaining work. Never mark work complete based only on intention.

## Goal and mentoring approach

Build a credible junior-level Java portfolio with practical shared-library workflows, secure staff accounts, tests, and a reviewer-friendly local setup. Favor correctness, readable code, and design choices the user can explain over adding technologies for appearance. This is a learning portfolio, not a claim of production readiness.

The idea started as a request for a Java portfolio with a different concept from the user's JavaScript Task Manager learning project. The user chose BookNest, a shared-library management application, to practice Java while building something with real business rules rather than a basic CRUD showcase.

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

BookNest has **one shared library and one shared inventory per running application/database**. All authenticated staff accounts use the same domain records. If three copies of a title are all borrowed or held, every authenticated account sees that no copies are available; one account's checkout changes the availability seen by all other accounts. Separate deployments/databases are separate installations and do not share inventory.

Login accounts represent staff/operators, not patrons. `Member` records represent the people who borrow books; members do not log in. Every successfully registered staff account has the same basic permissions. Do not introduce a `Library` table or account-owned copies of domain records for the current one-library scope; revisit this only if the user asks for multiple libraries or tenants.

Business records include books, individual copies, members, loans, reservations, and activity history. They belong to the one shared library, not to the staff account that created them.

### Accounts and shared access

- Current implementation is the legacy Phase 1 model: public registration creates a staff account and all authenticated accounts have the same authority. The user's approved target direction is a shared public library with distinct `ADMIN`, `STAFF`, and `PATRON` roles; do not treat legacy behavior as the final product model.
- Target behavior: patrons self-register, receive a linked borrower profile, and use self-service checkout/reservations. Staff manage library records and verify returns; admin manages staff/accounts and recovery. The detailed role matrix/checklist is pending user approval.
- Resolve authenticated account and acting identity from Spring Security's server-side context/session, never from client-supplied actor/user/role/member IDs.
- Keep shared books and copies common to the library, while restricting patron loan/reservation and personal information to the owning patron. Enforce authorization in backend services/controllers; hiding UI controls is not authorization.
- Anonymous access should be limited to the approved public catalog, registration, login, CSRF bootstrap, static assets, and safe health endpoints. Do not expose personal or transaction data.
- Public registration must never grant staff/admin permissions. Staff accounts are provisioned by admin under the new target model; no fixed/default admin password.

### Books, copies, members, loans

- A `Book` represents a title (for example title, author, genre, and relevant bibliographic details).
- A `BookCopy` represents an individual physical copy with an availability/loan/reservation status.
- In the current implementation, a `Member` is managed by staff and is not a login account. The approved target direction links each self-registered `PATRON` account to a Member profile; keep login credentials separate from member details and preserve historical references during migration.
- A `Loan` records its copy, member, checkout date, due date, return date, renewal state, and history after return.
- Do not allow borrowing when no copy is available, borrowing a missing member/copy, double lending, or returning a missing/already-returned loan.
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
- Provide a simple shared-library dashboard with useful counts such as available, loaned, overdue, and queued items.

### User interface

- Use plain HTML/CSS/JavaScript, with a clean, readable, reasonably responsive layout.
- Cover authentication, overview, books/copies, members, loans/returns/renewals, reservations/queue, and list search/filter/pagination/sorting.
- Include loading, empty, success, error, and important-action confirmation states.
- Use safe DOM APIs such as `textContent`; do not render untrusted user input as HTML.
- Keep frontend and API same-origin. JavaScript must obtain and send the CSRF token correctly for state-changing requests.

### Demo data

- Provide one demo account and one shared set of sample books/copies only in explicitly enabled local/demo configuration.
- Seed idempotently; restarting must not create duplicates.
- Seed the shared library once; do not create duplicate demo inventories for each staff account.
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
6. After the user explicitly accepts a completed phase, verify its final state, commit the phase changes, and push the commit to the configured GitHub remote. Do not commit or push work for a phase that is still in progress or awaiting acceptance. Do not claim a build, test, Docker run, or manual flow passed unless it was actually run and passed.
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

### Phase 1 — Authentication and shared-library access

Implement registration, login/logout, BCrypt, session cookie, CSRF, current staff-account lookup, and the shared-library access model. Every registered account receives the same basic staff permission; accounts identify who performed a request but do not own separate copies of books or business data.

**Accept when:** authentication behavior is tested; unauthenticated users cannot call business APIs; passwords are never stored/returned in clear text; registered accounts receive equal permissions; the shared-library model is documented without per-account domain-data isolation.

### Phase 2 — Books and individual copies

Implement shared book and copy management, statuses, validation, persistence constraints, and focused tests. Every authenticated staff account sees the same inventory and availability.

**Accept when:** create/list/update/delete and copy status behavior work; one account's changes to books/copies and stock availability are visible to other authenticated accounts; anonymous access is rejected; invalid data and referenced records are handled safely.

### Phase 3 — Members

Implement shared member management and rules for deletion when loans or history reference a member.

**Accept when:** all authenticated staff accounts see the same members; referential integrity/history is preserved; relevant behavior is tested.

### Phase 4 — Checkout and return

Implement borrowing, due dates, return, copy-status transitions, and activity records. Address concurrent attempts to borrow the last available copy.

**Accept when:** happy paths and rejection cases are tested; no copy can have two active loans; concurrency safety is verified against PostgreSQL where feasible, or its unverified status and reason are explicitly documented.

### Phase 5 — Renewal and reservation queue

Implement the one-renewal policy, eligibility rules, FIFO reservations, copy holds on return, and cancellation/queue advancement.

**Accept when:** eligible/ineligible renewal and queue ordering/cancellation are tested; a held copy cannot be lent to another member; transaction boundaries keep state consistent.

### Phase 6 — History and backend list features

Complete shared-library activity/history (including the acting staff account), search/filter, database-backed pagination, and allowlisted sorting for books and loans.

**Accept when:** history follows the lifecycle accurately; pagination/search/sort tests pass; invalid sort fields and invalid paging inputs are safely handled.

### Phase 7 — Dashboard

Implement simple shared-library inventory, loan, overdue, and reservation counts.

**Accept when:** dashboard values match the shared library's business data and are the same for staff accounts with equal permissions.

### Phase 8 — Plain web interface

The original schedule placed all UI work here, after the backend phases. The user chose an incremental approach instead: after the relevant backend phase is implemented and accepted, prepare a bilingual checklist and add its corresponding UI slice before moving on where practical. Do not wait until Phase 8 to begin all UI work. Use the same-origin HTML/CSS/JavaScript stack, CSRF-aware API calls, and usable error/empty/loading states. Phase 8 now means completing and polishing the remaining integrated screens, responsive behavior, and end-to-end flows.

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
- **Git history:** The shared-library workflow and accepted demo-data slice are committed and pushed; the demo slice is `a64bf11`. Phase 5 renewal is accepted and publication is authorized under the user's standing instruction to push accepted phases.
- **Current step:** Demo data, Phase 5 renewal, Phase 6 activity/search, Phase 7 dashboard, and Phase 8 UI polish are accepted and published. Phase 7 is commit `81c7aa0`; Phase 8 is `c6d7205` and was accepted with the documented ADMIN UI and keyboard-dialog verification limitations. Phase 9 documentation is implemented and verified, awaiting user acceptance; do not commit/push it before acceptance.
- **Current implementation:** An opt-in demo initializer adds three login accounts (`STAFF` and two `PATRON`), three linked/walk-in member profiles, three sample books, nine copies across available/on-loan/on-hold/maintenance/retired states, three loans (active, overdue, returned), and four reservation states. The option defaults off; Flyway V7 adds a seed-completion marker. No admin is created. Existing data and later user edits are preserved; public demo credentials are documented as local-only.
- **Latest demo-data verification:** The demo-data slice passed 45 tests, Compose config/build, V6→V7 migration, restart-persistence checks, HTTP 200 readiness, and browser role/access flows as detailed above. Demo initialization is disabled in the running Compose app. No volume, database, or unrelated Docker resource was deleted or reset.
- **Latest Phase 5 verification:** `.\mvnw.cmd --no-transfer-progress clean verify` passed 51 tests with 0 failures/errors/skips; JavaScript syntax checks, `git diff --check`, Compose config/build, V8 migration, and HTTP 200 app/DB readiness passed. Browser confirmed prompt, exact 14-day due-date extension, renewal actor, persistence after app restart/re-login, and removal of the action afterward. Database inspection found one renewed loan and preserved the existing 6/4/9/4/5/6 accounts/books/copies/members/loans/reservations. Full page width at 360px remains affected by an existing signed-in account-bar overflow; renewal did not change header CSS.
- **Phase status:** Demo-data, Phase 5 renewal, and Phase 6 are accepted and published as requested. The signed-in mobile account-bar overflow is pre-existing and tracked separately from Phase 6.
- **Latest Phase 7 verification:** Full `clean verify` passed 62 tests with 0 failures/errors/skips; JavaScript syntax checks, `git diff --check`, Compose config/build, readiness, browser role visibility, HTTP authorization (anonymous 401, patron 403), and API-to-PostgreSQL count comparison passed. Existing PostgreSQL data/volume was preserved. Phase 7 was accepted and published.
- **Phase 8 verification:** English/Vietnamese checklists record the responsive/integrated-UI scope, per-file ledger, 62-test Maven result, JS/Compose checks, browser role/viewport checks, unchanged PostgreSQL counts, and the two remaining verification limitations. The phase was accepted and published.
- **Phase 9 verification:** README now covers reviewer setup, architecture/data relationships, roles, local demo seeding and credentials, admin recovery, normal shutdown and explicitly warned destructive reset, representative CSRF-aware API examples, pagination, tests, design decisions, and limitations. `git diff --check` and `docker compose config --quiet` passed. This was documentation-only; no code/test suite, credentials, seed, DB records, containers, or volumes were changed. It awaits user acceptance.
