# /domain-status — Show Current State of All Domains

Read all domain-state files and CONTEXT_PRIMER, then report the complete project status. If $ARGUMENTS is a domain name, show only that domain in detail.

## What to read
1. `documents/CONTEXT_PRIMER.md`
2. `documents/domain-state/profile.md`
3. `documents/domain-state/wealth.md`
4. `documents/domain-state/health.md`
5. `documents/domain-state/household.md`
6. `documents/ROADMAP.md` (for upcoming milestones)

## Report Format

```
=== Suchika Project Status ===
Current version: v0.X | Date: YYYY-MM-DD

PROFILE  (port 8081) — [status]
  ✅ Done: <list complete items>
  🔲 Next: <list pending items>
  ⚠️  Issues: <list any known problems>

WEALTH   (port 8082) — [status]
  ✅ Done: ...
  🔲 Next: ...
  ⚠️  Issues: ...

HEALTH   (port 8083) — [status]
  ✅ Done: ...
  🔲 Next: ...
  ⚠️  Issues: ...

HOUSEHOLD (port 8084) — [status]
  ✅ Done: ...
  🔲 Next: ...
  ⚠️  Issues: ...

QUALITY GATES
  Tests:    X passing / X failing
  Coverage: X% (target: 80%)
  Sonar:    X open issues (target: 0)

NEXT MILESTONE: v0.X — <focus area>
  Features needed: <list>
```

## If $ARGUMENTS is a domain name

Show full detail for that domain:
- Complete schema tables (all columns)
- All API endpoints
- All key files with paths
- Full open issues list
- Design decisions / ADRs
- What to build next (specific files, migrations, steps)
