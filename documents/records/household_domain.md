# Household Operations Domain

**Focus:** Scheduling, human logistics, task execution, supply chain (inventory/groceries), and home infrastructure automation.

## v0.1: Prototype (Minimal Features, Happy Path)
**Objective:** Validate core family registry, basic scheduling, and simple external data ingestion.
**Data Constraint:** Ephemeral. The database is expected to be wiped frequently.

### Epic 1: Family Logistics & Calendar
#### Use Case 1.1: Household Profile Registry
* **Profile Creation:** The system must support the creation of distinct, hierarchical profiles (e.g., Primary, Partner, Child) to serve as the foundation for task assignment and scheduling.
#### Use Case 1.2: Event Scheduling
* **Chronological Mapping:** The system must allow the creation of calendar events with defined start dates, end dates, and assigned profiles.

### Epic 2: Supply Chain Ingestion (Groceries)
#### Use Case 2.1: Order History Parsing
* **Extraction:** The system must accurately extract item names, quantities, and purchase dates from standard external grocery order exports (e.g., Flipkart, Instamart, Country Delight).
* **Consolidation:** The system must aggregate the extracted items into a unified, raw inventory ledger, overriding platform-specific formatting.

## v0.2: Usable Local App (Usable Features)
**Objective:** Introduce logical links between schedules, actionable lists, and event coordination.

### Epic 3: Itinerary & Task Management
#### Use Case 3.1: Multi-Day Event Planning
* **Event Grouping:** The system must support grouping multiple chronological sub-events (e.g., travel segments, daily activities) under a single master event (e.g., family holidays, guest visits).
* **Conflict Detection:** The system must flag overlapping master events assigned to the same household profile.
#### Use Case 3.2: Milestone & Task Tracking
* **Assignments:** The system must allow specific, actionable tasks (e.g., school project preparations, speeches) to be assigned to specific child profiles with hard deadlines linked to the calendar.

## v0.3: Enhanced Local App (More Features)
**Objective:** Introduce infrastructure mapping and smart home automation logic.

### Epic 4: Home Automation Mapping
#### Use Case 4.1: Device State Configuration
* **Registry:** The system must allow the registration of smart home devices and their supported operational states.
* **Schedule Synchronization:** The system must allow the mapping of specific household daily routines (derived from the Calendar domain) to specific smart device configurations, creating a unified rule set for environment automation.

## v0.4: Error Handling (Unhappy Path)
**Objective:** Build system resilience to handle malformed external data and edge cases.

### Epic 5: Unstructured Data Resilience
#### Use Case 5.1: Malformed Supply Chain Data
* **Quarantine Protocol:** If a grocery export contains a malformed row or unrecognizable item categorization, the system must quarantine the specific row rather than rejecting the entire file.
* **Error Logging:** The system must explicitly log the quarantined item for manual user review and correction without disrupting the rest of the consolidated list.

## v1.0: Security & Persistence
**Objective:** Lock down the application architecture for persistent, real-world data storage.

### Epic 6: Multi-Tenancy & Access
#### Use Case 6.1: Transition to Persistent Data
* **Migration Strategy:** The database is no longer treated as ephemeral. All database modifications must strictly utilize versioned schema migration tools.
* **Role-Based Access:** The system must enforce strict boundaries between administrative profiles (Adults) and restricted profiles (Children).