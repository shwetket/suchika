# Wealth Domain State

## Objective

Give any agent or developer instant context on the wealth domain — accounts, transactions, CSV uploads, physical assets. Includes schema, ADRs baked into Key Design Decisions, and the current backlog.

## Use Cases

- Before working on wealth backend or frontend — check Implementation Status and Key Files
- When adding a new feature to wealth — review Key Design Decisions to avoid re-introducing known anti-patterns (e.g., profileId in CreateAccountCommand)
- After completing wealth work — update Implementation Status and Open Issues

---

**Last updated:** 2026-06-29
**Version:** v0.4 Phase 1+2 complete
**Port:** 8082

---

## Implementation Status

| Component | Status | Notes |
|---|---|---|
| Account CRUD | ✅ Complete | 7 account types |
| Transaction list/filter | ✅ Complete | Date range + type filters |
| CSV Statement Upload | ✅ Complete | PENDING → SUCCESS/FAILED |
| Upload rollback | ✅ Complete | Deletes all txns for an upload |
| Deduplication | ✅ Complete | Cross-file dups rejected; 4-field key (no description) |
| Physical Assets | ✅ Complete | |
| Malformed CSV rejection | ✅ Complete | CsvParseException → upload_error_log |
| Upload error log endpoint | ✅ Complete | GET /{account_id}/uploads/{upload_id}/errors |
| Skipped rows in upload response | ✅ Complete | skipped_duplicates list + inserted_count |
| Dashboard CQRS projections | 🔲 v0.3 | projections.dashboard_snapshot not live |
| Transaction pagination | 🔲 v0.3 | |

---

## Database Schema (`wealth` schema)

| Table | Key Columns |
|---|---|
| `account` | `id UUID PK`, `profile_id UUID FK`, `name VARCHAR`, `type VARCHAR`, `balance NUMERIC`, `is_active BOOLEAN` |
| `transaction` | `id UUID PK`, `account_id UUID FK`, `profile_id UUID FK`, `amount NUMERIC ≥0`, `type VARCHAR`, `date DATE`, `description VARCHAR` |
| `statement_upload` | `id UUID PK`, `account_id UUID FK`, `profile_id UUID FK`, `file_name VARCHAR`, `status VARCHAR`, `upload_date TIMESTAMP` |
| `upload_error_log` | `id UUID PK`, `upload_id UUID FK`, `error_type VARCHAR(50)`, `missing_columns TEXT[]`, `error_detail TEXT`, `created_at TIMESTAMPTZ` |
| `physical_asset` | `id UUID PK`, `profile_id UUID FK`, `name VARCHAR`, `type VARCHAR`, `value NUMERIC`, `acquisition_date DATE` |

Account types (VARCHAR, no SQL ENUM): `SAVINGS`, `CURRENT`, `CREDIT_CARD`, `HOME_LOAN`, `PERSONAL_LOAN`, `INVESTMENT`, `FD`
Transaction types: `CREDIT`, `DEBIT`
Upload status: `PENDING` → `SUCCESS` | `FAILED`
Error types: `MISSING_DATE_COLUMN`, `MISSING_AMOUNT_COLUMN`, `MISSING_REQUIRED_COLUMN`, `PARSE_ERROR`, `VALIDATION_ERROR`

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
- `POST   /accounts/{accountId}/uploads?profile_id=` — upload CSV (returns `StatementUploadResponse` with `inserted_count` + `skipped_duplicates`)
- `GET    /accounts/{accountId}/uploads`
- `DELETE /accounts/{accountId}/uploads/{uploadId}/rollback`
- `GET    /accounts/{accountId}/uploads/{uploadId}/errors` — NEW: returns `List<UploadErrorLogResponse>`

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
- Dedup key is **4 fields**: `(account_id, txn_date, amount, txn_type)` — description excluded to handle cases where the same transaction has slightly different narrations across files. DB unique constraint `uq_transaction_dedup` still includes description as a last-resort safety net.
- Same-file duplicate rows are disambiguated by appending ` #2`, ` #3` etc. to the description. These variants bypass the cross-file dedup check (tracked in `insertedThisBatch` set in `StatementUploadService`).
- `CsvParseException` extends `BadRequestException` and lives in `adapters/services/` — it carries a machine-readable `errorType` and `missingColumns` list so the upload service can write a structured `upload_error_log` row before re-throwing.
- `StatementUploadUseCase.uploadStatement()` returns `UploadResult` (not `StatementUpload`) — wraps the upload entity with `insertedCount` and `List<SkippedRow>`.
- No SQL ENUMs anywhere — all discriminators are `VARCHAR` enforced at the OpenAPI contract layer.
- All DB queries filtered by `profile_id` in the adapter layer only.

---

## Open Issues / Backlog

- Wire `projections.dashboard_snapshot` to live wealth data (v0.3)
- Add transaction pagination (v0.3)
- Frontend: display `skipped_duplicates` and `error log` in the upload UI (v0.4 Phase 3)
- Duplicate resolution UI for `is_duplicate=TRUE` rows (v0.4)
