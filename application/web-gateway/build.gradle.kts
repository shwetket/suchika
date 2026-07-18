plugins {
    id("io.quarkus")
}

val quarkusBomVersion = "3.29.0"
val junitVersion = "5.10.2"
val postgresqlVersion = "42.7.3"

dependencies {
    implementation(enforcedPlatform("io.quarkus.platform:quarkus-bom:$quarkusBomVersion"))

    // REST endpoints (server-side)
    implementation("io.quarkus:quarkus-rest")
    implementation("io.quarkus:quarkus-rest-jackson")
    implementation("io.quarkus:quarkus-vertx-http")
    implementation("io.quarkus:quarkus-smallrye-openapi")
    implementation("io.quarkus:quarkus-smallrye-health")

    // REST client (calls downstream domain services)
    implementation("io.quarkus:quarkus-rest-client-jackson")

    // JPA + PostgreSQL — used exclusively by ProjectionCalculationEngine
    // to write/read the projections.dashboard_snapshot CQRS read model.
    implementation("io.quarkus:quarkus-hibernate-orm")
    implementation("io.quarkus:quarkus-jdbc-postgresql")
    implementation("io.quarkus:quarkus-flyway")

    // Shared utilities (AppLogger, exceptions)
    implementation(project(":shared"))

    // Domain ports for aggregation (BFF pattern)
    implementation(project(":application:domain:wealth:ports"))
    implementation(project(":application:domain:health:ports"))
    implementation(project(":application:domain:household:ports"))

    // Tests
    testImplementation("io.quarkus:quarkus-junit5")
    testImplementation("io.quarkus:quarkus-junit5-mockito")
    testImplementation("io.quarkus:quarkus-jacoco")
    testImplementation("io.rest-assured:rest-assured")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.postgresql:postgresql:$postgresqlVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
}

tasks.named("jacocoTestReport") {
    mustRunAfter("quarkusBuild")
}
