# E2E Testing (Playwright)

| | |
|---|---|
| **Type** | Reference |
| **Audience** | Frontend developers |
| **Status** | Active |
| **Last updated** | 2026-07-12 (added missing `dashboard.spec.js` row — was undercounting 10 spec files as 9) |

## Objective

Document the Playwright E2E test suite — what is covered, how to run it, startup order when live data is needed, and the rules for writing new tests. For the coding conventions see [FRONTEND_GUIDELINES.md §9](./FRONTEND_GUIDELINES.md#9-e2e-testing-playwright).

## Use Cases

- Running the E2E suite locally for the first time
- Debugging a failing E2E test — check the spec inventory to understand what each file covers
- Writing a new E2E test — follow the locator and isolation rules in the "Writing New Tests" section

---

## Overview

38 tests across 10 spec files covering the main user-facing flows:

| Spec file | Tests | Coverage |
|---|---|---|
| `auth.spec.js` | 4 | Sign-in, home redirect, logout |
| `navigation.spec.js` | 4 | Nav links, dropdowns, dashboard cards |
| `dashboard.spec.js` | 3 | Dashboard page loads, heading, refresh button |
| `profiles.spec.js` | 2 | Profiles page loads, Add Profile button |
| `wealth.spec.js` | 4 | Accounts + transactions pages, URL redirects |
| `health.spec.js` | 3 | Vitals + doctor visits pages, URL redirects |
| `household.spec.js` | 7 | Calendar/Inventory/Goals pages + Add flows, Vacation Planner page |
| `admin-setup.spec.js` | 5 | New-admin redirect, setup wizard steps and validation, full wizard flows |
| `admin-policy.spec.js` | 3 | Policy settings page loads, threshold fields editable, Save button present |
| `action-center.spec.js` | 3 | Action Center page loads, no-linked-profile guidance, Refresh button state |

Config: `web/playwright.config.js` — Chromium only, headless, baseURL `http://localhost:3000`.

---

## Requirements

1. Dev server running at `http://localhost:3000` — required for all tests.
2. Gateway at `http://localhost:8080` — optional; page-load tests pass without a backend.

---

## Running Tests

Start the dev server first:

```bash
cd web && npm start
```

Then in a second terminal:

```bash
cd web && npm run test:e2e          # headless (CI-safe)
cd web && npm run test:e2e:headed   # visible browser (debug)
cd web && npm run test:e2e:report   # open HTML report from last run
```

---

## Startup Order for Full Data Tests

If tests need live data from the backend, start services in this order:

```bash
./gradlew :application:domain:profile:adapters:quarkusDev   # port 8081 — start first
./gradlew :application:domain:wealth:adapters:quarkusDev    # port 8082
./gradlew :application:domain:health:adapters:quarkusDev    # port 8083
./gradlew :application:domain:household:adapters:quarkusDev # port 8084
./gradlew :application:web-gateway:quarkusDev               # port 8080 (BFF)
cd web && npm start                                          # port 3000
```

---

## Writing New Tests

- Spec files go in `web/e2e/`.
- Use role-based locators (`getByRole`, `getByLabel`) — no CSS selectors.
- Scope nav locators to `page.getByRole('navigation')` to avoid matching page content that shares the same label text.
- Tests must pass when only the dev server is running (sign-in uses a demo fallback — no live auth backend required).
- Do not rely on specific data values from the database — test page structure and navigation, not row counts.

See [FRONTEND_GUIDELINES](./FRONTEND_GUIDELINES.md#9-e2e-testing-playwright) for the full conventions.
