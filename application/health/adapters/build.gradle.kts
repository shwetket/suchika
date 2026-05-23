plugins {
    id("io.quarkus")
}

dependencies {
    implementation(enforcedPlatform("io.quarkus.platform:quarkus-bom:3.29.0"))
    
    // Hexagonal architecture layers
    implementation(project(":application:health:domain"))
    implementation(project(":application:health:ports"))
    
    // Quarkus framework
    implementation("io.quarkus:quarkus-arc")
    implementation("io.quarkus:quarkus-rest")
    implementation("io.quarkus:quarkus-rest-jackson")
    implementation("io.quarkus:quarkus-mongodb-panache")
    implementation("io.quarkus:quarkus-smallrye-openapi")
    
    // Shared utilities
    implementation(project(":shared"))
    
    // Tests
    testImplementation("io.quarkus:quarkus-junit5")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.2")
}
