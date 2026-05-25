rootProject.name = "suchika"

// Backend domain modules - Hexagonal architecture with nested modules
include(
    "application:finance:domain",
    "application:finance:ports",
    "application:finance:adapters",
    "application:health:domain",
    "application:health:ports",
    "application:health:adapters",
    "application:records",
    "application:web-gateway"
)

// Shared + infrastructure
include(
    "infrastructure",
    "shared"
)