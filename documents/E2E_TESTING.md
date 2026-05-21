# E2E Testing (Playwright)

> End-to-end tests for the Suchika frontend. Uses Playwright (Chromium, headless by default).

---

## Overview

17 tests across 5 spec files covering the main user-facing flows:

| Spec file | Tests | Coverage |
|---|---|---|
| `auth.spec.js` | 4 | Sign-in, home redirect, logout |
| `navigation.spec.js` | 5 | Nav links, dropdowns, dashboard cards |
| `profiles.spec.js` | 2 | Profiles page loads, Add Profile button |
| `wealth.spec.js` | 4 | Accounts + transactions pages, URL redirects |
| `health.spec.js` | 3 | Vitals + doctor visits pages, URL redirects |

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
