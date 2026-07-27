# Native-Image Feasibility Investigation

| | |
|---|---|
| **Type** | Reference — Research Spike |
| **Audience** | Architect, DevOps, all developers |
| **Status** | Informational — no action taken, no ADR filed |
| **Last updated** | 2026-07-13 |

## Objective

Investigate what it would take to run Suchika's 5 Quarkus services (`profile`, `wealth`, `health`, `household` adapters modules + `web-gateway`) as GraalVM native-image executables instead of on the JVM. This is a **research spike, not a build** — no code changes, no Dockerfiles added, no Gradle config touched. Deferred until an actual trigger point (see Recommended Trigger Point below), explicitly not required for any near-term milestone.

**Roadmap-numbering flag:** the request that spawned this investigation used the phrase "not required until 3.0." `ROADMAP.md`'s actual v3.0 milestone is titled **"GitHub Ready"** — contribution guidelines, issue/PR templates, public roadmap. It has nothing to do with native-image or Docker. Docker containerization is currently scoped to **v2.1 "Cloud Ready."** This doc does not renumber `ROADMAP.md` — that is a product-owner call. Treat "3.0" in the original ask as informal shorthand for "not urgent," and reconcile the literal number against `ROADMAP.md` before using it in any planning doc. See "Recommended Trigger Point" below for where this actually belongs sequence-wise.

---

## 1. Extension-by-Extension Native-Image Compatibility

All 5 services (post Phase 1 of this same platform-improvements plan, which adds `quarkus-smallrye-health` everywhere) share one dependency shape, confirmed by reading each module's `build.gradle.kts` (e.g. `application/domain/wealth/adapters/build.gradle.kts`):

| Extension | Native support | Notes |
|---|---|---|
| `quarkus-arc` | Yes — foundational | Quarkus's CDI-lite DI container; build-time processed by design, the whole reason Quarkus native-images work at all. No config needed. |
| `quarkus-rest` (RESTEasy Reactive) | Yes — first-party, GA | Default REST layer for native since Quarkus 2.x; no known gaps for typical JAX-RS usage (`@Path`, `@GET`/`@POST`, etc. — matches this codebase's `*Resource` classes). |
| `quarkus-rest-jackson` | Yes — first-party, GA | Jackson serialization is build-time-scanned for `@RegisterForReflection`-equivalent handling of DTOs Quarkus can see. Records/DTOs referenced only via generics or reflection-heavy patterns (rare in this codebase's DTO style) are the usual edge case — see reflection-config note below. |
| `quarkus-hibernate-orm-panache` | Yes — first-party, GA | Panache entities are build-time enhanced; this is one of the most heavily native-tested Quarkus extensions. Every domain's `*Entity` classes (`AccountEntity`, `TransactionEntity`, etc.) are exactly the shape this extension expects. |
| `quarkus-jdbc-postgresql` | Yes — first-party, GA | Native PostgreSQL JDBC driver substitutions are maintained directly by Quarkus. |
| `quarkus-flyway` | Yes — first-party, GA | Runs migrations at startup same as JVM mode; classpath scanning for `V*__*.sql` files under `application/flyway/<domain>/` works the same in native (resource inclusion is automatic for the default migration locations). |
| `quarkus-smallrye-openapi` | Yes — first-party, GA | Generates the OpenAPI document at build time either way; no behavioral difference for native. |
| `quarkus-smallrye-health` | Yes — first-party, GA | Standard MicroProfile Health; landing via Phase 1 of this plan across all 5 services — no native-specific caveat. |

**Bottom line: every extension in current + planned (Phase 1) use is a first-party Quarkus extension with mature, GA native-image support.** This is not a "some extensions won't make it" situation — it's a clean, boring dependency set from a native-image perspective. No third-party or community extensions with partial/experimental native status are in use.

**Caveat on verification:** the extension-level compatibility above matches established Quarkus knowledge (these are all Tier-1 first-party extensions with native support since early Quarkus 2.x/3.x) and was spot-checked against current Quarkus docs (`quarkus.io/extensions/`) via live search — search results confirmed Hibernate ORM with Panache, Flyway, and SmallRye Health all remain actively documented as native-buildable for the current 3.29.x line. No red flags surfaced. This is not a line-by-line reflection-config audit of Suchika's actual classes — that only happens by actually running a native build (see item 2 below).

---

## 2. What's Missing to Even Attempt It

This is greenfield work — confirmed nothing below exists in the repo today.

### a) Dockerfiles — 5x `Dockerfile.native`, none exist
Standard Quarkus scaffolding normally generates `src/main/docker/{Dockerfile.jvm,Dockerfile.native,Dockerfile.legacy-jar}` per module. **None of the 5 modules have any of these three.** The only Docker artifact in the repo is `.devcontainer/docker-compose.yml`, which is a dev-container config (spins up Java + Postgres for local Codespaces dev), not a deployment artifact. This means before native packaging is even reachable, someone has to:
- Generate/write `Dockerfile.native` for `profile`, `wealth`, `health`, `household` adapters modules, and `web-gateway` (5 files)
- Decide on base image: `quay.io/quarkus/quarkus-micro-image` (distroless-style, smallest) vs `ubi-quarkus-native-binary` (RHEL-based, has a shell for debugging) — standard Quarkus choice, no Suchika-specific constraint either way
- Also **`Dockerfile.jvm` doesn't exist either** — see the sequencing argument in section 5, this is the more important gap to close first

### b) GraalVM/Mandrel-for-JDK-25 toolchain maturity
Phase 0 of this plan (complete, merged) bumped the whole repo to **Java 25**. Checked whether a matching native-image toolchain exists and is production-ready:
- **GraalVM for JDK 25** (both Oracle GraalVM 25 and GraalVM Community Edition 25) released **2025-09-16**, built directly on OpenJDK/Oracle JDK 25. Not a preview or early-access build — a full release.
- **Mandrel 25** (Red Hat's downstream distribution purpose-built for Quarkus native builds, generally the recommended choice over vanilla GraalVM CE for Quarkus projects) is also released, built on Eclipse Temurin 25 LTS, and the Quarkus team's own announcement states **Quarkus 3.27.0+ is the recommended minimum** to pair with Mandrel 25 (`quay.io/quarkus/ubi9-quarkus-mandrel-builder-image:jdk-25`). Suchika is on Quarkus BOM **3.29.0** — comfortably past that floor.
- **Conclusion: the toolchain is not a blocker.** This is a pleasant surprise relative to the original concern ("Java 25 is very recent, GraalVM's JDK 25 build may not be mature yet") — as of this investigation (2026-07-13), roughly 10 months after GraalVM 25 GA, both GraalVM CE and Mandrel have JDK-25-based releases that are the current recommended track, not a bleeding-edge outlier.

### c) Reflection config (`reflection-config.json` / `@RegisterForReflection`)
No such file exists anywhere in the repo (expected — nothing native-related exists yet). Likely needed once a real native build is attempted:
- **Jackson DTOs:** most of this codebase's request/response DTOs are plain records/POJOs directly referenced by `@Path` resource methods — Quarkus's build-time Jackson integration auto-registers these via static analysis. Risk is low but not zero for any DTO only reached through generics, `Object`-typed fields, or dynamic (de)serialization.
- **Panache entities:** `quarkus-hibernate-orm-panache` handles entity reflection registration automatically via build-time bytecode enhancement — this is exactly what the extension exists to do, not a manual reflection-config chore.
- **JSONB metadata handling:** Suchika stores several `metadata JSONB` columns (`account.metadata`, `transaction.metadata`, `physical_asset.metadata`, `admin.policy_settings`) parsed via `JsonbMetadataUtil`. If that utility does anything reflective/generic with Jackson (e.g., `ObjectMapper.readValue(json, Map.class)` or dynamic typing) it's a plausible candidate for needing an explicit reflection hint — this can only be confirmed by actually running `-Dquarkus.native.enabled=true` locally and reading the native-image build report for reflection warnings, not by static code review.
- **Practical approach when this is attempted:** run one module's native build, read GraalVM's build-time reachability warnings, add `@RegisterForReflection` or `reflection-config.json` entries as flagged — this is normally a fast iterate-and-fix loop for a codebase this size (4-5 entity classes per domain, no exotic reflection patterns), not a multi-week effort.

---

## 3. Build Time / CI Cost

Native-image compilation is a fundamentally heavier build step than a JVM build — this is a real, recurring cost, not a one-time toll:

| | JVM build (`quarkusBuild`, current) | Native build (`-Dquarkus.native.enabled=true`) |
|---|---|---|
| Per-service build time | Seconds to low tens of seconds | **Typically 1.5–5 minutes per service**, depending on classpath size and machine — this is normal for even a small Quarkus service, native-image's whole-program static analysis dominates the cost, not code volume |
| Memory during build | Standard JVM heap, unremarkable | **Native-image compilation itself commonly needs 4-6GB+ RAM** at build time (separate from the app's own runtime footprint, which shrinks dramatically once built) |
| Parallelism across 5 modules | Cheap to run concurrently in CI | Running 5 native builds concurrently in one CI job/runner multiplies the memory requirement — likely needs either a beefier runner tier or serialized builds, both of which lengthen or cost more than the current CI setup |

**Rough total cost estimate for this repo:** 5 modules x ~2-4 min native build each = **roughly 10-20 extra CI-minutes per pipeline run** if built serially, or a runner-memory upgrade if built in parallel — on top of the existing `./gradlew test` (ArchUnit + JUnit, 512+ backend tests) and Sonar scan steps already in `documents/CICD.md`'s pipeline. This is a genuine, standing CI cost across every future PR that triggers a native rebuild, not a single migration expense — worth weighing against the actual runtime benefit (faster cold start, lower memory footprint at runtime) before committing to it as the default CI path, versus e.g. only native-building on release tags.

---

## 4. No Impact on Local Dev

Worth stating plainly since this could otherwise read as scarier than it is: **`quarkusDev` (hot-reload dev mode) is unaffected either way.** `./gradlew :application:domain:wealth:adapters:quarkusDev` and the rest of the `dp`/`dw`/`dh`/`dho`/`dg` dev aliases always run in JVM mode with Quarkus's live-reload class loader — native-image is purely a **packaging/production-build concern**, invoked separately (`./gradlew build -Dquarkus.native.enabled=true` or a dedicated `%native` profile), never part of the everyday dev loop. Adopting native-image later:
- Does not change `dev-aliases.ps1`/`.sh`
- Does not change how any developer runs or debugs a service locally
- Does not add a new required tool to a developer's machine (only CI/build-runner machines would need the GraalVM/Mandrel toolchain installed)

This is a "when we ship, not how we build" decision.

---

## 5. Recommended Trigger Point

**Recommendation: sequence native-image work after Docker containerization exists, not gated on any literal "3.0" label.**

Reasoning:
1. **There is no Dockerfile of any kind in this repo yet** — not `Dockerfile.jvm`, not `Dockerfile.native`, not even a docker-compose for running the built services (only the unrelated dev-container compose). Native-image packaging without a JVM-mode Dockerfile baseline first would mean doing the harder version of a problem before the easier version is even solved once.
2. `ROADMAP.md`'s v2.1 **"Cloud Ready"** milestone already lists "Docker containerization" as a planned feature — that is the natural home for `Dockerfile.jvm` + docker-compose across the 5 services first. Native-image is a natural **follow-on** to that milestone, not a parallel or earlier track.
3. Toolchain readiness is not the blocker (section 2b) — sequencing readiness is. There's no reason to rush native-image ahead of getting basic containerized deployment working at all.
4. Practical suggested order once v2.1 is reached: (1) `Dockerfile.jvm` for all 5 services + docker-compose, verified working end-to-end; (2) evaluate whether native's runtime benefit (startup time, memory) is actually needed given the deployment target at that time; (3) if yes, add `Dockerfile.native` incrementally, one service at a time (start with `profile` — smallest, and everything else depends on it being up first per the existing startup order), verify CI cost is acceptable, then roll out to the rest.
5. Re: the "3.0" framing in the original ask — recommend the product owner either (a) treat "3.0" as informal shorthand for "not urgent, do later," with no literal roadmap dependency, or (b) if a firm milestone anchor is wanted, anchor it to v2.1 Cloud Ready instead, since that's where Docker work is already scoped. Retitling/reordering `ROADMAP.md` itself is a product decision, not made here.

---

## 6. ADR Judgment Call

**No formal ADR filed.** Reasoning: this investigation concludes with **no architectural decision** — nothing is being adopted, deferred-with-a-committed-design, or rejected outright. It's "the toolchain would work fine, but there's no reason to build it yet, and here's the natural sequencing when the time comes." That's exactly the kind of finding suited to living in a standalone reference doc (this one) rather than the ADR log, which is reserved for decisions the team commits to and later work must reconcile against (per `ARCHITECTURE_DECISIONS.md`'s own framing: "record every significant architectural decision... these decisions are final unless superseded").

If/when native-image is actually greenlit for implementation (post v2.1, per section 5), **that** is the point to file a real ADR — covering the concrete choice of base image, Mandrel vs. GraalVM CE, which services get native builds (all 5, or just the higher-traffic ones), and the CI pipeline change. Revisit this doc's findings as the starting point for that ADR rather than re-researching from scratch.

---

## Summary

| Question | Answer |
|---|---|
| Are the extensions native-compatible? | Yes — all 8 (7 current + 1 incoming from Phase 1) are first-party, GA-native-supported Quarkus extensions. Clean dependency set, no red flags. |
| Is the JDK 25 / GraalVM toolchain ready? | Yes, surprisingly so — GraalVM 25 and Mandrel 25 (JDK-25-based) are both released and are the current recommended track for Quarkus 3.27.0+; Suchika is on 3.29.0. |
| What's missing to attempt it? | 5x `Dockerfile.native` (none exist), possibly some `@RegisterForReflection` hints for Jackson/JSONB edge cases (discoverable only by running a real native build), and — more fundamentally — no `Dockerfile.jvm` baseline exists yet either. |
| What does it cost in CI? | Roughly 10-20 extra CI-minutes per pipeline run across 5 modules (serial), or a runner-memory upgrade if parallelized — a standing cost, not one-time. |
| Does it affect local dev? | No — `quarkusDev` stays JVM-mode hot-reload regardless; native is a packaging/prod-only concern. |
| When should this happen? | After Docker containerization (`ROADMAP.md` v2.1 "Cloud Ready"), not gated on the literal "v3.0" label (which is actually "GitHub Ready" and unrelated). |
| Formal ADR now? | No — no decision was made, just a feasibility finding. File one when/if native-image is actually greenlit for implementation. |
