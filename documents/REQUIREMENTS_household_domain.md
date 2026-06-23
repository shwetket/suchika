# Household Operations Domain

| | |
|---|---|
| **Type** | Requirements |
| **Audience** | Developers, product |
| **Status** | Active |
| **Last updated** | 2026-06-23 |

## Objective

Define all functional requirements, epics, and acceptance criteria for the Household domain across every version milestone. This domain is not yet implemented (v0.3 planned). This document is the source of truth for what must be built — calendar events, inventory, goals, and home automation.

## Use Cases

- Before starting v0.3 Household implementation — read this in full first
- When scoping new household features — check milestone assignment and constraints
- When reviewing a PR for the household domain — check that behaviour matches the acceptance criteria

---

**Focus:** Scheduling, human logistics, task execution, supply chain (inventory/groceries), and home infrastructure automation.

**v0.2 status:** Household domain is deferred to v0.3. No Household features are part of the v0.2 UAT scope. Zero Java implementation exists as of June 2026.

## v0.1: Prototype (Minimal Features, Happy Path) [DONE — via Profile domain]

**Objective:** Validate core family registry as the identity anchor for all other domains.
**Data Constraint:** Ephemeral. The database is expected to be wiped frequently.

**Note:** The Household profile registry (family member creation and listing) was delivered in v0.2 as the **Profile domain** — a dedicated domain serving as the identity anchor for Wealth, Health, and Household. Profile management (create admin, create/list/view/edit/deactivate members, relation types) is documented in the Profile domain, not here. What follows covers scheduling and supply chain features only.

### Epic 1: Family Logistics & Calendar

#### Use Case 1.1: Event Scheduling

* **Chronological Mapping:** The system must allow the creation of calendar events with defined start dates, end dates, and assigned `profile_id` references.

### Epic 2: Supply Chain Ingestion (Groceries)

#### Use Case 2.1: Order History Parsing

* **Extraction:** The system must accurately extract item names, quantities, and purchase dates from standard external grocery order exports (e.g., Flipkart, Instamart, Country Delight).
* **Consolidation:** The system must aggregate the extracted items into a unified, raw inventory ledger, overriding platform-specific formatting.

## v0.3: Enhanced Local App [PLANNED]

**Objective:** Deliver the Household domain as a fully usable module, completing the three-domain local application.

### Epic 3: Itinerary & Task Management

#### Use Case 3.1: Multi-Day Event Planning

* **Event Grouping:** The system must support grouping multiple chronological sub-events (e.g., travel segments, daily activities) under a single master event (e.g., family holidays, guest visits).
* **Conflict Detection:** The system must flag overlapping master events assigned to the same household `profile_id`.

#### Use Case 3.2: Milestone & Task Tracking

* **Assignments:** The system must allow specific, actionable tasks (e.g., school project preparations, speeches) to be assigned to specific child profiles with hard deadlines linked to the calendar.

### Epic 4: Inventory Management

#### Use Case 4.1: Grocery Inventory Listing

* **Listing:** The system must list all inventory items for a given `profile_id`.
* **Item Lifecycle:** The system must support marking items as consumed or restocking them.

### Epic 5: Home Automation Mapping

#### Use Case 5.1: Device State Configuration

* **Registry:** The system must allow the registration of smart home devices and their supported operational states.
* **Schedule Synchronization:** The system must allow the mapping of specific household daily routines (derived from the calendar) to specific smart device configurations, creating a unified rule set for environment automation.

## v0.4: Error Handling (Unhappy Path)

**Objective:** Build system resilience to handle malformed external data and edge cases.

### Epic 6: Unstructured Data Resilience

#### Use Case 6.1: Malformed Supply Chain Data

* **Quarantine Protocol:** If a grocery export contains a malformed row or unrecognizable item categorization, the system must quarantine the specific row rather than rejecting the entire file.
* **Error Logging:** The system must explicitly log the quarantined item for manual user review and correction without disrupting the rest of the consolidated list.

## v1.0: Security & Persistence

**Objective:** Lock down the application architecture for persistent, real-world data storage.

### Epic 7: Multi-Tenancy & Access

#### Use Case 7.1: Transition to Persistent Data

* **Migration Strategy:** The database is no longer treated as ephemeral. All database modifications must strictly utilize versioned schema migration tools.
* **Role-Based Access:** The system must enforce strict boundaries between administrative profiles (Adults) and restricted profiles (Children).
