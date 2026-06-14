val junitVersion = "5.10.2"

// Household Ports: Depends only on domain
dependencies {
    implementation(project(":application:domain:household:domain"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
}
