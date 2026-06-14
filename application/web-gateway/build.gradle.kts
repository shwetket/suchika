plugins {
    id("io.quarkus")
}

val quarkusBomVersion = "3.29.0"
val junitVersion = "5.10.2"

dependencies {
    implementation(enforcedPlatform("io.quarkus.platform:quarkus-bom:$quarkusBomVersion"))

    // REST endpoints (server-side)
    implementation("io.quarkus:quarkus-rest")
    implementation("io.quarkus:quarkus-rest-jackson")

    // REST client (calls downstream domain services)
    implementation("io.quarkus:quarkus-rest-client-jackson")

    // Shared utilities (AppLogger, exceptions)
    implementation(project(":shared"))

    // Domain ports for aggregation (BFF pattern)
    implementation(project(":application:domain:wealth:ports"))
    implementation(project(":application:domain:health:ports"))

    // Tests
    testImplementation("io.quarkus:quarkus-junit5")
    testImplementation("io.rest-assured:rest-assured")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
}

// ARCHITECTURAL GUARDRAIL VERIFICATION:
// ✅ NO PostgreSQL driver
// ✅ NO MongoDB driver
// ✅ NO Hibernate ORM
// ✅ NO Panache dependencies
// ✅ NO Flyway database migration
// ✅ PURE BFF: REST aggregation only
