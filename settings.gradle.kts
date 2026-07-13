pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    // Auto-provisions a matching JDK (e.g. 25) if one isn't already installed locally.
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "suchika"

// Backend domain modules - Hexagonal architecture with nested modules
include(
    "application:domain:profile:domain",
    "application:domain:profile:ports",
    "application:domain:profile:adapters",
    "application:domain:wealth:domain",
    "application:domain:wealth:ports",
    "application:domain:wealth:adapters",
    "application:domain:health:domain",
    "application:domain:health:ports",
    "application:domain:health:adapters",
    "application:domain:household:domain",
    "application:domain:household:ports",
    "application:domain:household:adapters",
    "application:web-gateway"
)

// Shared + infrastructure
include(
    "infrastructure",
    "shared"
)