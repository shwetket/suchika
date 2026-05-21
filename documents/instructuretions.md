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
cd web
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

... (file continues with planned edits and guidance)
