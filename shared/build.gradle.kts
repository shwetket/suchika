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
    testImplementation("io.quarkus:quarkus-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}