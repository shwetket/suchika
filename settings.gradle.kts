rootProject.name = "suchika"

// Backend domain modules - Hexagonal architecture with nested modules
include(
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