# ARCHITECTURE

## Frontend Architecture

The frontend is a React application located in `web/`.

- Uses `react-scripts` and standard Create React App structure.
- API client code is generated from OpenAPI and stored in `web/src/api/generated/`.
- The frontend communicates with the backend over HTTP at `http://localhost:8080`.
- Frontend concerns are strictly isolated from backend concerns.
- Route paths and API interactions are logically segmented by domain (`/wealth`, `/household`, `/health`).

### Frontend responsibilities

- Render UI, navigation, and domain-specific composite views (e.g., Vacation Planner, Unified Dashboard).
- Call backend APIs for data ingestion, scheduling, and profile management.
- Use generated OpenAPI client for typed, contract-driven requests.
- Keep state and presentation separate from business rules.

---

## Backend Architecture

The backend is a unified Quarkus application split across three domains:

| Module | Domain | DB |
|---|---|---|
| `application/wealth` | Wealth — transactions, accounts, vehicle compliance | PostgreSQL |
| `application/health` | Health — biometric tracking, fitness profiles | MongoDB |
| `application/household` | Household — profiles, calendar, inventory, automation | PostgreSQL |

All modules are served from a single Quarkus runtime on port `8080`.

### Hexagonal Architecture

The system follows Ports and Adapters across all modules:

- `domain/` — core business entities and logic. No framework dependencies.
- `ports/in/` — use case interfaces (input boundaries).
- `application/` — business workflow orchestration.
- `ports/out/` — repository and external service interfaces (output boundaries).
- `adapters/in/http/` — REST controllers and HTTP endpoints.
- `adapters/out/persistence/` — database access implementations.

### Domain Separation

Three isolated domains, each self-contained:

- `wealth` — Financial transaction ledgers, account management, investment records, and vehicle compliance tracking.
- `household` — Family profile registry, multi-day calendar scheduling, task assignment, grocery inventory, and smart home device mapping.
- `health` — Profile-linked, unstructured time-series biometric tracking stored in MongoDB.

**Key rules:**
- `domain/` must not depend on any framework, adapter, or other domain.
- Cross-domain logic is strictly enforced via API boundaries or application-layer orchestration in `shared/`.
- No direct database joins across domains — ever.

### Key Architectural Rules

- `application/` orchestrates domain use cases without direct DB or HTTP dependencies.
- `adapters/out/` depend on `infrastructure/` for shared plumbing.
- `infrastructure/` contains only wiring, database pools, and shared configuration.
- `shared/` contains cross-cutting utilities: authentication, logging, and cross-domain orchestration interfaces.

---

## Database Architecture

| Domain | Database | Migration Tool |
|---|---|---|
| Wealth | PostgreSQL (`app_db`) | Flyway |
| Household | PostgreSQL (`app_db`) | Flyway |
| Health | MongoDB | Schema-less (document model) |

- PostgreSQL migrations live in `application/wealth/src/main/resources/db/migration/`.
- Each domain owns its tables — no cross-domain joins in SQL.
- MongoDB collections are schema-validated at the application layer, not the DB layer.

---

## API Reference

The unified Quarkus application exposes all domain APIs from a single backend.

**Base URL:** `http://localhost:8080`
**OpenAPI Spec:** `http://localhost:8080/q/openapi`

---

### Wealth Endpoints

#### Accounts
- `GET    /api/v1/accounts` — list all accounts
- `POST   /api/v1/accounts` — create account
- `GET    /api/v1/accounts/{account_id}` — get account by ID
- `PATCH  /api/v1/accounts/{account_id}` — update account
- `DELETE /api/v1/accounts/{account_id}` — deactivate account

#### Transactions
- `POST /api/v1/transactions:uploadCsv` — upload a CSV file
- `GET  /api/v1/transactions` — list transactions (paginated)
- `GET  /api/v1/transactions:config` — get dropdown config values

#### Vehicle Assets
- `GET    /api/v1/vehicles` — list registered vehicles
- `POST   /api/v1/vehicles` — register a vehicle
- `GET    /api/v1/vehicles/{vehicle_id}` — get vehicle details
- `PATCH  /api/v1/vehicles/{vehicle_id}` — update vehicle or compliance deadlines
- `DELETE /api/v1/vehicles/{vehicle_id}` — remove vehicle

---

### Household Endpoints

#### Profiles
- `GET    /api/v1/household-profiles` — list household profiles
- `POST   /api/v1/household-profiles` — create profile (Primary, Partner, Child)
- `GET    /api/v1/household-profiles/{profile_id}` — get profile
- `PATCH  /api/v1/household-profiles/{profile_id}` — update profile
- `DELETE /api/v1/household-profiles/{profile_id}` — remove profile

#### Calendar Events
- `GET    /api/v1/events` — list calendar events
- `POST   /api/v1/events` — create event
- `GET    /api/v1/events/{event_id}` — get event with sub-events
- `PATCH  /api/v1/events/{event_id}` — update event
- `DELETE /api/v1/events/{event_id}` — delete event

#### Inventory (Supply Chain)
- `GET  /api/v1/inventory` — list inventory items
- `POST /api/v1/inventory:importOrder` — import grocery order export
- `GET  /api/v1/inventory/{item_id}` — get item
- `DELETE /api/v1/inventory/{item_id}` — remove item

#### Home Automation
- `GET    /api/v1/devices` — list registered smart devices
- `POST   /api/v1/devices` — register device
- `PATCH  /api/v1/devices/{device_id}` — update device state config
- `DELETE /api/v1/devices/{device_id}` — remove device

---

### Health Endpoints

#### Health Profiles
- `GET    /api/v1/health-profiles` — list health profiles
- `POST   /api/v1/health-profiles` — create health profile
- `GET    /api/v1/health-profiles/{profile_id}` — get health profile
- `PATCH  /api/v1/health-profiles/{profile_id}` — update health profile
- `DELETE /api/v1/health-profiles/{profile_id}` — delete health profile

#### Biometrics
- `GET  /api/v1/biometrics` — list biometric entries (chronological)
- `POST /api/v1/biometrics` — log a biometric entry
- `GET  /api/v1/biometrics/{entry_id}` — get single entry
- `DELETE /api/v1/biometrics/{entry_id}` — delete entry

---

### Cross-Domain Endpoints (v0.5+)

- `GET /api/v1/dashboard/actions` — unified action center (read-only alerts across all domains)
- `GET /api/v1/trips/{event_id}/feasibility` — vacation planner: budget + vehicle compliance check

---

### Frontend Integration

- The frontend syncs with the runtime OpenAPI spec using `npm run generate:api`.
- API client code is generated and stored in `web/src/api/generated/`.
- OpenAPI contract files live in `openapi/wealth.yaml`, `openapi/health.yaml`, `openapi/household.yaml`.
- All endpoints are served from the single Quarkus runtime.

---

## Security & Multi-Tenancy Architecture (v1.0+)

### Authentication and Role-Based Access Control (RBAC)

- The system delegates identity management to an external OIDC/OAuth2 Identity Provider.
- The Quarkus security context manages the active user session.
- Strict boundaries exist between Administrative profiles (Adults) and Restricted profiles (Children).
- Every endpoint validates the active profile's role before processing.

### Data Isolation

- Every database query across all domains (`wealth`, `household`, `health`) must be scoped to the active `profile_id`.
- Adapters are responsible for injecting this filter — never the domain layer.
- Cross-domain composite views must respect visibility limits.
- Restricted profiles must not trigger queries to unauthorized domains (e.g., a Child profile cannot query Wealth ledgers for trip budgets).

### Data Privacy & Hardening

- **Encryption at Rest:** Sensitive financial ledgers within the `wealth` domain are encrypted at the application layer (`adapters.out.persistence`) before database insertion.
- **External API Tokens:** Only short-lived OAuth access tokens are used for external integrations (e.g., Google Fit). Storing offline or refresh tokens in the database is strictly prohibited.
- **Schema Migrations:** All PostgreSQL schema changes must use versioned Flyway migrations. No manual schema edits on persistent databases.
- **Data Export/Import:** Exported data is encrypted and signed with a private key. Importing data requires a matching public key and signature verification.
- **Data Retention:** User data is retained for 5 years after last activity, then securely deleted.
- **Audit Logging:** All data access and modifications are logged with timestamps and user IDs for forensic analysis.
- **Access Control:** Role-based access control (RBAC) ensures only authorized users can view or modify data.
- **Throttling & Rate Limiting:** API endpoints are rate-limited to prevent abuse and ensure fair usage.
- **DDoS Protection:** Automated DDoS mitigation measures are in place to protect against distributed denial-of-service attacks.
- **Regular Security Audits:** Periodic security audits and penetration testing are conducted to identify and fix vulnerabilities.
- **Incident Response Plan:** A comprehensive incident response plan is in place to handle security breaches and data breaches.
- **Compliance:** Adherence to relevant data protection regulations (e.g., GDPR, CCPA) is enforced through automated compliance checks and regular audits.
- **Data Breach Notification:** Users are promptly notified of any data breaches and provided with steps to protect their accounts.
- **Privacy by Design:** Privacy considerations are integrated into the design and development process from the start.