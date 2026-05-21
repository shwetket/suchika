plugins {
    java
}

dependencies {
    implementation(enforcedPlatform("io.quarkus.platform:quarkus-bom:3.29.0"))
    implementation("io.quarkus:quarkus-arc")
}