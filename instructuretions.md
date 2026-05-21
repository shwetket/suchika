# AI Developer Instructions — Architecture Cleanup

## Context

This project has been refactored from two separate Quarkus services to a
**single unified Quarkus application**. The event/messaging layer has also
been removed. Most documents are already updated. This file lists the
**remaining fixes** — four files, eight specific changes. Follow them exactly
and in order.

Do not change anything that is not listed here.

---

## After all edits: run these commands

Run these from the repository root before committing:

```bash
# 1. Regenerate the React API client (always run after any document or API change)
cd ui/web
npm install
npm run generate:api
cd ../..

# 2. Verify the backend still starts cleanly
./gradlew :application:finance:quarkusDev
```

If `npm run generate:api` fails, check that `openapi/finance.yaml` and
`openapi/health.yaml` exist and are valid YAML.

---

## File 1 — `GETTING_STARTED.md`

### Fix 1.1 — Remove stale port reference in Known Setup Notes

Find this line (in the **Known Setup Notes** section):

```
- **Port binding:** Make sure ports 8080, 8081, and 3000 are available.
```

Replace with:

```
- **Port binding:** Make sure ports 8080 and 3000 are available.
```

### Fix 1.2 — Fix the step numbering gap

The file currently jumps from **Step 5** to **Step 7** (Step 6 is missing
because the Health service step was deleted but the heading numbers were not
renumbered).

Find:

```
## Step 7 — View API documentation
```

Replace with:

```
## Step 6 — View API documentation
```

### Fix 1.3 — Fix the Flyway version range in Known Setup Notes

Find:

```
- **Single database:** Both domains point to `app_db`. Flyway migration versions are globally sequential (V1–V3 Finance, V4–V6 Health) to avoid conflicts.
```

Replace with:

```
- **Single database:** Both Finance and Health domains share `app_db`. Flyway migration versions are globally sequential (V1–V6) to avoid conflicts.
```

---

## File 2 — `documents/Project_Architecture.md`

### Fix 2.1 — Remove the stale "Message" line from the architecture diagram

Find this block (inside the ASCII art diagram near the top of the file):

```
   HTTP Request ──► │  ports/in  →  application  →     │ ──► Database
   Message     ──►  │  (use case interfaces)           │ ──► External API
```

Replace with:

```
   HTTP Request ──► │  ports/in  →  application  →     │ ──► Database
                    │  (use case interfaces)           │ ──► External API
```

### Fix 2.2 — Remove the duplicate `infrastructure/` block in the file tree

The file tree currently lists `infrastructure/` **twice** — once as a
one-liner stub and again with full detail. Find and delete only the stub line:

```
├── infrastructure/               # Shared plumbing (DB connections, config)
├── shared/                       # Cross-cutting concerns (auth, logging, errors)
│
├── infrastructure/               # Shared plumbing
```

Replace the whole block with just the detailed version (keep everything after
the blank line):

```
├── infrastructure/               # Shared plumbing
```

So the result reads:

```
├── infrastructure/               # Shared plumbing
│   ├── build.gradle.kts
│   └── src/main/java/com/suchika/infrastructure/
│       ├── persistence/         # Shared DB connection pool
│       └── config/              # Shared configuration
│
├── shared/                       # Cross-cutting concerns
```

### Fix 2.3 — Remove the stale port comment in the configuration example

Find:

```
# Port
quarkus.http.port=8080  # Finance
# or 8081 for Health
```

Replace with:

```
# Port
quarkus.http.port=8080
```

### Fix 2.4 — Add single-service decision to the Design Decisions table

Find the Design Decisions table. It currently ends with:

```
| **Google AIP style** | Industry standard for resource-oriented APIs. Consistent with Cloud APIs conventions. |
```

Add one new row immediately after it:

```
| **Single Quarkus application** | One JVM process for both Finance and Health domains. Package boundaries enforce hexagonal isolation. Splitting into separate services is a one-afternoon refactor if Phase 3 requires it. |
```

---

## File 3 — `README.md`

### Fix 3.1 — Remove stale "Events" line from the architecture diagram

Find this block in the **Architecture Overview** section:

```
              ┌──── DOMAIN ────┐
   HTTP   → │ ports/in        │ → DB
   Events → │   ↓             │ → External APIs
```

Replace with:

```
              ┌──── DOMAIN ────┐
   HTTP   → │ ports/in        │ → DB
```

### Fix 3.2 — Fix the Flyway version count in the Database section

Find:

```
- **Migrations:** Managed by Flyway (sequential versions V1–V5)
```

Replace with:

```
- **Migrations:** Managed by Flyway (sequential versions V1–V6)
```

---

## File 4 — `CONTRIBUTING.md`

No content changes needed — this file is already correct. However, add a
**pre-commit hook** section at the bottom so the npm requirement is enforced
automatically:

Append to the end of the file:

```markdown
## Pre-commit hook (Husky)

A pre-commit hook is configured in `.husky/pre-commit` to run
`npm run generate:api` automatically. If you do not have Husky installed:

```bash
cd ui/web
npm install
npx husky install
```

If the hook is skipped (e.g. `git commit --no-verify`), regenerate the client
manually before pushing:

```bash
cd ui/web
npm run generate:api
```
```

---

## Summary of all changes

| File | Fix | What changes |
|---|---|---|
| `GETTING_STARTED.md` | 1.1 | Remove port 8081 from port-binding note |
| `GETTING_STARTED.md` | 1.2 | Renumber Step 7 → Step 6 |
| `GETTING_STARTED.md` | 1.3 | Fix Flyway version range to V1–V6 |
| `Project_Architecture.md` | 2.1 | Remove "Message ──►" line from diagram |
| `Project_Architecture.md` | 2.2 | Remove duplicate `infrastructure/` stub |
| `Project_Architecture.md` | 2.3 | Remove `# or 8081 for Health` comment |
| `Project_Architecture.md` | 2.4 | Add single-service row to Design Decisions |
| `README.md` | 3.1 | Remove "Events →" line from diagram |
| `README.md` | 3.2 | Fix Flyway version count to V1–V6 |
| `CONTRIBUTING.md` | 4.1 | Append Husky pre-commit hook section |

---

## What NOT to change

- Do not touch `Business_Requirement.md` — it is already correct.
- Do not add a messaging or event bus section anywhere.
- Do not add a Health-specific `.env` file or Gradle module.
- Do not change Java version — project uses Java 25.
- Do not add a second Quarkus port or a second Swagger UI URL.
- Do not edit any Flyway migration file that has already been run.