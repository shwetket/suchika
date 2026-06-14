val junitVersion = "5.10.2"

// Wealth Ports: Depends only on domain
dependencies {
    implementation(project(":application:domain:wealth:domain"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
}
