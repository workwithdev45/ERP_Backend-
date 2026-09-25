# Operations runbook (G25)

This is the wildcard DNS/TLS, backup, and monitoring plan the product plan flagged as missing.
Nothing here requires code changes — it's a checklist for whoever has AWS console access,
written against the architecture already assumed by `docker-compose.yml` (EC2 + Portainer running
the backend container, RDS Postgres, CloudFront in front for TLS and subdomain routing).

## 1. Wildcard DNS + TLS

Each tenant workspace is reached at `<portalId>.<PORTAL_BASE_DOMAIN>` (see
`app.portal.base-domain` in `application-prod.yml`). Until a domain is purchased, the app runs
against a plain IP/port. Once a domain is bought:

1. **Request a wildcard ACM certificate** for `*.yourdomain.com` (and the bare `yourdomain.com`)
   in `us-east-1` (CloudFront only accepts ACM certs from that region, regardless of where the
   rest of the stack lives) — validate via DNS (add the CNAME ACM gives you to the domain's DNS).
2. **Create a wildcard DNS record**: `*.yourdomain.com` → CNAME to the CloudFront distribution's
   domain name. One record covers every current and future tenant subdomain — no DNS change
   needed when a new workspace is provisioned.
3. **CloudFront distribution**: origin = the EC2 instance's public DNS on port 8080 (HTTP is fine
   for the CloudFront→origin leg since that hop stays inside AWS); viewer protocol policy =
   "Redirect HTTP to HTTPS"; attach the wildcard cert from step 1.
4. **CloudFront Function** (viewer-request, ~10 lines): read the `Host` header's subdomain and
   set it as the `X-Tenant-ID` request header before forwarding to the origin — this is what lets
   the existing header-based tenant resolution (`TenantFilter`) work without a code change.
5. **Lock down the EC2 security group** to the `com.amazonaws.global.cloudfront.origin-facing`
   managed prefix list on port 8080, so the box is only reachable through CloudFront, never
   directly by IP.
6. Update `PORTAL_BASE_DOMAIN`, `PORTAL_SCHEME=https`, `FRONTEND_URL` (frontend env var
   `VITE_...`), and `CORS_ALLOWED_ORIGINS=https://*.yourdomain.com` once the domain is live —
   `SecurityConfig` already uses `setAllowedOriginPatterns`, so the wildcard pattern is honoured.

## 2. Database backups + restore drill

RDS Postgres takes automated snapshots on its own, but "backups exist" only counts once a restore
has actually been rehearsed.

1. **Enable automated backups** on the RDS instance: Modify → Backup retention period → 7 days
   minimum (14–30 if the plan allows; free tier / small instances support this at no extra
   compute cost, only extra storage for the snapshots).
2. **Turn on deletion protection** on the RDS instance so it can't be dropped by mistake.
3. **Quarterly restore drill** (put this on a recurring calendar reminder — RDS doesn't do this
   for you):
   - Restore the latest automated snapshot into a new, throwaway RDS instance.
   - Point a local backend (`SPRING_PROFILES_ACTIVE=dev`, `DB_URL` pointed at the restored
     instance) at it and confirm the app boots and Flyway reports the migrations as already
     applied (i.e. the schema round-trips cleanly).
   - Spot-check a handful of rows in `tenants` and `users` against what's expected.
   - Delete the throwaway instance afterward — it exists only to prove the backup is restorable.
4. **Before any manual migration or data fix in prod**, take a manual RDS snapshot first (a few
   clicks, and it's the fastest rollback path if the fix goes wrong).

## 3. Error tracking

Nothing is wired up yet. Recommended: **Sentry** (has a free tier that's enough for a single
small app) — add the `sentry-spring-boot-starter` dependency, set `SENTRY_DSN` as an env var in
`docker-compose.yml`, and errors thrown from any controller/service surface with a stack trace,
the tenant ID, and the endpoint, instead of only being visible in `docker logs`. This is a follow-up
task, not yet implemented, since it needs a Sentry account to generate the DSN.

## 4. Uptime alerts

Nothing is wired up yet. Recommended, in order of effort:

- **UptimeRobot** (free tier): a single HTTP(S) monitor hitting a lightweight health endpoint
  (Spring Boot Actuator's `/actuator/health`, once added — it isn't on the classpath yet) every
  5 minutes, alerting by email/SMS/Slack on failure. Fastest to set up, needs no AWS changes.
- **CloudWatch alarm** on the EC2 instance's `StatusCheckFailed` metric, if staying entirely
  inside AWS is preferred over a third-party monitor.

This is a follow-up task, not yet implemented — it needs an UptimeRobot (or equivalent) account
and, for the health-check option, adding `spring-boot-starter-actuator` to `pom.xml`.
