rootProject.name = "suchika"

// Backend domain modules
include(
    "application:records"
)

// Shared + infrastructure
include(
    "infrastructure",
    "shared"
)