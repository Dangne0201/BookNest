# BookNest

BookNest is a Java portfolio application for managing one shared library. Visitors can browse the catalog; patrons register themselves, manage their own profile, borrow available copies, join/cancel reservation queues, renew eligible loans, and see only their own activity. Staff manage the shared inventory, walk-in member records, and physical returns. An administrator provisions staff accounts and issues verified temporary passwords.

## Stack

- Java 17 and Spring Boot 4.0.8
- Spring MVC, Spring Data JPA, Hibernate, Spring Security
- PostgreSQL 17 and Flyway
- Maven Wrapper
- Plain HTML, CSS, and JavaScript
- Docker Compose

## What is implemented

- Public, searchable book catalog with individual copy availability.
- Patron self-registration and linked member profile; profile, loan, reservation, and activity views are scoped to the signed-in patron.
- Staff workflows for books/copies, unlinked walk-in members, checkout, physical return, reservation queues, and a shared operations dashboard.
- Admin-only staff-account provisioning and verified patron/staff password resets; temporary passwords must be changed at sign-in.
- Database-backed book/loan pagination and an activity timeline for loan and reservation lifecycle actions.

The app is a same-origin Spring MVC/API plus static HTML/CSS/JavaScript application. Each PostgreSQL database represents one shared library; accounts do not own separate inventories.

```text
Browser (HTML/CSS/JavaScript)
          | same-origin HTTP, session cookie, CSRF
          v
Spring Boot (controllers -> services -> repositories)
          | JPA/Hibernate; Flyway migrations
          v
PostgreSQL (accounts, books/copies, members, loans,
            reservations, activity snapshots)
```

Core relationships: a `Book` has physical `BookCopy` rows; a `Member` can have a linked patron account or be an unlinked walk-in record; a `Loan` connects one member to one copy and stores checkout/due/return dates and actors; a `Reservation` connects a member to a title and may hold a copy; activity rows keep lifecycle snapshots without owning or replacing the business records.

## Run locally

Requirements: Git and Docker Desktop with Docker Compose.

Optional local environment file:

```powershell
Copy-Item .env.example .env
```

Start the app and database:

```powershell
docker compose up --build -d
```

Open <http://localhost:8080/>. Compose waits for PostgreSQL health before starting the app. The app binds to loopback (`127.0.0.1`); PostgreSQL is not published as a host port. Compose uses the named `booknest-postgres-data` volume so ordinary stop/restart and application rebuilds preserve database data.

The fallback database username/password in Compose and `.env.example` are local-development values only. Keep `.env` private (it is ignored by Git); do not use these values or the demo accounts for an internet-facing deployment.

### First administrator setup and recovery

The first administrator is created through a local operational command, not through the public registration form. With the database service running, open a second terminal in the project directory and run:

```powershell
docker compose run --rm app --spring.main.web-application-type=none --booknest.admin-recovery=true
```

The command prints a randomly generated temporary password once. Sign in as `admin` and immediately choose **Change temporary password**. Keep the temporary password private and do not save it in the repository. Running the command again rotates the administrator password and invalidates sessions created with the previous credential.

There is no email-based recovery. A patron or staff member must contact the library; an administrator verifies the request and issues a temporary password from **Accounts**. Staff cannot reset passwords. Admin recovery must be run by someone with access to the host/server.

### Use the application

1. Register from **Create account** as a patron, providing a name and password. A linked borrowing profile is created automatically.
2. Sign in and browse the public catalog. If a copy is available, borrow it immediately; otherwise join the book's reservation queue. An eligible active loan may be renewed once for 14 calendar days from its current due date; overdue, already-renewed, or reservation-queued titles cannot be renewed.
3. Use **My profile** to update your own contact information. **My activity** shows only your loans and reservations.
4. Staff accounts are provisioned by the administrator. Staff maintain books/copies and member records, and confirm physical returns.
5. Admins use **Accounts** to provision staff accounts or reset a verified patron/staff account. Temporary passwords are shown once and require a change at the next sign-in.

Demo data is disabled by default. See [Local demo data](#local-demo-data) for opt-in seeding instructions and local-only credentials.

Stop services without deleting data:

```powershell
docker compose down
```

**Destructive local reset — warning:** this deletes the PostgreSQL data volume, including all accounts, books, copies, members, loans, reservations, and activity in this local BookNest installation. Back up anything you need first. Only run it when you intentionally want to erase BookNest's local database:

```powershell
docker compose down -v
docker compose up --build -d
```

The reset uses this Compose project and its named BookNest volume; it is not needed for normal shutdown, application rebuilds, schema migrations, or account recovery. Do not use Docker-wide prune commands. Migrations are forward-only; take a database backup before manual database operations.

## Local demo data

Demo data is **off by default**. To seed the local database once, opt in for the startup:

```powershell
$env:BOOKNEST_DEMO_DATA_ENABLED = "true"
docker compose up --build -d
Remove-Item Env:BOOKNEST_DEMO_DATA_ENABLED
docker compose up -d
```

The seed creates one sample staff account, two patron accounts with linked profiles, one walk-in member, three titles with copies in several states, representative active/overdue/returned loans, and held/waiting/cancelled/fulfilled reservations. It does not create or reset an administrator. A database marker makes initialization repeat-safe: later starts do not duplicate or overwrite demo or user-edited records. Disabling the option after seeding does not remove the seeded records; a deliberate database reset is required to erase them.

| Role | Username | Password |
| --- | --- | --- |
| Staff | `demo-staff` | `BookNestDemoStaff2026!` |
| Patron | `demo-patron` | `BookNestDemoReader2026!` |
| Patron | `demo-patron-two` | `BookNestDemoReaderTwo2026!` |

These published sample credentials are intentionally convenient for local exploration only. Never enable demo seeding or use these credentials in a public deployment.

### Suggested reviewer walkthrough

1. Open the public catalog without signing in; inspect title details and copy availability.
2. Sign in as `demo-patron` and open **My activity** to review that patron's loans, held/waiting reservations, and activity. Avoid **Check out book** and cancellation actions if you want to preserve the seeded state.
3. Sign out, sign in as `demo-staff`, and inspect the shared dashboard, books/copies, members, loans/returns, reservation queue, and activity. Avoid submitting forms or return/queue actions unless you intend to change the local sample data.
4. Admin is intentionally not seeded. If reviewing account provisioning, use the local first-admin/recovery procedure above; it prints a one-time password, and running it again rotates the admin password and invalidates prior sessions.

For a design discussion, explain the same-origin session-cookie and CSRF flow, backend role/ownership checks, PostgreSQL constraints and transaction/locking rules, forward-only Flyway migrations, opt-in repeat-safe demo seed, and the documented limits of this local learning portfolio.

## Access and security

- Roles are enforced by backend authorization: `ADMIN`, `STAFF`, and `PATRON`. Visitors may only read the catalog.
- Patrons' own-profile, loan, and reservation operations are scoped on the server to the authenticated account; client-supplied account/member ownership is not trusted.
- Write requests use CSRF protection and server-side sessions with an HTTP-only, SameSite=Lax cookie. Passwords are BCrypt-hashed.
- Temporary passwords are generated randomly, stored only as hashes, and require a password change before business access.
- Password resets rotate a persisted credential version so existing sessions are rejected, including when admin recovery is run in a separate process.
- Changing one's password requires the current password and ends the current session. Other sessions are not explicitly revoked by an ordinary self-service password change.
- Keep the Compose services loopback-bound. This local portfolio setup is not an internet-facing production deployment.

## API overview

All write requests require a CSRF token. `GET /api/auth/csrf` starts/continues a session and returns the token/header name (`X-CSRF-TOKEN`).

| Method and path | Access | Purpose |
| --- | --- | --- |
| `POST /api/auth/register` | Public, CSRF | Register a patron and linked member profile |
| `POST /api/auth/login` | Public, CSRF | Sign in with username and password |
| `GET /api/auth/me` | Authenticated | Read the current account and role |
| `POST /api/auth/password` | Authenticated, CSRF | Change password; current session ends |
| `POST /api/auth/logout` | Authenticated, CSRF | End the current session |
| `GET /api/books` | Public | Search/filter/page the catalog and available-copy counts |
| `GET /api/books/options` | Public | Compact title list for existing checkout/reservation selectors |
| `/api/books/**` writes | Staff/admin, CSRF | Manage titles and physical copies |
| `/api/members/me` | Patron, CSRF for update | Read/update only the current patron's profile |
| `/api/members/**` | Staff/admin, CSRF for writes | Manage unlinked member records |
| `GET /api/loans` | Patron/staff/admin | Search/filter/page loans; patrons see only their own |
| `/api/loans/**` | Patron/staff/admin; CSRF for writes | Patrons access own loans; staff/admin manage returns |
| `POST /api/loans/{loanId}/renew` | Patron (own loan), staff/admin; CSRF | Renew an eligible loan once; add 14 days to its current due date |
| `/api/reservations/**` | Patron/staff/admin; CSRF for writes | Join, view, cancel, and fulfill reservation queues |
| `GET /api/activity` | Authenticated | Search/page recent lifecycle events; patrons see only their own |
| `GET /api/dashboard` | Staff/admin | Read shared available-copy, active/overdue-loan, and active-reservation counts |
| `/api/admin/accounts/**` | Admin, CSRF for writes | List accounts, provision staff, and reset patron/staff passwords |

Admin self-recovery is intentionally not exposed as a web endpoint; it is the local Docker Compose command above.

The dashboard reports counts for `availableCopies`, `activeLoans`, `overdueLoans`, and `activeReservations`. Active loans have no return date; overdue loans are the active subset with a due date before today. Active reservations include both waiting and held requests. Counts are queried from the database and shared across staff/admin accounts; patrons continue to see only their own borrowing and reservation information.

Paged list responses contain `items`, zero-based `page`, `size`, `totalElements`, `totalPages`, `first`, and `last`. The default page size is 20 and the maximum is 100. Book filters accept `q`, `genre`, and `availability` (`AVAILABLE` or `UNAVAILABLE`); book sort fields are `title`, `author`, `genre`, `publicationYear`, and `id`. Loan filters accept `q` and `state` (`ACTIVE`, `OVERDUE`, or `RETURNED`); loan sort fields are `checkoutDate`, `dueDate`, `returnDate`, `bookTitle`, and `memberName`. Both accept `page`, `size`, `sort`, and `direction` (`asc` or `desc`). Activity accepts `q`, `type`, `page`, and `size`; it is ordered newest first.

The activity timeline records checkout, return, renewal, and reservation placement/hold/cancellation/fulfillment from migration V9 onward. It keeps safe snapshots of the acting username and relevant book/member details without creating foreign-key dependencies. Earlier loan details remain available from loan records; historical reservation transitions are not backfilled.

### Example API requests

All writes require the server-side session cookie and CSRF header. First request the token; keep the returned session cookie and send the returned `token` in the returned `headerName` for each write. The examples use placeholders rather than credentials:

```http
GET /api/auth/csrf
```

```json
{"headerName":"X-CSRF-TOKEN","token":"<csrf-token>"}
```

Register a patron (the server creates and links the borrowing profile):

```http
POST /api/auth/register
Content-Type: application/json
X-CSRF-TOKEN: <csrf-token>
Cookie: JSESSIONID=<session-cookie>

{
  "username": "reader-example",
  "password": "<choose-a-local-password>",
  "fullName": "Example Reader",
  "email": null,
  "phone": null,
  "notes": null
}
```

The response is `201 Created`, for example:

```json
{"id":42,"username":"reader-example","role":"PATRON","memberId":42,"passwordChangeRequired":false}
```

Login uses URL-encoded form fields (not a JSON request body):

```http
POST /api/auth/login
Content-Type: application/x-www-form-urlencoded
X-CSRF-TOKEN: <csrf-token>
Cookie: JSESSIONID=<session-cookie>

username=reader-example&password=<chosen-password>
```

Successful sign-in returns `200 OK`; `GET /api/auth/me` returns the current account and role. A patron borrows an available copy without supplying a member/account ID—the server resolves the linked profile from the authenticated session:

```http
POST /api/loans
Content-Type: application/json
X-CSRF-TOKEN: <csrf-token>
Cookie: JSESSIONID=<session-cookie>

{"copyId":17}
```

Staff can create a book title; physical copies are added separately through `/api/books/{bookId}/copies`:

```http
POST /api/books
Content-Type: application/json
X-CSRF-TOKEN: <csrf-token>
Cookie: JSESSIONID=<session-cookie>

{
  "title": "Example Book",
  "author": "Example Author",
  "isbn": null,
  "genre": "Fiction",
  "publicationYear": 2025,
  "description": null
}
```

This returns `201 Created` with the saved title and derived inventory counts, for example `{"id":5,"title":"Example Book","author":"Example Author","isbn":null,"genre":"Fiction","publicationYear":2025,"description":null,"totalCopies":0,"availableCopies":0}`.

Staff checkout supplies the selected walk-in or linked member explicitly, while the acting account still comes from the session:

```json
{"memberId":8,"copyId":17}
```

Checkout returns `201 Created` with a loan record containing the derived due date, active/overdue state, and server-recorded actor. Return and renewal use `POST /api/loans/{loanId}/return` and `POST /api/loans/{loanId}/renew`; both require CSRF and return the updated loan. A patron may act only on their own loan; physical returns require staff/admin. Renewal is allowed once, only when the loan is eligible.

Join a title's reservation queue (patron `memberId` is server-derived):

```http
POST /api/reservations
Content-Type: application/json
X-CSRF-TOKEN: <csrf-token>
Cookie: JSESSIONID=<session-cookie>

{"bookId":5}
```

`GET /api/books?q=clean&page=0&size=20&sort=title&direction=asc` is public and returns a page with `items`, `page`, `size`, `totalElements`, `totalPages`, `first`, and `last`. Book and loan page sizes are limited to 1–100. For example, `GET /api/dashboard` is staff/admin-only and returns:

```json
{"availableCopies":2,"activeLoans":3,"overdueLoans":1,"activeReservations":3}
```

Unauthenticated protected requests return `401`; authenticated callers without the required role receive `403`. Validation/conflict responses use safe error codes/messages rather than exposing stack traces.

## Tests

The Maven test profile uses an in-memory H2 database and runs all Flyway migrations; it does not start PostgreSQL or Docker. Docker Desktop and Compose are required for the local full-stack run described above.

Windows:

```powershell
.\mvnw.cmd --no-transfer-progress clean verify
```

macOS/Linux:

```bash
./mvnw --no-transfer-progress clean verify
```

Automated tests cover registration, roles and authorization, profile ownership, temporary-password flows, reset/session invalidation, CSRF, books/copies, members, checkout/return, one-time renewal eligibility and attribution, concurrent checkout/renewal, renewal-versus-queue serialization, reservation queue behavior, and dashboard metrics/access.

## Design decisions and limitations

- Keep the UI in plain same-origin HTML/CSS/JavaScript to match the portfolio's learning goals and avoid a separate frontend build/deployment.
- Use server-side sessions, HTTP-only cookies, CSRF, and BCrypt rather than JWT/OAuth; ownership and role checks are enforced in backend services and queries, not just by hiding UI controls.
- Keep the library shared within one database. JPA plus PostgreSQL constraints, transactions, row locking, and a uniqueness guard protect loan/queue consistency without adding a message broker or distributed architecture.
- Use bounded, database-backed pagination and allowlisted sort fields rather than loading all records into memory.
- This is a learning portfolio, not a production-ready library platform. There is no email delivery/account recovery, reservation expiry scheduler, multi-library tenancy, cloud deployment, high-availability setup, or comprehensive operational monitoring. Admin bootstrap/recovery requires host access and emits a temporary password once. The activity timeline starts at V9 and does not invent earlier reservation history. Demo rows remain in the volume until that local volume is deliberately reset.
- The Compose defaults, public demo credentials, and local admin-recovery workflow are for local review only; do not expose this setup as a public service.

See [PHASE_CHECKLIST.md](PHASE_CHECKLIST.md) for acceptance and verification status. Project/agent context is in [AGENTS.md](AGENTS.md).
