# Roadmap — Future Milestones

This document outlines planned features beyond the current **v0.1** milestone.
For business rules and acceptance criteria per version, see [BUSINESS_REQUIREMENTS](./BUSINESS_REQUIREMENTS.md).

---

## v0.2 — Usable Local App

**Focus:** Logical data integrity, usable ledger features, and basic scheduling.

### Features

- [ ] **Deduplication Logic**
  - Same-file duplicates: store all as distinct valid events
  - Cross-file duplicates: reject matching records from new uploads

- [ ] **Biometric History**
  - Query and display biometric entries in chronological order

- [ ] **Itinerary & Task Management**
  - Group sub-events under a master event (holidays, guest visits)
  - Conflict detection for overlapping events on same profile
  - Assign tasks to child profiles with hard deadlines

---

## v0.3 — Enhanced Local App

**Focus:** Expand parsing capabilities, introduce physical asset compliance and home automation.

### Features

- [ ] **Investment CSV Parsing**
  - Extract date, amount, type from mutual fund CSVs
  - Store domain-specific metadata: Units and NAV

- [ ] **Vehicle Asset Compliance Tracking**
  - Registry: Make, Model, Registration Number, Registration Type
  - Track PUC, Insurance, and Road Tax (including BH-Series biennial schedule)

- [ ] **Home Automation Mapping**
  - Register smart home devices and their operational states
  - Map household daily routines to device configurations

---

## v0.4 — Error Handling (Unhappy Path)

**Focus:** System resilience for malformed and edge-case data.

### Features

- [ ] **Malformed CSV Rejection**
  - Reject entire file if date or amount columns are missing
  - Log missing fields clearly for user review

- [ ] **Quarantine Protocol (Grocery Data)**
  - Quarantine malformed rows instead of rejecting entire file
  - Log quarantined items for manual correction

- [ ] **Duplicate Resolution UI**
  - View flagged duplicates (`is_duplicate=TRUE`)
  - Accept (keep both) or Reject (delete marked copy)
  - Batch accept/reject actions

---

## v0.5 — Beta Release

**Focus:** Stable build for controlled local testing. First cross-domain logic.

### Features

- [ ] **Vacation Planner (Cross-Domain)**
  - Budget validation: check liquid savings against trip cost
  - Asset compliance block: warn if vehicle PUC/Insurance expires before trip

- [ ] **Consolidated Action Center**
  - Single read-only dashboard aggregating alerts from all 3 domains
  - Upcoming calendar events, vehicle compliance deadlines, biometric streak gaps

- [ ] **Profile Association for Health**
  - Tag every biometric document with a valid `profile_id` from Household domain

---

## v0.6 — Testing Foundation

**Focus:** Automated test coverage.

### Features

- [ ] Unit tests for all domain use cases
- [ ] Integration tests for adapters
- [ ] Contract tests for OpenAPI endpoints
- [ ] Pre-commit test gate via Gradle

---

## v1.0 — Security & Persistence

**Focus:** Auth, encryption, persistent real-world data. No more ephemeral DB.

### Features

- [ ] **Persistent Data Migration**
  - Flyway versioned migrations enforced and locked — no more ephemeral resets
  - All five schemas treated as production data from this point forward

- [ ] **Authentication (OIDC/OAuth2)**
  - External Identity Provider integration
  - Role-Based Access Control: Admin (Adult) vs Restricted (Child)

- [ ] **Encryption at Rest**
  - Financial ledgers encrypted at application layer before DB insert

- [ ] **Google Fit Integration (Manual Sync)**
  - User-triggered sync only — no background polling
  - Upsert deduplication keyed on `(profile_id, timestamp, metric)`
  - Short-lived tokens only — refresh/offline tokens strictly prohibited

- [ ] **Cross-Domain Security Enforcement**
  - Restricted profiles blocked from triggering Wealth domain queries
  - All cross-domain queries scoped to active `profile_id`

---

## v1.1 — Multi-User

**Focus:** Multiple user accounts within a household.

### Features

- [ ] Multiple user accounts per household
- [ ] Family sharing with role assignments
- [ ] Admin can invite members
- [ ] Viewer role: read-only access

---

## v1.2 — Public Local Release

**Focus:** Stable local release for general users.

### Features

- [ ] Packaging for easy local installation
- [ ] Setup wizard for first-time users
- [ ] Full documentation for non-developer users

---

## v1.3 — Export / Import

**Focus:** Cross-domain data archiving and portability.

### Features

- [ ] **Unified Data Export**
  - Single trigger exports all data from all five PostgreSQL schemas
  - Packaged as structured JSON/CSV local backup

- [ ] **1-Click Batch Folder Import**
  - Scan local folder and upload multiple CSVs in one action
  - Batch status dashboard

- [ ] **Rule-Based Tagging Engine**
  - If description contains "SWIGGY" → tag "Food"
  - If description contains "FUEL" → tag "Transport"
  - Admin UI to create/manage rules

- [ ] **Unified Search & Export**
  - Full-text search on transaction descriptions
  - Export filtered results to clean CSV
  - Date range, amount range, account filters

---

## v2.0 — Local AI

**Focus:** Local LLM as unified reasoning engine over personal data.

### Features

- [ ] **Cross-Domain Context API**
  - Read-only API layer for local AI to simultaneously query Wealth, Household, and Health data

- [ ] **Daily Briefing Generation**
  - AI generates contextual insights across all domains
  - Example: *"You have a road trip to Munnar tomorrow, your Tata Nexon insurance expires today, and savings need topping up to cover the trip budget."*

- [ ] **Transfer Reconciliation**
  - Auto-link transfers between accounts (same amount, opposite direction, same date)
  - Manual override for fuzzy matches

---

## v2.1 — Cloud Ready

**Focus:** Architecture preparation for cloud deployment.

### Features

- [ ] Docker containerization
- [ ] Multi-region DB replication design
- [ ] Load balancer + auto-scaling setup
- [ ] Redis session store

---

## v2.2 — Mobile App

**Focus:** Companion mobile application.

### Features

- [ ] Mobile-responsive web frontend
- [ ] Native mobile app (iOS/Android) — evaluation phase

---

## v3.0 — GitHub Ready

**Focus:** Open-source collaboration readiness.

### Features

- [ ] Contribution guidelines finalized
- [ ] Issue templates and PR templates
- [ ] Public roadmap published

---

## v3.1 — Integrations

**Focus:** External service connections.

### Features

- [ ] Google Drive sync
- [ ] Google Calendar integration
- [ ] Fitbit data import
- [ ] Automated bank integration (Plaid or Setu API)

---

## v3.2 — Plugin Framework

**Focus:** System extensibility.

### Features

- [ ] Plugin interface definition
- [ ] First-party plugin examples

---

## v3.3 — Marketplace

**Focus:** Plugin/module ecosystem.

### Features

- [ ] Plugin registry
- [ ] Community submissions

---

## v4.0 — Cloud Launch

**Focus:** Full commercial cloud deployment.

### Features

- [ ] Multi-tenant PostgreSQL (row-level security or per-tenant schemas)
- [ ] Public domain deployment
- [ ] CDN for static assets
- [ ] SLA: 99.5% uptime

---

## v4.1 — Commercial Launch

**Focus:** Licensing, billing, regulatory compliance.

### Features

- [ ] Subscription billing (Stripe or Razorpay)
- [ ] Free tier / Pro tier definition
- [ ] GDPR compliance (data deletion, export, right-to-be-forgotten)
- [ ] Terms of Service and Privacy Policy
- [ ] Public API with rate limiting and API key auth

---

## Dependency Chain
v0.1 → v0.2 → v0.3 → v0.4 → v0.5 → v0.6
↓
v1.0 → v1.1 → v1.2 → v1.3
↓
v2.0 → v2.1 → v2.2
↓
v3.0 → v3.1 → v3.2 → v3.3
↓
v4.0 → v4.1

Each milestone requires the previous to be stable before starting.

---

## Success Metrics

| Milestone | Key Metric |
|---|---|
| v0.1 | Upload 100+ transactions from 3+ CSVs without data loss |
| v0.4 | Zero silent data drops on malformed input |
| v0.5 | Cross-domain vacation planner works end-to-end |
| v1.0 | Auth + encryption pass local security review |
| v1.3 | Full data export/import round-trip verified |
| v2.0 | Local AI daily briefing generates without errors |
| v4.1 | 1000+ active Pro users, <100ms API p99 |

---

## Communication

- **Feature requests:** GitHub Issues with `vX.Y` milestone label
- **Roadmap updates:** This file + project announcements
- **Breaking changes:** Changelog + notification to active users
- **Security issues:** GitHub Issues with `security` label
