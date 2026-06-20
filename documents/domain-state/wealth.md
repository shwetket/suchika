# Wealth Domain State

**Last updated:** 2026-06-20
**Version:** v0.2 complete — UAT-ready
**Port:** 8082

---

## Implementation Status

| Component | Status | Notes |
|---|---|---|
| Account CRUD | ✅ Complete | 7 account types |
| Transaction list/filter | ✅ Complete | Date range + type filters |
| CSV Statement Upload | ✅ Complete | PENDING → SUCCESS/FAILED |
| Upload rollback | ✅ Complete | Deletes all txns for an upload |
| Deduplication | ✅ Complete | Cross-file dups rejected to error_log |
| Physical Assets | ✅ Complete | |
| Dashboard CQRS projections | 🔲 v0.3 | projections.dashboard_snapshot not live |
| Transaction pagination | 🔲 v0.3 | |

---

## Database Schema (`wealth` schema)

| Table | Key Columns |
|---|---|
| `account` | `id UUID PK`, `profile_id UUID FK`, `name VARCHAR`, `type VARCHAR`, `balance NUMERIC`, `is_active BOOLEAN` |
| `transaction` | `id UUID PK`, `account_id UUID FK`, `profile_id UUID FK`, `amount NUMERIC ≥0`, `type VARCHAR`, `date DATE`, `description VARCHAR` |
| `statement_upload` | `id UUID PK`, `account_id UUID FK`, `profile_id UUID FK`, `file_name VARCHAR`, `status VARCHAR`, `upload_date TIMESTAMP` |
| `upload_error_log` | `id UUID PK`, `upload_id UUID FK`, `row_number INT`, `raw_row TEXT`, `error_message TEXT` |
| `physical_asset` | `id UUID PK`, `profile_id UUID FK`, `name VARCHAR`, `type VARCHAR`, `value NUMERIC`, `acquisition_date DATE` |

Account types (VARCHAR, no SQL ENUM): `SAVINGS`, `CURRENT`, `CREDIT_CARD`, `HOME_LOAN`, `PERSONAL_LOAN`, `INVESTMENT`, `FD`
Transaction types: `CREDIT`, `DEBIT`
Upload status: `PENDING` → `SUCCESS` | `FAILED`

---

## API Contract

File: `application/contract/wealth.yaml`
Base path: `/api/v1/wealth`
- `GET    /accounts?profile_id=`
- `POST   /accounts?profile_id=`
- `GET    /accounts/{id}`
- `PUT    /accounts/{id}`
- `DELETE /accounts/{id}`
- `GET    /accounts/{id}/transactions`
- `POST   /accounts/{accountId}/uploads?profile_id=` — upload CSV
- `GET    /accounts/{accountId}/uploads`
- `DELETE /accounts/{accountId}/uploads/{uploadId}/rollback`

---

## Key Files

| Layer | Path |
|---|---|
| Domain | `application/domain/wealth/domain/src/main/java/com/suchika/wealth/domain/` |
| Ports | `application/domain/wealth/ports/src/main/java/com/suchika/wealth/ports/` |
| Adapters | `application/domain/wealth/adapters/src/main/java/com/suchika/wealth/adapters/` |
| Flyway | `application/flyway/wealth/` |
| Frontend | `web/src/pages/Wealth/` (Accounts.js, Transactions.js, Reports.js) |
| API module | `web/src/api/wealth.js` |

---

## Key Design Decisions (ADRs)

- `CreateAccountCommand` has **7 fields** — `profileId` is passed separately: `createAccount(UUID profileId, CreateAccountCommand cmd)`. Never add `profileId` back into the command (SonarQube S107 — too many constructor params).
- Dedup: rows from the same file are kept (user intent). Cross-file identical rows → rejected to `upload_error_log`.
- No SQL ENUMs anywhere — all discriminators are `VARCHAR` enforced at the OpenAPI contract layer.
- All DB queries filtered by `profile_id` in the adapter layer only.

---

## Open Issues / v0.3+ Backlog

- Wire `projections.dashboard_snapshot` to live wealth data (v0.3)
- Add transaction pagination (v0.3)
- Error handling for malformed CSV rows (v0.4)
- Duplicate resolution UI for `is_duplicate=TRUE` rows (v0.4)
