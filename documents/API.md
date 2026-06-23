# /sonar — Run SonarQube Analysis and Fix All Issues

Run a full SonarQube scan and fix every new issue. If $ARGUMENTS is a domain or `web`, scope the fix to that area. Default: full scan.

## Step 1 — Ensure SonarQube is running

```powershell
# Check if SonarQube is up
$r = Invoke-WebRequest -Uri http://localhost:9000 -UseBasicParsing -ErrorAction SilentlyContinue
if ($r.StatusCode -ne 200) { Write-Host "Start SonarQube first: .\scripts\sonar-start.ps1" }
```

If not running: `.\scripts\sonar-start.ps1` (or `sonar-start` alias)

## Step 2 — Generate Coverage Reports

### Java (JaCoCo)
```
./gradlew test jacocoTestReport
```
*(Run only if jacoco plugin is enabled in build.gradle.kts — check first)*

### JavaScript (LCOV)
```
cd web && npm run test:coverage
```
This generates `web/coverage/lcov.info` which sonar-scanner reads.

## Step 3 — Run the Scan

```powershell
# From repo root:
sonar-scanner
# or:
.\scripts\sonar-scan.ps1
# or alias:
ss
```

Wait for: `INFO: ANALYSIS SUCCESSFUL`

Then wait 15 seconds for background processing before querying results.

## Step 4 — Pull Results

```powershell
$token = "sqp_67feaac2593aeeda89632d4dbbdc5f828c1c5437"
$issues = Invoke-RestMethod "http://localhost:9000/api/issues/search?componentKeys=suchika&resolved=false&ps=100" `
  -Headers @{Authorization = "Bearer $token"}
Write-Host "Open issues: $($issues.total)"
$issues.issues | ForEach-Object {
    "$($_.severity) | $($_.component.Split('/')[-1]):$($_.line) | $($_.message)"
}
```

## Step 5 — Fix All Issues

Fix every BLOCKER, CRITICAL, and MAJOR first. Then MINOR. Group by rule:

### Common Java Rules
| Rule | What it means | Fix |
|---|---|---|
| S106 | `System.out.println` | Replace with `AppLogger.info(...)` |
| S1481 | Unused local variable | Remove the variable |
| S2259 | Potential null pointer | Add null check or use `Optional` |
| S3776 | Cognitive complexity | Extract nested logic to private method |
| S1192 | Duplicate string literals | Extract to constant |
| S112 | `throws Exception` | Use typed exception from `shared/exception/` |
| S2629 | Logger with `+` concat | Use format args: `logger.info("msg {}", val)` |

### Common JavaScript Rules
| Rule | What it means | Fix |
|---|---|---|
| S6819 | `<div role="button">` | Use `<button type="button">` |
| S6582 | `ref && ref.click()` | Use `ref?.click()` |
| S7756 | `FileReader#readAsText` | Use `file.text().then(...)` |
| S2699 | Test with no assertion | Add `expect(...)` to the test |
| S1481 | Unused variable | Remove it |

## Step 6 — Re-run Tests After Fixes

```
# Java
./gradlew test

# JavaScript
cd web && npm run test:ci

# Both + rebuild coverage
cd web && npm run test:coverage && cd .. && sonar-scanner
```

## Step 7 — Confirm Zero Issues

```powershell
$issues = Invoke-RestMethod "http://localhost:9000/api/issues/search?componentKeys=suchika&resolved=false" `
  -Headers @{Authorization = "Bearer $token"}
if ($issues.total -eq 0) { Write-Host "SONAR CLEAN ✓" } else { Write-Host "Still $($issues.total) issues" }
```

## Coverage Target

Minimum: **80% line coverage** for JavaScript (Jest/LCOV).
Java coverage not yet enforced (JaCoCo plugin pending).

Check JS coverage:
```
cd web && npm run test:coverage 2>&1 | Select-String "Lines"
# Must show: Lines : 80%+
```


# /ux-review — UX & Frontend Quality Review

Review the React component or page specified in $ARGUMENTS (or the most recently changed frontend file if no argument). If $ARGUMENTS is a domain name (wealth, health, profile, household), review all pages in that domain.


## Step 1 — Read context
- Read `documents/CONTEXT_PRIMER.md` for current project state
- Read `documents/FRONTEND_GUIDELINES.md` for the full standards


## Step 2 — Audit the file(s) against these criteria


### Layout & Styling
- [ ] Tailwind CSS only — no `style={{}}`, no CSS modules, no other frameworks
- [ ] Responsive: uses `sm:` / `md:` breakpoints where appropriate
- [ ] Consistent spacing using Tailwind scale (not arbitrary `p-[13px]`)
- [ ] Loading states shown with spinner or skeleton text
- [ ] Error states shown with red text or alert box
- [ ] Empty states have a friendly message (not a blank page)


### Accessibility (SonarQube S6819 + general a11y)
- [ ] No `<div role="button">` — use actual `<button type="button">`
- [ ] No `<div role="link">` — use `<a>` or `<Link>`
- [ ] All interactive elements have accessible labels (`aria-label` or visible text)
- [ ] `<input>` fields have associated `<label htmlFor>`
- [ ] Images have `alt` text
- [ ] Focus management works for modals/dialogs


### React Code Quality
- [ ] No business logic in render — API calls in `useEffect` or custom hooks only
- [ ] No `console.log` in committed code
- [ ] No inline `style={{}}` props
- [ ] Component under 200 lines — split if larger
- [ ] Props destructured in function signature
- [ ] Custom hook for any stateful logic reused across components
- [ ] `useCallback`/`useMemo` used where re-renders are expensive


### SonarQube JS Rules
- [ ] Optional chaining: `ref?.current?.click()` not `ref.current && ref.current.click()`
- [ ] `Blob#text()` not `FileReader#readAsText()` for async file reads
- [ ] `===` not `==` for equality
- [ ] All async functions have `try/catch`


### API Integration
- [ ] All API calls use `web/src/api/<domain>.js` — no raw `fetch()` in components
- [ ] `profile_id` passed as query param — never hardcoded
- [ ] Loading flag set before call, cleared in `finally`


## Step 3 — Report
For each violation, report:
```
FILE:LINE — Rule violated — What to change
```

Then: total issue count per category. If zero issues, say "UX review clean."


## Step 4 — Fix
Fix all issues found. Run after fixing:
```
cd web && npm run lint && npm run test:ci && npm run build
```
