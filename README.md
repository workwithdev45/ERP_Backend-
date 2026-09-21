# Hospital Management System (HMS) - Backend API

Enterprise-grade, Multi-Tenant Hospital Management System (HMS) backend built with **Spring Boot 3.4**, **Java 21**, **Spring Security 6 (JWT)**, **Spring Data JPA**, **PostgreSQL**, **Redis**, **Flyway**, and **Springdoc OpenAPI (Swagger 3)**.

---

## 🏗️ Architecture & Modules

The application adopts a **Modular Domain / Package-by-Feature** architecture:

```
src/main/java/com/hms/
├── HmsApplication.java
├── config/              # Security, OpenAPI, Redis, WebMvc, JPA Auditing
├── common/              # BaseEntity, GlobalExceptionHandler, ApiResponse, PagedResponse
├── tenant/              # Multi-tenancy context, filter, interceptor, tenant resolver
├── auth/                # JWT TokenProvider, UserPrincipal, Login/Register
├── accesscontrol/       # RBAC: Role & Permission controllers, services, entities
├── user/                # User accounts & staff management
├── hospital/            # Branches, Departments, Rooms, Bed inventory
├── patient/             # Patient registration, UHID, medical history
├── doctor/              # Doctor profiles, specialties, consulting schedules
├── nursing/             # Nursing stations, shift rosters, duty history
├── opd/                 # Outpatient appointments, consultations, prescriptions
├── ipd/                 # Inpatient admissions, bed transfers, vitals, discharge summaries
├── laboratory/          # Diagnostic tests, lab orders, test results
├── pharmacy/            # Medicine stock batches, prescription dispensing
├── billing/             # Invoices, payments, refunds
├── hr/                  # Employee onboarding, leave management, payroll
├── reports/             # Hospital analytics, KPIs, dashboard statistics
└── audit/               # Spring AOP aspect logging for mutating REST calls
```

---

## 🚀 Getting Started

### Prerequisites
- **Java 21 LTS**
- **PostgreSQL 15+**
- **Redis 7+**

### Run Database & Redis (Docker Compose optional)
```bash
docker run --name hms-postgres -e POSTGRES_DB=hms_db -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres -p 5432:5432 -d postgres:16-alpine
docker run --name hms-redis -p 6379:6379 -d redis:alpine
```

### Build & Run Locally
```powershell
# Compile & verify codebase
.\mvnw.cmd clean compile

# Run application
.\mvnw.cmd spring-boot:run
```

---

## 📖 API Documentation & Swagger UI
Once running, interactive OpenAPI Swagger documentation is accessible at:
- **Swagger UI**: [http://localhost:8080/api/v1/swagger-ui.html](http://localhost:8080/api/v1/swagger-ui.html)
- **OpenAPI JSON**: [http://localhost:8080/api/v1/v3/api-docs](http://localhost:8080/api/v1/v3/api-docs)

---

## 🔐 Multi-Tenancy & Headers
All requests can supply the `X-Tenant-ID` header (defaults to `hms-main`):
```http
X-Tenant-ID: hms-main
Authorization: Bearer <JWT_TOKEN>
```
