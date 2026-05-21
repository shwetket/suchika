# Roadmap — Future Phases

This document outlines the planned features and phases beyond Phase 1 (V1) Finance CSV upload.

---

## Phase 2: Workflow Automation & Power-User Tools (Local)

**Focus:** Reduce friction for power users. Support local file batching, manual entry, and duplicate resolution.

### Features

- [ ] **1-Click Batch Folder Import**
  - Scan a local folder and upload multiple CSVs in one action
  - Batch status dashboard showing progress

- [ ] **Manual Transaction Entry**
  - UI form to add ad-hoc transactions (cash, peer transfers, etc.)
  - Validate against account/fund names from config

- [ ] **Duplicate Resolution UI**
  - View flagged duplicates (transactions with `is_duplicate=TRUE`)
  - Accept (keep both) or Reject (delete marked copy)
  - Batch accept/reject actions

- [ ] **Transfer Reconciliation**
  - Auto-link transfers between accounts (same amount, opposite direction, same date)
  - Show reconciliation suggestions
  - Manual override for fuzzy matches

- [ ] **Rule-Based Tagging Engine**
  - If description contains "SWIGGY" → tag "Food"
  - If description contains "FUEL" → tag "Transport"
  - Admin UI to create/manage rules

- [ ] **Unified Search & Export**
  - Full-text search on description
  - Export filtered transactions to clean CSV
  - Date range, amount range, account filters

### Tech Notes

- All local processing (no cloud upload)
- Rules stored in database, not config
- Transaction tags stored in new `transaction_tag` junction table

---

## Phase 3: Multi-Tenancy & Family Cloud (SaaS Foundation)

**Focus:** Move from single-user localhost to a centralized, multi-tenant cloud service.

### Features

- [ ] **Centralized Hosting Deployment**
  - Deploy to private cloud VPS (AWS, DigitalOcean, Heroku, etc.)
  - Docker containerization for consistency
  - Load balancer + auto-scaling setup

- [ ] **Identity & Auth**
  - Keycloak or OAuth2 provider integration
  - Login via Google, GitHub, or email/password
  - Session management, token refresh

- [ ] **Multi-Tenancy Database Architecture**
  - Add `tenant_id` column to all tables
  - Row-level security policies
  - Strict data isolation

- [ ] **Dynamic Account Management**
  - Move account/fund names from `application.properties` to UI + database
  - Admin panel to add/update account types
  - No code redeploy needed

- [ ] **Family Sharing / RBAC**
  - Multiple users per tenant
  - Roles: Admin, Editor, Viewer
  - Admin can invite family members
  - Viewer can see data, not modify

- [ ] **Net Worth & Analytics Dashboard**
  - Aggregated account balance over time
  - Monthly trends, spending patterns
  - Per-category breakdowns
  - Shared dashboard for family view

### Tech Notes

- Deploy as multi-instance Quarkus app (Kubernetes or manual)
- Use separate multi-tenant PostgreSQL or per-tenant schemas
- Session store in Redis
- Event streaming for audit logs

---

## Phase 4: Ecosystem Integration (Public SaaS)

**Focus:** Add automation, monetization, and public API readiness.

### Features

- [ ] **Automated Bank Integration**
  - Plaid or Setu API for real-time transactions
  - Eliminate manual CSV uploads
  - Support for 1000+ banks

- [ ] **Subscription Billing**
  - Free tier: 1 user, 5 linked accounts, 2-month history
  - Pro tier: family sharing, unlimited accounts, unlimited history, export
  - Stripe or Razorpay integration

- [ ] **Public API & Webhooks**
  - `/api/v1/webhooks` — custom callbacks for external integration
  - Rate limiting (per tenant, per endpoint)
  - API key authentication

- [ ] **Public Domain Readiness**
  - GDPR compliance (data deletion, export)
  - PCI compliance for payment handling (if storing cards)
  - Terms of Service, Privacy Policy
  - Support ticketing system

- [ ] **Data Retention & Cleanup**
  - Configurable retention policies
  - Automated archival of old transactions
  - GDPR right-to-be-forgotten compliance

### Tech Notes

- Add `api_key` table and `rate_limit` middleware
- Multi-region database replication for resilience
- CDN for static assets (charts, exports)

---

## Implementation Timeline

| Phase | Duration | Team |
|---|---|---|
| Phase 1 (V1) | 2 weeks | 1 backend + 1 frontend |
| Phase 2 | 4 weeks | Same team |
| Phase 3 | 8 weeks | 2+ backend (infra, auth), 1 frontend |
| Phase 4 | 12+ weeks | Expand team as needed |

**Phase 1** is complete when:
- CSV upload works end-to-end
- Deduplication logic in place
- All APIs match OpenAPI contract

**Phase 2** gate:
- Phase 1 is production-ready locally
- User feedback collected

**Phase 3 gate:**
- Phase 2 features stable
- Hosting infrastructure planned
- Security audit passed

**Phase 4 gate:**
- Phase 3 deployed and tested in staging
- Bank partner integrations approved
- Billing & subscription logic validated

---

## Out of Scope (Never)

| Feature | Reason |
|---|---|
| **AI Categorization** | Complex, low ROI. Rule-based tagging sufficient for MVP. |
| **File Archiving** | Privacy concern, storage cost. Parse and discard philosophy. |
| **Desktop App** | Web-first strategy. Mobile web before native. |
| **Expense Forecasting** | Too opinionated. Focus on data collection first. |
| **Social Features** | Out of scope — not a network app. |

---

## Success Metrics (Per Phase)

**Phase 1:**
- ✅ Upload 100+ transactions from 3+ bank CSVs without data loss
- ✅ All 4 API endpoints tested and stable
- ✅ Zero silent data drops

**Phase 2:**
- ✅ Batch import reduces upload clicks by 80%
- ✅ Duplicate resolution covers 95% of cases
- ✅ Transfer reconciliation detects 90% of transfers
- ✅ Rule engine covers top 20 categories

**Phase 3:**
- ✅ Multi-user auth works for 5 family members
- ✅ Data isolation (row-level security) enforced
- ✅ SaaS deployment to production passes security audit
- ✅ Uptime SLA: 99.5%

**Phase 4:**
- ✅ Plaid integration connects 50+ banks
- ✅ 1000+ active users on Pro tier
- ✅ Webhook delivery SLA: 99.9%
- ✅ <100ms API response p99

---

## Dependency Map

```
Phase 1 (V1)
    ↓
Phase 2 (Power-user tools)
    ↓
Phase 3 (Multi-tenant SaaS)
    ↓
Phase 4 (Public API + Monetization)
```

Each phase builds on the previous. Phase 2 requires stable Phase 1. Phase 3 requires production-ready Phase 2, etc.

---

## Communication

- **Feature requests:** GitHub Issues with `phase-X` label
- **Roadmap updates:** This file + Discord announcements
- **Breaking changes:** Changelog + email notification to users
