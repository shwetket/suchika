rootProject.name = "suchika"

// Backend domain modules
include(
    "application:finance",
    "application:health"
)

// Shared + infrastructure
include(
    "infrastructure",
    "shared"
)