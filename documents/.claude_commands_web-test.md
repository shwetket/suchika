# /web-test — Run and Fix Web E2E Tests (Playwright)

Run Playwright E2E tests for the area specified in $ARGUMENTS (e.g. `auth`, `wealth`, `health`, `profiles`, or `all`). If no argument, run all specs.

## Step 1 — Read context
- Read `documents/E2E_TESTING.md` for the full test inventory and conventions
- Read `documents/CONTEXT_PRIMER.md` for current service status

## Step 2 — Pre-flight Check

Playwright tests require all services running. Verify:
```
# Check if frontend is running
curl -s http://localhost:3000 | grep -q "Suchika" && echo "Frontend OK" || echo "Frontend DOWN"

# Check if gateway is running
curl -s http://localhost:8080/q/health | python -c "import sys,json; d=json.load(sys.stdin); print('Gateway', d.get('status'))" 2>/dev/null || echo "Gateway DOWN"
```

If services are down, start them before running tests:
```
# Terminal 1: Backend services (profile first)
./gradlew :application:domain:profile:adapters:quarkusDev

# Terminal 2: Gateway
./gradlew :application:web-gateway:quarkusDev

# Terminal 3: Frontend
cd web && npm start
```

## Step 3 — Run Tests

```
# All tests
cd web && npm run test:e2e

# Specific spec file
cd web && npx playwright test e2e/<area>.spec.js

# Run headed (shows browser) — use for debugging
cd web && npm run test:e2e:headed

# Show last HTML report
cd web && npm run test:e2e:report
```

## Step 4 — Interpret Failures

Playwright failure patterns and fixes:

**Strict mode violation (multiple elements matched)**
```
Error: locator resolved to 2 elements
Fix: Scope the locator — use page.getByRole('navigation').getByRole('link', { name: '...' })
```

**Element not found**
```
Error: Locator not found after 30s
Fix: Check the actual text/role in the DOM. Use getByText() with regex for flexible matching.
     Prefer getByRole() over getByText() for interactive elements.
```

**Navigation not visible**
```
The Navigation component only renders when isAuthenticated=true.
Fix: All nav tests must sign in first using the auth fixture.
```

**Flaky timing**
```
Fix: Use await expect(locator).toBeVisible() instead of page.waitForSelector()
     Playwright auto-waits — don't add explicit sleeps.
```

## Step 5 — Fix Failures

For each failing test:
1. Read the spec file
2. Read the actual page component it's testing
3. Check if the component changed (text, role, structure)
4. Update the spec to match current UI — do NOT change the component to match the test

## Step 6 — Add Missing Tests

If $ARGUMENTS specifies a page with no spec, create one in `web/e2e/<area>.spec.js`:
- Test: page loads successfully
- Test: key interactive elements are visible
- Test: main user flow works (happy path)
- Test: protected page redirects if not signed in

## Step 7 — Report

```
Passed: X
Failed: X  
Skipped: X

Failed tests:
  - spec:line — what failed — how it was fixed
```
