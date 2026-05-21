# QA API Test Results

Manual API verification executed by the quality-manager agent.  
Each run is appended as a new section below.

---

## Run 1 — 2026-06-16 06:00–08:10 IST

**Tester:** quality-manager agent  
**Services under test:** profile :8081 · health :8083 · wealth :8082 · gateway :8080  
**Database:** app_db (PostgreSQL, local)  
**Method:** `curl` calls against running `quarkusDev` processes

### Bugs found and fixed during this run

| ID | Severity | File | Description | Status |
|---|---|---|---|---|
| BUG-1 | BLOCKER | `wealth/adapters/services/AccountService.java` | `@Transactional` missing on `createAccount`, `updateAccount`, `deactivateAccount` → 500 on every write | Fixed |
| BUG-2 | BLOCKER | `health/adapters/services/VitalReadingService.java` | `@Transactional` missing on `recordReading`, `delete` → 500 on every write | Fixed |
| BUG-3 | BLOCKER | `health/adapters/services/DoctorVisitService.java` | `@Transactional` missing on `create`, `update`, `delete` → 500 on every write | Fixed |
| BUG-4 | BLOCKER | `web-gateway` (new file `ClientErrorMapper.java`) | Gateway swallowed all domain 4xx errors and returned 500 instead; no `ExceptionMapper<WebApplicationException>` existed | Fixed |

---

### Profile Service — Direct (:8081)

| # | Endpoint | Method | Payload / Params | Expected | Actual | Pass |
|---|---|---|---|---|---|---|
| P-01 | `/v1/admins` | POST | `display_name`, `email_address` | 201 + body | 201 ✓ | ✓ |
| P-02 | `/v1/profiles` | POST | `admin_id`, `full_name`, `dob`, `relation_to_admin`, `email_address` | 201 + body | 201 ✓ | ✓ |
| P-03 | `/v1/profiles` | GET | — | 200 + `{profiles:[…], total_size:N}` | 200 total_size=1 ✓ | ✓ |
| P-04 | `/v1/profiles/{id}` | GET | valid UUID | 200 + profile body | 200 name=QA Member ✓ | ✓ |
| P-05 | `/v1/profiles/{id}` | PATCH | `{"email_address":"…"}` | 200 + updated body | 200 email updated ✓ | ✓ |
| P-06 | `/v1/profiles/{id}` | PATCH | `{"full_name":"…"}` | 200, `full_name` unchanged (not patchable by design) | 200, name unchanged ✓ | ✓ |
| P-07 | `/v1/profiles/{id}` | DELETE | valid UUID | 204 | 204 ✓ | ✓ |
| P-08 | `/v1/profiles/00000000…` | GET | unknown UUID | 404 + error body | 404 NOT_FOUND ✓ | ✓ |

---

### Health Service — Direct (:8083)

#### Vital Readings

| # | Endpoint | Method | Payload / Params | Expected | Actual | Pass |
|---|---|---|---|---|---|---|
| H-01 | `/v1/vitals` | POST | `WEIGHT`, `value_primary=72.5`, `unit=kg` | 201 + body | 201 ✓ | ✓ |
| H-02 | `/v1/vitals` | POST | `BLOOD_PRESSURE`, `value_primary=120`, `value_secondary=80`, `unit=mmHg` | 201 + body | 201 ✓ | ✓ |
| H-03 | `/v1/vitals?profile_id={id}` | GET | valid profile UUID | 200 + list, total_size=2 | 200 total=2 ✓ | ✓ |
| H-04 | `/v1/vitals/{id}` | GET | valid UUID | 200 + vital body | 200 type=WEIGHT val=72.5 ✓ | ✓ |
| H-05 | `/v1/vitals/{id}` | DELETE | valid UUID | 204 | 204 ✓ | ✓ |
| H-06 | `/v1/vitals` | POST | `BLOOD_PRESSURE` missing `value_secondary` | 400 BAD_REQUEST | 400 "value_secondary (diastolic) is required for BLOOD_PRESSURE" ✓ | ✓ |

#### Doctor Visits

| # | Endpoint | Method | Payload / Params | Expected | Actual | Pass |
|---|---|---|---|---|---|---|
| H-07 | `/v1/doctor-visits` | POST | `visited_doctor=true`, `doctor_name="Dr. Sharma"`, `hospital_name`, `follow_up_date` | 201 + body | 201 all fields present ✓ | ✓ |
| H-08 | `/v1/doctor-visits` | POST | `visited_doctor=false`, no `doctor_name` | 201 + body | 201 ✓ | ✓ |
| H-09 | `/v1/doctor-visits?profile_id={id}` | GET | valid profile UUID | 200 + list, total_size=2 | 200 total=2 ✓ | ✓ |
| H-10 | `/v1/doctor-visits/{id}` | GET | valid UUID | 200 + visit body | 200 doctor, hospital, follow_up all present ✓ | ✓ |
| H-11 | `/v1/doctor-visits/{id}` | PATCH | `{"diagnosis":"…","notes":"…"}` | 200 + updated body | 200 diagnosis + notes updated ✓ | ✓ |
| H-12 | `/v1/doctor-visits/{id}` | DELETE | valid UUID | 204 | 204 ✓ | ✓ |
| H-13 | `/v1/doctor-visits` | POST | `visited_doctor=true`, no `doctor_name` | 400 BAD_REQUEST | 400 "doctor_name is required when visited_doctor is true" ✓ | ✓ |
| H-14 | `/v1/doctor-visits` | POST | `to_date` before `from_date` | 400 BAD_REQUEST | 400 "to_date cannot be before from_date" ✓ | ✓ |

---

### Wealth Service — Direct (:8082)

| # | Endpoint | Method | Payload / Params | Expected | Actual | Pass |
|---|---|---|---|---|---|---|
| W-01 | `/v1/accounts` | POST | `SAVINGS`, `opening_balance=50000` | 201 + body | 201 ✓ | ✓ |
| W-02 | `/v1/accounts` | GET | — | 200 + list | 200 total=1 ✓ | ✓ |
| W-03 | `/v1/accounts?account_type=SAVINGS` | GET | filter by type | 200 + filtered list | 200 total=1 ✓ | ✓ |
| W-04 | `/v1/accounts/{id}` | GET | valid UUID | 200 + account body | 200 name, type, balance, active all present ✓ | ✓ |
| W-05 | `/v1/accounts/{id}` | PATCH | `{"account_name":"…","opening_balance":75000}` | 200 + updated body | 200 name and balance updated ✓ | ✓ |
| W-06 | `/v1/accounts/{id}` | DELETE | valid UUID (no transactions) | 204 | 204 ✓ | ✓ |
| W-07 | `/v1/accounts/00000000…` | GET | unknown UUID | 404 + error body | 404 NOT_FOUND ✓ | ✓ |
| W-08 | `/v1/accounts` | POST | `account_type=INVALID` | 400 BAD_REQUEST | 400 "Invalid account_type: INVALID" ✓ | ✓ |
| W-09 | `/v1/accounts` | POST | missing `account_type` | 400 BAD_REQUEST | 400 "account_type is required" ✓ | ✓ |

---

### Gateway (BFF) — Proxy Routes (:8080)

| # | Endpoint | Method | Expected proxy behaviour | Actual | Pass |
|---|---|---|---|---|---|
| G-01 | `/v1/profiles` | GET | proxies to :8081, returns same body | 200 total=3 ✓ | ✓ |
| G-02 | `/v1/profiles` | POST | proxies to :8081, returns 201 | 201 profile created ✓ | ✓ |
| G-03 | `/v1/profiles/{id}` | GET | proxies to :8081 | 200 name=GW Profile ✓ | ✓ |
| G-04 | `/v1/profiles/{id}` | PATCH | proxies to :8081 | 200 email updated ✓ | ✓ |
| G-05 | `/v1/profiles/{id}` | DELETE | proxies to :8081 | 204 ✓ | ✓ |
| G-06 | `/v1/vitals` | GET | proxies to :8083 | 200 total=1 ✓ | ✓ |
| G-07 | `/v1/vitals` | POST | proxies to :8083, returns 201 | 201 ✓ | ✓ |
| G-08 | `/v1/vitals/{id}` | GET | proxies to :8083 | 200 type=WEIGHT ✓ | ✓ |
| G-09 | `/v1/vitals/{id}` | DELETE | proxies to :8083 | 204 ✓ | ✓ |
| G-10 | `/v1/doctor-visits` | GET | proxies to :8083 | 200 total=1 ✓ | ✓ |
| G-11 | `/v1/doctor-visits` | POST | proxies to :8083, returns 201 | 201 ✓ | ✓ |
| G-12 | `/v1/doctor-visits/{id}` | GET | proxies to :8083 | 200 visited=False notes present ✓ | ✓ |
| G-13 | `/v1/doctor-visits/{id}` | PATCH | proxies to :8083 | 200 notes updated ✓ | ✓ |
| G-14 | `/v1/doctor-visits/{id}` | DELETE | proxies to :8083 | 204 ✓ | ✓ |
| G-15 | `/v1/accounts` | GET | proxies to :8082 | 200 total=2 ✓ | ✓ |
| G-16 | `/v1/accounts` | POST | proxies to :8082, returns 201 | 201 ✓ | ✓ |
| G-17 | `/v1/accounts/{id}` | GET | proxies to :8082 | 200 name, type, balance ✓ | ✓ |
| G-18 | `/v1/accounts/{id}` | PATCH | proxies to :8082 | 200 name updated ✓ | ✓ |
| G-19 | `/v1/accounts/{id}` | DELETE | proxies to :8082 | 204 ✓ | ✓ |

#### Gateway Error Propagation (post BUG-4 fix)

| # | Scenario | Expected | Actual | Pass |
|---|---|---|---|---|
| G-E1 | GET unknown profile via gateway | 404 NOT_FOUND body | 404 NOT_FOUND ✓ | ✓ |
| G-E2 | GET unknown vital via gateway | 404 NOT_FOUND body | 404 NOT_FOUND ✓ | ✓ |
| G-E3 | GET unknown account via gateway | 404 NOT_FOUND body | 404 NOT_FOUND ✓ | ✓ |
| G-E4 | GET unknown doctor-visit via gateway | 404 NOT_FOUND body | 404 NOT_FOUND ✓ | ✓ |
| G-E5 | POST invalid account_type via gateway | 400 BAD_REQUEST body | 400 BAD_REQUEST ✓ | ✓ |
| G-E6 | POST BP vital missing value_secondary via gateway | 400 BAD_REQUEST body | 400 "value_secondary (diastolic) is required…" ✓ | ✓ |

---

### Summary

| Domain | Tests run | Passed | Failed | Bugs fixed |
|---|---|---|---|---|
| Profile :8081 | 8 | 8 | 0 | 0 |
| Health :8083 | 14 | 14 | 0 | 2 (BUG-2, BUG-3) |
| Wealth :8082 | 9 | 9 | 0 | 1 (BUG-1) |
| Gateway :8080 | 25 | 25 | 0 | 1 (BUG-4) |
| **Total** | **56** | **56** | **0** | **4** |

All 56 test cases pass. Zero open defects. All four bugs were found and fixed within the same QA session.
