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

### Demo-data slice — local sample library (approved; implemented and verified, awaiting acceptance)

The user asked for a populated local database to explore the application and approved a complete demo dataset with patron and staff accounts, without creating a default administrator. This is a focused slice of Phase 9, not approval to implement the rest of Phase 9.

#### Scope

- Add an explicitly opt-in, repeat-safe local demo initializer for sample staff/patron accounts, linked and walk-in member profiles, books/copies across relevant inventory states, loan history, and reservation states.
- Never create or reset an administrator account. Keep demo initialization disabled by default, document credentials as local/demo-only, and preserve all existing user data.
- Seed once per natural demo record; do not overwrite passwords, profiles, book details, loan history, or user-modified statuses on restart. Report conflicting reserved demo usernames/ISBNs explicitly.
- Keep the sample rows relationally valid: patron-owned activity must link to its patron profile; active loans and held reservations must agree with copy status; staff actions must reference the demo staff account.

#### Acceptance checks

- [x] User approved the demo-data scope before implementation.
- [x] Demo data is disabled by default and only initializes when the local demo option is explicitly enabled.
- [x] The seed provides demo STAFF and PATRON sign-ins, linked patron profiles plus a walk-in profile, several titles and copies, active/overdue/returned loan examples, and held/waiting reservation examples; no admin is seeded.
- [x] Demo passwords are stored only as BCrypt hashes and are clearly documented as local/demo-only credentials; conflicts never silently replace an existing account or library record.
- [x] Re-running initialization creates no duplicate accounts, members, books, copies, loans, or reservations and preserves existing records.
- [x] Tests verify seed contents, ownership/state consistency, opt-in behavior, and repeat safety.
- [x] Existing data in the user's Compose PostgreSQL volume remains intact; live counts and demo sign-in/views are verified without deleting or resetting the volume.
- [x] README, AGENTS.md, and both checklists document the exact enablement steps, demo credentials, and verified results.

#### Anticipated file ledger

- [x] `src/main/java/com/booknest/demo/DemoDataInitializer.java` — add the opt-in, transactional, repeat-safe sample dataset.
- [x] `src/main/java/com/booknest/demo/DemoSeedRun.java` and `DemoSeedRunRepository.java` — record successful completion so later restarts preserve demo records the user has edited.
- [x] `src/main/resources/db/migration/V7__track_demo_seed_run.sql` — add a forward-only seed marker table; no existing library rows are altered.
- [x] `src/main/resources/application.yml`, `compose.yaml`, and `.env.example` — wire the demo flag with a disabled-by-default value.
- [x] `src/test/java/com/booknest/demo/DemoDataInitializerTests.java` — verify opt-in behavior, relationships, and idempotency.
- [x] `src/test/java/com/booknest/BookNestApplicationTests.java` — verify the initializer bean is absent when demo mode is disabled.
- [x] `README.md` — document local demo activation, accounts/passwords, sample records, and the no-admin policy.
- [x] `AGENTS.md`, `PHASE_CHECKLIST.md`, and `PHASE_CHECKLIST.vi.md` — record approved scope, file ledger, and actual verification.

**State:** Approved demo-data slice is implemented, verified, and accepted by the user. `.\mvnw.cmd --no-transfer-progress clean verify` passed 45 tests with 0 failures/errors/skips; `docker compose config --quiet` passed and the final Docker image rebuilt successfully. Existing PostgreSQL data was preserved through Flyway V6→V7. Counts changed from the pre-seed 3 accounts/1 book/0 copies/1 member/0 loans/0 reservations to 6/4/9/4/3/4 plus one seed marker. App restart preserved the same counts, readiness returned HTTP 200, and Compose now runs with demo initialization disabled. Browser checks verified public catalog, demo staff access to books/members/loans/queues, and patron access to only their own active loan and held reservation. No volume, database, or unrelated Docker resource was deleted or reset.

### Phase 5 increment — Loan renewal (queue already implemented; implemented, awaiting acceptance)

The earlier reservation work already implements FIFO waiting, holds on return, and cancellation/queue advancement. This increment adds the outstanding one-time loan renewal policy and its browser controls; it must not reimplement or regress the existing reservation queue.

#### Implemented scope

- Allow at most one renewal per loan. Permit renewal only for an active, not-yet-overdue loan; reject a renewal if the title has an active `WAITING` or `HELD` reservation.
- Patrons may renew only their own loans; staff/admin may renew loans for library operations. Derive the acting account from the authenticated session, never from client-supplied account/member/staff IDs.
- Persist whether renewed, renewal timestamp, and renewing account so the business record retains the renewal actor.
- Add a transactional renewal endpoint. Lock the loan and relevant book in the same order used by the existing return path; serialize renewal against reservation creation/cancellation so a newly queued reader cannot be bypassed by a concurrent renewal.
- Add renewal state to API responses and show a **Gia hạn** action and outcome in the existing Loans/Activity UI. The server remains authoritative; hide/disable actions when renewal is clearly unavailable, while rendering safe localized errors for conflicts.
- Preserve checkout, return, patron ownership, queue behavior, CSRF, and session rules. No auto-renewal, email, extra renewal, renewal of overdue loans, or activity/event-sourcing subsystem.

#### Policy choice required before implementation

The user selected the existing 14-calendar-day loan period with the current due date as the base:

- **Approved:** add 14 calendar days to the loan's current due date.

#### Acceptance checks

- [x] User selects the due-date calculation rule before implementation: add 14 calendar days to the current due date.
- [x] A permitted patron can renew their own active, not-overdue, not-yet-renewed loan exactly once; staff/admin can renew for library operations; patrons cannot renew another member's loan.
- [x] A returned, overdue, already-renewed, or reservation-blocked loan is rejected with a safe conflict response and unchanged loan/copy/queue state.
- [x] New due date follows the user-selected 14-calendar-day calculation. API/UI expose renewal state and actor without accepting actor IDs from the client.
- [x] Concurrent renewal attempts produce at most one success; concurrent queue insertion and renewal leave neither a queued patron bypassed nor partial state (automated concurrency tests use H2).
- [x] UI displays renewal action only where appropriate, refreshes loan/activity data after success, and handles loading, success, network, and conflict states accessibly and safely.
- [x] Existing checkout, return, reservation, authentication/authorization, CSRF, and responsive behavior remain intact for this increment. A 360px browser check found overall page overflow from the pre-existing signed-in account bar, while the loan table itself stays inside its horizontal-scroll wrapper; renewal did not change header CSS and the account-bar issue is tracked separately.
- [x] Focused tests cover policy cases, role/ownership checks, concurrency/state consistency, and the selected due-date rule; full `clean verify`, Compose validation, migration/rebuild with the existing PostgreSQL volume, and browser flows are recorded with exact results.
- [x] README, AGENTS.md, and both bilingual checklists describe the implemented renewal policy and verification.

#### Anticipated file ledger

- [x] `src/main/java/com/booknest/loan/Loan.java`, `LoanService.java`, `LoanRepository.java`, `LoanController.java`, and `LoanResponse.java` — renewal state, policy, locking, and API representation.
- [x] `src/main/resources/db/migration/V8__add_loan_renewal.sql` — add renewal tracking without changing existing loan history.
- [x] `src/main/java/com/booknest/reservation/ReservationRepository.java` and `ReservationService.java` — query active waiting/held reservation state for renewal eligibility.
- [x] `src/main/java/com/booknest/web/ApiExceptionHandler.java` and `src/main/resources/static/js/api.js` — safely map renewal conflicts.
- [x] `src/main/resources/static/js/loans.js` — provide role-aware renewal controls and status without requiring markup/CSS changes.
- [x] `src/test/java/com/booknest/loan/LoanControllerTests.java` — cover renewal rules, ownership, CSRF, concurrent renewal, and concurrent queue insertion.
- [x] `README.md`, `AGENTS.md`, `PHASE_CHECKLIST.md`, and `PHASE_CHECKLIST.vi.md` — document policy, implementation, and verified outcomes.

**Verification:** `.\mvnw.cmd --no-transfer-progress clean verify` passed 51 tests with 0 failures/errors/skips. JavaScript syntax checks, `git diff --check`, and `docker compose config --quiet` passed. `docker compose up -d --build` rebuilt and started the app against the existing PostgreSQL volume; Flyway V8 succeeded and readiness returned HTTP 200 with app/DB UP. Database inspection found 6 accounts, 4 books, 9 copies, 4 members, 5 loans, 6 reservations, and exactly 1 renewed loan. The renewed demo-patron loan now has a due date of 2026-10-31 and records `demo-patron` as its actor. Browser test confirmed the confirmation prompt, extension from 2026-10-17 to 2026-10-31, persisted actor/status after app restart and sign-in, and no renewal action after use. The full signed-in page has the noted pre-existing 360px account-bar overflow; the loan table remains horizontally scrollable within its wrapper.

**State:** Accepted by the user on 2026-10-06. The separate pre-existing signed-in account-bar overflow at 360px remains documented and outside this increment.

### Phase 6 — Activity history and searchable, paginated lists

This phase adds an append-only activity timeline and database-backed discovery for the public/shared book catalog and loans. Keep authorization in the backend: staff/admin can browse shared loan/history records; patrons remain limited to their own loans and activity.

#### Approved implementation scope

- Add a paged response contract with zero-based pages, default size 20, maximum size 100, and stable page metadata. Reject negative pages, sizes outside 1–100, unknown sort fields, and invalid sort directions with a safe 400 response.
- Books: case-insensitive free-text search over title, author, and ISBN; optional exact genre and availability filters; allowlisted sorting by title, author, genre, publication year, or ID. Keep filtering/sorting/paging in PostgreSQL, not in-memory.
- Loans: case-insensitive search over book title and member name; optional `ACTIVE`, `OVERDUE`, or `RETURNED` filter; allowlisted sorting by checkout date, due date, return date, book title, or member name. Derive overdue using the current date. Preserve patron ownership scope in every query.
- Record timestamped checkout, return, renewal, reservation placement/hold/cancellation/fulfillment events with acting username and safe entity snapshots. Store no passwords, credentials, session/CSRF data, or unnecessary patron contact details. Do not let history rows prevent ordinary book/member/account deletion; preserve snapshots without cascading or inventing foreign-key ownership.
- The new activity timeline begins with actions performed after migration V9. Do not fabricate historical reservation actors or transition times; older loan history remains available from existing loan records and their stored actors/dates.
- Provide an authenticated, newest-first, paginated activity endpoint. Staff/admin see shared-library events; patrons see only events associated with their own linked account/member, enforced in the query.
- Add usable search/filter/sort/page controls to the existing Books and Loans views and a paginated activity section. Keep inputs encoded, results rendered with safe DOM APIs, query state reset to page zero when filters change, and loading/empty/error states accessible.
- Preserve existing response/behavior needed by details, checkout, reservation, and role-specific views; update all list consumers when introducing the paged response shape. Do not add dashboard, member pagination, export, or unbounded audit payloads.

#### Acceptance checks

- [x] Book search/genre/availability filters, allowlisted sort and database paging produce correct results/counts; sorting is stable for ties.
- [x] Loan search/status filtering, allowlisted sort and database paging work for staff/admin and are strictly scoped for patrons. Overdue is date-derived.
- [x] Negative page, size 0 or above 100, unknown sort, invalid direction, and invalid status/type inputs are rejected safely; clients cannot inject arbitrary SQL/property names.
- [x] Checkout, return, renewal, and reservation placement/hold/cancel/fulfill each create accurate immutable history rows in the same transaction as their business change, with the authenticated actor and correct safe snapshots.
- [x] Staff/admin can view shared activity; patrons cannot see another patron's events or personal details; anonymous access is rejected.
- [x] Migration V9 preserves existing data and old loan history; deleting otherwise deletable catalog/member/account records is not blocked by activity snapshots.
- [x] Search/filter UI flows were exercised against PostgreSQL for staff and patron views, and results use safe DOM rendering. A previously tracked, app-wide 360px horizontal overflow remains due to the signed-in account bar; this change does not alter that header.
- [x] Focused tests cover query correctness, page metadata/counts, stable allowlisted sort, invalid inputs, privacy/authorization, all event types, transactional rollback, and migration compatibility. Full verification, Compose validation/build, V9 migration/readiness, and browser checks passed as recorded in `AGENTS.md`.
- [x] README, AGENTS.md, and both bilingual checklists reflect implemented API contracts, history scope, and actual test outcomes.

#### Anticipated file ledger

- [x] `src/main/java/com/booknest/common/PageResponse.java` and `PageRequestFactory.java` — bounded, consistent page metadata and input validation.
- [x] `src/main/java/com/booknest/activity/ActivityEvent.java`, `ActivityEventType.java`, `ActivityEventRepository.java`, `ActivityService.java`, `ActivityController.java`, and `ActivityResponse.java` — immutable snapshots, role-scoped history and endpoint.
- [x] `src/main/resources/db/migration/V9__create_activity_events.sql` — additive activity storage/index migration that leaves existing rows intact.
- [x] `src/main/java/com/booknest/book/BookRepository.java`, `BookCopyRepository.java`, `BookService.java`, `BookController.java`, `BookCopyService.java`, and `BookCopyController.java` — database-backed book filters/sort/page, reservation-hold actor propagation, and safe lookup behavior for existing list consumers.
- [x] `LoanRepository.java`, `LoanService.java`, and `LoanController.java` — database-backed loan filters/sort/page while preserving ownership and renewal eligibility.
- [x] `src/main/java/com/booknest/reservation/ReservationRepository.java`, `ReservationService.java`, and `loan/LoanService.java` — record transactional lifecycle activity using server-derived actors.
- [x] `src/main/java/com/booknest/book/BookCopyService.java` and `BookCopyController.java` — preserve authenticated actor attribution when manually making a copy available causes a queued hold.
- [x] `src/main/java/com/booknest/security/SecurityConfig.java`, `web/ApiExceptionHandler.java`, and `static/js/api.js` — protect history and safely reject/localize invalid query parameters.
- [x] `src/main/resources/static/index.html`, `css/styles.css`, `js/books.js`, `js/loans.js`, `js/reservations.js`, and `js/app.js` — accessible discovery controls and activity timeline; keep safe DOM rendering.
- [x] `src/test/java/com/booknest/book/BookControllerTests.java`, `loan/LoanControllerTests.java`, `reservation/ReservationControllerTests.java`, and new `activity/ActivityControllerTests.java` — search/pagination/sort, lifecycle, permissions, privacy, and rollback coverage.
- [x] `src/test/resources/cleanup.sql` — isolate appended activity rows between tests.
- [x] `README.md`, `AGENTS.md`, `PHASE_CHECKLIST.md`, and `PHASE_CHECKLIST.vi.md` — document API/UI behavior and verified outcomes.

**State:** Accepted by the user on 2026-10-06 and published as commit `a100990`. `.\mvnw.cmd --no-transfer-progress clean verify` passed 59 tests with 0 failures/errors/skips. JavaScript syntax, `git diff --check`, `docker compose config --quiet`, Compose rebuild, V9 migration, HTTP 200 app/database readiness, PostgreSQL catalog/search/loan/activity queries, and staff/patron browser flows passed. Existing PostgreSQL rows were preserved. The previously tracked signed-in account-bar overflow at a 360px viewport remains outside this phase.

### Phase 7 — Shared-library dashboard

This phase adds a compact operational overview for staff and administrators. Per the user's decision, patrons do not receive aggregate operational counts and continue using their own loan/reservation/activity screens.

#### Approved implementation scope

- Add an authenticated dashboard summary endpoint restricted to `STAFF` and `ADMIN`. Use existing persistent data only; no demo data or schema migration.
- Return counts for available copies, active loans, overdue active loans, and active reservations. A loan is active when it has no return date; overdue is the active subset whose due date is before today. Active reservations include `WAITING` and `HELD`; therefore overdue loans are included within (not added to) active-loan counts.
- Calculate counts with database aggregate/count queries; do not load unbounded records into application memory. Empty installations return zero for every count.
- Show the shared counts in a readable overview for staff/admin and hide the operational dashboard from patrons and public users. Keep the existing catalog page counts distinct from shared totals.
- Refresh the summary after relevant inventory, checkout, return, renewal, reservation, and reservation-hold changes, and when staff/admin sign in or return to the dashboard view. Show accessible loading and safe error states.
- Document the API and count definitions. Do not add charts, date ranges, export, or patron aggregate analytics.

#### Acceptance checks

- [x] The dashboard reports accurate counts for available copies, active/overdue loans, and waiting/held reservations; returned loans and cancelled/fulfilled reservations are excluded as appropriate.
- [x] Empty data returns zero counts, and ordinary changes refresh the visible summary without a full-page reload.
- [x] Staff/admin can retrieve the same shared metrics; patrons and anonymous callers cannot access the operational summary.
- [x] Counts are computed in the database without fetching full loan/copy/reservation lists.
- [x] Tests cover status boundaries, role access, zero counts, and metric updates after lifecycle mutations.
- [x] README, AGENTS.md, and both bilingual checklists record the endpoint, definitions, exact verification outcomes, and any limitations.

#### Anticipated file ledger

- [x] `src/main/java/com/booknest/dashboard/DashboardController.java`, `DashboardService.java`, and `DashboardSummary.java` — authorized endpoint, count aggregation, and stable response shape.
- [x] `src/main/java/com/booknest/book/BookCopyRepository.java`, `loan/LoanRepository.java`, and `reservation/ReservationRepository.java` — efficient status-aware database counts.
- [x] `src/main/java/com/booknest/security/SecurityConfig.java` — staff/admin-only dashboard authorization.
- [x] `src/main/resources/static/index.html`, `css/styles.css`, `js/dashboard.js`, and `js/app.js` — responsive overview and role-aware refresh/error states.
- [x] `src/test/java/com/booknest/dashboard/DashboardControllerTests.java` — counts, status boundaries, zero values, and role authorization.
- [x] `README.md`, `AGENTS.md`, `PHASE_CHECKLIST.md`, and `PHASE_CHECKLIST.vi.md` — API contract, accepted scope, implementation ledger, and verified results.

**State:** Accepted by the user on 2026-10-06 and published to GitHub in commit `81c7aa0`. `.\mvnw.cmd --no-transfer-progress clean verify` passed 62 tests (0 failures/errors/skips); JavaScript syntax checks, `git diff --check`, and `docker compose config --quiet` passed. Compose rebuilt against the existing PostgreSQL volume; app and database readiness returned HTTP 200/UP, with V9 still the latest migration and no schema change. The STAFF browser session showed all four dashboard values; API response `availableCopies=2`, `activeLoans=3`, `overdueLoans=1`, `activeReservations=3` matched direct PostgreSQL counts `2|3|1|3`. Anonymous API access returned 401; the PATRON browser session showed no operational dashboard and API access returned 403. No data or volume was reset.

### Phase 8 — Integrated interface completion and polish

The existing incremental UI slices cover the public catalog, patron profile and activity, staff inventory/member/loan workflows, admin account tools, and the shared dashboard. Phase 8 finishes integration and usability rather than adding new business rules. An existing signed-in header/account bar can overflow horizontally on phone-sized screens; resolve it as part of responsive polish.

#### Approved implementation scope

- Inspect and refine the existing same-origin HTML/CSS/JavaScript screens and role transitions; preserve current product rules, API contracts, server-side authorization, and session/CSRF behavior.
- Ensure account controls, workspace navigation, cards/forms/dialogs, and tables remain usable at phone, tablet, and desktop widths. Prevent document-level horizontal overflow at 360px and 375px; keep genuinely wide tables scrollable inside their own wrappers.
- Check keyboard navigation and accessible state changes for login/register/workspace tabs, important dialogs, loading/empty/error feedback, and confirmations; make only focused fixes where checks expose problems.
- Exercise integrated browser journeys for public catalog access, patron registration/sign-in/profile/loan/reservation/personal activity, staff books/copies/members/checkout/return/queue/dashboard, and admin staff-account provisioning/password reset. Confirm role boundaries and CSRF-aware writes.
- Verify refresh after mutations, clear success/conflict/network-error messages, confirmation for consequential operations, safe text rendering, and no dead/fake controls.
- Avoid new dependencies, frontend frameworks, new domain behavior, and schema/API changes unless a concrete UI defect requires a narrowly documented correction.

#### Acceptance checks

- [x] At 360px and 375px viewport widths, `document.documentElement.scrollWidth` does not exceed `clientWidth`; tables, if wider than the viewport, scroll only inside their table wrapper. Layout also remains usable at tablet and desktop widths.
- [x] Patron and staff primary journeys remain available end to end: public catalog, patron sign-in/profile/loan/reservation/activity, and staff books/members/loans/reservations/dashboard. Earlier UI-slice tests cover their respective mutations; this polish pass rechecked role navigation and dependent screens.
- [x] Patron/private data remains scoped to the signed-in patron; staff/admin-only tools stay hidden from patrons and are rejected by the API for unauthorized roles.
- [x] Loading, empty, success, validation, conflict, and server/network error feedback is visible and exposed through status/alert semantics where appropriate; tested async loading, focus retention, and a success toast without persisting mock data. Empty-state text now has polite live announcements.
- [x] Keyboard Enter opened the real reset confirmation; the user pressed Escape directly in the visible browser, the prompt closed, and the test account remained unchanged. The accept path was tested only with a mocked confirmation and intercepted POST.
- [ ] Enter/Space activation of the one-time credential dialog's close controls remains unverified. A disposable credential was placed in the dialog on the current app; automation and the reported manual Enter attempt left the dialog open because the shared page was not visible/receiving input. The fake text was then cleared using the close handler; no real credential or API was involved.
- [x] Writes continue to use same-origin API requests and CSRF tokens; successful mutations refresh dependent lists/counts; errors remain understandable and do not report success.
- [x] Untrusted catalog, profile, account, and history text renders as text, with no injected elements; no committed control is a placeholder or inert action.
- [x] Relevant automated tests, JavaScript syntax checks, Compose config, and browser checks pass; results and any limitation are recorded in both language checklists.
- [x] Admin account provisioning/password-reset UI was exercised in a real ADMIN browser session: required username validation blocked a blank submission; duplicate `admin-ui-check-20261006` returned HTTP 409 with a safe message; exactly one explicitly approved STAFF test account was created and retained; its one-time password modal displayed and cleared the temporary credential on close. The reset confirmation was canceled for an existing demo patron without mutation, then accepted for the test account only and its one-time credential modal was verified. The ADMIN session was established through the previously user-approved server-side recovery; no existing non-test account password was reset.

#### Anticipated file ledger

- [x] `src/main/resources/static/css/styles.css` — responsive account bar and workspace tabs; maintain contained table overflow.
- [x] `src/main/resources/static/js/books.js` — register search/reset/pagination handlers once rather than on every list load.
- [x] `src/main/resources/static/index.html` and `src/main/resources/static/js/admin.js` — retain the accessible one-time credential dialog; the keyboard feedback follow-up adds immediate credential clearing from its explicit close controls.
- [x] Relevant existing test files under `src/test/java/com/booknest/` — confirmed the existing full suite covers backend roles, CSRF, and account recovery; no backend behavior changed, so no test source changes were needed.
- [x] `AGENTS.md`, `PHASE_CHECKLIST.md`, and `PHASE_CHECKLIST.vi.md` — maintain the per-file ledger and exact verification/acceptance state.

**State:** Accepted by the user on 2026-10-06 and published to GitHub. At acceptance, the user accepted the live ADMIN UI and exhaustive keyboard-dialog/focus/confirmation checks as limitations. The responsive CSS and book-list listener fix were implemented and verified; full `.\mvnw.cmd --no-transfer-progress clean verify` passed 62 tests (0 failures/errors/skips), all static JS files passed `node --check`, Compose config/build passed, and readiness was UP/UP. Browser checks confirmed no document overflow at 360/375/768/1280px for STAFF and at 360px for PATRON; the 920px loan table scrolls inside its wrapper while the document remains 345px wide at 360px. Staff navigation showed 4 members, 5 loans, 6 reservation cards, and dashboard values `2/3/1/3`; patron saw only own loan/held reservation/profile with staff/admin tabs and dashboard hidden. Staff received 403 from `/api/admin/accounts`; anonymous users receive 401 where checked. One book search generated exactly one API request; a simulated 503 displayed the expected error. Real ADMIN UI coverage and follow-up feedback-state results are recorded below. Remaining verification is limited to keyboard-only response within native confirmation dialogs and keyboard activation of the one-time credential close controls.

**ADMIN account UI follow-up (2026-10-06):** With explicit user approval, signed in through the authorized admin-recovery procedure and tested the real ADMIN account screen. Blank username was blocked; a duplicate username returned HTTP 409 and a safe message. Created and retained the explicitly approved STAFF account `admin-ui-check-20261006`; its temporary credential dialog displayed a 32-character password once and cleared it on close. Canceled a reset confirmation for an existing demo patron without mutation, then completed reset for the test account only and verified the one-time credential dialog/clear-on-close behavior. No existing non-test account password was reset. PostgreSQL account count is now 7, exactly one more than the prior six; all other library data and the named volume were preserved. The ADMIN session remains signed in. The test account has no UI deletion flow and is intentionally retained.

### Phase 9 — Reviewer documentation and demo setup

The opt-in, idempotent local demo initializer is already implemented and verified as a focused earlier slice. This phase completes reviewer-facing documentation only: make the real startup, roles, data model, API examples, test commands, design decisions, limitations, and destructive reset warning easy to understand. Do not alter seed behavior, credentials, schema, or application behavior.

#### Approved implementation scope

- Update README with a concise project summary, implemented capabilities, stack, system/data model overview, role boundaries, key business policies, design trade-offs, and honest non-production limitations.
- Document the exact one-command Compose startup, URL/loopback behavior, clean shutdown preserving the named volume, `.env.example` setup, where to find demo credentials, opt-in sample initialization, restart/idempotency behavior, and the warned destructive reset procedure without running it.
- Add concise representative request/response examples for major implemented API flows, including CSRF-aware writes and the role/access model. Examples must match current DTOs, endpoints, status codes, and authorization.
- Explain both Maven Wrapper test commands and Docker requirements; do not imply Docker Compose runs the full test suite.
- Keep demo passwords and local fallback database credentials explicitly local-only; never expose or add real secrets. Do not change `.env.example`, Compose defaults, code, database, container/volume state, or add dependencies during this docs phase.
- Update this bilingual checklist and `AGENTS.md` with exact documentation validation; no code tests are required for documentation-only edits unless documentation tests exist.

#### Acceptance checks

- [x] A fresh reviewer can follow README from prerequisites through Compose startup, reach the app, create/recover an admin using the documented safe path, and understand how to access local demo accounts.
- [x] Demo seeding is clearly opt-in, repeat-safe, shared once per database, disabled by default, and never creates an admin; public demo credentials and local Compose credentials are warned as unsafe for public deployment.
- [x] README explains normal shutdown separately from the explicitly warned, data-destroying local reset and does not instruct deleting unrelated Docker resources.
- [x] Architecture/data relationships, role boundaries, borrowing/renewal/queue rules, key trade-offs, and actual limitations agree with implementation and do not imply production readiness.
- [x] Representative API requests/responses, CSRF requirements, roles, pagination/filter parameters, and health/testing instructions match current contracts.
- [x] Documentation-only review finds no unverified claims, stale phase status, real secrets, or accidental changes to app/deployment/database behavior.

#### Anticipated file ledger

- [x] `README.md` — reviewer setup, system/data model, demo and reset guidance, representative API examples, testing, decisions, and limitations.
- [x] `AGENTS.md`, `PHASE_CHECKLIST.md`, and `PHASE_CHECKLIST.vi.md` — accurate Phase 9 scope, bilingual ledger, and verification status.

**State:** Accepted by the user on 2026-10-06 and published as `e7ae666`. README documents reviewer setup, architecture/data relationships, local Compose setup and volume, admin recovery, opt-in demo seeding and credentials, safe shutdown and warned destructive reset, role/security rules, API examples and response shapes, pagination, tests, design choices, and limitations. `git diff --check` and `docker compose config --quiet` passed. No application code, seed, credentials, database rows, containers, or volumes were changed for Phase 9; no Maven suite was run because this was documentation-only.

### Phase 10 — End-to-end verification (in progress)

This final phase verifies the committed application and reviewer workflow against the accepted scope. Prefer read-only checks and preserve all existing PostgreSQL data and named volumes. Do not reset the database, delete Docker resources, rotate credentials, or change application behavior as part of verification.

#### Scope

- Run the complete Maven verification suite and validate the Compose configuration; build/start the BookNest stack if Docker is available.
- Verify app/database readiness and the served static page, then exercise demo authentication and a patron checkout followed by a staff return if the user authorizes the resulting persistent history records.
- Verify the application can restart while the existing named PostgreSQL volume and data remain intact; do not run `docker compose down -v` or any Docker prune command.
- Compare before/after database counts and state; report all exact commands, outcomes, environmental blockers, and any unverified flows.
- Do not make code fixes or reset/cleanup existing database records without first discussing the scope and impact with the user.

#### Acceptance checks

- [x] `.\mvnw.cmd --no-transfer-progress clean verify` passes; report exact test totals and any failure.
- [x] `docker compose config --quiet` passes and the BookNest image builds successfully, when Docker is available.
- [x] BookNest app and PostgreSQL become healthy; readiness reports the database up and `/` serves the expected static UI.
- [x] Demo sign-in works and an end-to-end checkout/return flow succeeds, or the flow is explicitly marked unverified with the reason. Any persistent test loan/activity records are disclosed and require user approval before creation.
- [x] Restart verification confirms existing database state persists; existing named volumes are never removed or reset.
- [x] Final database state, exact checks, failures/limitations, and changed files are recorded without claiming unrun verification.

#### Anticipated file ledger

- [x] `PHASE_CHECKLIST.md` and `PHASE_CHECKLIST.vi.md` — matching Phase 10 plan, exact verification results, and any limitations.
- [x] `AGENTS.md` — update the durable checkpoint with factual Phase 10 results after verification.
- [x] Application or deployment files — no changes were needed; no runtime defect was found.

**State:** Accepted by the user on 2026-10-06 and published as `3514063`. `.\mvnw.cmd --no-transfer-progress clean verify` passed 62 tests (0 failures, errors, or skips); every static JS file passed `node --check`; `docker compose config --quiet` and `docker compose build` passed. The app image was rebuilt and app container recreated using `docker compose up --build -d`; after its normal startup interval both services were healthy, readiness was UP with DB UP, and `/` returned HTTP 200 with title `BookNest | Thư viện của bạn`. Demo STAFF and PATRON sign-ins succeeded. With explicit user approval, the patron checked out The Hobbit copy #19 (due 2026-10-20), then staff returned it; the UI showed the returned state, checkout/return actors, and both lifecycle events. The returned test loan and two activity events are intentionally retained. Counts before the transaction were accounts/books/copies/members/loans/reservations/activity = `6/4/9/4/5/6/0`; after it and after app recreation they were `6/4/9/4/6/6/2`. Loan #12 remains returned and copy #19 is `AVAILABLE`; all other tracked table counts remained unchanged. The `booknest-postgres-data` volume was present after restart. No volume/container/database was deleted or reset. One readiness probe immediately after container recreation raced startup and the connection closed; after 12 seconds the app was healthy and the readiness and static-page checks passed. No application code or deployment files changed.

### Priority follow-up — keyboard dismissal for accessible dialogs (approved)

The user prioritized resolving the remaining keyboard-dialog limitation. Browser verification found that modal focus remains contained and returns to the invoking control after closing with the close button, but Escape did not dismiss an open ordinary dialog. The user approved Escape-to-close for ordinary dialogs while explicitly preserving the one-time temporary-password dialog.

#### Scope

- Make Escape close ordinary open application dialogs (book, copies, member, checkout, password-change, and patron-profile dialogs) while preserving focus restoration.
- Preserve patron-profile dialog focus restoration when opening follows an asynchronous profile fetch.
- Keep the one-time temporary-password dialog open on Escape so its credential is not accidentally dismissed.
- Preserve existing close/cancel buttons, validation, forms, and mutation behavior; do not introduce page/data changes.
- Validate with keyboard-driven browser checks, static JavaScript syntax, and the existing Maven suite if implementation warrants it.

#### Acceptance checks

- [x] Escape closes each ordinary dialog checked and restores focus to the control that opened it, including the async-loaded patron profile dialog.
- [x] Tab navigation remains inside an open modal; Enter on an explicit Close/Cancel button still works.
- [x] Escape does not dismiss the one-time temporary-password dialog.
- [x] No form submission or persistent business-data mutation occurs during these dismissal checks.
- [x] Relevant JavaScript syntax checks and Maven verification pass; both checklists record exact results.

#### Anticipated file ledger

- [x] `src/main/resources/static/js/app.js` — central Escape dismissal behavior for all dialogs except the one-time temporary-password dialog.
- [x] `src/main/resources/static/js/profile.js` — preserve the invoking button as the previous focus target after async profile loading.
- [x] `PHASE_CHECKLIST.md`, `PHASE_CHECKLIST.vi.md`, and `AGENTS.md` — scope, verification, and durable status.

**State:** Implemented, verified, and accepted by the user on 2026-10-06. `app.js` closes every open ordinary dialog on Escape while excluding `#temporary-password-dialog`. `profile.js` restores the async-loaded profile dialog's invoking control as its focus-return target. `node --check` passed for every static JavaScript file; `.\mvnw.cmd --no-transfer-progress clean verify` passed 62 tests (0 failures/errors/skips); `docker compose config --quiet`, `docker compose build`, and readiness passed. The refreshed Compose app was healthy with DB UP. Browser checks confirmed Escape closed book, copies, member, checkout, password, and patron-profile dialogs; focus returned to each opener, including the async profile. Fourteen successive Tab presses remained inside the copies modal. The one-time temporary-password dialog remained open on Escape and closed with its explicit button activated by Enter. The patron profile explicit Close button also worked with Enter and restored focus. No form was submitted or business data mutated; table counts remained accounts/books/copies/members/loans/reservations/activity = `6/4/9/4/6/6/2`. The existing named volume and all transaction data were preserved. Admin UI coverage and exhaustive keyboard confirmation/feedback-state coverage remain separate follow-ups.

### Final completion checklist — remaining work to finish the portfolio

This checklist tracks the remaining verification and handoff work for the currently agreed BookNest scope. It is not approval to add product features, change the stack, reset data, or deploy publicly. Complete one item at a time, record only observed results, and ask before any action that changes user data or credentials.

#### 1. Finish keyboard and feedback-state verification

- [x] Exercise the safe confirmation paths: keyboard Tab + Enter opened the real account-reset prompt and its wording was inspected; the user pressed Escape in the visible browser, closing the prompt without changing the test account. The accepted path was exercised only with a mocked confirmation and intercepted POST, so no real account credential changed.
- [x] Exercise loading, empty, success, validation/conflict, and network/server-error states; verify status/alert semantics, focus retention, first-invalid-field focus, and success focus return. Empty-state announcements were added to the affected UI text.
- [x] Reconfirm the one-time temporary-password dialog stays open on Escape; both explicit close controls close it and clear displayed credentials immediately, verified with disposable mock content.
- [ ] Verify Enter/Space activation of the one-time credential dialog's close controls on a visible browser page. The current shared page did not receive automation key events; the user reported pressing Enter, but inspection showed the dialog still open. The disposable mock value was cleared using its close handler, not a real credential. Do not perform a real password reset for this check.
- [x] Record the keyboard-input limitation explicitly and leave the activation test unchecked; do not treat an unreceived key or synthetic close-handler invocation as a keyboard pass.
- [x] Do not reset another real account, create further persistent fixtures, submit business mutations, or invoke admin recovery without explicit approval; no such operation was performed in this follow-up.
- [x] Fixes were limited to `src/main/resources/static/js/admin.js` (clear temporary credential immediately from explicit close controls, retaining close-event cleanup) and `src/main/resources/static/index.html` (polite announcements for empty-state text); both bilingual ledgers were updated before the edits and the exact behavior was exercised with mocked browser responses.

**Section 1 outcome (2026-10-06):** Feedback states and safe confirmation branches are tested; a credential-retention defect was fixed and regression-checked with mock content. Keyboard Enter opened the real native confirmation and the user dismissed it with Escape without mutation. Enter/Space activation of the one-time credential dialog's close controls remains unverified: the shared browser page did not receive key events, and the reported manual Enter attempt left it open. The fake value was cleared through the tested click handler; that is not counted as keyboard verification. No real credential or business record changed.

#### 2. Close the verification record

- [x] Update the Phase 8 acceptance item and this checklist in both language versions with the exact keyboard/browser results; leave any unverified behavior unchecked and explain the limitation.
- [x] Run `git diff --check` and inspect the complete diff for scope, stale claims, generated files, and secrets.
- [x] For the application code changes, run `.\mvnw.cmd --no-transfer-progress clean verify` (62 tests, no failures/errors/skips), `node --check` for all static JavaScript, `docker compose config --quiet`, the mocked browser regression checks, and `docker compose build`. With the user's approval, `docker compose up -d --no-deps app` recreated only the app container; the ADMIN session was invalidated, and the database volume was preserved.
- [x] Complete the final read-only smoke check after the approved app-only restart: app/DB healthy, readiness UP, `/` HTTP 200, and counts stayed `7/4/9/4/6/6/2` (accounts/books/copies/members/loans/reservations/activity). At a 360px browser viewport, document width equaled client width (345px available content width) with no horizontal page overflow. Public catalog loaded; local demo STAFF saw 4 book rows, 4 member rows, 6 loan rows, and dashboard counts `2/3/1/3`; local demo PATRON saw only its 2 loan rows and own reservation/activity, with no admin tab. No domain data changed. The user-authorized `admin-ui-check-20261006` account remains in the database.

#### 3. Review and publish the accepted work

- [x] Review the final diff and repository status; ensure only intended source/documentation changes are included and no credential or generated artifact is committed.
- [x] Present the results and remaining limitation for user acceptance; the user accepted the diff on 2026-10-06. Enter/Space dialog activation remains explicitly unverified as recorded above.
- [x] After acceptance, committed using the repository's Conventional Commit style and required co-author trailer; `b1020ec` (`fix: clear one-time credentials on dialog close`) was pushed to `origin/main`.
- [x] Verified `b1020ec` on `origin/main` and a clean working tree immediately after the push. Exact commit and verification results are recorded here and in `AGENTS.md`: `clean verify` passed 62 tests; all 10 static JavaScript files passed `node --check`; Compose config/build, readiness, read-only role/viewport smoke, and `git diff --check` passed. The Enter/Space interaction remains documented as unverified.

#### 4. Prepare a reviewer/demo handoff (polish, not a feature gate)

- [x] Draft a concise demo sequence using existing accounts/data: public catalog, patron self-service and activity, staff circulation/queue/dashboard/history, and the optional admin setup. The README warns which actions mutate demo data.
- [x] Prepare short explanations of session/CSRF, server-side role/ownership checks, transaction/concurrency rules, migrations, opt-in demo data, and local-only Compose setup; these are documented in the README design/security sections.
- [x] Verify the README startup/shutdown/demo/admin-recovery instructions match current Compose behavior; add the reviewer walkthrough without adding production deployment, integrations, or unapproved features.

**Definition of done:** all required keyboard checks are passed or explicitly documented as unverified; bilingual checklists and project checkpoint match the actual state; applicable regression checks pass; user accepts the final diff; accepted changes are published and the repository is synchronized. The demo handoff may be completed separately and does not block technical completion.
