val junitVersion = "5.10.2"

// profile Ports: Depends only on domain
dependencies {
    implementation(project(":application:domain:profile:domain"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
}
