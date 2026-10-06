# MSME ERP — Backend API

Multi-tenant ERP backend for Indian MSMEs (GST-ready sales, purchase and inventory), built with
**Spring Boot 3.4**, **Java 21**, **Spring Security 6 (JWT)**, **Spring Data JPA**, **PostgreSQL**
and **Flyway**. Outbound email goes through **AWS SES**.

---

## Architecture & modules

Package-by-feature under `src/main/java/com/msmeerp/`:

```
├── MsmeErpApplication.java
├── config/          # Security, JPA auditing, AWS
├── common/          # Base entities, exceptions, ApiResponse/PagedResponse, rate limiting, validation
├── tenant/          # Tenant context, X-Tenant-ID filter/interceptor, tenant resolution
├── auth/            # JWT login, refresh token, forgot/reset/change password
├── onboarding/      # Company sign-up: register → OTP → claim portal ID → set admin password
├── accesscontrol/   # Roles, permissions, role/user module permissions, per-tenant module switches + guard
├── user/            # Members, invites, accept-invite, company details
├── inventory/       # Products, warehouses, stock levels, adjust/transfer/reserve, movement ledger
├── trade/           # Parties, sales & purchase document flows, payments, ageing
├── compliance/      # E-invoice, e-way bill, payment reminders (sandbox GSP + logging WhatsApp client)
└── reports/         # Dashboard, sales/purchase registers, stock valuation
```

### Feature status

| Module | Status |
|---|---|
| Onboarding, auth, users, RBAC, module switches | Implemented |
| Inventory | Implemented |
| Sales: quotation → order → delivery → invoice, credit notes, receipts, receivables ageing | Implemented |
| Purchase: order (approval) → receipt → bill, debit notes, payments, payables ageing, reorder suggestions | Implemented |
| E-invoice / e-way bill / reminders | Implemented against a **sandbox** GSP — no live provider yet |
| Reports | Implemented |
| Audit log | Not started (`V8` migration is a placeholder) |
| Production, Accounts, CRM, HR | Not started (present only in `ModuleCode`) |

---

## Getting started

### Prerequisites
- Java 21
- PostgreSQL 15+ with a database named `msmeerp_dev` (user/password `postgres`/`postgres` by default)

### Run locally
```powershell
$env:SPRING_PROFILES_ACTIVE = "dev"
.\mvnw.cmd spring-boot:run
```

Or create a `run-local.ps1` (gitignored) that sets these variables plus any AWS credentials.
Flyway applies all pending migrations on startup.

With the `dev` profile, mail is **logged instead of sent** unless `MAIL_ENABLED=true`, so OTPs,
invite links and reset links appear in the console. Override the database with `DB_URL`,
`DB_USER` and `DB_PASSWORD`.

### Tests
```powershell
.\mvnw.cmd test
```

---

## API

- Base URL: `http://localhost:8080/api/v1`
- Tenant header: `X-Tenant-ID: <portalId>` (defaults to `msmeerp-main`)
- Auth header: `Authorization: Bearer <JWT>`

There is no Swagger UI. The controllers under `*/controller/` are the reference for endpoints.

---

## Deployment & operations

- `Dockerfile` and `docker-compose.yml` run the backend container against an external RDS Postgres;
  see `.env.example` for the required variables.
- `docs/OPERATIONS.md` covers wildcard DNS/TLS, backups and error tracking.
