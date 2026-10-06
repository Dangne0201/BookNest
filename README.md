# BookNest

BookNest is a shared-library portfolio application. Visitors can browse the catalog; patrons register themselves, manage their own profile, borrow available copies, join/cancel reservation queues, and see only their own activity. Staff manage library inventory, members, and physical returns. A single administrator manages staff accounts and password recovery.

## Stack

- Java 17 and Spring Boot 4.0.8
- Spring MVC, Spring Data JPA, Hibernate, Spring Security
- PostgreSQL 17 and Flyway
- Maven Wrapper
- Plain HTML, CSS, and JavaScript
- Docker Compose

## Run locally

Requirements: Git and Docker Desktop with Docker Compose.

```powershell
docker compose up --build
```

Open <http://localhost:8080/>. The application is bound to loopback only; PostgreSQL is not published as a host port. Compose's default credentials are for local development only and must not be reused for a public deployment. `.env.example` can be copied to `.env` for local configuration; `.env` is ignored by Git.

### First administrator setup and recovery

The first administrator is created through a local operational command, not through the public registration form. With the database service running, open a second terminal in the project directory and run:

```powershell
docker compose run --rm app --spring.main.web-application-type=none --booknest.admin-recovery=true
```

The command prints a randomly generated temporary password once. Sign in as `admin` and immediately choose **Hoàn tất đổi mật khẩu**. Keep the temporary password private and do not save it in the repository. Running the command again rotates the administrator password and invalidates sessions created with the previous credential.

There is no email-based recovery. A patron or staff member must contact the library; an administrator verifies the request and issues a temporary password from **Tài khoản**. Staff cannot reset passwords. Admin recovery must be run by someone with access to the host/server.

### Use the application

1. Register from **Tạo tài khoản** as a patron, providing a name and password. A linked borrowing profile is created automatically.
2. Sign in and browse the public catalog. If a copy is available, borrow it immediately; otherwise join the book's reservation queue. An eligible active loan may be renewed once for 14 calendar days from its current due date; overdue, already-renewed, or reservation-queued titles cannot be renewed.
3. Use **Hồ sơ của tôi** to update your own contact information. **Hoạt động của tôi** shows only your loans and reservations.
4. Staff accounts are provisioned by the administrator. Staff maintain books/copies and member records, and confirm physical returns.
5. Admins use **Tài khoản** to provision staff accounts or reset a verified patron/staff account. Temporary passwords are shown once and require a change at the next sign-in.

Demo data is disabled by default. To initialize a local sample library, explicitly enable it before starting Compose:

```powershell
$env:BOOKNEST_DEMO_DATA_ENABLED = "true"
docker compose up --build -d
Remove-Item Env:BOOKNEST_DEMO_DATA_ENABLED
docker compose up -d
```

The first command starts the app with demo initialization enabled; the final command recreates the app with the default-off setting after the seed is recorded. The initializer adds sample accounts, linked patron profiles, a walk-in member, *The Pragmatic Programmer*, *Clean Code*, and *The Hobbit* with copies in several inventory states, active/overdue/returned loans, and held/waiting/cancelled/fulfilled reservations. It does not create an administrator. The seed is repeat-safe and does not delete existing records; once inserted, demo rows remain in the PostgreSQL volume even if the option is later disabled. Do not enable this option outside a local/demo environment.

Demo sign-in credentials (local use only):

| Role | Username | Password |
| --- | --- | --- |
| Staff | `demo-staff` | `BookNestDemoStaff2026!` |
| Patron | `demo-patron` | `BookNestDemoReader2026!` |
| Patron | `demo-patron-two` | `BookNestDemoReaderTwo2026!` |

These public demo credentials are unsuitable for any public deployment. They are stored in the database only as BCrypt hashes. To try checkout as a patron, sign in as either demo patron; for catalog/member/loan management, sign in as demo staff. Loans are due 14 calendar days after checkout. When a copy is returned, it is held for the first eligible patron in the FIFO queue.

Stop services without deleting data:

```powershell
docker compose down
```

Do not remove the `booknest-postgres-data` volume to apply migrations or recover an account. Back up the database before any manual database operation. Migrations are forward-only.

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
| `GET /api/books/**` | Public | Browse catalog and available-copy counts |
| `/api/books/**` writes | Staff/admin, CSRF | Manage titles and physical copies |
| `/api/members/me` | Patron, CSRF for update | Read/update only the current patron's profile |
| `/api/members/**` | Staff/admin, CSRF for writes | Manage unlinked member records |
| `/api/loans/**` | Patron/staff/admin; CSRF for writes | Patrons access own loans; staff/admin manage returns |
| `POST /api/loans/{loanId}/renew` | Patron (own loan), staff/admin; CSRF | Renew an eligible loan once; add 14 days to its current due date |
| `/api/reservations/**` | Patron/staff/admin; CSRF for writes | Join, view, cancel, and fulfill reservation queues |
| `/api/admin/accounts/**` | Admin, CSRF for writes | List accounts, provision staff, and reset patron/staff passwords |

Admin self-recovery is intentionally not exposed as a web endpoint; it is the local Docker Compose command above.

## Tests

The test profile uses an in-memory H2 database and runs all Flyway migrations.

Windows:

```powershell
.\mvnw.cmd --no-transfer-progress clean verify
```

macOS/Linux:

```bash
./mvnw --no-transfer-progress clean verify
```

Automated tests cover registration, roles and authorization, profile ownership, temporary-password flows, reset/session invalidation, CSRF, books/copies, members, checkout/return, one-time renewal eligibility and attribution, concurrent checkout/renewal, renewal-versus-queue serialization, and reservation queue behavior.

## Current scope

The responsive interface uses same-origin HTML/CSS/JavaScript and does not store passwords or patron data in browser storage. Search/filter/backend pagination and a dashboard are not implemented yet; there is no frontend framework, cloud deployment, email service, OAuth, JWT, or microservice architecture.

See [PHASE_CHECKLIST.md](PHASE_CHECKLIST.md) and [PHASE_CHECKLIST.vi.md](PHASE_CHECKLIST.vi.md) for acceptance and verification status. Project/agent context is in [AGENTS.md](AGENTS.md).
