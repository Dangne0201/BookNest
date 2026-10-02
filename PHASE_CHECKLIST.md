# Phase acceptance checklists

Use these checklists to review each phase. A box should be marked complete only after the item has been checked against the implementation or command output. If an item cannot be verified, leave it unchecked and explain why in `AGENTS.md`.

**User-confirmed versions:** Java 17, Spring Boot 4.0.8, PostgreSQL 17, Maven 3.9.16, and Maven Wrapper 3.3.4. Ask before changing this baseline.

## Phase 0 — Project foundation

- [x] Spring Boot skeleton is configured for Java 17.
- [x] Maven Wrapper scripts and wrapper configuration are present.
- [x] PostgreSQL connection settings are externalized through environment variables.
- [x] Docker Compose defines the app and PostgreSQL with a BookNest-specific named volume.
- [x] PostgreSQL has a health check and the app waits for it to become healthy.
- [x] App port is bound to loopback; PostgreSQL is not published to the host.
- [x] A multi-stage Dockerfile builds with Java 17 and runs as a non-root user.
- [x] Actuator readiness health includes the database health indicator.
- [x] A README explains local startup, health URL, shutdown, test command, and current limitations.
- [x] Local-only environment values are documented; `.env` is ignored and `.env.example` contains no real secrets.
- [x] After correcting to Java 17, `.\mvnw.cmd --no-transfer-progress clean verify` passes on host Temurin Java 25 (main/test compiled with `release 17`; 2 tests pass).
- [x] `docker compose config --quiet` passes.
- [x] Java 17 Docker image builds and the app/database start successfully (`docker compose up -d --build`).
- [x] Running readiness health check reports both app and database as healthy (`status=UP`, `components.db.status=UP`); container `java -version` reports Temurin 17.0.20.1.
- [x] Changes are limited to the BookNest repository.
- [x] Removed the explicitly tagged Java 21 build/runtime images and generated `target/` output after correcting the version baseline; did not prune unrelated Docker resources.

Docker Hub image pulls initially failed because of a local registry authentication error. The same official image tags were pulled from the public Amazon ECR mirror and tagged in the local Docker cache for verification; project files still reference the standard official image names.

### Phase 0 file ledger

These files were created or changed during Phase 0. The ledger is retrospective; the Phase 0 checklist itself was added after implementation began, which did not follow the user's requested process. For all future phases, prepare that phase's checklist before implementation and update this ledger as each file is completed.

- [x] `pom.xml` — Spring Boot dependencies and Java 17 build configuration.
- [x] `.gitignore` — ignore build artifacts and local environment files.
- [x] `.gitattributes` — line-ending attributes.
- [x] `.env.example` — documented local-only Compose configuration.
- [x] `.mvn/wrapper/maven-wrapper.properties` — Maven Wrapper distribution configuration.
- [x] `mvnw` and `mvnw.cmd` — Unix and Windows Maven Wrapper launchers.
- [x] `src/main/java/com/booknest/BookNestApplication.java` — Spring Boot application entry point.
- [x] `src/main/resources/application.yml` — PostgreSQL and health configuration.
- [x] `src/test/java/com/booknest/BookNestApplicationTests.java` — context and readiness smoke tests.
- [x] `src/test/resources/application-test.yml` — isolated H2 test profile.
- [x] `Dockerfile` — multi-stage Java 17 image build and non-root runtime.
- [x] `compose.yaml` — app/database services, health checks, loopback binding, and dedicated volume.
- [x] `README.md` — Phase 0 scope and local run/test instructions.
- [x] `AGENTS.md` — shared project context, phase status, and agent workflow rules.
- [x] `PHASE_CHECKLIST.md` — phase acceptance checks and file ledger.
- [x] `pom.xml`, `Dockerfile`, `README.md`, `AGENTS.md`, `PHASE_CHECKLIST.md`, and `PHASE_CHECKLIST.vi.md` — corrected project baseline from Java 21 to Java 17 per user clarification; Maven tests and Java 17 Docker build/runtime were rerun successfully.

## Later phases

### Phase 1 — Authentication and shared-library access (revised before implementation)

#### Acceptance checks

- [x] Product model confirmed: one shared library per running application/database; login accounts are staff/operators, `Member` records are borrowers, and all registered accounts receive equal basic permissions.
- [x] Registration creates a staff account using a unique username and BCrypt-hashed password; password/hash is never returned.
- [x] Login/logout use Spring Security server-side sessions and cookies; no JWT or custom authentication implementation.
- [x] CSRF protection remains enabled; the browser can obtain the token before login and unsafe requests without a valid token are rejected.
- [x] Anonymous requests cannot access protected API endpoints; only registration, login, CSRF bootstrap, static assets, and explicitly safe health endpoints are public.
- [x] Current staff identity is resolved from Spring Security, never from a client-supplied actor/account ID.
- [x] Every registered account receives the same basic authority; no role/permission hierarchy is introduced.
- [x] No per-account `Library` or ownership boundary is added to shared books, copies, members, loans, reservations, or history.
- [x] Authenticated accounts do not receive separate inventories; tests verify authentication context/authority is equivalent for independently registered accounts. Shared inventory visibility and stock transitions will receive domain tests in Phase 2.
- [x] Username uniqueness is enforced in both application behavior and the database; relevant safe error responses are tested.
- [x] `.\mvnw.cmd --no-transfer-progress clean verify` passes: 10 tests, 0 failures/errors/skips. `docker compose config --quiet` and `docker compose up -d --build` passed; PostgreSQL 17.11 applied Flyway V1, app and DB became healthy, readiness was UP, CSRF/session-cookie checks passed, anonymous `/api/auth/me` returned 401, and a temporary registration/login/me/logout flow returned 201/200/200/204. The temporary account was removed.
- [x] Both bilingual per-file ledgers below were kept in sync as implementation files were completed.

#### Planned file ledger (revise before adding an unplanned file)

- [x] `pom.xml` — add Spring Security, Spring Boot Flyway/PostgreSQL migration support, and Spring Security test dependencies.
- [x] `src/main/java/com/booknest/account/StaffAccount.java` — login identity only; does not own shared library records.
- [x] `src/main/java/com/booknest/account/StaffAccountRepository.java` — username lookup and unique persistence.
- [x] `src/main/java/com/booknest/account/RegistrationRequest.java` — validate username/password inputs.
- [x] `src/main/java/com/booknest/account/StaffAccountResponse.java` — expose only account ID and username.
- [x] `src/main/java/com/booknest/account/StaffAccountAlreadyExistsException.java` — represent a duplicate normalized username.
- [x] `src/main/java/com/booknest/account/AccountService.java` — registration, normalized username uniqueness, account lookup, BCrypt encoding.
- [x] `src/main/java/com/booknest/account/AccountController.java` — registration, current staff identity, CSRF bootstrap; login/logout are handled by Spring Security filters.
- [x] `src/main/java/com/booknest/web/ApiExceptionHandler.java` — safe validation/conflict responses; extended in Phase 2 for malformed JSON and safe not-found/business-conflict errors.
- [x] `src/main/java/com/booknest/security/SecurityConfig.java` — session-based authentication, CSRF, public/protected routes, shared staff authority.
- [x] `src/main/resources/db/migration/V1__create_staff_accounts.sql` — create the shared application's staff login table with a database-enforced unique username.
- [x] `src/main/resources/application.yml` — configure an HTTP-only same-site session cookie and session timeout; no secrets.
- [x] `src/test/java/com/booknest/account/AccountControllerTests.java` — registration, duplicate username, BCrypt persistence, safe response, and equal staff authority tests.
- [x] `src/test/java/com/booknest/security/SecurityConfigurationTests.java` — session login/logout, CSRF, and anonymous rejection tests.
- [x] `src/test/resources/application-test.yml` — use the shared migration and Hibernate validation in the H2 test profile.
- [x] `AGENTS.md` — record confirmed shared-library behavior and Phase 1 authorization/checkpoint.
- [x] `README.md` — describe one shared library, staff login accounts, and borrower `Member` records.
- [x] `PHASE_CHECKLIST.md` — replace obsolete tenant-isolation acceptance criteria and prepare the Phase 1 implementation ledger.
- [x] `PHASE_CHECKLIST.vi.md` — keep the Vietnamese Phase 1 checklist aligned with the English version.
- [x] `README.md` — document verified Phase 1 API behavior, current limitations, and test/run instructions.

**State:** Phase 1 implementation, verification, and user acceptance are complete. The shared-library model, equal staff permissions, and `Member` borrowers were confirmed before implementation. Phase 2 was started after the user's explicit request and has since been accepted.

### Phase 2 — Books and individual copies

The user reviewed and approved this scope before Phase 2 implementation. The checks below record verified implementation and user acceptance.

#### Approved scope

- Manage book titles and their individual physical copies in the one shared library.
- Every authenticated staff account can create, read, update, and manage the same inventory; no account ownership fields or per-account filtering.
- A book title represents bibliographic information; each physical copy is a separate record linked to exactly one title.
- Proposed initial book fields: required title and author; optional ISBN, genre, publication year, and description. Enforce sensible length/range validation and normalize optional ISBN before uniqueness checks.
- Proposed physical-copy statuses: `AVAILABLE`, `MAINTENANCE`, and `RETIRED`. Only `AVAILABLE` copies count as available to borrow; loan/reservation states are deferred to later phases.
- A title cannot be deleted while it has copies; do not cascade-delete inventory. A copy can be removed only when doing so does not violate references or inventory rules.
- Search, filtering, database pagination/sorting, members, loans, reservations, dashboard, and UI are out of scope for this phase.

#### Acceptance checks

- [x] User reviewed and approved the proposed book fields, copy statuses, and deletion rules before implementation.
- [x] Authenticated staff can create, list, view, update, and safely delete book titles; validation and missing-record errors have safe, consistent API responses.
- [x] Authenticated staff can add, list, update status, and safely remove individual copies belonging to a title; copies cannot refer to a missing title.
- [x] Database constraints enforce required fields, foreign-key integrity, and uniqueness of non-null ISBN values.
- [x] Available-copy counts are derived consistently from copy records and status; only `AVAILABLE` copies are counted.
- [x] A second authenticated staff account sees the first account's title, copy, status changes, and availability; no per-account inventory is created.
- [x] Anonymous requests cannot read or modify book/copy business APIs; write requests require the existing CSRF protection.
- [x] Tests cover successful operations, validation, duplicate ISBN, nonexistent references, invalid status, deletion constraints, authorization, and shared visibility.
- [x] Existing Phase 0/1 behavior remains intact; `.\mvnw.cmd --no-transfer-progress clean verify` passes (16 tests, 0 failures/errors/skips).
- [x] Migration V2 and the application are verified against the existing Docker Compose PostgreSQL database without removing/resetting its volume; live tests confirmed two staff accounts shared title/copy/status/count changes.
- [x] Both bilingual file ledgers are kept in sync and updated as each file is completed.

#### Anticipated file ledger (revise both checklists before adding an unplanned file)

- [x] `src/main/java/com/booknest/book/Book.java` — shared title/bibliographic entity and validated fields.
- [x] `src/main/java/com/booknest/book/BookCopy.java` — individual physical copy, title relationship, and status.
- [x] `src/main/java/com/booknest/book/BookRepository.java` — persistence and title lookups.
- [x] `src/main/java/com/booknest/book/BookCopyRepository.java` — copy persistence and available-copy counts.
- [x] `src/main/java/com/booknest/book/BookRequest.java` and `BookResponse.java` — validated title input and safe API representation including derived copy counts.
- [x] `src/main/java/com/booknest/book/BookCopyRequest.java` and `BookCopyResponse.java` — validated copy operations and safe API representation.
- [x] `src/main/java/com/booknest/book/BookService.java` — title operations, ISBN normalization/uniqueness, derived inventory counts, and safe deletion behavior.
- [x] `src/main/java/com/booknest/book/BookCopyService.java` — copy operations, status changes, and parent/reference checks.
- [x] `src/main/java/com/booknest/book/BookController.java` and `BookCopyController.java` — REST endpoints protected by the existing authenticated `/api/**` security rule.
- [x] `src/main/resources/db/migration/V2__create_books_and_copies.sql` — schema, foreign key, year/status checks, ISBN uniqueness, and copy lookup index.
- [x] `src/test/java/com/booknest/book/BookControllerTests.java` and `BookCopyControllerTests.java` — API, validation, access, ISBN normalization, deletion constraints, copy counts, and cross-staff shared-inventory tests.
- [x] `README.md` — document Phase 2 endpoints, copy statuses, deletion rules, and current scope.
- [x] `AGENTS.md` — record Phase 2 decisions, implementation state, verification, and remaining work.
- [x] `PHASE_CHECKLIST.md` and `PHASE_CHECKLIST.vi.md` — keep the reviewed scope, acceptance results, and per-file ledgers aligned.

**State:** Phase 2 implementation, verification, and user acceptance are complete.

### Incremental UI slice — Account access and books/copies

This UI slice follows the user's decision to build the interface incrementally after its backend is accepted. The user approved this scope before implementation. The checks below record implementation verification and subsequent user acceptance.

#### Approved scope

- Use the existing same-origin Spring Boot app and plain HTML, CSS, and JavaScript; do not add a frontend framework or separate server.
- Provide a minimal registration/login/logout interface so staff can establish a session and use the protected books/copies screens in a browser.
- Provide a shared book inventory screen with list and derived total/available-copy counts; create, edit, view details, and delete a title subject to the existing no-copies rule.
- Provide copy management for a selected title: list copies, add a copy, change status, and remove a copy.
- Obtain CSRF token from the existing endpoint and send it with every state-changing request; rely on the HTTP-only session cookie rather than reading it from JavaScript.
- Keep the UI limited to authentication and books/copies. Member, loan, reservation, dashboard, advanced search/filter/paging, and a polished full-application shell remain later work.

#### Acceptance checks

- [x] User reviewed and approved the proposed browser UI scope before implementation.
- [x] The app root serves a usable, responsive BookNest page from the same-origin Spring Boot app.
- [x] A staff member can register, log in, see the current identity, and log out through the interface; errors are understandable and no password is persisted in browser storage.
- [x] Authenticated staff can list and inspect shared titles and copy counts; create/update/delete titles through forms with validation and clear success/error states.
- [x] Authenticated staff can list/add/update/remove copies for a title; status labels are understandable and available counts update after changes.
- [x] CSRF bootstrap/session handling works across login and logout; unsafe API requests send the returned CSRF header/token and anonymous business access is not exposed by the UI.
- [x] Untrusted book/member text is inserted with safe DOM APIs such as `textContent`; a browser check confirmed malicious-looking title/description strings remain text and create no injected elements.
- [x] Loading, empty, validation, network/API error, success, and destructive-action confirmation states are present; no fake controls or placeholder actions.
- [x] Core screens remain usable at desktop and phone widths and have labels/keyboard-operable controls; a 375px viewport had no horizontal page overflow.
- [x] Existing backend behavior remains intact; `.\mvnw.cmd --no-transfer-progress clean verify` passes (16 tests) and registration/login/book/copy/status/delete/logout flows were exercised in the running Compose app.
- [x] Both bilingual UI checklists and their per-file ledgers are kept in sync as each file is completed.

#### Anticipated file ledger (revise both checklists before adding an unplanned file)

- [x] `src/main/resources/static/index.html` — accessible app shell/landmarks, authentication forms and validation regions, labelled dialogs, title-detail and copy views/forms.
- [x] `src/main/resources/static/css/styles.css` — responsive styling, status indicators, and focus states using local system fonts.
- [x] `src/main/resources/static/js/api.js` — same-origin requests, CSRF bootstrap/header handling, and safe API error parsing.
- [x] `src/main/resources/static/js/auth.js` — registration/login/logout and current-session UI.
- [x] `src/main/resources/static/js/books.js` — title/copy loading, detail views, forms, status actions, confirmations, and safe DOM rendering.
- [x] `src/main/resources/static/js/app.js` — initialize page behavior, coordinate UI modules, and supply auth notifications.
- [x] `README.md` — document the browser entry point, first-use registration, verified UI scope, and current limitations.
- [x] `AGENTS.md` — record the approved UI scope, files, test/manual verification, and checkpoint.
- [x] `PHASE_CHECKLIST.md` and `PHASE_CHECKLIST.vi.md` — keep the UI scope, results, and file ledgers aligned.

**State:** The approved account/books/copies UI slice is implemented, verified, and accepted by the user. Do not start the next UI/backend slice until the user explicitly asks to continue.

### Phase 3 — Members (approved; implementation verified)

This backend phase is the next roadmap item. Keep member UI out of this phase; after the member API is implemented and accepted, prepare a separate bilingual checklist for its UI slice before coding it.

#### Proposed scope

- Manage borrower/member records in the same shared library; members are not login accounts.
- A member has a required full name and optional email address, phone number, and notes. Do not require email or phone uniqueness because contact details may be shared.
- Provide authenticated API operations to create, list, view, update, and delete members. Normalize surrounding whitespace and represent blank optional fields as null.
- Allow hard deletion only while a member has no loan, reservation, or activity-history references. Do not cascade-delete or erase lending history. Since those records are introduced in later phases, define and test the deletion behavior when those references are added; use restrictive database foreign keys and a safe conflict response.
- Keep search/filter/pagination, loan workflows, reservation workflows, history screens, and member UI out of this backend phase.
- Use the existing Java 17/Spring Boot/PostgreSQL/Flyway/Maven stack, session authentication, CSRF, validation, error handling, and shared-library model. Add no new framework or service.

#### Acceptance checks

- [x] User reviewed and approved the proposed member fields, optional contact semantics, and delete/history rule before implementation.
- [x] Authenticated staff can create, list, view, update, and delete unreferenced members through a documented REST API; tests confirm all staff accounts see the same records.
- [x] Member name is required and bounded; optional email, phone, and notes are validated and bounded. Whitespace is normalized and blank optional values are stored/returned as null.
- [x] Database constraints enforce required fields and lengths; member data does not include staff login credentials or account ownership.
- [x] Anonymous requests cannot use member business endpoints; write requests require CSRF.
- [x] Invalid input and missing member IDs are handled with safe, consistent API responses and suitable status codes.
- [ ] When a member is referenced by a loan, reservation, or history record, deletion must return a safe conflict response and preserve all related history. Verify when those references are implemented.
- [ ] Member deletion never cascades or destroys loans, reservations, or activity history. Future references must use restrictive foreign keys and reject deletion with a conflict; verify this rule when those referencing phases are implemented.
- [x] Tests cover CRUD, validation/normalization, not-found behavior, authorization/CSRF, shared visibility between staff accounts, and deletion of unreferenced members. Referenced-member behavior is deferred until the loan/history schema exists.
- [x] Existing Phase 0–2 and account/books UI behavior remains intact; `.\mvnw.cmd --no-transfer-progress clean verify` passes (20 tests, 0 failures/errors/skips).
- [x] Flyway V3 and live member CRUD/normalization are checked against the existing Compose PostgreSQL database without deleting/resetting its volume; app and DB are healthy and readiness is UP.
- [x] Both bilingual checklists and their anticipated file ledgers remain aligned.

**Verification note:** Member loans, reservations, and activity history do not exist yet, so there are currently no member references to exercise. Hard deletion works for an unreferenced member. Preserve the member row and reject deletion via restrictive foreign keys when those future relationships are introduced; that referenced-delete case remains a later verification item.

#### Anticipated file ledger (revise both checklists before adding an unplanned file)

- [x] `src/main/java/com/booknest/member/Member.java` — shared borrower entity with validated member/contact fields.
- [x] `src/main/java/com/booknest/member/MemberRepository.java` — persistence and deterministic list/read queries.
- [x] `src/main/java/com/booknest/member/MemberRequest.java` and `MemberResponse.java` — validated input and safe API representation.
- [x] `src/main/java/com/booknest/member/MemberService.java` — normalized CRUD and safe delete behavior.
- [x] `src/main/java/com/booknest/member/MemberController.java` — authenticated REST endpoints for member operations.
- [x] `src/main/resources/db/migration/V3__create_members.sql` — member schema and database constraints.
- [x] `src/test/java/com/booknest/member/MemberControllerTests.java` — API, validation, access, shared visibility, and deletion-policy tests.
- [x] `README.md` — document member fields, API endpoints, deletion/history behavior, and current scope.
- [x] `AGENTS.md` — record approved member decisions, implementation status, verification, and remaining work.
- [x] `PHASE_CHECKLIST.md` and `PHASE_CHECKLIST.vi.md` — maintain matching acceptance results and file ledger.

**State:** Member backend implementation, verification, and user acceptance are complete. The referenced-member deletion case remains for verification when loan/reservation/history phases add those relationships. Member UI is a separate incremental slice and requires its own approved bilingual checklist before implementation.

### Incremental UI slice — Members (approved; implementation verified)

This slice adds browser access to the accepted member API. Do not change UI code until the user approves this checklist.

#### Proposed scope

- Extend the existing authenticated workspace with a simple **Books / Members** navigation; keep the current books/copies UI and behavior intact.
- Provide a shared member list and create/edit/delete flows using the existing `/api/members` endpoints and same-origin `apiRequest` helper.
- Member forms use required full name and optional email, phone, and notes; apply client-side constraints consistent with the backend and explain optional/blank contact fields.
- Render all member-provided content safely with DOM APIs and `textContent`. Show loading, empty, validation, API/network error, success, and delete-confirmation states.
- Preserve the existing session-cookie/CSRF behavior; do not read the HTTP-only cookie or store member data/passwords in browser storage.
- Keep search, filtering, pagination, member loans, reservations, history, and dashboard out of this UI slice.

#### Acceptance checks

- [x] User reviewed and approved the member UI scope before implementation.
- [x] Authenticated staff can switch between Books and Members without breaking or hiding existing book/copy workflows; the member view is usable on desktop and phone widths.
- [x] Staff can list shared members, create/update members with required name and optional contact/notes fields, and delete unreferenced members through the UI.
- [x] The UI trims values consistently with the API and handles blank optional fields as null; browser validation and API errors are understandable.
- [x] Delete requires confirmation; a 409 conflict response is safely shown in the UI. The actual referenced-member conflict waits for future loan/history relationships.
- [x] Untrusted name/contact/notes content is rendered as text, not HTML; no use of `innerHTML`, `outerHTML`, or browser storage for this feature. Browser DOM checks found no injected `img` or `script` elements.
- [x] Loading, empty, validation, success, error, and destructive-action states are present; controls are labelled and keyboard-operable.
- [x] Existing authentication and books/copies behavior remains intact; all static JavaScript syntax checks, `.\mvnw.cmd --no-transfer-progress clean verify` (20 tests), and Compose browser tests pass.
- [x] Browser checks covered member CRUD, invalid email/blank optional fields, a simulated API 409 conflict, safe rendering, and a 375px viewport with no horizontal document overflow (document width 360px).
- [x] Both bilingual checklists and their file ledgers remain aligned.

#### Anticipated file ledger (revise both checklists before adding an unplanned file)

- [x] `src/main/resources/static/index.html` — add accessible workspace navigation and the members list/form/dialog/feedback regions while preserving books markup.
- [x] `src/main/resources/static/css/styles.css` — style member navigation, list, and form responsively using the existing design system.
- [x] `src/main/resources/static/js/members.js` — load/render member data and implement create/update/delete flows using safe DOM APIs.
- [x] `src/main/resources/static/js/app.js` — initialize the member module and connect view switching/authentication lifecycle.
- [x] `src/main/resources/static/js/api.js` — localize safe member-not-found API errors and reuse the generic conflict message.
- [x] `README.md` — document the member UI and update the current browser feature scope.
- [x] `AGENTS.md` — record the approved UI scope, implementation, verification, and remaining work.
- [x] `PHASE_CHECKLIST.md` and `PHASE_CHECKLIST.vi.md` — keep the member UI criteria and per-file ledgers synchronized.

**Verification note:** No loan/history relationship exists yet, so the conflict display was checked with a simulated 409 response. The corresponding database-backed referenced-member deletion rule remains a future backend verification item.

**State:** The approved member UI slice is implemented, verified, and accepted by the user. Do not start the next backend/UI slice until the user explicitly asks to continue.

### Phase 4 — Checkout and return (implementation verified; awaiting user acceptance)

This backend phase implements checkout and return. The user approved a default loan period of 14 calendar days from the checkout date and approved the complete scope before implementation.

#### Proposed scope

- A loan links exactly one shared-library member to one physical book copy. The checkout API accepts the member ID and copy ID; derive the acting staff identity from the authenticated session, never from request data.
- Record checkout date, due date, return date, checkout staff, and returning staff on the loan record. Keep returned loan records as history; the active/overdue state is derived from return date and due date rather than a mutable status flag.
- Use the user's selected policy: checkout date is the current library business date and due date is 14 calendar days later. The request cannot override the due date. Returning a loan records the current business date; overdue means an unreturned loan whose due date is before the current business date.
- A physical copy may have at most one active loan. Checkout changes its status from `AVAILABLE` to `ON_LOAN`; return changes it back to `AVAILABLE`. Reject unavailable, missing, retired, or maintenance copies, missing members, duplicate active checkout, and repeated return.
- Serialize checkout attempts for the same physical copy with a transaction and a database row lock (or an equally reliable atomic strategy), so concurrent attempts for the last available copy cannot both succeed.
- Preserve members, copies, and loan records with restrictive foreign keys. A member with loan history and a copy with loan history cannot be deleted; do not cascade-delete. Map a referenced-member delete conflict to a safe, understandable API error.
- Provide authenticated, CSRF-protected checkout and return REST operations, with safe validation/not-found/conflict responses and tests.
- Keep renewals, reservations/queues/holds, activity search/history endpoints, pagination, dashboard, and the loan/member UI out of this backend phase.
- Use the existing Java 17/Spring Boot/PostgreSQL/Flyway/Maven, session/CSRF, API error, and shared-library patterns; add no new framework or service.

#### Acceptance checks

- [x] User selected a fixed default loan period of 14 calendar days from checkout; clients cannot choose/override the due date.
- [x] User reviewed and approved the proposed loan fields/audit attribution, copy status transition, return behavior, and referenced-record deletion rule before implementation.
- [x] Authenticated staff can check out an available copy to an existing member and return its active loan; all staff accounts see the same active loans and updated inventory state.
- [x] Checkout and return persist dates and acting staff IDs from the server-side authentication context. Returned loans remain available as historical records.
- [x] Due dates use the library business date plus 14 calendar days; overdue state is derived from an active loan's due date, not stored separately.
- [x] Only one active loan can exist for a copy. Checkout rejects missing members/copies and copies not `AVAILABLE`; return rejects missing or already-returned loans.
- [x] Copy status transitions are consistent (`AVAILABLE` → `ON_LOAN` → `AVAILABLE`) and staff cannot manually make a loaned copy available or delete a copy with loan history.
- [x] Concurrent checkout attempts for the same last available copy are serialized; at most one succeeds. H2 tests and a simultaneous Compose PostgreSQL API check each produced exactly one success and one conflict.
- [x] Restrictive foreign keys prevent deletion of a member or copy referenced by loan history; the member conflict response is safe, and no loan/history row is cascaded away.
- [x] Anonymous access is rejected and write APIs require CSRF; validation, not-found, business-conflict, and malformed-request responses are safe and consistent.
- [x] Tests cover checkout/return happy paths, shared visibility, dates/overdue derivation, missing/invalid references, repeated return, copy-status rules, referenced deletion, authorization, and CSRF.
- [x] Existing Phase 0–3 backend and accepted account/books/member UI behavior remains intact; `.\mvnw.cmd --no-transfer-progress clean verify` passes (25 tests, 0 failures/errors/skips).
- [x] Flyway V4 and live checkout/return behavior are verified against the existing Compose PostgreSQL volume without deleting/resetting it; app and database are healthy.
- [x] Both bilingual checklists and their anticipated file ledgers are aligned with the implemented and verified scope.

#### Anticipated file ledger (revise both checklists before adding an unplanned file)

- [x] `src/main/java/com/booknest/loan/Loan.java` — persistent checkout/return record, due/return dates, and checkout/return staff attribution.
- [x] `src/main/java/com/booknest/loan/LoanRepository.java` — loan lookup, active-loan checks, and queries needed for safe checkout/return.
- [x] `src/main/java/com/booknest/loan/CheckoutRequest.java` and `LoanResponse.java` — validated checkout input and safe loan representation with derived active/overdue values.
- [x] `src/main/java/com/booknest/loan/LoanService.java` — transactional checkout/return, 14-day due-date calculation, reference validation, and business rules.
- [x] `src/main/java/com/booknest/loan/LoanController.java` — authenticated checkout and return endpoints.
- [x] `src/main/java/com/booknest/book/BookCopy.java`, `BookCopyRepository.java`, and `BookCopyService.java` — add `ON_LOAN`, lock the selected copy, and guard status/delete operations while loaned.
- [x] `src/main/resources/db/migration/V4__create_loans_and_loaned_copy_status.sql` — loans schema, restrictive foreign keys/constraints/indexes, and allowed `ON_LOAN` copy status.
- [x] `src/test/java/com/booknest/loan/LoanControllerTests.java` — checkout/return, dates, overdue calculation, access, copy/member references, preservation of loan history, and concurrent checkout.
- [x] `src/test/java/com/booknest/book/BookCopyControllerTests.java` — ensure `ON_LOAN` cannot be assigned manually; loan tests cover editing/deleting referenced copies.
- [x] `src/test/java/com/booknest/account/AccountControllerTests.java`, `src/test/java/com/booknest/security/SecurityConfigurationTests.java`, and `src/test/java/com/booknest/member/MemberControllerTests.java` — clear loan fixtures before deleting referenced accounts/members so the full suite honors restrictive foreign keys.
- [x] `src/main/resources/static/js/books.js` — display `ON_LOAN` copies as read-only in the existing inventory UI.
- [x] `src/main/resources/static/js/api.js` — provide safe, localized member/loan conflict messages.
- [x] `src/main/java/com/booknest/web/ApiExceptionHandler.java` — map safe business-conflict codes for loan and referenced-record operations.
- [x] `README.md` — document checkout/return endpoints, 14-day rule, status transitions, and current limitations.
- [x] `AGENTS.md` — record approved Phase 4 decisions and exact implementation/verification results.
- [x] `PHASE_CHECKLIST.md` and `PHASE_CHECKLIST.vi.md` — maintain matching criteria and per-file ledgers.

**State:** The approved Phase 4 backend is implemented and verified, including PostgreSQL migration and live concurrent checkout; awaiting user acceptance. Do not start loan UI or later-phase work before acceptance.

### Incremental UI slice — Checkout and return (approved; implementation verified, awaiting user acceptance)

This UI slice adds browser workflows on top of the verified Phase 4 API. The user approved the scope before implementation.

#### Approved scope

- Add an accessible **Books / Members / Loans** workspace tab, preserving existing book/copy/member behavior.
- List all loans with member, title/copy, checkout/due dates, active/overdue/returned state, and staff attribution.
- Provide checkout using an existing member and only currently available copies; the server remains authoritative for copy availability and the 14-day due date.
- Provide return for active loans with confirmation; refresh loan list and inventory after checkout/return. Retain returned loans in the list.
- Use the existing same-origin `apiRequest` helper for session/CSRF; create DOM content safely; include loading, empty, success, conflict/error, accessible keyboard and responsive states.
- No demo seed, reservation, renewal, dashboard, search, filtering, or pagination in this slice. For a manual test on an empty database, create member/book/available copy in the existing UI first.

#### Acceptance checks

- [x] User reviewed and approved the UI scope before implementation.
- [x] Staff can switch among Books, Members, and Loans; previous screens and keyboard tab navigation continue to work.
- [x] Staff can inspect active, overdue, and returned loans with dates and actor attribution; returned records remain visible.
- [x] Checkout dialog lists existing members and only `AVAILABLE` copies; empty member/copy options are explained and submission is disabled when required data is absent.
- [x] Checkout submits member/copy IDs only, then reflects server-calculated 14-day due date and `ON_LOAN` inventory status.
- [x] Staff can confirm return; returned state and inventory `AVAILABLE` status refresh; an unavailable-copy 409 is shown safely.
- [x] Loading, empty, API/network errors, conflict feedback, success feedback, and destructive-action confirmation are usable and accessible.
- [x] Loan/member/book content is inserted safely via DOM APIs; no HTML injection or sensitive browser storage.
- [x] UI works at a 360px viewport without document-level horizontal overflow; loan table scrolls within its container.
- [x] All static JavaScript syntax checks, `.\mvnw.cmd --no-transfer-progress clean verify` (25 tests), Compose configuration/rebuild/readiness, and browser checkout/return checks pass; temporary account/member/book/copy/loan rows were removed and confirmed absent.
- [x] README, agent guide, and both bilingual acceptance ledgers match implemented behavior and exact verification results.

#### Anticipated file ledger

- [x] `src/main/resources/static/index.html` — add the loan workspace panel and checkout dialog.
- [x] `src/main/resources/static/js/app.js` — initialize loan module and connect the third tab/authentication lifecycle.
- [x] `src/main/resources/static/js/loans.js` — load/render loans; populate checkout options; submit checkout and return through the shared API helper.
- [x] `src/main/resources/static/css/styles.css` — style loan statuses and responsive/scrollable loan list.
- [x] `README.md` — describe checkout/return UI and how to prepare records for a manual test on an empty database.
- [x] `AGENTS.md` — record approved UI scope and verified implementation results.
- [x] `PHASE_CHECKLIST.md` and `PHASE_CHECKLIST.vi.md` — keep acceptance criteria and file ledger synchronized.

**State:** Approved UI slice implemented and verified; awaiting user acceptance before later phase work.

### Incremental account UI slice — Change password (approved; implemented and verified, awaiting acceptance)

The user asked for a password-change option after noting it is not visible in the current interface. The user selected the post-change behavior: end the current session and require sign-in again. The user approved the scope, including the current-session sign-out behavior.

#### Proposed scope

- Add a clearly labelled **Change password** action to the authenticated account area without changing registration, login, or logout behavior.
- Add an authenticated API operation that changes only the current staff account's password; derive the account from the authenticated principal, never accept an account ID from the client.
- Require current password, new password, and new-password confirmation in the UI. Require a valid current password; enforce the existing 8–72 character password policy for the new password, and reject reusing the current password.
- Store only the new BCrypt hash. Never return or log passwords/hash values. Use CSRF protection and same-origin session behavior.
- On success, invalidate the current session and require login with the new password. Other active sessions are not explicitly revoked/tracked in this slice; document this limitation unless a later scope changes it.
- Show safe, understandable feedback for wrong current password, validation/mismatch, network errors, and successful change/sign-out; do not reveal account existence or password material.
- No password-reset email, recovery codes, administrator reset, multi-session revocation, or account management in this slice.

#### Acceptance checks

- [x] User reviewed and approved the change-password scope and current-session sign-out behavior before implementation.
- [x] Authenticated staff can reach the change-password form; anonymous clients cannot use the API.
- [x] Correct current password plus a valid new password changes only the signed-in account's BCrypt hash; the confirmation must match in the UI.
- [x] Wrong current password is rejected without changing the stored password; the error is safe and does not reveal secrets.
- [x] New password below 8 or above 72 characters, confirmation mismatch, and reuse of the current password are rejected with clear feedback.
- [x] After success, the current session is invalidated and the user must sign in again; the new password succeeds and the old password fails.
- [x] Write requests require CSRF; malformed/invalid requests receive safe consistent responses. No account ID is accepted from the client.
- [x] Passwords and hashes do not appear in API responses, logs, or browser storage.
- [x] Existing registration/login/logout and authenticated Books/Members/Loans flows remain intact; `.\mvnw.cmd --no-transfer-progress clean verify` passes (29 tests, 0 failures/errors/skips), all static JavaScript syntax checks pass, Compose rebuilt without resetting data, readiness is UP, and browser password-change checks pass.
- [x] Both bilingual checklists and project guidance record exact implementation and verification results.

#### Anticipated file ledger (revise both checklists before adding an unplanned file)

- [x] `src/main/java/com/booknest/account/AccountController.java` — add the authenticated password-change endpoint and invalidate the current session on success.
- [x] `src/main/java/com/booknest/account/AccountService.java` and `ChangePasswordRequest.java` — verify current password, enforce policy, encode and update the current account safely.
- [x] `src/main/java/com/booknest/account/StaffAccount.java` — add a narrowly scoped password-hash update method.
- [x] `src/main/java/com/booknest/security/SecurityConfig.java` — verified existing authenticated `/api/**` and CSRF/session rules cover the endpoint; no config change was needed.
- [x] `src/test/java/com/booknest/account/AccountControllerTests.java` and `src/test/java/com/booknest/security/SecurityConfigurationTests.java` — cover success, wrong/reused password, input policy, authentication, CSRF, session invalidation, new-password login, and old-password rejection without password/hash disclosure.
- [x] `src/main/resources/static/index.html`, `src/main/resources/static/js/auth.js`, and `src/main/resources/static/css/styles.css` — add an accessible account action/form and success/error states.
- [x] `src/main/resources/static/js/api.js` — localize safe password-change errors.
- [x] `README.md`, `AGENTS.md`, `PHASE_CHECKLIST.md`, and `PHASE_CHECKLIST.vi.md` — document scope, session behavior, limitations, and verification tracking.

**State:** Approved UI/API slice implemented and verified; awaiting user acceptance. Other active sessions are not explicitly revoked or tracked.

### Account-model change — Patron self-registration and password recovery (approved; implementation in progress)

The user approved this detailed checklist and implementation. The product direction is one public shared library where patrons self-register and use borrowing/reservation; staff manage the catalog and support library operations. The agreed no-email recovery process has an administrator verify the requester and issue a temporary password; the administrator recovers their own account through an operational procedure on the host/server.

#### Proposed scope

- Enforce three distinct backend roles: `ADMIN`, `STAFF`, and `PATRON`. Never trust a role, user ID, member ID, or actor ID from the client; hiding a UI control is not authorization.
- Proposed permission matrix: visitors can only browse the public catalog; patrons can view/edit their own profile, borrow/reserve, and view their own transactions; staff manage books/copies, member records, and library operations; admins have full access, provision/manage staff, and reset accounts. Only admins reset staff/patron passwords.
- Patrons self-register with username/password. On successful registration, create or link the corresponding borrower/member profile so staff do not manually enter every patron. Authentication accounts and member profiles remain distinct concepts with an explicit relationship.
- Reuse the existing Member profile rules: full name required; email, phone, and notes optional; blank optional fields normalize to null. Usernames are unique and duplicate registration returns a safe error.
- Anonymous visitors can browse public book titles and available-copy counts, but cannot view personal data, accounts, loans, or other patrons' information.
- An authenticated patron can immediately borrow an `AVAILABLE` copy; the server applies the current 14-calendar-day due-date policy and retains protection against concurrent checkout of the last copy. Staff confirm returns after physically receiving copies; return does not erase history.
- When no copy is available, patrons can join a FIFO title queue and cancel their own reservation. On return, hold the copy for the first eligible queue member under the existing policy; no email or automatic expiry scheduler. Patrons see only their own loans/reservations; staff/admin see and handle workflows according to role.
- Add a **Forgot password?** link to the sign-in screen. With no email, it instructs the user to contact the library for admin verification/reset; staff can relay the request but cannot reset accounts. Do not add a shared secret/password or a public endpoint that can reset arbitrary accounts.
- After verifying a specific patron/staff account, an administrator can reset that account. Generate a random one-time temporary password, display it once, persist only its hash, safely record the event, and require the account to change it at next login. Temporary credentials are never shared defaults.
- Bootstrap the single highest-level administrator through an authenticated local/server operational procedure; reserve username `admin` if feasible. Never hardcode `admin/admin` or put the admin password in the repository, image, or website. Admin bootstrap/recovery issues a random temporary credential and requires a change after login.
- Admin manages accounts/staff and recovery. Staff manage titles/copies and perform only the approved library operations. Patrons do not depend on staff creating their accounts.
- Preserve server-side session cookies, CSRF, BCrypt, current password policy, current-session invalidation after password change, and secret non-disclosure. Enforce least privilege for admin/staff/patron APIs.
- Migrate schema without destroying existing data; do not remove/reset the volume. Decide how existing accounts are classified and preserve Member/Loan links/history before migration.
- Preserve existing staff accounts by assigning `STAFF`; preserve legacy Member records without guessing/linking them to new accounts. Create links only for new patron registrations. Use safe forward-only migrations; never direct the user to reset the volume.
- Out of scope unless separately approved: email/SMTP, OAuth, JWT, CAPTCHA, external services, public-web admin recovery, custom roles, and revoking all sessions.

#### Acceptance checks (mark only after implementation and verification)

- [ ] Patrons self-register and self-service borrow/reserve in the shared library; there is one highest-level admin; staff manage the library; account recovery does not use email.
- [ ] Duplicate usernames are rejected safely; registration automatically creates a linked Member with required full name and optional email/phone/notes under existing validation.
- [ ] Checkout is effective immediately when an `AVAILABLE` copy exists; server sets the 14-day due date. Staff confirm physical returns. Patrons see only their own loan/reservation data.
- [ ] When no copy is available, patrons can join/cancel a FIFO queue; hold a returned copy for the first eligible queue member; no email or automatic expiry scheduler.
- [ ] User approves the proposed role matrix: visitors browse catalog only; patrons access only their own profile/transactions and self-service borrowing/reservation; staff manage books/copies/members/transactions; admins manage all accounts, password resets, and library operations. Backend rejects all unauthorized access.
- [ ] The **Forgot password?** link directs users to library/admin verification; staff may relay but cannot reset; it reveals no account status and cannot reset without verification.
- [ ] Admin reset verifies the specific target account, issues a random one-time temporary password, requires password change before business access, does not store/log plaintext, and safely audits actor/target/time.
- [ ] Patron/staff temporary passwords are single-use for sign-in/change; subsequent normal sign-ins use the password they set.
- [ ] Admin can be bootstrapped/recovered by an authenticated local/server operation without email; no fixed/default password exists in committed source/config/UI; a temporary password must be changed.
- [ ] Existing change-password behavior remains: invalidate the current session and require sign-in again.
- [ ] Database changes preserve all existing account/member/book/copy/loan data; migration and recovery/rollback steps are documented, with no volume reset.
- [ ] Automated tests cover patron registration, member linkage, authorization and cross-role denial, reset/first-login password behavior, CSRF/session, secret non-disclosure, and data preservation; UI is checked on desktop/mobile.
- [ ] README, agent guidance, and bilingual checklists accurately describe admin bootstrap/recovery and security limitations; report only commands/tests actually run.

#### Implementation file checklist

- [x] `src/main/java/com/booknest/account/` — roles, patron/staff registration, temporary credentials, password change/reset, and admin bootstrap.
- [x] `src/main/java/com/booknest/member/` — link patron accounts to member profiles without losing history.
- [x] `src/main/java/com/booknest/security/` — backend authorization, public routes, session-version enforcement, and CSRF.
- [x] `src/main/resources/db/migration/` — forward migration for roles, password-change state, account/member linkage, audit, and credential version.
- [x] `src/main/resources/static/index.html`, `src/main/resources/static/js/`, and `src/main/resources/static/css/styles.css` — patron registration, forgot-password guidance, profile, and role-aware workflows.
- [x] `src/test/java/com/booknest/` — role, recovery, profile ownership, session invalidation, and business-flow tests.
- [x] `README.md`, `AGENTS.md`, `PHASE_CHECKLIST.md`, and `PHASE_CHECKLIST.vi.md` — implementation guidance and status.

**State:** Checklist approved; implementation and the 40-test regression suite are complete. The Compose PostgreSQL migration advanced V4→V6 while preserving the pre-existing row counts (1 account, 1 book, 0 copies/members/loans); live browser checks covered public catalog, patron signup/sign-in, profile load/save, reservation/cancel, and 360px layout. The actual admin-recovery CLI was verified on an isolated PostgreSQL scratch database, including temporary-password enforcement and audit; only that scratch database was removed. The phase still awaits user acceptance; do not commit or push before acceptance.
