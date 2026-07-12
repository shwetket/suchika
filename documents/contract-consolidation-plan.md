# Contract Consolidation Plan — Shared OpenAPI Components

| | |
|---|---|
| **Type** | Plan — approved, not yet executed |
| **Audience** | Architect, backend developers |
| **Status** | **Q49-Q53 all resolved 2026-07-04 — ready to execute.** No file under `application/contract/` has been touched yet; `application/contract/shared.yaml` does not exist. See resolution deltas below before starting Phase 0. |
| **Last updated** | 2026-07-05 (resolution deltas added) |

## Update 2026-07-07 — pagination question resolved on a separate track before this plan executed

The "which pagination shape wins" question (raised as Q54 in the now-archived `OpenQuestions.md`, since this plan's own Q50 resolution said "unify" without picking a winner) was settled independently by the pre-v1.0 pagination pass — see `documents/ROADMAP.md`'s "Open Questions for the Product Owner" item 7. **Option B won:** offset-based `page`/`size` (0-indexed, default 50, max 200) is now the single project-wide shape, shipped as reusable `Page`/`Size` parameters in `shared.yaml` and extended to every list endpoint (transactions, doctor visits, vitals, inventory items, calendar events, goals, physical assets). The dead cursor-based `page_token`/`next_page_token` scaffolding was deleted (it was declared but never actually implemented behind `AccountResource`). **Action for whoever executes Phase 0/3 below:** the `PageSize`/`PageToken` components described in section 1d and the phase snippets are stale — confirm `shared.yaml` already has the shipped `Page`/`Size` shape instead, and treat every "wealth's listTransactions is a third incompatible shape" statement below as resolved, not a live decision to make.

## Resolution Deltas (2026-07-05) — read before executing Phase 0

| This plan asked | Resolved | Effect on the plan as drafted |
|---|---|---|
| Q49: 3 profile_id param variants (path-required, query-optional, query-required) — or standardize required-ness first? | **Standardize profile_id as required across all domains first** | **Simplifies to 2 variants, not 3** — drop the query-*optional* variant entirely. `wealth`/`gateway` list endpoints that today accept `profile_id` as `required: false` must become `required: true`. This is a real API behavior change, not pure contract tidying — confirm no existing caller omits `profile_id` on those endpoints before Phase 3/5 (grep `web/src/api/` call sites for `listAccounts`/`listPhysicalAssets`/gateway equivalents). |
| Q50: leave wealth's `listTransactions` `page`/`size` int-pagination as a documented exception? | **No — every domain must use the same shared pagination** | Plan's Phase 3 verification step ("confirm this is out of scope") is now wrong — it's in scope. **New question raised: Q54 below** — the resolution says "unify," not which shape wins (token-based `page_size`/`page_token` vs. `listTransactions`'s 0-indexed `page`/`size`). Do not guess; this changes `listTransactions`'s wire format either way. |
| Q51: verify `openapi-typescript` external multi-file `$ref` behavior before Phase 1 | **Yes — do the spike first** | Not yet done. Blocks Phase 0 sign-off — run the scratch test (temp copy of `gateway.yaml`, one endpoint converted, diff generated types) before touching any real contract file. |
| Q52: automate canonical→mirror sync? | **No — keep manual sync, out of scope** | Plan's existing assumption (manual sync per PR) stands as-is. No change needed. |
| Q53: split profile.yaml's additive error-schema fix into its own PR? | **No — ship it as part of this consolidation** | Phase 4 stays in this initiative; still flag it in the PR description as behavior-additive (first typed error body for that contract), per the plan's own Section 5 item 1. |

Full decision record: `documents/OpenQuestions.md` Q49-Q54.

## Objective

Move cross-domain OpenAPI fields (error responses, `profile_id` param, pagination params, and any other near-identical field across 3+ domains) into ONE canonical shared contract file, referenced via `$ref` from every domain contract. Stop redefining the same shape per-domain.

This is a plan only. No file under `application/contract/` or `application/web-gateway/src/main/resources/` is edited by this document.

---

## 1. Audit — what exists today

### 1a. The precedent already exists, half-adopted

`application/web-gateway/src/main/resources/shared.yaml` already defines:
- `parameters.ProfileIdParam`, `parameters.PageSize`, `parameters.PageToken`
- `schemas.Error` (AIP-193 shape: `code`/`status`/`message`/`details[]`), `schemas.ValidationIssue`, `schemas.Currency`
- `responses.BadRequest/NotFound/Conflict/FailedPrecondition/InternalError`

Confirmed via `grep -rl "shared.yaml" application/contract/` → **zero hits**. Only one file references it: `application/web-gateway/src/main/resources/profile.yaml` (a mirror, not canonical). The canonical `application/contract/*.yaml` files have never adopted this pattern.

### 1b. Inline duplication in canonical contracts — error responses

Every one of the 5 canonical contracts defines its own local `Error` schema + `BadRequest`/`NotFound`/`InternalError`/`Conflict` responses, nearly identical but not textually identical:

| File | Has own `Error`? | Has own `BadRequest/NotFound/...`? | `details[]` item shape |
|---|---|---|---|
| `profile.yaml` | No — uses inline `description: Not found` strings, no schema at all (see `/v1/admins/{admin_id}` GET 404) | No | N/A |
| `wealth.yaml` | Yes (line 1308) | Yes (`BadRequest`, `NotFound`, `Conflict`, `InternalError`) | inline `type: object` with `field`/`issue` properties |
| `health.yaml` | Yes (line 537) | Yes (`BadRequest`, `NotFound`, `InternalError` — no `Conflict`) | inline `type: object`, same shape as wealth |
| `household.yaml` | Yes (line 873) | Yes (`BadRequest`, `NotFound`, `InternalError` — no `Conflict`) | inline `type: object`, same shape |
| `gateway.yaml` | Yes (line 2229, **added this session**) | Yes (`BadRequest`, `NotFound`, `Conflict`, `InternalError` — added this session) | inline `type: object`, same shape |

**Key finding:** `wealth.yaml`, `health.yaml`, `household.yaml`, and `gateway.yaml`'s local `Error` schema are *structurally* identical to `shared.yaml`'s `Error` (same 4 fields, same required set) except one real difference — `shared.yaml.Error.details[].items` is `$ref: '#/components/schemas/ValidationIssue'` (a named schema) while all four domain files inline `{field, issue}` as an anonymous object. Same shape, different schema identity. Converting to `$ref` is a structural refactor, not a breaking change, IF the generated TypeScript types collapse to the same effective shape (see Phase verification steps — must confirm `openapi-typescript` emits equivalent types for named vs inline object schema).

**profile.yaml is the outlier — flag this, do not silently normalize it (see item 5 below).** It has no `Error` schema and no reusable error responses at all; failure responses are bare `description` strings with no `content`/schema (e.g., `"404": description: Not found` — no body shape declared). This is not "the same shape, defined thrice" — it's a **missing** shape. Adopting `shared.yaml` here is not a pure refactor; it's the first time `profile.yaml` gets a typed error body. Low risk (additive), but must be called out as a behavior change, not a no-op.

### 1c. Inline duplication — `profile_id` parameter

`profile_id` as a query or path parameter is defined inline (not via `$ref`) in:
- `wealth.yaml`: 8 separate inline definitions (`listAccounts`, `createAccount`, `getAccountBalance`, `getAmortization`, `listTransactions`, `createTransaction`, `listPhysicalAssets`, `createPhysicalAsset`) — all textually identical `{name: profile_id, in: query, required: false, schema: {type: string, format: uuid}}`
- `health.yaml`: 3 inline definitions (`listVitalReadings`, `listDoctorVisits`), both `required: true` (differs from wealth's `required: false` — see note below)
- `household.yaml`: 3 inline definitions (`listCalendarEvents`, `listInventoryItems`, `listGoals`), all `required: true`
- `gateway.yaml`: mirrors wealth's pattern, ~13 inline definitions (`required: false`, since gateway aggregates and treats `profile_id` as an optional filter in most list endpoints)
- `profile.yaml`: does NOT use `profile_id` as a parameter shape the same way — it has `admin_id` (path, required) and `profile_id` as a path segment named `id` in some places (see 1e below) — different semantics, do not force into the same shared parameter.

**Real difference found, not cosmetic:** `wealth`/`gateway` treat `profile_id` as `required: false` (optional filter on list endpoints); `health`/`household` treat it as `required: true` (mandatory scoping — no unscoped list allowed). **`shared.yaml`'s existing `ProfileIdParam` is a `required: true` PATH parameter**, not a query parameter — it does not match either usage today. This means the existing shared component can't be reused as-is for the wealth/gateway *query*-param case without a second variant. Plan must add a `ProfileIdQueryParam` (optional, query) alongside the existing `ProfileIdParam` (required, path) rather than force one shape onto both. Flagged as **Q49**.

### 1d. Inline duplication — pagination params

`PageSize`/`PageToken` already exist as named components in **both** `wealth.yaml` (its own local copy, lines 716-737) and `shared.yaml` (lines 23-45) — textually near-identical (same bounds: min 1, max 100, default 20). `wealth.yaml` is the only domain contract that currently uses pagination at all (`listAccounts`, `listPhysicalAssets`). `gateway.yaml` mirrors the same inline shape for its proxied list endpoints. `health.yaml`/`household.yaml`/`profile.yaml` have no pagination today (v0.6 added `page`/`size` int-based pagination to `wealth.yaml`'s `listTransactions` instead — a **third, different pagination shape**: 0-indexed `page`/`size` integers, not `page_token`/`page_size`. This is a real inconsistency already in the codebase, not introduced by this plan — flagged as **Q50**, not silently unified).

### 1e. Other near-identical fields across 3+ domains (candidates checked)

| Field | Found in | Verdict |
|---|---|---|
| `created_at` (response field, `type: string, format: date-time, readOnly: true`) | gateway(6), household(4), wealth(4), health(2), profile(2) — all 5 files | Textually identical everywhere checked. **Good shared-schema candidate** — but as a reusable property fragment, not a full schema (it's a field, not an object). OpenAPI has no clean "shared property" `$ref` inside `properties:` without `allOf` gymnastics per-field, which adds noise for a 3-line saving. **Recommend: do NOT centralize this one** — see Decision section. |
| `is_active` (`type: boolean`, sometimes `default: true`) | gateway(15), wealth(10), profile(5); health/household have 0 (no soft-delete boolean field in those two response schemas as of v0.6) | Same reasoning as `created_at` — trivial field, not worth `$ref` ceremony. Not centralizing. |
| UUID-typed ID path param (`{type: string, format: uuid}`) | every domain, e.g. `AccountId`, `TxnId`, `AssetId`, `Id` (health/household), `admin_id`/`id` (profile) | Already named per-domain (`AccountId`, `Id`, etc.) — the *shape* is identical but the *name/semantics* differ per resource. Forcing these into one shared `GenericIdParam` would lose the self-documenting parameter name in generated clients/Swagger UI. **Not centralizing** — only `profile_id` and `admin_id` are true cross-domain identity concepts worth a shared component; per-resource IDs (`account_id`, `txn_id`) are legitimately domain-local. |
| `total_size` (`type: integer`, list response wrapper field) | every `List*Response` schema, all 5 files | Same as `created_at`/`is_active` — trivial scalar field repeated by convention, not a schema worth `$ref`. Not centralizing. |
| `Currency` enum (`INR` only) | Only in `shared.yaml` today — **not used by any canonical contract yet** (Currency doesn't appear in `wealth.yaml`, which is the one domain that would logically use it) | Not a duplication problem today (no domain has its own competing `Currency` enum), but confirms `shared.yaml`'s existing content was written speculatively ahead of adoption. No action needed now — noted for completeness. |

**Conclusion on 1e:** the only components worth centralizing are: `Error` schema, `ValidationIssue` schema, `BadRequest`/`NotFound`/`Conflict`/`FailedPrecondition`/`InternalError` responses, `ProfileIdParam` (path, required — existing), a new `ProfileIdQueryParam` (query, optional — new, Q49), and `PageSize`/`PageToken` (existing). Scalar response fields (`created_at`, `is_active`, `total_size`) and per-resource ID params are **not** worth centralizing — the `$ref` indirection cost exceeds the ~1-line duplication saved, and per-resource param names carry useful self-documentation.

---

## 2. Where the canonical file lives

**Decision: `application/contract/shared.yaml` becomes the new canonical source of truth.** `application/web-gateway/src/main/resources/shared.yaml` becomes its mirror — consistent with the existing documented convention in `CLAUDE.md`: "Domain service contracts (`application/contract/{domain}.yaml`) are mirrored into `application/web-gateway/src/main/resources/` for the Rest Client."

Today the relationship is backwards: the web-gateway resources copy is the only one with real content, and the canonical location has nothing. This plan reverses that — canonical `application/contract/shared.yaml` becomes the file every domain and the gateway `$ref`s into, and the web-gateway resources copy becomes a byte-for-byte mirror kept in sync manually (same as `profile.yaml`/`wealth.yaml`/etc. mirrors are today — there is no automated sync step in this repo; mirrors are updated by hand alongside the canonical file in the same PR).

**Content: extend in place, do not restructure.** The existing `application/web-gateway/src/main/resources/shared.yaml` content (parameters, schemas, responses sections) is already well-organized and matches AIP-193/158 conventions the rest of the codebase follows. Plan:
1. Copy it byte-for-byte to `application/contract/shared.yaml` (new canonical).
2. Add the new `ProfileIdQueryParam` (Q49) to the canonical copy.
3. Leave `application/web-gateway/src/main/resources/shared.yaml` as the mirror (re-copy after step 2 so both are identical).

No section reshuffling needed — the file's internal structure (`parameters` / `schemas` / `responses`) already matches what every domain contract needs.

---

## 3. Phased rollout order

**Phase order: `household.yaml` → `health.yaml` → `wealth.yaml` → `profile.yaml` → `gateway.yaml`.**

| Phase | File | Why this position |
|---|---|---|
| 0 | Create `application/contract/shared.yaml` + mirror | Prerequisite — nothing else can `$ref` it until it exists |
| 1 | `household.yaml` | Smallest, most uniform error-response usage (`BadRequest`/`NotFound`/`InternalError` only, no `Conflict`, no pagination, `profile_id` always `required: true` query param, consistent across all 3 resources). Least duplication risk — every occurrence is the same shape, easy to verify by diff. |
| 2 | `health.yaml` | Same profile as household (no `Conflict`, no pagination, `profile_id` always `required: true`) — same conversion recipe as Phase 1, low risk, reinforces the pattern before touching a more complex file. |
| 3 | `wealth.yaml` | Most complex domain contract: has `Conflict`, has its own `PageSize`/`PageToken` (must dedupe against `shared.yaml`'s existing copy, not just add a `$ref` on top), has `profile_id` as `required: false` query param (needs the new `ProfileIdQueryParam`, Q49), and has the page/size int-pagination inconsistency (Q50) sitting alongside it. Doing this third, after two clean reps, means the team has already validated the mechanical `$ref` swap twice before hitting wealth's edge cases. |
| 4 | `profile.yaml` | Deliberately after wealth, not before. `profile.yaml` requires the identified behavior change (item 1b: no existing `Error` schema at all — first time this file gets typed error bodies). Isolating this as the last domain file (before gateway) keeps the "structural, zero-behavior-change" phases (1-3) cleanly separated from the one domain file where the change is additive-but-not-purely-structural. |
| 5 | `gateway.yaml` | Last, as recommended in the task brief — it aggregates all four domains, is the largest file (2280 lines), is what `npm run generate:api` reads directly, and already has the newly-added (this session) duplicate `Error`/`BadRequest`/`NotFound`/`Conflict`/`InternalError` block that is the concrete anti-pattern instance to remove. Also must apply the `ProfileIdQueryParam` (from wealth's Phase 3 precedent) across its ~13 inline `profile_id` occurrences. Doing it last means every shared-component edge case has already been resolved once in a domain file first. |

Each phase is its own PR. Do not batch phases — each must pass `npm run generate:api` + a type-diff check independently before the next phase starts.

---

## 4. Before/after snippets + verification per phase

### Phase 0 — create `application/contract/shared.yaml`

Action: copy `application/web-gateway/src/main/resources/shared.yaml` verbatim to `application/contract/shared.yaml`, then add:

```yaml
    ProfileIdQueryParam:
      name: profile_id
      in: query
      required: false
      description: >
        Optional filter/scoping by household member profile. Some list endpoints
        require this (health, household); others treat it as an optional filter
        (wealth, gateway) — required-ness is set per-operation via override,
        this component only fixes name/location/schema.
      schema:
        type: string
        format: uuid
        example: "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
```

Note: OpenAPI parameter `$ref` does not support overriding `required` at the call site in 3.1 the same way `allOf` works for schemas — a `$ref`'d parameter is used as-is. Since health/household need `required: true` and wealth/gateway need `required: false`, **two named parameters are needed** (`ProfileIdParam` stays path/required for existing use in `profile.yaml`-style resource paths; add `ProfileIdQueryParam` for the optional-query case; add a third `ProfileIdRequiredQueryParam` for health/household's mandatory-query case). This 3-variant reality is why Q49 is flagged — confirm naming before Phase 1.

Verification: no consumer references it yet — nothing to regenerate. Just validate the YAML parses (`npx swagger-cli validate application/contract/shared.yaml` or equivalent linter already used in this repo, confirm in `CICD.md`/pre-commit config which linter is wired in).

---

### Phase 1 — `household.yaml`

**Before** (repeated 3x for `listCalendarEvents`, `listInventoryItems`, `listGoals`):
```yaml
        - name: profile_id
          in: query
          required: true
          schema:
            type: string
            format: uuid
```

**After:**
```yaml
        - $ref: './shared.yaml#/components/parameters/ProfileIdRequiredQueryParam'
```

**Before** (error responses + schema, lines 871-915):
```yaml
    Error:
      type: object
      required: [code, message, status]
      properties:
        code:
          type: integer
        status:
          type: string
        message:
          type: string
        details:
          type: array
          items:
            type: object
            properties:
              field:
                type: string
              issue:
                type: string

  responses:

    BadRequest:
      description: Request validation failed
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/Error'

    NotFound:
      description: Resource not found
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/Error'

    InternalError:
      description: Unexpected server error
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/Error'
```

**After:** delete the local `Error` schema and all three local `responses` entries entirely. Every call site that did `$ref: '#/components/responses/BadRequest'` changes to `$ref: './shared.yaml#/components/responses/BadRequest'` (same for `NotFound`, `InternalError` — household has no `Conflict` today, don't add one it never used).

**Verification:**
1. `cd application/contract && npx swagger-cli bundle household.yaml -o /tmp/household-bundled.yaml` (or whatever bundler the repo uses) — confirm it resolves without error.
2. Diff the *bundled/dereferenced* output against a bundle of the pre-change file — the fully-resolved schema must be identical (this proves the refactor is behavior-preserving at the schema level, independent of `$ref` structure).
3. `household.yaml` is not directly consumed by `npm run generate:api` (only `gateway.yaml` is) — but it IS mirrored to `application/web-gateway/src/main/resources/household.yaml` and consumed by the household `RestClient` in `web-gateway`. Run `./gradlew :application:web-gateway:compileJava` to confirm the Rest Client interface still compiles against the mirrored contract (update the mirror in the same PR).
4. Run household domain adapter tests (`./gradlew :application:domain:household:adapters:test`) — contract-driven codegen (if the household module uses OpenAPI-generated server stubs; confirm via `build.gradle` whether it does) must still compile.

---

### Phase 2 — `health.yaml`

Same recipe as Phase 1 (household). One extra note: health's `profile_id` is `required: true` query param in both `listVitalReadings` and `listDoctorVisits` — same `ProfileIdRequiredQueryParam` component as household. No `Conflict` response used here either — don't introduce one.

Verification: identical 4 steps as Phase 1, targeted at `health.yaml` / `application/web-gateway/src/main/resources/health.yaml` / `:application:domain:health:adapters:test`.

---

### Phase 3 — `wealth.yaml`

**Before** (`profile_id` as optional query param, repeated 8x, e.g. `listAccounts`):
```yaml
        - name: profile_id
          in: query
          required: false
          schema:
            type: string
            format: uuid
```
**After:**
```yaml
        - $ref: './shared.yaml#/components/parameters/ProfileIdQueryParam'
```

**Before** (wealth's own local `PageSize`/`PageToken`, lines 716-737 — must be deleted, not `$ref`'d on top of):
```yaml
    PageSize:
      name: page_size
      in: query
      required: false
      description: >
        Maximum number of items to return.
        Follows AIP-158. Defaults to 20, maximum 100.
      schema:
        type: integer
        minimum: 1
        maximum: 100
        default: 20

    PageToken:
      name: page_token
      in: query
      required: false
      description: >
        Token from previous response next_page_token.
        Omit for first page.
      schema:
        type: string
```
**After:** delete both blocks. Existing call sites already do `$ref: '#/components/parameters/PageSize'` (internal ref) — change to `$ref: './shared.yaml#/components/parameters/PageSize'` (external ref). Confirm bounds match exactly first (they do: min 1, max 100, default 20 — verified identical in section 1d) before deleting, so no silent behavior change slips in under cover of the refactor.

**Before** (`Error` + responses, lines 1308-1390 — same recipe as household/health, but wealth HAS `Conflict`, keep that mapping):
```yaml
    Error: { ... same shape ... }
  responses:
    BadRequest: { ... }
    NotFound: { ... }
    Conflict: { ... }
    InternalError: { ... }
```
**After:** delete all four local response defs + local `Error` schema; repoint all `$ref: '#/components/responses/X'` call sites to `$ref: './shared.yaml#/components/responses/X'` (four distinct names, `Conflict` included).

**Verification:**
1. Bundle + diff (as Phase 1/2), paying special attention to the pagination params since this is the one place an actual component (not just an error shape) is being de-duplicated against an existing near-twin in `shared.yaml` — confirm `minimum`/`maximum`/`default` match exactly pre/post.
2. `wealth.yaml` IS one of the domain contracts mirrored for the wealth `RestClient` in `web-gateway` — same Gradle compile check as Phase 1/2, targeted at `:application:domain:wealth:adapters:test` and `:application:web-gateway:compileJava`.
3. `wealth.yaml` is not read by `npm run generate:api` directly, but the gateway's own `listAccounts`/`listPhysicalAssets` operations mirror wealth's pagination shape (see Phase 5) — no frontend regen needed yet at this phase, only after gateway is converted.

---

### Phase 4 — `profile.yaml`

**This phase is not purely structural — flag explicitly per task item 5.** `profile.yaml` today has *no* `Error` schema and *no* reusable error responses; failures are declared as bare `description` strings with no `content`/schema at all, e.g.:

**Before** (`/v1/admins/{admin_id}` GET):
```yaml
        "404":
          description: Not found
```
**After:**
```yaml
        "404":
          $ref: './shared.yaml#/components/responses/NotFound'
```

This is **additive, not a pure rename** — clients that previously received a 404 with an empty or undefined body now receive a structured JSON body (`{code, status, message}`). This is technically backward-compatible for any consumer that wasn't asserting on body shape (very likely true here — the gateway proxies these calls and the frontend never inspects raw domain-service error bodies directly, only the gateway's own response), but it is a genuine behavior change to the contract, not a no-op refactor. **Do not fold this into the "just a refactor" narrative in the PR description** — call it out.

Same treatment applies to every bare `description:`-only error response in `profile.yaml` (check all of `/v1/admins`, `/v1/admins/{admin_id}`, `/v1/admins/{admin_id}/policy`, `/v1/profiles`, `/v1/profiles/{id}` — several currently have zero content schema on their 4xx responses).

`profile_id`/`admin_id` params: `profile.yaml` uses `admin_id` as a **required path** parameter (`/v1/admins/{admin_id}`) and `id` as a required path parameter for `/v1/profiles/{id}` (not literally named `profile_id`). The existing `shared.yaml` `ProfileIdParam` (path, required, named `profile_id`) does not textually match `profile.yaml`'s `id`-named path param — parameter `name` must match the path template placeholder, so `ProfileIdParam` can only be reused where the path segment is literally `{profile_id}`. `profile.yaml`'s `/v1/profiles/{id}` uses `{id}`, not `{profile_id}` — **do not force a rename of the path template as part of this refactor**; that would be an actual breaking API change (URL shape), out of scope. Leave `id`-named path params alone; they are not the same thing as the `profile_id` query-param duplication problem this plan targets. Note this distinction explicitly in the PR — do not `$ref` something that doesn't match.

**Verification:**
1. Bundle + diff for the parts that ARE pure refactor (none currently, since profile.yaml has no duplicated error shape to begin with — this phase is 100% additive).
2. Because this is additive/behavior-changing, add a note to the PR description flagging exactly which endpoints now return a body where none was specified before. This is the one phase where "no consumer-visible change" cannot be asserted — flag for explicit sign-off before merge, not just automated verification.
3. `./gradlew :application:domain:profile:adapters:test` — confirm `ApplicationExceptionMapper` output (which already returns structured JSON today, per `LOGGING_AND_EXCEPTIONS.md`) matches the newly-declared contract shape. This is actually a **contract-catches-up-to-implementation** fix, not implementation change — the Java layer via `ApplicationExceptionMapper` already returns `{code, status, message}` JSON at runtime (per `CLAUDE.md`'s documented exception-mapping); `profile.yaml` was simply the one contract file that never declared this in its schema. Confirm this by reading `ApplicationExceptionMapper` before executing this phase — if true, this phase is actually a **bug fix** in the contract (making it match reality), and should be framed that way, not as a pure mechanical refactor.

---

### Phase 5 — `gateway.yaml`

**Before** (the newly-added-this-session duplicate block, lines 2229-2280+):
```yaml
    Error:
      type: object
      required: [code, message, status]
      properties: { ... same 4 fields ... }

  responses:
    BadRequest: { ... }
    NotFound: { ... }
    Conflict: { ... }
    InternalError: { ... }
```
**After:** delete entirely. Every `$ref: '#/components/responses/X'` across gateway.yaml's ~13+ operations repoints to `$ref: './shared.yaml#/components/responses/X'`.

**Before** (13 inline `profile_id` query param occurrences, same shape as wealth's):
```yaml
        - name: profile_id
          in: query
          required: false
          schema:
            type: string
            format: uuid
```
**After:**
```yaml
        - $ref: './shared.yaml#/components/parameters/ProfileIdQueryParam'
```

**Verification — the critical phase, since this is what `npm run generate:api` reads directly:**
1. `cd web && npm run generate:api` — regenerate `web/src/api/generated.ts`.
2. `git diff web/src/api/generated.ts` — inspect the diff by hand. Expected: **zero type-shape changes** for every operation whose param/response was converted (same effective TS type for `Error`, `BadRequest` response body, `profile_id` param type). Any diff beyond whitespace/comment reordering is a regression — do not merge.
3. If `openapi-typescript` (the tool in `package.json`) emits a *different* TS type for a named external `$ref` vs a same-shape inline schema (some generators produce `components["schemas"]["Error"]` either way but some collapse differently for external files) — verify this explicitly with a scratch run before Phase 1 even starts, not just at Phase 5. **Recommend doing this check as part of Phase 0**, since if `openapi-typescript` mishandles external multi-file `$ref` resolution at all, the entire plan's approach needs to change (e.g., some codegen tools require a pre-bundle step before pointing the generator at a single file). This is flagged as **Q51** — verify tool compatibility before Phase 1 starts, not discovered at Phase 5.
4. Run full frontend test suite (`npm run test:ci`) — confirms no component broke from a generated-type shift.
5. Run `./gradlew test` (full backend suite) — gateway's own tests (`@InjectMock @RestClient` pattern, ADR-011) stub domain clients; confirm none of them assert on the literal JSON shape of error bodies in a way that a schema-identity change (named vs inline) could break (unlikely, since Mockito stubs return Java objects, not raw JSON — but confirm, since the mirrored `*.yaml` files under `application/web-gateway/src/main/resources/` are what MicroProfile Rest Client generates against, and Quarkus's rest-client codegen could differ from `openapi-typescript`'s TS codegen in how it handles external refs).

---

## 5. Where "same field" is NOT actually the same shape — explicit flag

Per task item 5, called out explicitly (not silently unified):

1. **`profile.yaml` has no error schema at all today** (section 1b, Phase 4) — adopting `shared.yaml`'s `Error` here is additive (first time this contract declares an error body shape), not a pure refactor. The underlying Java (`ApplicationExceptionMapper`) likely already returns this shape at runtime — but the *contract* never said so. This is a contract-vs-implementation gap being closed, which is good, but must not be described as risk-free in the PR.

2. **`profile_id` required-ness differs by domain** (section 1c) — `wealth`/`gateway` = optional query filter; `health`/`household` = mandatory query param. These are two different parameters with the same name, not one component with an override. Plan introduces two (or three, see Q49) named variants rather than pretending it's one shape.

3. **Pagination has THREE competing shapes already in the codebase**, not one being centralized (section 1d): (a) `shared.yaml`'s `page_size`/`page_token` (opaque token-based, AIP-158), (b) `wealth.yaml`'s own near-identical `page_size`/`page_token` copy (same shape, duplicate definition — this one IS safe to dedupe), (c) `wealth.yaml`'s v0.6 `listTransactions` endpoint using a **third, incompatible** `page`/`size` 0-indexed-integer shape (added in v0.6, per `CONTEXT_PRIMER.md`). (a) and (b) are safe to merge (same shape, real duplication). (c) is a **different pagination paradigm entirely** — do not attempt to fold it into the shared `PageSize`/`PageToken` components; that would be a breaking change to `listTransactions`. Flagged as **Q50** — confirm out of scope for this consolidation, tracked separately if a future unification is ever wanted.

---

## Summary of file changes (for execution phase, not done now)

| File | Change |
|---|---|
| `application/contract/shared.yaml` | **New** — canonical shared components, copied from web-gateway mirror + `ProfileIdQueryParam`/`ProfileIdRequiredQueryParam` additions |
| `application/web-gateway/src/main/resources/shared.yaml` | Re-synced to match canonical (mirror convention) |
| `application/contract/household.yaml` | Phase 1 — error responses + `profile_id` param → `$ref` |
| `application/web-gateway/src/main/resources/household.yaml` | Mirror update, same PR as Phase 1 |
| `application/contract/health.yaml` | Phase 2 — same as household |
| `application/web-gateway/src/main/resources/health.yaml` | Mirror update, same PR as Phase 2 |
| `application/contract/wealth.yaml` | Phase 3 — error responses + `profile_id` param + pagination params → `$ref`, local duplicates deleted |
| `application/web-gateway/src/main/resources/wealth.yaml` | Mirror update, same PR as Phase 3 |
| `application/contract/profile.yaml` | Phase 4 — bare `description`-only error responses → `$ref` (additive, flagged) |
| `application/web-gateway/src/main/resources/profile.yaml` | Already uses `$ref` (existing precedent) — confirm it still matches canonical after Phase 4, re-sync if canonical adds anything profile.yaml's mirror doesn't have |
| `application/contract/gateway.yaml` | Phase 5 — remove this-session's duplicate `Error`/responses block, convert `profile_id` params, regenerate frontend client |
| `web/src/api/generated.ts` | Regenerated via `npm run generate:api` at Phase 5, verified byte-diff-equivalent in type shape |

---

## Open Questions

**Q49.** `shared.yaml`'s existing `ProfileIdParam` is a required **path** parameter. Wealth/gateway need `profile_id` as an optional **query** parameter; health/household need it as a required **query** parameter. That's three distinct shapes sharing one field name. Proposed fix: keep `ProfileIdParam` (path, required) as-is for any future path-based usage, add `ProfileIdQueryParam` (query, optional) for wealth/gateway, add `ProfileIdRequiredQueryParam` (query, required) for health/household. Confirm naming and whether 3 variants is acceptable, or whether the product owner would rather standardize required-ness across domains first (a product decision, not just a contract-tidiness one — e.g., should wealth's account list actually require `profile_id` too, closing a filtering gap, rather than adding a second shared parameter to match wealth's current optional behavior?).

**Q50.** `wealth.yaml`'s v0.6 `listTransactions` endpoint uses a third pagination shape (`page`/`size`, 0-indexed integers) that is incompatible with the `page_size`/`page_token` shape used everywhere else (including wealth's own `listAccounts`/`listPhysicalAssets`). This plan treats it as out-of-scope/pre-existing and does not attempt to unify it. Confirm this is acceptable, or whether a future pagination-unification pass should be scheduled (tracked as a backlog item either way, separate from this consolidation).

**Q51.** Needs verification before Phase 1 begins, not discovered at Phase 5: does `openapi-typescript` (the codegen tool wired into `npm run generate:api`, per `web/package.json`) correctly resolve external multi-file `$ref` (`./shared.yaml#/components/...`) when pointed at `application/contract/gateway.yaml`, and does it produce an identical TypeScript type for a schema reached via external named `$ref` versus the current inline-duplicated version? Recommend a throwaway scratch test (point the tool at a temp copy of `gateway.yaml` with one endpoint converted, diff the generated type) before committing to the phase order above. If the tool requires a pre-bundle step (e.g., `swagger-cli bundle` before `openapi-typescript`), that changes the `generate:api` npm script and is a bigger change than this plan currently assumes — needs a yes/no before Phase 1.

**Q52.** Should the mirrored copies under `application/web-gateway/src/main/resources/*.yaml` be updated in the *same* PR as each canonical-contract phase (this plan's assumption, matching how `profile.yaml`'s mirror was apparently kept in sync historically), or does the team want a follow-up automated sync step (e.g., a Gradle/npm task that copies canonical → mirror, replacing manual copy-paste) as part of this consolidation, given that keeping N mirrors hand-synced is itself a duplication-risk pattern this plan is otherwise trying to eliminate? Out of scope to build now, but worth flagging since the plan's own Phase 0-5 mechanism doesn't fix the meta-problem of manual mirror sync.

**Q53.** Phase 4 (`profile.yaml`) is additive — it gives previously-bare error responses a real schema for the first time. Should this ship as part of the "contract consolidation" initiative at all, or should it be split out as its own small independent PR/ADR ("profile.yaml error responses were never contract-typed — fix it") so that the consolidation PRs (1, 2, 3, 5) can be honestly described as zero-behavior-change refactors, and Phase 4 can be reviewed under a different bar (behavior change, needs explicit sign-off, not just a diff-check)? Recommend splitting, but the product owner should confirm before Phase 4 is scheduled.
