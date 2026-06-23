## What does this PR do?
<!-- One sentence. Be specific. "Add X to Y" not "Some changes" -->

## Why?
<!-- Business reason or issue link. Reviewers need context, not just code. -->

## Domain(s) affected
- [ ] profile
- [ ] wealth
- [ ] health
- [ ] household
- [ ] web-gateway
- [ ] infrastructure / CI / docs

## Type of change
- [ ] `feat` — new feature
- [ ] `fix` — bug fix
- [ ] `refactor` — no behaviour change
- [ ] `docs` — documentation only
- [ ] `chore` — build, deps, config
- [ ] `test` — tests only

## Test plan
<!-- How did you verify this works? Manual steps if applicable. -->

- [ ] `./gradlew test` passes
- [ ] `npm run test:ci` passes (if frontend changed)
- [ ] Manually tested in browser at `http://localhost:3000`

## Architecture checklist
- [ ] Domain layer (`domain/`) has zero framework imports (`@Inject`, JPA, HTTP)
- [ ] All DB queries filtered by `profile_id` (ADR-006)
- [ ] No new SQL ENUMs — discriminators use plain `VARCHAR`
- [ ] New Flyway migration created if schema changed (never edit an existing one)
- [ ] OpenAPI contract updated + `npm run generate:api` re-run if contract changed
- [ ] `AppLogger` used — not raw SLF4J / `System.out`
- [ ] Typed exception from `shared/exception/` thrown — not raw `RuntimeException`

## SonarQube
- [ ] Quality gate green — run locally with `/sonar` before requesting review

## Screenshots (if UI changed)
<!-- Paste before/after screenshots or a short screen recording -->
