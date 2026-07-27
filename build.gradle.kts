plugins {
    java
    id("io.quarkus") version "3.29.0" apply false
}

allprojects {
    group = "com.suchika"
    version = "1.0.0"
    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "jacoco")
    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(25)
        }
        sourceCompatibility = JavaVersion.VERSION_25
        targetCompatibility = JavaVersion.VERSION_25
    }

    dependencies {
        val testImplementation by configurations
        val testRuntimeOnly by configurations
        testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
        testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    }

    tasks.withType<Test> {
        useJUnitPlatform()
        finalizedBy(tasks.named("jacocoTestReport"))
    }

    tasks.withType<org.gradle.testing.jacoco.tasks.JacocoReport> {
        // Merge standard unit-test exec with quarkus-jacoco exec (@QuarkusTest coverage)
        executionData.setFrom(
            fileTree(layout.buildDirectory) {
                include("**/jacoco/test.exec", "**/jacoco-quarkus.exec")
            }
        )
        reports {
            xml.required.set(true)
            html.required.set(false)
        }
    }
}