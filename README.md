# CampusBite

A single-canteen college cafeteria pre-ordering system: students pre-order food, the system auto-assigns the
earliest **realistic** pickup slot based on live kitchen load, and staff run the kitchen off a
live, slot-sorted queue.

The repository includes focused security, authorization, concurrency, idempotency and integration tests plus a GitHub Actions build.
This release is intended as a final-year Java/Spring Boot project; run the full Maven verification locally or in CI before deployment.

---


## 0. Product scope

CampusBite models **one college canteen** (the campus main canteen), not a restaurant marketplace or multi-canteen network. Its core promise is queue-free pre-ordering: students browse the campus menu, pay from their wallet, receive a capacity-aware pickup slot, and collect using a QR/token at the canteen counter.

College-specific capabilities include:
- Student USN/login ID plus department, academic year and section profile details.
- One configurable canteen with opening/closing hours and an online-ordering toggle.
- Admin-managed college holidays / maintenance closure dates.
- Pickup slots are automatically kept inside operating hours and skip closed dates.
- Student, kitchen staff, teacher and admin workflows are separated with RBAC; only students and teachers can place orders, while privileged accounts are admin-provisioned.
- Kitchen queue, load-aware slot allocation, QR pickup, cancellation/refund rules, no-show handling and ratings.
- Wallet payments are simulated for the project; no real banking/payment gateway is required.

## 1. Stack

- **Backend:** Java 21, Spring Boot 3.3, Spring Security + JWT (DB-backed refresh token
  rotation), Spring Data JPA/Hibernate, MySQL, Maven, STOMP/WebSocket for live updates,
  Web Push (real browser notifications) via `nl.martijndwars:web-push`
- **Frontend:** Plain HTML5/CSS3/vanilla JS + Bootstrap 5 (layout grid only) + Axios + Chart.js
  + SockJS/STOMP. All libraries bundled locally under `frontend/vendor/` — nothing loads from a
  CDN, and there's no external font import (system font stacks only).
- **Architecture:** package-by-feature (`auth`, `menu`, `cart`, `order`, `slot`, `kitchenload`,
  `trust`, `wallet`, `analytics`, `push`, `rating`, `broadcast`, `user`)


## 1.1 Engineering and security highlights

CampusBite is deliberately more than a CRUD demonstration. The implementation includes:

- **Authentication:** short-lived JWT access tokens with database-backed refresh-token rotation, row locking, and session revocation.
- **Authorization:** role-based access control for `STUDENT`, `STAFF`, `ADMIN`, and `TEACHER`, including resource-level WebSocket subscription checks; deactivated accounts lose existing REST/refresh access immediately.
- **Credential security:** BCrypt password hashing, atomic password-reset/email-verification token consumption, inactive-account checks, and session revocation on deactivation.
- **Concurrency:** pessimistic/atomic database operations for refresh tokens, wallets, slot capacity, and other state transitions where duplicate requests could otherwise overspend or overbook; refresh/reset/verification tokens are stored hashed at rest.
- **Idempotency:** duplicate order submissions can be safely recognized without creating duplicate orders.
- **Transaction consistency:** live WebSocket broadcasts are emitted after successful database commit rather than before transaction completion; stale pickup orders are automatically expired by a backend scheduler.
- **Browser security:** HttpOnly refresh-token cookie, origin-restricted CORS, HTML escaping for rendered API data, and safe image URL handling.
- **Operational workflows:** capacity-aware pickup slots, kitchen queue management, QR/token pickup, cancellation/refund rules, no-show handling, ratings, push notifications, and analytics.
- **Quality controls:** focused regression tests for IDOR, authorization, WebSocket access, concurrency, optimistic locking, idempotency, wallet initialization, no-show timing, stale-order expiry and rate limiting, plus CI on JDK 21/MySQL 8.

## 1.2 Resume-ready project description

**CampusBite — College Cafeteria Pre-Ordering System**

Java 21 · Spring Boot · Spring Security · JWT · JPA/Hibernate · MySQL · Flyway · WebSocket/STOMP · HTML/CSS/JavaScript

A single-canteen pre-ordering platform that lets students place capacity-aware food orders, pay from a simulated wallet, receive pickup tokens/QR codes, and track order status in real time. Built with role-based access control, refresh-token rotation, transactional concurrency controls, idempotent order submission, authenticated WebSocket subscriptions, and automated regression tests.

## 2. Database migrations

Flyway owns the schema. A brand-new database is migrated automatically through V1–V6. If you are
upgrading a very old development database from before Flyway was added, recreating it is the
simplest option because old seed data may not match the current authentication/profile model.

```sql
DROP DATABASE campusbite;
CREATE DATABASE campusbite;
```

## 3. Run the backend

```bash
cd backend
export DB_USER=campusbite
export DB_PASSWORD=yourpassword
export JWT_SECRET="replace-with-a-long-random-secret-at-least-32-bytes"
mvn spring-boot:run
```

Demo privileged accounts are **not created by default**. To seed local demo admin/staff accounts, explicitly set `SEED_DEMO_ACCOUNTS=true`, `SEED_ADMIN_PASSWORD` and `SEED_STAFF_PASSWORD` in the environment. Never commit those passwords.

Students self-register with a campus login/USN; teacher, staff and admin accounts are provisioned by an administrator.

## 3A. Run the full stack with Docker Compose

Copy `.env.example` to `.env`, provide `DB_PASSWORD`, `MYSQL_ROOT_PASSWORD` and `JWT_SECRET`, then run:

```bash
docker compose up --build
```

The frontend is served at `http://localhost:5500`, the API at `http://localhost:8080`, and MySQL is kept private inside the Compose network. The backend health endpoint is used as the frontend startup dependency.

## 4. Run the frontend

For a local static-server setup:

```bash
cd frontend
python3 -m http.server 5500
# open http://localhost:5500/index.html
```

Docker Compose also serves the frontend through Nginx on port 5500.

## 5. Full feature list

**Student:** menu browsing (search, veg/non-veg + allergen filter, real images, meal-time-gated
categories e.g. Breakfast only visible 6-11am), item customization picker, cart, smart slot
allocation, wallet payments, live order ticket with countdown timer and multi-order support, edit
or cancel an order before it's confirmed, push notifications (ready + running-late), QR/token
pickup, post-pickup star rating, order history/reorder, trust score, session management
(view/revoke logins from other devices), email verification and password reset (email delivery is not wired in this demo; development links are returned only when explicitly enabled).

**Staff:** live queue sorted by pickup slot with an audio chime on new orders, one-tap status
advance, QR/token scan-to-close, instant stock toggle, searchable order history. Stale PLACED/CONFIRMED tickets are automatically expired by the backend after their pickup window or when the canteen closes.

**Admin:** menu + category CRUD (create AND edit, with optional meal-time windows), order
search/lookup across all students, analytics (summary, best sellers, peak hours, slot
utilization, average rating), slot capacity config, user management, staff/teacher/admin account provisioning,
broadcast banner shown across student/staff pages.

**Automatic background jobs:** kitchen crowd-meter recalculation, no-show detection (auto-marks
unclaimed Ready orders, dents trust score), delay notifications (pushes an alert if kitchen
backlog threatens an order's promised pickup window), prep-time-prediction feedback loop feeding
back into slot allocation.

## 6. Known simplifications

- **Email verification & password reset** have no real email service wired up. Development links are
  returned only when `DEV_RESET_LINKS_ENABLED=true`; keep it false unless you explicitly need a local development link.
- **Payments** are a simulated campus wallet, not a real gateway.
- **Web Push** is optional; configure `VAPID_PUBLIC_KEY` and `VAPID_PRIVATE_KEY` to enable browser push. No private VAPID keys are committed in the repository. Authentication endpoints and wallet top-up also use a lightweight fixed-window rate limiter.
- **There is intentionally one canteen**; no restaurant/canteen selector is implemented.
- **Order editing** only lets a student adjust quantities on items already in the order (or
  remove one), not add brand-new items — keeps the wallet-delta logic bounded and simple.
- **Prep-time prediction** is a rolling-average accuracy factor, not a trained ML model.
- **Automated integration/concurrency tests are included.** A fresh environment should run `mvn clean verify`
  against MySQL before deployment; Flyway is wired into the Maven build and applies the complete V1–V6 migration chain on a new database.

## 7. Project layout

```
campusbite/
  backend/    Spring Boot app (Maven)
  frontend/   static HTML/CSS/JS
```

## Final engineering notes

- CampusBite intentionally models **one college canteen**; there is no multi-canteen or delivery marketplace layer.
- Pickup slots are constrained by the canteen's configured operating hours and closure dates.
- Wallet payments are simulated; admin-provisioned teacher/POSTPAID orders are accumulated for monthly statements rather than refunded through the wallet.
- JWT secrets and database credentials are required through environment variables; no production secret is embedded in the application configuration.
- The application timezone defaults to **Asia/Kolkata** and can be changed with `APP_TIME_ZONE`.
- Web Push is optional. Leave `VAPID_PUBLIC_KEY` and `VAPID_PRIVATE_KEY` empty when push is not configured.
- Before deployment, run `mvn clean verify` with a fresh MySQL database and verify the documented first-boot flow.

### Verification checklist

1. Copy `.env.example` to `.env` and replace the placeholder database/JWT/root-password values.
2. Start MySQL and the backend with Docker Compose, or configure a local MySQL instance.
3. Run `mvn clean verify` from `backend/`.
4. Register a student, add menu items to the cart, place a pre-order, and verify the pickup-slot/QR flow.
5. Log in as staff and move the order through `CONFIRMED → PREPARING → READY → PICKED_UP`.
6. Log in as admin and verify canteen hours, closure dates, menu, slot capacity and analytics.
7. Verify an expired pickup window is automatically removed from the kitchen queue and, for wallet orders, refunded.
8. Verify the WebSocket connection and authenticated per-user/staff subscriptions; polling remains as a fallback.


### Final security/configuration notes
- STOMP/WebSocket connections authenticate with the same short-lived JWT used by REST APIs; subscriptions to staff and per-user order topics are authorization-checked. The client falls back to polling if the socket is unavailable.
- WebSocket origins use the configured `CORS_ORIGINS` list rather than a wildcard.
- User-controlled text rendered by the frontend is HTML-escaped before insertion into dashboard templates to prevent stored XSS.
- Refresh, password-reset, and email-verification token consumption uses row-level locking; opaque authentication tokens are stored only as SHA-256 hashes at rest.
- Live WebSocket broadcasts are registered for transaction `afterCommit`, preventing clients from seeing events for transactions that later roll back.
- `DEV_RESET_LINKS_ENABLED=true` is for local development only because no email provider is wired up. Keep it `false` in deployed environments; forgot-password and verification-link responses are generic/empty when disabled.
- Wallet endpoints are restricted to `STUDENT`; teachers use the postpaid order flow.
- Wallet creation is concurrency-safe and awards the signup bonus exactly once.
- Meal-time menu gating uses the configured campus timezone.
- Database credentials and JWT secrets are environment-provided. Docker Compose uses a dedicated `campusbite` MySQL application user rather than root; only the backend health endpoint is intentionally public, while application APIs remain protected by their route-level security rules.
- Frontend API/WebSocket endpoints are centralized in `frontend/js/config.js`.

## 9. Release verification

Run these checks before presenting or deploying the project:

```bash
cd backend
mvn clean verify
```

The GitHub Actions workflow performs Maven verification on JDK 21 with MySQL 8. The repository also includes frontend JavaScript that can be syntax-checked with:

```bash
find ../frontend -name "*.js" -not -path "*/vendor/*" -print0 | xargs -0 -n1 node --check
```

For production deployment, provide strong random `JWT_SECRET`, `DB_PASSWORD`, and `MYSQL_ROOT_PASSWORD` values, use HTTPS, configure `campusbite.cookie.secure=true`, keep `DEV_RESET_LINKS_ENABLED=false`, and use a shared rate limiter if running multiple backend instances.

