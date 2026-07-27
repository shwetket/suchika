plugins {
    java
}

tasks.withType<Jar> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

val quarkusBomVersion = "3.29.0"
val archunitVersion = "1.4.1"

dependencies {
    implementation(enforcedPlatform("io.quarkus.platform:quarkus-bom:$quarkusBomVersion"))
    implementation("io.quarkus:quarkus-arc")
    implementation("io.quarkus:quarkus-logging-json")
    implementation("io.quarkus:quarkus-rest")
    implementation("io.quarkus:quarkus-rest-jackson")

    testImplementation("com.tngtech.archunit:archunit-junit5:$archunitVersion")
    testImplementation(project(":application:domain:wealth:domain"))
    testImplementation(project(":application:domain:health:domain"))
    testImplementation(project(":application:domain:household:domain"))
    // ADR-023 revision (2026-07-13): so DomainRulesTest's ArchUnit scan can
    // see shared-adapter's own compiled classes and enforce the new
    // shared_adapter_must_not_depend_on_domain_modules rule. Not a real
    // dependency cycle -- shared-adapter's *main* sourceSet depends on
    // shared:jar (already built by then); shared's *test* sourceSet depends
    // on shared-adapter:jar. Same shape as the three domain testImplementation
    // lines above.
    testImplementation(project(":shared-adapter"))
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}