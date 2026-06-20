# /sync-context — Sync Domain-State Files from Current Code

Read the actual source code and update all domain-state files to match reality. Run this after any session where agents may not have self-updated, or at the start of a new milestone.

## When to run
- Start of a new version milestone (v0.3, v0.4, etc.)
- After a session where changes were made but domain-state wasn't updated
- When domain-state files feel stale or out of sync with code

## Step 1 — Audit each domain

For each domain (profile, wealth, health, household):

### Check implementation status
- Read all Java files in `application/domain/<domain>/domain/`
- Read all Java files in `application/domain/<domain>/adapters/`
- Read `application/contract/<domain>.yaml`
- Read all frontend files in `web/src/pages/<Domain>/` and `web/src/api/`
- Read `application/flyway/<domain>/` to get current schema

### Check test coverage
- Count test files in `application/domain/<domain>/domain/src/test/`
- Count test files in `application/domain/<domain>/adapters/src/test/`
- Count test files in `web/src/` matching `*.test.js`

## Step 2 — Update domain-state files

For each domain, update `documents/domain-state/<domain>.md`:

**Implementation Status table:** Mark ✅ anything with working code + tests. Mark 🔲 anything planned but not done.

**Database Schema table:** Derive from the Flyway migration files (V1__, V2__, etc.) — these are the ground truth. Update all column lists to match the latest migration.

**API Contract section:** Derive from the OpenAPI yaml file. Update endpoint list.

**Key Files:** Verify each path still exists. Remove stale paths. Add new files.

**Open Issues:** Prune issues that are now resolved. Add new ones discovered during the audit.

**Last updated:** Set to today's date.

## Step 3 — Update CONTEXT_PRIMER.md

Update the domain status table to match:
```
| Domain | Backend | Frontend | Status |
```

Update the quality gates section with current numbers (run `npm run test:coverage` to get fresh coverage %).

## Step 4 — Report

```
Sync complete. Changes made:
- profile.md: <what changed>
- wealth.md: <what changed>
- health.md: <what changed>
- household.md: <what changed>
- CONTEXT_PRIMER.md: <what changed>
```

If nothing changed: "All domain-state files already in sync."
