// shared-adapter: shared JAX-RS resource + Panache repository/service base
// classes for cross-domain vertical slices (currently: Phase 4's error_log
// Application Console, ADR-023 revision 2026-07-13).
//
// A leaf library module, not a Quarkus application (plain `plugins { java }`,
// applied repo-wide by the root build.gradle.kts's `subprojects` block --
// same shape as `shared/`). Consumed only by each domain's own `adapters`
// module -- never by `domain` or `ports` (see DomainRulesTest's new
// shared_adapter_must_not_depend_on_domain_modules rule for the enforced
// half of that; the "never consumed by domain/ports" half is enforced simply
// by no domain/ports build.gradle.kts declaring this project as a dependency).

plugins {
    java
}

tasks.withType<Jar> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

val quarkusBomVersion = "3.29.0"
val junitVersion = "5.10.2"

dependencies {
    implementation(enforcedPlatform("io.quarkus.platform:quarkus-bom:$quarkusBomVersion"))
    implementation(project(":shared"))

    implementation("io.quarkus:quarkus-arc")
    implementation("io.quarkus:quarkus-rest")
    implementation("io.quarkus:quarkus-rest-jackson")
    implementation("io.quarkus:quarkus-hibernate-orm-panache")

    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junitVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
}
