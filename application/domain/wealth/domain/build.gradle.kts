// Wealth Domain: Pure domain models, no framework dependencies

val junitVersion = "5.10.2"

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
}
