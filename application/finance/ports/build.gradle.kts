// Finance Ports: Depends only on domain
dependencies {
    implementation(project(":application:finance:domain"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.2")
}
