# User Guide — Using suchika

This guide walks you through using the Finance and Health modules of suchika.

---

## Finance Module — CSV Upload Workflow

### Overview

Upload transaction data from your bank or investment accounts in CSV format. The app parses, normalizes, and stores your transactions. Files are never persisted to disk — only parsed and stored in the database.

**Current Phase (V1):**
- Upload CSVs from multiple accounts/investments
- Automatic deduplication (overlapping uploads are flagged, not silently rejected)
- No AI categorization, no file archiving, no transfer reconciliation
- Configuration-driven: admins add new account types in `application.properties`

---

## Getting Started with Finance

### 1. Login to the app

Open `http://localhost:3000` in your browser.

*(Multi-user auth is Phase 3. For now, local single-user workflow.)*

---

### 2. Navigate to Upload

Click **Finance** → **Upload CSV**

---

### 3. Select Account Details

**Step A: Choose Account Name**

Click the **Account Name** dropdown. Options are configured by your admin in `application.properties`.

Example values:
- `HDFC_SAVINGS` (Bank savings account)
- `ICICI_CREDIT_CARD` (Credit card)
- `SBI_LOAN` (Loan account)
- `PF` (Provident Fund investment)
- `NPS` (National Pension System investment)

**Step B: Choose Account Type**

| Type | Usage |
|---|---|
| `SAVINGS` | Bank savings account |
| `CREDIT_CARD` | Credit card statement |
| `LOAN` | Loan statement (principal, interest, EMI) |
| `INVESTMENT` | Investment fund (mutual funds, stocks, pension schemes) |

✅ If you choose `INVESTMENT`, the Account Name dropdown switches to fund names only.

---

### 4. Upload CSV File

1. Click **Select File**
2. Choose your bank/investment CSV export
3. Click **Upload**

**Supported CSV headers (case-insensitive, fuzzy match):**

**Banking transactions** (minimum required):
- **Date:** `Date`, `Txn Date`, `Value Date`, `Transaction Date`
- **Amount:** `Amount`, `Txn Amount`, `Debit/Credit Amount`
- **Transaction Type:** `Dr/Cr`, `Type`, `Debit`, `Credit` (or derived from negative amount)

Optional:
- `Description` / `Narration` / `Particulars` / `Remarks`
- `Balance` / `Running Balance` / `Closing Balance`

**Investment transactions** (minimum required):
- **Date:** `Date`, `Transaction Date`, `NAV Date`
- **Amount:** `Amount`, `Purchase Amount`, `Redemption Amount`
- **Type:** `Type`, `Transaction Type`, `Dr/Cr`

Optional:
- `Units` / `No. of Units`
- `NAV` / `NAV per Unit`

---

### 5. Amount Normalization

The app automatically handles different CSV formats:

| CSV Format | How it's processed |
|---|---|
| `Amount: 500, Type: DEBIT` | Stored as amount=500, txn_type=DEBIT |
| `Amount: -500` | Converted to amount=500, txn_type=DEBIT |
| `Debit: 500, Credit: (blank)` | Stored as amount=500, txn_type=DEBIT |
| `Debit: (blank), Credit: 500` | Stored as amount=500, txn_type=CREDIT |

**All amounts are stored as positive numbers.** The `txn_type` (CREDIT/DEBIT) indicates the direction.

---

### 6. Review Upload Summary

After successful upload, you see:

```json
{
  "inserted": 142,
  "duplicates_flagged": 3,
  "rejected_rows": 0
}
```

| Field | Meaning |
|---|---|
| `inserted` | New rows added to the database |
| `duplicates_flagged` | Rows that matched an existing transaction (same account, date, amount, direction) — still inserted but marked for review in Phase 2 |
| `rejected_rows` | Rows that couldn't be parsed (invalid date, non-numeric amount) — skipped |

---

### 7. View Transactions

Click **Finance** → **View All Transactions**

You see a paginated list of all your transactions across all uploaded accounts:

| Column | Description |
|---|---|
| **Account Name** | Account or fund name (e.g., `HDFC_SAVINGS`, `PF`) |
| **Account Type** | `SAVINGS`, `CREDIT_CARD`, `LOAN`, or `INVESTMENT` |
| **Date** | Transaction date |
| **Amount** | Positive amount |
| **Type** | `CREDIT` or `DEBIT` |
| **Description** | Bank-provided narrative (unreliable, may be blank) |
| **Balance** | Account balance at time of transaction (if CSV contained it) |
| **Status** | `Normal` or `Flagged Duplicate` |
| **Created At** | When the row was uploaded |

---

### 8. Filter and Pagination

(Coming in Phase 2)

Currently you can view all transactions. Phase 2 will add:
- Filter by account, date range, amount range
- Search by description
- Export to clean CSV

---

## Health Module (Placeholder)

The Health module is under development. For now, it provides database schema and infrastructure.

Future features:
- Health profile management (self + family members)
- Doctor visit logging
- Vital readings (BP, weight, blood sugar, heart rate)

---

## Common Questions

### Q: What happens if I upload the same CSV twice?

**A:** Overlapping rows are detected by matching: `account + date + amount + direction`. These rows are still inserted but flagged as `is_duplicate = TRUE`. Phase 2 will add a UI to accept/reject flagged transactions.

---

### Q: Can I edit or delete transactions?

**A:** Not in V1. If you uploaded incorrect data, contact your admin. Phase 2 will add manual transaction entry and edit UI.

---

### Q: Are my transactions backed up?

**A:** Yes. They are stored in PostgreSQL database (`app_db`). Always back up your PostgreSQL instance using standard tools (`pg_dump`, automated backups, etc.).

---

### Q: Why was my CSV rejected?

**A:** The app rejects entire files if:
1. Required columns (date, amount, type) cannot be found
2. No valid dates are found in the date column
3. No valid amounts are found in the amount column
4. Account Name or Fund Name you selected is not in the config list

**Fix:** Check your CSV headers match the supported names. If still stuck, ask your admin to log the error and update the parser.

---

### Q: Where is my uploaded file stored?

**A:** Nowhere. The file is parsed in memory and immediately discarded. Only transaction rows are stored in the database. No files are archived.

---

## Support

- **Setup issues?** See [Getting Started](../GETTING_STARTED.md)
- **Architecture questions?** See [Project Architecture](./Project_Architecture.md)
- **API technical details?** See [Business Requirement](./Business_Requirement.md)
