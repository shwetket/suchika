rootProject.name = "suchika"

// Backend domain modules - Hexagonal architecture with nested modules
include(
    "application:wealth:domain",
    "application:wealth:ports",
    "application:wealth:adapters",
    "application:health:domain",
    "application:health:ports",
    "application:health:adapters",
    "application:household:domain",
    "application:household:ports",
    "application:household:adapters",
    "application:web-gateway"
)

// Shared + infrastructure
include(
    "infrastructure",
    "shared"
)