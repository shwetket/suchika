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
$token = $env:SONAR_TOKEN  # set this first: $env:SONAR_TOKEN = "sqp_..." (get one at http://localhost:9000/account/security)
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
