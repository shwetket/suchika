# Business Requirements — Finance CSV Upload (V1)

**Status:** Phase 1 implementation underway

This document contains the functional and technical requirements for the Finance CSV upload feature.

---

## 1. Scope

A single-user personal finance tool. Non-technical users upload CSV files exported from their bank or investment accounts. The app parses, normalizes, and stores data in PostgreSQL. No file is ever persisted to disk.

**In scope for V1:**
- Single-user CSV upload workflow
- Banking transaction storage (SAVINGS, CREDIT_CARD, LOAN)
- Investment transaction storage
- Automatic deduplication (flagging, not rejection)
- Configuration-driven account/fund names

**Out of scope for V1:**
- Multi-user / authentication
- Duplicate Accept/Reject UI (Phase 2)
- Transfer reconciliation (Phase 2)
- Dashboard / analytics (Phase 3+)
- AI categorization (Never)
- File archiving (Never)

See [Roadmap](./ROADMAP.md) for future phases.

---

## 2. Configuration — application.properties

Account names and fund names are ENUM-driven, validated at runtime against a config-loaded list. No code recompile required to add new accounts.

```properties
# Banking account names
app.finance.account-names=HDFC_SAVINGS,ICICI_CREDIT_CARD,SBI_LOAN,HDFC_CREDIT_CARD

# Investment fund names
app.finance.fund-names=PF,NPS,FLEXI_FUND,LARGE_CAP,MID_CAP
```

**Rules:**
- Values are `SCREAMING_SNAKE_CASE` (no spaces — avoids encoding issues)
- Display labels are derived at UI layer (e.g., `HDFC_SAVINGS` → "HDFC Savings")
- Backend validates submitted values against loaded list; rejects if unknown
- NOT PostgreSQL ENUM types — stored as `VARCHAR`, validated in application logic
  - Reason: Adding a PG ENUM requires DDL migration; `application.properties` + restart is cheaper

**Stable ENUMs (immutable without migration):**
```
AccountType: SAVINGS | CREDIT_CARD | LOAN | INVESTMENT
TxnType:    CREDIT | DEBIT
```

---

## 3. Database Schema

All transactions stored in PostgreSQL database `app_db`. Schema managed via Flyway.

### Table: `banking_transaction`

```sql
CREATE TABLE banking_transaction (
    id             BIGSERIAL       PRIMARY KEY,
    account_name   VARCHAR(100)    NOT NULL,
    account_type   VARCHAR(20)     NOT NULL CHECK (account_type IN ('SAVINGS', 'CREDIT_CARD', 'LOAN')),
    date           DATE            NOT NULL,
    amount         NUMERIC(15, 2)  NOT NULL,
    txn_type       VARCHAR(10)     NOT NULL CHECK (txn_type IN ('CREDIT', 'DEBIT')),
    description    TEXT,
    balance        NUMERIC(15, 2),
    is_duplicate   BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMPTZ     NOT NULL DEFAULT now(),

    CONSTRAINT uq_banking_txn UNIQUE (account_name, account_type, date, amount, txn_type)
);
```

**Design notes:**
- `amount` always positive; direction in `txn_type` (CREDIT/DEBIT)
- Negative amounts in CSV are converted to positive and marked as `DEBIT`
- `description` stored as-is; never used for dedup or logic
- `balance` populated only if CSV contains it
- `is_duplicate = TRUE` when row violates UNIQUE constraint — row is inserted anyway. Phase 2 adds accept/reject UI
- UNIQUE constraint is single source of truth for deduplication

### Table: `investment_transaction`

```sql
CREATE TABLE investment_transaction (
    id           BIGSERIAL       PRIMARY KEY,
    fund_name    VARCHAR(100)    NOT NULL,
    date         DATE            NOT NULL,
    amount       NUMERIC(15, 2)  NOT NULL,
    txn_type     VARCHAR(10)     NOT NULL CHECK (txn_type IN ('CREDIT', 'DEBIT')),
    metadata     JSONB,
    is_duplicate BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMPTZ     NOT NULL DEFAULT now(),

    CONSTRAINT uq_investment_txn UNIQUE (fund_name, date, amount, txn_type)
);
```

**Metadata examples:**
```json
{ "units": 12.543, "nav": 45.67 }
{ "units": 100.0 }
{}
```

---

## 4. CSV Parsing Rules

### 4.1 User-provided inputs (manual selection, not auto-detected)

User selects on upload form:
- **Account Name** — dropdown from `app.finance.account-names`
- **Account Type** — dropdown: `SAVINGS | CREDIT_CARD | LOAN | INVESTMENT`
  - If `INVESTMENT`, Account Name dropdown switches to `app.finance.fund-names`

### 4.2 Auto-detected columns

**Banking CSV — minimum required (case-insensitive, fuzzy match):**

| Canonical field | Example header variants |
|---|---|
| `date` | Date, Txn Date, Value Date, Transaction Date |
| `amount` | Amount, Txn Amount, Debit/Credit Amount |
| `txn_type` | Dr/Cr, Type, Debit, Credit (or derived from sign) |

Optional:
- `description` / Narration / Particulars / Remarks
- `balance` / Running Balance / Closing Balance

**Investment CSV — minimum required (case-insensitive):**

| Canonical field | Example header variants |
|---|---|
| `date` | Date, Transaction Date, NAV Date |
| `amount` | Amount, Purchase Amount, Redemption Amount |
| `txn_type` | Type, Transaction Type, Dr/Cr |

Optional:
- `units` / Units / No. of Units / Allotted Units
- `nav` / NAV / NAV per Unit / Price per Unit

### 4.3 Amount normalization logic

```
IF separate Debit and Credit columns exist:
    amount = whichever column has a value
    txn_type = DEBIT or CREDIT accordingly

ELSE IF single Amount column:
    IF value < 0  → amount = ABS(value), txn_type = DEBIT
    IF value >= 0 → amount = value,      txn_type = CREDIT

ELSE IF Dr/Cr flag column exists alongside Amount:
    use the flag directly
```

### 4.4 File rejection rules

Reject **entire file** if:
- Required columns cannot be found after fuzzy matching
- Date column exists but no rows parse as valid date
- Amount column exists but all values are non-numeric
- Account Name or Fund Name selected by user not in config list

**Error response must state:**
- Which column is missing/invalid
- Which config value is unrecognized

### 4.5 Duplicate handling (V1 — flag only)

When a row matches the UNIQUE constraint:
1. Insert the row anyway
2. Set `is_duplicate = TRUE`
3. Count and report in upload response
4. No silent drops — all data is kept for Phase 2 review UI

---

## 5. API Contract — OpenAPI 3.1.0 (Google AIP)

Base path: `/api/v1`

### 5.1 Upload CSV

```
POST /api/v1/transactions:uploadCsv
Content-Type: multipart/form-data
```

**Request fields:**

| Field | Type | Required | Description |
|---|---|---|---|
| `file` | file | yes | CSV file |
| `account_name` | string | yes | From config ENUM |
| `account_type` | string | yes | `SAVINGS`, `CREDIT_CARD`, `LOAN`, `INVESTMENT` |

**Response 200 — success:**
```json
{
  "inserted": 142,
  "duplicates_flagged": 3,
  "rejected_rows": 0
}
```

**Response 422 — validation failure:**
```json
{
  "code": 422,
  "status": "UNPROCESSABLE_ENTITY",
  "message": "CSV rejected: required column 'date' could not be found.",
  "details": []
}
```

**Note:** Google AIP uses `:verb` pattern for non-CRUD actions. CSV upload is not a standard `POST /transactions` (which would create one transaction), hence `:uploadCsv`.

---

### 5.2 List Transactions

```
GET /api/v1/transactions
```

**Query parameters (Google AIP pagination):**

| Param | Type | Default | Description |
|---|---|---|---|
| `page_size` | integer | 100 | Rows per page |
| `page_token` | string | — | Opaque cursor for next page |

**Response 200:**
```json
{
  "transactions": [
    {
      "id": 1,
      "account_name": "HDFC_SAVINGS",
      "account_type": "SAVINGS",
      "date": "2024-03-15",
      "amount": "1500.00",
      "txn_type": "DEBIT",
      "description": "UPI/SWIGGY",
      "balance": "24500.00",
      "is_duplicate": false,
      "created_at": "2025-05-06T10:23:00Z"
    }
  ],
  "next_page_token": "eyJpZCI6MTAwfQ=="
}
```

**Note:** Investment transactions are returned in the same endpoint — `account_type: "INVESTMENT"`, `account_name` holds the fund name, `metadata` field is included.

---

### 5.3 Get Config (dropdowns)

```
GET /api/v1/transactions:config
```

Returns currently loaded ENUM values for frontend dropdowns.

**Response 200:**
```json
{
  "account_names": ["HDFC_SAVINGS", "ICICI_CREDIT_CARD", "SBI_LOAN"],
  "fund_names": ["PF", "NPS", "FLEXI_FUND", "LARGE_CAP"],
  "account_types": ["SAVINGS", "CREDIT_CARD", "LOAN", "INVESTMENT"]
}
```

---

## 6. What is NOT in V1

| Feature | Phase | Reason |
|---|---|---|
| Duplicate Accept/Reject UI | 2 | Dedup logic in place; UI added later |
| Transaction filters/search | 2 | Basic list first; filters added when needed |
| Categories | Never | Out of scope; rule-based tagging in Phase 2 |
| Transfer reconciliation | 2 | Design TBD; too complex for V1 |
| Multi-user / auth | 3 | SaaS feature; local-only for V1 |
| Dashboard / analytics | 3+ | Depends on multi-tenancy |
| AI categorization | Never | Insufficient ROI; rule-based is simpler |
| File archiving | Never | Privacy & simplicity — parse & discard |

---

## 7. Approval Checklist

Before implementation, confirm:

- [ ] Schema (Section 3) — tables, columns, constraints, dedup logic
- [ ] CSV parsing rules (Section 4) — column detection, amount normalization, rejection criteria
- [ ] API endpoints (Section 5) — paths, request/response shape, status codes
- [ ] Configuration (Section 2) — account/fund names driven by `application.properties`
- [ ] Scope (Section 1) — what's in V1, what's deferred to Phase 2+

---

## 9. Health Domain (Household Management)

**Status:** Phase 2 Planning

The Health domain tracks time-series biometric data across a multi-tenant household model. Data is unstructured and highly variable, requiring a document-based persistence strategy (MongoDB).

### 9.1 Scope
- **In Scope:** - Multi-tenant household grouping.
  - Profile management for adults and children.
  - Time-series tracking for height and weight.
  - User-initiated, manual one-way data import from Google Fit API.
- **Out of Scope:** - Background/Automated synchronization with Google Fit.
  - Medical diagnosis, symptom tracking, or HIPAA-compliant EMR features.

### 9.2 Data Models (MongoDB)
* `Household`: Root aggregate.
* `Profile`: `name`, `date_of_birth`, `relationship_type` (PRIMARY, PARTNER, CHILD).
* `BiometricRecord`: `profile_id`, `timestamp`, `metric` (HEIGHT, WEIGHT), `value`, `unit`.

### 9.3 Google Fit Integration Rules (Manual Sync)
- **Trigger:** User explicitly clicks a "Sync Data" button on the UI.
- **Flow:** User completes Google OAuth consent. System uses the short-lived access token to fetch data for a user-specified date range (e.g., "Last 7 Days").
- **Persistence:** System maps the fetched data into `BiometricRecord` documents. Duplicate detection is managed via upsert operations keyed on `(profile_id, timestamp, metric)`.
- **Security:** The system **will not** request or store offline access/refresh tokens. Access tokens live only in memory for the duration of the sync request.

---

## 10. Vehicle Domain (Asset Lifecycle)

**Status:** Phase 2 Planning

The Vehicle domain tracks physical asset compliance, maintenance ledgers, and recurring regulatory deadlines. Data is highly relational, requiring standard PostgreSQL schemas.

### 10.1 Scope
- **In Scope:** - Core vehicle metadata tracking.
  - Regulatory compliance deadlines (PUC, Insurance).
  - Tax scheduling (e.g., biennial BH-Series renewals).
  - Service history ledger (odometer readings, itemized costs).
  - User-initiated export of deadlines to Google Calendar.
- **Out of Scope:** - Automated background scheduling or live GPS/OBD-II tracking.

### 10.2 Database Schema (PostgreSQL)

```sql
CREATE TABLE vehicle (
    id                  BIGSERIAL PRIMARY KEY,
    make                VARCHAR(50) NOT NULL,
    model               VARCHAR(50) NOT NULL,
    registration_number VARCHAR(20) UNIQUE NOT NULL,
    registration_type   VARCHAR(20) NOT NULL CHECK (registration_type IN ('STANDARD', 'BH_SERIES'))
);

CREATE TABLE compliance_document (
    id             BIGSERIAL PRIMARY KEY,
    vehicle_id     BIGINT REFERENCES vehicle(id),
    doc_type       VARCHAR(20) NOT NULL CHECK (doc_type IN ('PUC', 'INSURANCE', 'ROAD_TAX')),
    issue_date     DATE NOT NULL,
    expiry_date    DATE NOT NULL,
    is_active      BOOLEAN DEFAULT TRUE
);

CREATE TABLE service_record (
    id               BIGSERIAL PRIMARY KEY,
    vehicle_id       BIGINT REFERENCES vehicle(id),
    service_date     DATE NOT NULL,
    odometer_reading INTEGER NOT NULL,
    cost             NUMERIC(10, 2) NOT NULL,
    description      TEXT
);

## 8. Next Steps

- **Architecture & file structure:** See [ARCHITECTURE](./ARCHITECTURE.md)
- **Local development setup:** See [GETTING_STARTED](./GETTING_STARTED.md)
- **How to use the app:** See [GETTING_STARTED](./GETTING_STARTED.md)
- **Future phases:** See [ROADMAP](./ROADMAP.md)
