plugins {
    id("io.quarkus")
}

dependencies {
    implementation(enforcedPlatform("io.quarkus.platform:quarkus-bom:3.29.0"))
    
    // REST endpoints
    implementation("io.quarkus:quarkus-rest")
    implementation("io.quarkus:quarkus-rest-jackson")
    
    // Domain ports for aggregation (BFF pattern)
    implementation(project(":application:wealth:ports"))
    implementation(project(":application:health:ports"))
    
    // Tests
    testImplementation("io.quarkus:quarkus-junit5")
    testImplementation("io.rest-assured:rest-assured")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.2")
}

// ARCHITECTURAL GUARDRAIL VERIFICATION:
// ✅ NO PostgreSQL driver
// ✅ NO MongoDB driver
// ✅ NO Hibernate ORM
// ✅ NO Panache dependencies
// ✅ NO Flyway database migration
// ✅ PURE BFF: REST aggregation only
