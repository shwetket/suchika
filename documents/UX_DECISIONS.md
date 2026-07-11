# UX Decisions

| | |
|---|---|
| **Type** | Reference — UX Decision Log |
| **Audience** | All developers, product |
| **Status** | Active |
| **Last updated** | 2026-07-11 (UX-006, UX-007, UX-010 through UX-013, UX-015 through UX-018 implemented by react-developer — see each entry's Status line and `documents/domain-state/wealth.md`; UX-008/UX-009/UX-014/UX-019 remain out of scope for this round) |

## Objective

Record every significant UX/interaction review finding for this project, along with its rationale and current status — the interaction-design counterpart to `ARCHITECTURE_DECISIONS.md`. Entries here are produced by the `ux-developer` persona/agent (judges how a page *feels* to use; does not write code) and handed off to `react-developer` for implementation after user review.

## Use Cases

- Before implementing a frontend interaction/layout change flagged in a UX review
- When questioning why a page's control was resized, moved, or hidden ("why is Edit an icon now?")
- During onboarding to a page that has already had a UX pass, to understand what was deliberately changed and why

## UX Decision Index

| UX | Title | Status |
|---|---|---|
| [UX-001](#ux-001-deactivate-moved-off-the-accounts-list-card-into-the-edit-modal) | Deactivate Moved Off the Accounts List Card, Into the Edit Modal | Implemented |
| [UX-002](#ux-002-edit-becomes-an-icon-only-affordance-on-the-accounts-list-card) | Edit Becomes an Icon-Only Affordance on the Accounts List Card | Implemented |
| [UX-003](#ux-003-single-confirmed-path-for-deactivate-reactivate-stays-unconfirmed-and-separate) | Single Confirmed Path for Deactivate; Reactivate Stays Unconfirmed and Separate | Implemented |
| [UX-004](#ux-004-distinguish-failed-balance-fetch-from-a-real-balance) | Distinguish a Failed Balance Fetch From a Real Balance | Implemented |
| [UX-005](#ux-005-search-input--balance-visual-hierarchy-for-the-32-account-grid) | Search Input + Balance Visual Hierarchy for the 32-Account Grid | Implemented |
| [UX-006](#ux-006-deactivate-moved-off-the-physical-assets-list-card-into-the-edit-modal) | Deactivate Moved Off the Physical Assets List Card, Into the Edit Modal | Implemented |
| [UX-007](#ux-007-single-confirmed-path-for-deactivatereactivate-on-physical-assets) | Single Confirmed Path for Deactivate/Reactivate on Physical Assets | Implemented |
| [UX-008](#ux-008-edit-stays-a-labeled-action-on-physical-assets-moved-to-the-card-header) | Edit Stays a Labeled Action on Physical Assets, Moved to the Card Header | Recommended |
| [UX-009](#ux-009-promote-current-value-reorder-compliance-status-above-static-registration-metadata) | Promote Current Value, Reorder Compliance Status Above Static Registration Metadata | Recommended |
| [UX-010](#ux-010-reports-should-default-to-the-logged-in-users-own-profile) | Reports Should Default to the Logged-In User's Own Profile | Implemented |
| [UX-011](#ux-011-promote-net-balance-to-hero-position-de-emphasize-account-count-metadata) | Promote Net Balance to Hero Position, De-Emphasize Account-Count Metadata | Implemented |
| [UX-012](#ux-012-add-a-last-calculated-timestamp-next-to-the-net-balance-figure) | Add a Last-Calculated Timestamp Next to the Net Balance Figure | Implemented |
| [UX-013](#ux-013-distinguish-a-failed-net-worth-fetch-from-not-yet-calculated) | Distinguish a Failed Net-Worth Fetch From "Not Yet Calculated" | Implemented |
| [UX-014](#ux-014-flagged-reports-vs-dashboard-redundancy-and-an-inaccurate-coming-soon-placeholder) | Flagged: Reports vs. Dashboard Redundancy, Inaccurate "Coming Soon" Placeholder | Flagged for business-analyst/architect |
| [UX-015](#ux-015-transactions-next-page-appears-stuck-missing-stale-response-guard-in-load) | Transactions "Next Page" Appears Stuck — Missing Stale-Response Guard in `load()` | Implemented |
| [UX-016](#ux-016-color-the-amount-column-by-txn-type-not-just-the-badge) | Color the Amount Column by Txn Type, Not Just the Badge | Implemented |
| [UX-017](#ux-017-truncated-transaction-description-needs-a-title-tooltip) | Truncated Transaction Description Needs a `title` Tooltip | Implemented |
| [UX-018](#ux-018-de-emphasize-add-transaction-relative-to-its-real-usage-frequency) | De-Emphasize "+ Add Transaction" Relative to Its Real Usage Frequency | Implemented |
| [UX-019](#ux-019-flagged-no-free-text-description-search-on-transactions) | Flagged: No Free-Text Description Search on Transactions | Flagged for architect/business-analyst |

---

## UX-001: Deactivate Moved Off the Accounts List Card, Into the Edit Modal

**Status:** Implemented (2026-07-11) — `web/src/pages/Wealth/Accounts.js`. Deactivate button removed from `AccountCard`'s footer (footer row removed entirely, see UX-002); a red text-link "Deactivate this account" now lives inside the Edit modal below the form fields (below the Loan Details section when present), separated by the same `border-t border-gray-200 pt-4` divider treatment. Clicking it closes the Edit modal and opens the existing confirm dialog (`confirmTarget`/`handleDeactivate`, `deactivateAccount` unchanged) via new `handleDeactivateFromEdit`.

**Context:** `web/src/pages/Wealth/Accounts.js`, `AccountCard` component (footer block, originally lines 128–145). The real household user (32 active real accounts rendered in a dense 1/2/3-column grid) reported: *"what if I accidentally click on that as its big button. I believe the deactivate button should be inside edit page [instead]."* The Deactivate button sat as a `flex-1` bordered button, visually equal in size/weight to the adjacent Edit button, directly on every card in the main list.

**Decision:** Remove the Deactivate button from the card footer entirely. Add a de-emphasized "Deactivate this account" text-link inside the Edit modal, below the form fields and above the modal's own Cancel/Save row, separated by a divider (matching the existing Loan Details divider treatment). Clicking it still opens the existing confirm dialog and calls the existing `deactivateAccount` endpoint — unchanged safety net, relocated entry point only.

**Why:** Deactivating a real financial account is a rare, deliberate action; it should require navigating into Edit first (a deliberate gesture) rather than being one misclick away on a dense grid of 32 cards. This directly addresses the reported misclick risk without removing any existing safety behavior (the confirm dialog is untouched).

**Alternatives considered:**
- *Keep the button but shrink/recolor it* — rejected; still a peer action next to Edit on the same dense card, doesn't remove the core risk (adjacent, equally-clickable targets).
- *Require a "hold to deactivate" gesture on the card* — rejected as a new interaction pattern not used anywhere else in the app; inconsistent with existing confirm-dialog convention.

---

## UX-002: Edit Becomes an Icon-Only Affordance on the Accounts List Card

**Status:** Implemented (2026-07-11) — `web/src/pages/Wealth/Accounts.js`. Labeled Edit button replaced with an icon-only button (inline pencil SVG, `currentColor`/`stroke`, matching the existing inline-SVG convention in `Navigation.js`/`ActionCenter.js`/`Dashboard.js`), placed in the card header row next to `StatusBadge`, with `aria-label="Edit account"` + native `title`. The card's bottom action-row `div` is gone (UX-001 also removed Deactivate from it).

**Context:** Same `AccountCard` component. The user's reasoning: *"as user I will not be changing the detail, after uploading CSV files these details will be automatically updated"* — manual field edits are rare for this household since balances/transactions mostly arrive via CSV upload. The Edit button was a full-width, labeled, bordered button — same prominence as a primary action — occupying permanent space on all 32 cards.

**Decision:** Replace the labeled Edit button with a small icon-only button (inline SVG pencil glyph, `currentColor`, no new dependency — matches the existing inline-SVG icon convention already used in `Navigation.js`/`Dashboard.js`/`ActionCenter.js`), placed in the card's header row next to the `StatusBadge`, with `aria-label="Edit account"` and a native `title` tooltip for accessibility since there is no visible text label. With Deactivate also removed (UX-001), the card's bottom action row disappears entirely.

**Why:** Matches actual usage frequency — a rare action should not have permanent full-width visual weight across a 32-card grid. Removing the footer action row also shortens every card, reducing overall visual density and indirectly reducing misclick surface (fewer, smaller clickable targets per card).

**Alternatives considered:**
- *Keep Edit as a labeled button but shrink it* — rejected; still competes for attention with balance/status information the user actually scans for.
- *Move Edit into a card overflow ("⋮") menu alongside Deactivate* — rejected as unnecessary indirection for an action that, unlike Deactivate, is not destructive and doesn't need the same degree of friction; a direct icon click is simpler.

---

## UX-003: Single Confirmed Path for Deactivate; Reactivate Stays Unconfirmed and Separate

**Status:** Implemented (2026-07-11) — `web/src/pages/Wealth/Accounts.js`. Raw "Active" checkbox removed from the general Save-Changes form (and from `EMPTY_EDIT`/`handleEditSubmit`'s payload). Replaced with two mutually-exclusive controls gated on `editingAccount.is_active`: active → the red "Deactivate this account" link from UX-001 (confirmed, via existing dialog); inactive → a plain "Reactivate account" button (no confirmation) that calls `updateAccount(accountId, profileId, { is_active: true })` directly via new `handleReactivate` — confirmed against `web/src/api/wealth.js` this is the only reactivation mechanism.

**Context:** Found during this review, not explicitly reported by the user. The Edit modal (`Accounts.js`, `is_active` checkbox block, originally lines 709–721) already lets a user flip `is_active` to `false` and click "Save Changes" (`updateAccount(...)`), silently deactivating a real account with **zero confirmation** — a second, less-guarded path to the exact same destructive state change that the dedicated Deactivate confirm-dialog flow exists to protect against. Checked `web/src/api/wealth.js`: there is no separate "reactivate" endpoint; `updateAccount({ is_active: true })` is the only mechanism for turning an account back on.

**Decision:** Remove the raw "Active" checkbox from the general Save-Changes form. Once UX-001 lands, gate two small, mutually-exclusive controls on `account.is_active`: if active, show the red "Deactivate this account" link (confirmed, calls `deactivateAccount`); if inactive, show a plain "Reactivate account" action (no confirmation needed — reactivating isn't destructive — calls `updateAccount({ is_active: true })`, the only mechanism available today).

**Why:** There should be exactly one way to deactivate a real financial account (confirmed) and exactly one way to reactivate one (unconfirmed, since it isn't destructive). The checkbox-plus-Save path was strictly riskier than the button it's being used to justify removing, since it required no confirmation at all.

**Alternatives considered:**
- *Leave the checkbox as-is alongside the new Deactivate link* — rejected; reintroduces exactly the redundant, unconfirmed path this decision closes.
- *Add a confirmation prompt to Save Changes whenever `is_active` is being toggled off* — rejected as more complex than necessary; removing the checkbox from the general form is simpler and matches the user's mental model ("deactivate is its own action").

---

## UX-004: Distinguish a Failed Balance Fetch From a Real Balance

**Status:** Implemented (2026-07-11) — `web/src/pages/Wealth/Accounts.js`, new `BalanceLine` component. `undefined` → "Balance: Loading..." (unchanged); `null` (fetch failed) → distinct "Balance unavailable" in `text-amber-600`, no longer silently substituting `account.opening_balance`; a real number → "Balance: {formatCurrency(balance)}" in a promoted `text-lg font-semibold` line (also folds in UX-005b).

**Context:** Found during this review. `AccountCard`'s balance line (`Accounts.js`, originally lines 108–120) renders `formatCurrency(balance ?? account.opening_balance)`. The `balances` state (populated by `Promise.all` over `getAccountBalance` calls, lines ~326–345) is `undefined` while loading, a number on success, or `null` on a caught fetch failure (`.catch(() => [a.account_id, null])`). The `null` case silently falls back to `account.opening_balance`, rendered under the identical "Balance:" label as a real, current balance — with no indication the number is stale or that the fetch failed.

**Decision:** Split the `null` case out from the numeric case. Loading → "Loading..." (unchanged). Fetch failed (`null`) → an explicit "Balance unavailable" state in gray/amber text, distinct from a real balance, instead of silently substituting `opening_balance`. A real balance renders as today.

**Why:** This is a personal finance app for a household actively relying on it as source of truth. A network blip that fails one account's balance fetch should never look identical to a healthy, current balance — that's a data-trust problem, not just a cosmetic one.

**Alternatives considered:**
- *Keep showing `opening_balance` but add a small caveat label next to it* — viable alternative, slightly less clear than an explicit "unavailable" state but preserves some information; left as a fallback option for react-developer/user to pick between if "Balance unavailable" is judged too stark.

---

## UX-005: Search Input + Balance Visual Hierarchy for the 32-Account Grid

**Status:** Implemented (2026-07-11) — `web/src/pages/Wealth/Accounts.js`. (a) Text-search input (`inputClass` styling, `aria-label="Search accounts"`) added above the type-tabs row; client-side-filters the loaded `accounts` array into `filteredAccounts` by case-insensitive `account_name`/`institution_name` substring match, no backend change; a distinct "No accounts match your search" empty state added alongside the existing "No accounts found" one. (b) Balance line promoted to `text-lg font-semibold`, its own line via `BalanceLine`, visually separated from the lighter-weight `As of`/Credit Limit/Interest Rate group below it (folded into the same change as UX-004).

**Context:** Found during this review while assessing the dense 32-account grid the user described. Filtering is currently limited to account-type tabs and an active/inactive toggle (`Accounts.js`, lines ~523–563) — there is no free-text search, so locating one specific account means visually scanning up to 32 cards across a 3-column grid. Separately, the single most decision-relevant number per card — current balance — renders at the same `text-sm text-gray-600` weight as Credit Limit and Interest Rate (lines 108–127), with no visual emphasis.

**Decision:** (a) Add a plain text-search input (existing `inputClass` styling, no new component) above/beside the filter-tabs row that client-side-filters the already-loaded `accounts` array by `account_name`/`institution_name` — no backend change needed, data is already fully fetched. (b) Promote the balance line's typography (e.g., `text-lg font-semibold text-gray-900`) and give it its own visual line, separated from the lighter-weight credit-limit/interest-rate/as-of details.

**Why:** With 32 real accounts and growing, text search is the fastest way to locate one account, and it costs nothing extra from the backend since the list is already loaded client-side. Emphasizing balance typography makes the card's headline number scannable at a glance, which matters more as the grid grows.

**Alternatives considered:**
- *Server-side search/pagination* — rejected for now; 32 accounts is well within a client-side filter's comfortable range, and accounts are already fetched in full for the balance-loading effect, so a server round-trip would add complexity with no real benefit yet. Revisit only if the account count grows an order of magnitude.
- *Do nothing since it wasn't explicitly reported* — considered, but the task explicitly invited flagging other real problems found on this page; recommending it as optional/separate from the mandatory fixes keeps it clearly the user's call.

---

## UX-006: Deactivate Moved Off the Physical Assets List Card, Into the Edit Modal

**Status:** Implemented (2026-07-11) — `web/src/pages/Wealth/PhysicalAssets.js`. Deactivate button removed from `AssetCard`'s footer entirely (footer now holds only Edit); a red "Deactivate this asset" text-link now lives inside the Edit modal, below the form fields, using the same `border-t border-gray-200 pt-4` divider Accounts.js uses, routing into the existing `confirmTarget`/`handleDeactivate`/`deactivatePhysicalAsset` flow unchanged via new `handleDeactivateFromEdit` — mirrors Accounts.js's UX-001 implementation exactly.

**Context:** `PhysicalAssets.js`, `AssetCard` component, footer block (lines 100–117). Identical shape to the pre-fix Accounts.js bug (UX-001): Edit and Deactivate render as a pair of equal-width (`flex-1`), equal-weight bordered buttons directly on every card in the main grid — Edit in blue, Deactivate in red, side by side. Deactivating a physical asset (selling the car, disposing of a gold holding, etc.) is a real, consequential, not-trivially-undone action (it is reversible only by going back into Edit and toggling `is_active`, same as Accounts), sitting one misclick away from a card the user is otherwise just scanning for compliance-deadline status.

**Decision:** Remove the Deactivate button from `AssetCard`'s footer entirely. Add a de-emphasized "Deactivate this asset" text-link inside the Edit modal, below the form fields and above the modal's Cancel/Save row, using the same `border-t border-gray-200 pt-4` divider treatment Accounts.js uses. Clicking it closes the Edit modal and opens the existing confirm dialog (`confirmTarget` / `handleDeactivate`, `deactivatePhysicalAsset` call unchanged) via a new `handleDeactivateFromEdit`, mirroring Accounts.js's implementation exactly.

**Why:** Same reasoning as UX-001, and it applies with equal force here regardless of edit frequency (see UX-008) — prominence of a destructive action should track how consequential and hard-to-reverse it is, not how often the *other* button next to it gets used. A household with a handful of real, valuable physical assets (real estate, gold, a motorcycle) should not have "deactivate this asset" one misclick away from "edit its expiry date."

**Alternatives considered:**
- *Keep it but shrink/recolor it* — rejected for the same reason as UX-001: still an adjacent, equally-clickable target on the same card.
- *Leave it as-is because the physical-assets grid is much smaller than the 32-account grid* — rejected; misclick risk on a destructive action isn't a function of grid density, it's a function of the two targets being adjacent and similarly weighted. A 5-card grid with one misclick is exactly as bad as a 32-card grid with one misclick.

---

## UX-007: Single Confirmed Path for Deactivate/Reactivate on Physical Assets

**Status:** Implemented (2026-07-11) — `web/src/pages/Wealth/PhysicalAssets.js`. Raw "Active" checkbox removed from the general Save-Changes form (dropped from `EMPTY_EDIT`, `handleEditOpen`, and the `updatePhysicalAsset` payload built in `handleEditSubmit` — key omitted, not sent as `null`, so PATCH leaves it untouched). Replaced with two mutually-exclusive controls gated on `editingAsset.is_active`: active → the red "Deactivate this asset" link from UX-006 (confirmed, via existing dialog); inactive → a plain "Reactivate asset" button (no confirmation) calling `updatePhysicalAsset(assetId, profileId, { is_active: true })` directly via new `handleReactivate` — confirmed against `web/src/api/wealth.js` this is the only reactivation mechanism (no separate endpoint), mirroring Accounts.js's UX-003 implementation exactly.

**Context:** Found during this review — the exact same pattern as the Accounts.js bug closed by UX-003. The Edit modal's general form (`PhysicalAssets.js`, lines 578–590) has a raw "Active" checkbox wired straight into `EMPTY_EDIT`/`editForm.is_active`, and `handleEditSubmit` (lines 269–295) sends `is_active: editForm.is_active` on every "Save Changes" click. A user can uncheck "Active" and hit Save with **zero confirmation** — a second, unguarded path to the same destructive state change UX-006's confirm dialog exists to protect against. Checked `web/src/api/wealth.js`: there is no separate reactivate endpoint; `updatePhysicalAsset(id, profileId, { is_active: true })` is the only reactivation mechanism, same shape as Accounts.

**Decision:** Remove the raw "Active" checkbox from the general Save-Changes form (drop `is_active` from `EMPTY_EDIT` and from the `updatePhysicalAsset` payload built in `handleEditSubmit` — omit the key rather than sending `null`, so PATCH leaves it untouched, matching Accounts.js's approach). Once UX-006 lands, gate two mutually exclusive controls on `editingAsset.is_active`: active → the red "Deactivate this asset" link (confirmed, routes into `confirmTarget`/`handleDeactivate`); inactive → a plain "Reactivate asset" button (no confirmation — reactivating isn't destructive) calling `updatePhysicalAsset(assetId, profileId, { is_active: true })` directly via a new `handleReactivate`.

**Why:** There should be exactly one way to deactivate a physical asset (confirmed) and exactly one way to reactivate one (unconfirmed, since it isn't destructive) — identical rationale to UX-003. The checkbox-plus-Save path is strictly riskier than the flow it would sit alongside, since it requires no confirmation at all for the same state change.

**Alternatives considered:**
- *Leave the checkbox alongside the new Deactivate link* — rejected; reintroduces the redundant unconfirmed path this decision closes.
- *Add a confirm-on-submit only when `is_active` is being toggled off* — rejected as more complex than removing the checkbox outright, and inconsistent with the pattern just established on Accounts (same app, same domain, should feel the same).

---

## UX-008: Edit Stays a Labeled Action on Physical Assets, Moved to the Card Header

**Status:** Recommended (2026-07-11) — not yet implemented. Target file: `web/src/pages/Wealth/PhysicalAssets.js`.

**Context:** Accounts.js (UX-002) demoted Edit to an icon-only pencil button because the household's real workflow for accounts is CSV-upload-driven — the user's own words were "I will not be changing the detail, after uploading CSV files these details will be automatically updated." Physical assets have no equivalent ingestion path: there is no CSV/bulk-upload mechanism for a house, a gold holding, or a motorcycle. Per the task brief, this household's real physical assets include real estate, gold, and a motorcycle — asset types whose records are **inherently hand-maintained**: PUC/insurance/road-tax expiry dates on the motorcycle get renewed and re-entered roughly 1–3 times a year each, and `current_value`/`valuation_date` on real estate/gold (added in the v1.0 net-worth-model pass) are meant to be periodically updated as the household reappraises those holdings. That's meaningfully more frequent, relative to how rarely this page's records get created in the first place, than the near-zero manual edit rate on Accounts. Card count is also much lower here (a handful of real assets vs. 32 accounts), so an icon-only button buys back less density benefit than it did on Accounts.

**Decision:** Do **not** copy UX-002's icon-only treatment verbatim. Instead: once UX-006 removes Deactivate from the footer, move the Edit button up into the card's header row (next to `StatusBadge`), matching Accounts.js's structural placement for consistency — but keep it as a small labeled control (e.g. a plain "Edit" text/link button), not an icon-only pencil glyph. This keeps the cross-page layout convention (header row holds status + the one available card-level action) while preserving a visible label, justified by the materially higher real edit frequency on this page.

**Why:** Prominence should track actual frequency, and the frequency story here is different from Accounts, not identical to it — the task brief explicitly warned against assuming the Accounts pattern transfers, and on inspection it doesn't cleanly. Icon-only would under-signal an action this household will genuinely reach for a few times a year per asset (compliance renewals), more than once-a-quarter-if-ever on Accounts.

**Alternatives considered:**
- *Copy UX-002 exactly (icon-only Edit)* — rejected per the frequency analysis above; would optimize for a workflow this page doesn't have (bulk/CSV-driven data that rarely needs hand-correction).
- *Leave Edit exactly where it is today (footer, `flex-1` label button)* — viable minimal alternative once Deactivate is removed (UX-006 alone resolves the adjacent-destructive-action risk, since Edit would become the sole footer button). Moving it to the header is a smaller polish recommendation for cross-page consistency, not a safety fix — react-developer/user should feel free to skip the header move and just leave Edit in the footer alone if minimizing diff is preferred.

---

## UX-009: Promote Current Value, Reorder Compliance Status Above Static Registration Metadata

**Status:** Recommended (2026-07-11) — not yet implemented. Target file: `web/src/pages/Wealth/PhysicalAssets.js`.

**Context:** `AssetCard`'s detail block (`PhysicalAssets.js`, lines 86–99) renders, in order: `registration_type`, `current_value` (+ `valuation_date`), then the three `ComplianceRow`s (PUC/Insurance/Road Tax — already color-coded red/amber/gray by urgency, which is good existing design). All four render at the same lightweight `text-sm text-gray-600` weight. This buries the two numbers a user actually opens this page to check: (1) for a vehicle, whether PUC/insurance/road tax is expired or due soon — the actionable, time-sensitive reason to look at this card at all; (2) for real estate/gold (non-vehicle types, which have no compliance fields), `current_value` is the direct equivalent of Accounts' "balance" — the headline number — yet it renders identically to static lookup metadata like `registration_type`.

**Decision:** (a) Promote `current_value` to a bolder headline treatment (`text-lg font-semibold`, own line) mirroring Accounts.js's `BalanceLine` component, for any asset where it's populated. (b) Reorder the detail block so the three `ComplianceRow`s (time-sensitive, already color-coded) render above the static `registration_type` line — the expiry rows are what the user came to check; registration type is occasional-lookup metadata.

**Why:** Matches the same principle already applied to Accounts (UX-004/UX-005b): the number/status the user actually scans for shouldn't compete visually with, or be listed below, secondary metadata. For this page specifically, that "headline" differs by asset type (compliance urgency for vehicles, current value for real estate/gold) — the reorder + promotion serves both without adding a new component beyond one modeled directly on `BalanceLine`.

**Alternatives considered:**
- *Leave ordering as-is, only bold `current_value`* — considered; rejected as a partial fix since it leaves compliance rows (the vehicle-relevant urgent info) below a static field.
- *Do nothing* — rejected; ties directly into a real, already-tracked gap (see flag below) — worth surfacing together.

**Scope flag (not a UX prominence issue, noted for visibility):** Per `documents/domain-state/wealth.md`, the v1.0 net-worth-model pass added `current_value`/`valuation_date` to the schema/contract and expanded `AssetType` to include `REAL_ESTATE`/`GOLD_JEWELRY`/`GOLD_BOND`, but explicitly left the frontend create/edit forms un-expanded — confirmed still true on this read: `ASSET_TYPES` (line 14) is hardcoded to `['VEHICLE']` only, and `handleAddSubmit` (lines 206–225) unconditionally requires `make`/`model`/`registration_number`/`registration_type` regardless of `asset_type`, so the Add modal cannot actually create a real-estate or gold asset today — and neither the Add nor Edit form exposes `current_value`/`valuation_date` for entry at all. This means the field UX-009 recommends promoting can't currently be set through this page. This is a functional/contract-shaped gap, not a prominence judgment call — already tracked as an open item in `documents/domain-state/wealth.md`; flagging here only because it's directly adjacent to UX-009 and worth scheduling together. Recommend business-analyst/architect confirm scope (asset-type-conditional form fields is a real feature addition, not a small polish) before react-developer picks it up alongside UX-006 through UX-009.

---

## UX-010: Reports Should Default to the Logged-In User's Own Profile

**Status:** Implemented (2026-07-11) — `web/src/pages/Wealth/Reports.js`. `selectedProfileId` now initializes to `user?.profile_id ?? ''` (matching `Setup.js:78`'s precedent exactly); `useAuth()` moved above the state declaration so `user` is available at initialization time. The dropdown is untouched and still fully editable for the admin's legitimate need to check other household members' reports. Live-verified against the running app (real household data): navigating to `/wealth/reports` after signing in as admin now loads the report immediately with no blank-picker prompt.

**Context:** `Reports.js` lines 46–47 initialize `selectedProfileId` to `''` and render a mandatory "Select a profile..." dropdown (lines 116–129) before any content appears — line 131–133 gates the entire page behind it with "Select a profile to view the report." Every other data page in this app that shows a single member's own numbers skips this step entirely: `Dashboard.js` (line 288) and `ActionCenter.js` (line 64) both do `const profileId = user?.profile_id ?? null;` straight from `useAuth()` and render immediately — no picker. `Setup.js` (line 78) shows the closest match to what Reports actually needs: `useState(user?.profile_id ?? null)`, an editable default rather than a blank required field, which still supports the admin picking a different household member if they want to.

**Decision:** Initialize `selectedProfileId` to `user?.profile_id ?? ''` instead of `''`. Keep the dropdown — the admin legitimately needs to check other household members' reports occasionally — but the logged-in user's own report should render on page load without an extra click, matching the `Setup.js` precedent exactly.

**Why:** For the one real household using this app, "check my/our numbers" is the default reason to open Reports; making that the zero-click state instead of a required selection removes friction from the single most common visit, while the dropdown still covers the admin's legitimate need to check a different member.

**Alternatives considered:**
- *Remove the dropdown entirely, always show only the logged-in user's own report* — rejected; the admin role does need to check other members' reports (this is exactly why the selector exists, per the `listProfiles(user?.admin_id, ...)` call), so removing it would regress a real capability. Defaulting it, not removing it, is the fix.

---

## UX-011: Promote Net Balance to Hero Position, De-Emphasize Account-Count Metadata

**Status:** Implemented (2026-07-11) — `web/src/pages/Wealth/Reports.js`. New `NetBalanceHeroCard` component renders Net Balance as a standalone lead tile above the account-count row (`bg-indigo-50`, `text-3xl font-bold text-indigo-900`, mirroring Dashboard.js's `SnapshotSummary` net-worth hero-tile treatment). Total/Active/Inactive Accounts moved into their own smaller `grid-cols-3` row beneath it, with `SummaryCard`'s typography de-emphasized from `text-2xl font-bold text-gray-900` to `text-lg font-semibold text-gray-700`. Also folds in UX-012 (last-calculated timestamp) and UX-013 (distinct error state) into the same hero component.

**Context:** `Reports.js` lines 147–160 render four `SummaryCard`s — Total Accounts, Net Balance, Active Accounts, Inactive Accounts — in one `grid-cols-4` row. `SummaryCard` (lines 29–43) applies identical typography to all four: `text-2xl font-bold text-gray-900` for the value, regardless of which card it is. The number this household actually opens Reports to check — net balance — renders at the exact same size and weight as "Inactive Accounts," a count of dormant/closed accounts nobody is checking day-to-day. Compare `Dashboard.js`'s `SnapshotSummary` (lines 169–196): net worth gets its own colored hero tile (`bg-indigo-50`, `text-xl font-bold text-indigo-900`) visually distinct from the plainer supporting tiles beside it.

**Decision:** Pull "Net Balance" out of the 4-up grid into its own lead card above the others — larger typography (e.g. `text-3xl` or `text-4xl font-bold`), optionally a colored background (e.g. `bg-indigo-50`, matching Dashboard's hero-tile treatment for the same underlying number) so it reads as the page's headline figure. Keep Total/Active/Inactive Accounts as a smaller 3-up row beneath it, with de-emphasized typography (e.g. `text-lg` instead of `text-2xl`) since they are supporting metadata, not the reason the page is being opened.

**Why:** Directly the "what does the user need to see first" test — net balance is the headline number, account counts are secondary context. Equal visual weight across all four currently forces the user to read every card to find the one they came for.

**Alternatives considered:**
- *Leave all four in one grid but bold only the Net Balance value* — rejected as a half-measure; a same-size, same-position card with only font-weight different is easy to miss when scanning quickly, which is the exact failure mode being fixed.

---

## UX-012: Add a Last-Calculated Timestamp Next to the Net Balance Figure

**Status:** Implemented (2026-07-11) — `web/src/pages/Wealth/Reports.js`. `formatTimestamp` duplicated from `Dashboard.js` (identical `en-IN` locale/options) rather than extracted to a shared util — only two call sites today, below the "3+" bar the Scope check in this entry set for justifying a `src/utils/` extraction. `NetBalanceHeroCard` renders "Last calculated: {formatTimestamp(calculatedAt)}" beneath the net balance figure whenever a snapshot exists and the fetch didn't fail.

**Context:** `Reports.js`'s net balance card (lines 149–157) shows either "Not calculated" or a formatted number, with a static `subLabel` of "Opening balance + transaction history" — no indication of *when* that number was computed. The underlying snapshot object (`netWorthSnapshot`, set from `getDashboard()`'s response) carries a `calculated_at` field — confirmed via `Dashboard.js`, which already reads the equivalent field and renders `Last refreshed: {formatTimestamp(lastRefreshed)}` (lines 262–266, helper at lines 59–68). Reports.js never reads or displays this field, so a real, successfully-computed net balance from two weeks ago looks identical to one computed thirty seconds ago.

**Decision:** Copy `Dashboard.js`'s `formatTimestamp` helper (or extract it to a shared util if both pages need it — see Scope check) and render "Last calculated: {formatTimestamp(netWorthSnapshot.calculated_at)}" beneath the Net Balance figure whenever a snapshot exists.

**Why:** A household relying on this number to make financial decisions needs to know its age at a glance, not just its value — this is the same staleness-visibility principle behind Dashboard's existing "Last refreshed" line and behind the Accounts page's UX-004 fix (a number with no freshness signal is easy to mistake for current).

**Alternatives considered:**
- *Do nothing, rely on the user remembering to hit Refresh* — rejected; the whole point of surfacing a timestamp is to remove that burden of memory, especially since Reports has no auto-refresh-on-load behavior.

**Scope check:** If `react-developer` finds `formatTimestamp` needed in three or more places (Dashboard, Reports, and potentially others), it's a reasonable candidate to extract into `src/utils/` per the existing "Custom hooks for reusable logic — don't duplicate it" guidance in `FRONTEND_GUIDELINES.md` — a small refactor, not a new component or page, so no architect sign-off needed.

---

## UX-013: Distinguish a Failed Net-Worth Fetch From "Not Yet Calculated"

**Status:** Implemented (2026-07-11) — `web/src/pages/Wealth/Reports.js`. New `netWorthError` state, parallel to the existing `refreshError`/`error` states. `loadNetWorth`'s catch block now sets `netWorthError` (to the caught error's message) alongside nulling the snapshot, instead of only nulling it. `NetBalanceHeroCard` renders a distinct red (`bg-red-50`/`text-red-700`) "Unavailable" / "Couldn't load net balance — try Refresh" state when `netWorthError` is set and no snapshot exists, instead of the "Not calculated" copy. `netWorthError` is cleared on profile change, at the start of every `loadNetWorth` call, and on a successful manual refresh.

**Context:** `loadNetWorth` (lines 63–71) wraps its `getDashboard()` call in a try/catch that, on any failure (network error, gateway down, etc.), silently does `setNetWorthSnapshot(null)` — identical to the case where the household has simply never clicked Refresh yet. The UI then shows "Not calculated / Click Refresh to calculate" (lines 150–157) for both a genuine first-time-use state and a real fetch failure, with no way for the user to tell which one they're looking at. This is the same class of bug as the Accounts page's UX-004 (a failed fetch silently indistinguishable from a legitimate different state) — that one was fixed for balances; this is the same pattern recurring on Reports for net worth.

**Decision:** Add a `netWorthError` state, parallel to the existing `refreshError`/`error` states already in this file. In `loadNetWorth`'s catch block, set `netWorthError` (e.g. to the caught error's message or a fixed string) instead of only nulling the snapshot. When `netWorthError` is set, render a distinct message — e.g. "Couldn't load net balance — try Refresh" in the same red/amber treatment already used for `refreshError` (line 171) — instead of the "Not calculated" copy, which reads as "you haven't done this yet" rather than "something went wrong."

**Why:** Same rationale as UX-004: a household using this as financial source of truth should never see a fetch failure that reads identically to "nothing to see here yet." The two states mean very different things (one is expected/benign, the other means data may be missing that the user thinks they should have).

**Alternatives considered:**
- *Reuse the existing `error` state (currently only set by the accounts-list fetch)* — rejected; conflating the accounts-list error with the net-worth error would make the red banner at the top ambiguous about which piece of data failed, when the two are fetched independently and can fail independently.

---

## UX-014 (Flagged): Reports vs. Dashboard Redundancy, and an Inaccurate "Coming Soon" Placeholder

**Status:** Flagged for business-analyst/architect review (2026-07-11) — not a pure UX fix, not implemented, no react-developer handoff yet.

**Context:** `Reports.js` calls the exact same `getDashboard`/`refreshProjections` APIs as `Dashboard.js`, but only reads one snapshot key (`WEALTH_NET_WORTH`, line 11) out of the full set the gateway computes — it ignores `WEALTH_NET_WORTH_FAMILY` (the family rollup — per `documents/domain-state/wealth.md` this household's real net worth, ~₹1.64 crore, is a family figure, not a single member's), `WEALTH_EMI_TRACKING_FAMILY`, `WEALTH_LIQUIDITY_TIERS_FAMILY`, `WEALTH_FORMULA_GOALS_FAMILY` (the 5 Epic 8 formula goals), and `WEALTH_VALIDATION_REPORT_FAMILY` — all of which `Dashboard.js`'s `SnapshotSummary` (lines 121–269) already reads from the identical API response and renders today. Reports.js then shows a static "Deeper Analytics — Coming Soon" placeholder (lines 174–179) promising "category breakdown, and trend charts" — but it undersells what's already built: goals, EMI, liquidity tiers, and a data-quality validation report are live and already displayed, just not on this page. Per `documents/domain-state/wealth.md`, Reports.js's only prior maintenance was a narrow net-balance calculation bug fix (v0.5 Phase 0) — it appears to predate the Epic 8 projection work and was never revisited to consume the newer snapshot keys, even though the page is literally named "Wealth Reports" and would be the natural home for a "deeper" view (e.g. per-member breakdown, drill-down) that Dashboard's compact summary card doesn't have room for.

**Why this isn't a plain UX fix:** Deciding what Reports.js *is for*, relative to Dashboard, is a product-scope call, not a layout tweak — e.g., should it become a genuine superset of Dashboard's summary (family rollup + full per-member breakdown + EMI + liquidity + goals + validation, with drill-down Dashboard has no room for), or should its unique value (if any, beyond what's already on Dashboard) be folded into Dashboard and this page removed/repurposed? Either direction is a real scope decision — it changes what a page promises and touches more than Tailwind/layout.

**Recommendation (for business-analyst/architect, not self-authorized):** At minimum, fix the misleading placeholder — "Coming Soon" should not describe data that already exists and is already computed by the same API call this page makes. Beyond that, decide whether Reports should be built out to surface the family rollup and Epic 8 data Dashboard already has (making the two pages differ by depth/breakdown, not by dataset), or whether the page's remaining unique value doesn't justify a second page for a single-household app.

**Alternatives considered:**
- *Silently leave as-is since it technically has no bugs, just a misleading placeholder* — rejected; recommending a decision be made explicitly (even if the decision is "leave it") is more useful than letting a stale placeholder persist through inertia, especially given the sibling `PhysicalAssets`/`Transactions` reviews running in this same round may surface a similar pattern.

---

## UX-015: Transactions "Next Page" Appears Stuck — Missing Stale-Response Guard in `load()`

**Status:** Implemented (2026-07-11) — `web/src/pages/Wealth/Transactions.js`'s `TransactionsTab.load()` and `web/src/pages/Wealth/PhysicalAssets.js`'s `loadAssets()`. Both `load()`/`loadAssets()` are invoked both by an effect (whenever page/filters change) and manually (after add/edit/deactivate), so a plain closure `cancelled` flag returned from a `useEffect` cleanup — the literal shape `Accounts.js` uses for its balance-loading effect — doesn't cleanly cover the manual-invocation call sites too. Implemented instead with a `loadRequestIdRef`/`loadRequestIdRef` request-id ref per component: each call increments and captures a request id before awaiting the API call, and every subsequent `setState` call (transactions/assets, totalSize, error, loading) is guarded by `requestId === ref.current` — an older in-flight request resolving after a newer one is a no-op instead of clobbering state. Same guarantee as the `cancelled`-flag pattern (never let a stale response win), adapted for a call site that isn't purely effect-driven.
New Jest test in both `Transactions.test.js` and `PhysicalAssets.test.js` proves the guard: mocks the list API with a manually-controlled (deferred) promise for the first ("stale") request and an immediately-resolving second ("fresh") request triggered by a filter change before the first resolves; asserts the fresh data is shown and resolving the stale promise afterward does not overwrite it.
Live-verified against the running app (real household dev data, read-only): `/wealth/transactions` with the real 8-transaction Salary Account renders with Next correctly disabled (single page); confirmed via Playwright smoke test that all three affected pages compile, render, and behave correctly end-to-end. Full reproduction of the original ~8-page race (needs ~150+ real transactions on one account) remains blocked on re-uploading a real bank statement to the dev DB per this entry's original Investigation section — that data-seeding step is unrelated to whether the code fix itself is correct, which the request-id-ref test directly proves.

**User's report:** *"on http://localhost:3000/wealth/transactions page I see transaction of page 1 and it also show we have 8 pages but it does not allow me to goto next page."*

**Investigation performed (code + live system, not code-reading alone):**
1. Traced the full pipeline end-to-end: `Transactions.js` → `web/src/api/wealth.js`'s `listTransactions()` → gateway `WealthGatewayResource.listTransactions`/`WealthServiceClient.listTransactions` → wealth service `TransactionResource.listTransactions` → `TransactionService.listByAccountPaginated` → `TransactionPanacheRepository` (`findByAccountId` with `.page(Page.of(page, size))`, separate `countByAccountId`). Parameter names (`page`, `size`, `profile_id`, `from`, `to`, `txn_type`) and the JSON field the frontend reads (`total_size`) match exactly at every hop — no name/order mismatch anywhere in the chain.
2. Verified this live against the running stack (all services + gateway were already up): `GET /v1/accounts/{HDFC Salary account}/transactions?profile_id=...&page=1&size=1` through `page=7&size=1` against an account with `total_size: 8` returned 8 distinct, correctly-ordered transactions, one per page, with `total_size` staying consistent throughout. This proves the backend pagination mechanism (query, offset, count) is correct right now.
3. Could **not** reproduce an account with enough live data to naturally produce 8 pages at the frontend's real `PAGE_SIZE = 20` (would need ~141–160 transactions). Every account currently in the dev DB has 0, 8, or 13 transactions — all of which fit on a single page at size 20, so `totalPages = Math.ceil(total/20) = 1` and Next is *correctly* disabled for all of them today. Checked `GET /v1/accounts/{id}/uploads` for the two accounts that do have data (HDFC Salary = 8 txns, Kotak Joint = 13 txns): **both return an empty upload history** — meaning the rows currently live were seeded directly, not ingested via a real CSV upload. Combined with the accounts/profiles' `created_at` timestamps (2026-07-10, i.e. yesterday relative to this review), this points to the dev DB having been reset/reseeded since the user's report was filed — the account that genuinely had ~150 rows and exhibited the bug no longer exists in its bug-triggering state, so a byte-for-byte repro isn't currently possible.

**Root cause identified regardless (real, not hypothetical):** `load()` has no guard against out-of-order network responses — no `AbortController`, no `cancelled`/`ignore` flag, unlike the sibling `Accounts.js`, which already solved exactly this problem for its balance-fetch effect (`Accounts.js` lines 345–364: `let cancelled = false; ... return () => { cancelled = true; }`, checked before every `setBalances` call). `web/src/index.js` wraps the app in `<React.StrictMode>`, which double-invokes effects on mount in development — so on first mount, two `load()` calls for `page=0` can be in flight simultaneously. If a user then clicks Next quickly (a normal thing to do while reviewing several pages of statement history), a *third*, newer request for `page=1` fires. Nothing in `load()` stops an older `page=0` response from resolving *after* the `page=1` response and silently overwriting `transactions`/`totalSize` state with the stale page's data — even though the `page` state itself has already advanced to 1. The visible symptom is exactly what was reported: the user clicks Next, the table snaps back to (or never leaves) page-1 content, and repeated clicks look like they're doing nothing.

**Recommendation:** Add the same stale-response guard `Accounts.js` already uses, applied to `load()`:
```
const load = useCallback(async () => {
  if (!accountId) return;
  let cancelled = false;
  setLoading(true);
  setError(null);
  try {
    const data = await listTransactions(accountId, profileId, fromDate || null, toDate || null, txnType, page, PAGE_SIZE);
    if (!cancelled) {
      setTransactions(data.transactions ?? []);
      setTotalSize(data.total_size ?? (data.transactions ?? []).length);
    }
  } catch (err) {
    if (!cancelled) setError(err.message || 'Failed to load transactions');
  } finally {
    if (!cancelled) setLoading(false);
  }
  return () => { cancelled = true; };
}, [accountId, profileId, fromDate, toDate, txnType, page]);
```
(exact wiring — e.g. whether the cleanup return needs to move into the `useEffect` that calls `load()` rather than inside `load()` itself — is react-developer's call; the shape to match is `Accounts.js`'s existing pattern, not a new one.) Apply the identical fix to `PhysicalAssets.js`'s `loadAssets`, which has the same gap. Separately, **before considering this closed**, wealth-developer/react-developer should re-upload a real, full-length Kotak or HDFC statement (the account's empty upload history suggests this hasn't been done against the current dev DB) to restore a genuine multi-page account, and manually click through Next several times in the actual browser to confirm both that the guard fixes the race and that no other issue is hiding behind it.

**Why:** This is the same class of finding as UX-004 (data-trust: a stale/failed value silently looking identical to a fresh one) but for pagination state instead of a balance figure — the interface should never silently show old data as if it were the result of the user's last action. It's also directly actionable per `documents/FRONTEND_GUIDELINES.md`'s "match existing patterns" principle: the fix already exists in this exact codebase, one file over.

**Alternatives considered:**
- *Migrate `TransactionsTab`/`PhysicalAssets` to React Query* — would eliminate this whole class of bug for free (React Query cancels/ignores stale requests by query key automatically) and is the direction ADR-018 already points the codebase (`Dashboard.js` is the named reference pattern). Not recommending it as the immediate fix because it's a bigger refactor than a two-page pagination fix warrants on its own — worth raising to architect/react-developer as a candidate for the next page migrated to React Query, but the `cancelled`-flag patch is the right size for this specific bug.
- *Do nothing until it's reproduced live* — rejected; the missing guard is a real defect independent of whether I can currently force it to fire, and it's a one-line-pattern fix already proven safe elsewhere in this codebase.

---

## UX-016: Color the Amount Column by Txn Type, Not Just the Badge

**Status:** Implemented (2026-07-11) — `web/src/pages/Wealth/Transactions.js`, `TransactionRow`. New `amountColorClass(txnType)` helper reuses `TxnTypeBadge`'s exact palette (`text-green-800` for CREDIT, `text-red-800` for DEBIT) applied directly to the Amount cell's text color, replacing the flat `text-gray-900`. Badge is untouched (still shows the literal CREDIT/DEBIT text). Live-verified against the running app: a real DEBIT row's amount cell class includes `text-red-800`.

**Context:** Each row renders `Amount` (`text-sm text-gray-900 font-medium`, line 161) and, in a separate column, a `TxnTypeBadge` (line 163) that's already green for CREDIT / red for DEBIT (`txnTypeButtonClass`/`TxnTypeBadge`, lines 44–54). The amount figure itself — the number a household actually scans down the column for when reviewing spending — carries no color signal; only the adjacent badge does. For someone scanning a page of 20 rows to answer "how much went out this month," reading two columns per row (amount, then badge) to determine direction is slower than reading one.

**Decision:** Apply the same red/green semantic already defined for `TxnTypeBadge` directly to the Amount cell's text color — e.g. `text-red-700` for DEBIT, `text-green-700` for CREDIT — replacing the flat `text-gray-900`.

**Why:** This is a pure "what does the user need to see first" fix: color-coding the number itself (not just a secondary badge) is the fastest scan path down a column of amounts, and it costs nothing — the color values already exist in this file, reused, not invented.

**Alternatives considered:**
- *Drop the badge and rely on amount color alone* — rejected; the badge also carries the literal "CREDIT"/"DEBIT" text, useful for anyone who can't rely on color alone (colorblindness) — keep both, this is additive, not a replacement.

---

## UX-017: Truncated Transaction Description Needs a `title` Tooltip

**Status:** Implemented (2026-07-11) — `web/src/pages/Wealth/Transactions.js`, `TransactionRow`. `title={txn.description || undefined}` added to the description `<td>`, matching the native-tooltip pattern already established elsewhere in this app. Live-verified against the running app: the description cell for a real transaction carries a non-empty `title` attribute.

**Context:** `truncate(txn.description, 60)` cuts real bank-CSV narrations (often verbose UPI/NEFT reference strings) at 60 characters with an ellipsis, and nothing else in the cell exposes the rest. For real bank-generated descriptions, the identifying part (merchant name, reference number) isn't always in the first 60 characters, so a household trying to recall "what was this ₹4,500 debit" may have no way to find out from this page. This is the same pattern Accounts.js UX-002 already uses elsewhere in this app (native `title` attribute as a lightweight, no-new-component way to surface full text on hover) — reusing it here isn't a new interaction pattern.

**Decision:** Add `title={txn.description}` to the description `<td>` so hovering reveals the untruncated text via the browser's native tooltip.

**Why:** Zero-cost, no new component, matches an interaction already established in the same codebase, and directly serves the "reviewing real spending" workflow — being unable to identify a transaction from a truncated CSV narration defeats the point of reviewing the ledger.

**Alternatives considered:**
- *Increase the truncation limit instead* — rejected; a longer fixed limit still eventually truncates and doesn't solve the underlying problem, whereas `title` gives the full text on demand without widening the column or wrapping rows.

---

## UX-018: De-Emphasize "+ Add Transaction" Relative to Its Real Usage Frequency

**Status:** Implemented (2026-07-11) — `web/src/pages/Wealth/Transactions.js`, `TransactionsTab`. Button restyled from solid primary (`bg-blue-600 text-white`) to outline/secondary (`border border-blue-600 text-blue-600 hover:bg-blue-50`), same position, still fully labeled. Live-verified against the running app: the button's class list no longer includes `bg-blue-600` and now includes `border-blue-600`.

**Context:** "+ Add Transaction" renders as a solid-fill primary blue button (`bg-blue-600 text-white`), permanently visible in the header row whenever the Transactions tab is active. Per `documents/BUSINESS_REQUIREMENTS.md`/`domain-state/wealth.md`, this household's transaction data arrives overwhelmingly via weekly CSV statement upload (Q7's "Manual Transaction Entry" exists for occasional corrections/cash transactions the bank CSV won't have) — the same "CSV-driven, manual edits are rare" workflow that justified demoting Accounts' Edit button from a full labeled button to an icon (UX-002, citing the user's own words: *"after uploading CSV files these details will be automatically updated"*). Unlike Accounts' Edit, this button isn't destructive and doesn't need to be hidden or relocated — but it's currently styled with the same visual weight as a page's primary call-to-action, when the primary reason this page gets opened is to review/filter existing transactions, not create new ones.

**Decision:** Restyle "+ Add Transaction" from a solid primary button to a lighter secondary/outline treatment (e.g. `border border-blue-600 text-blue-600 hover:bg-blue-50` instead of `bg-blue-600 text-white`), keeping it in the same position and fully labeled (it's rare but not so rare it should lose its label, unlike Deactivate/Edit on Accounts — creating a transaction isn't destructive and users still need to recognize it by name).

**Why:** Visual weight should track how often this action is actually the reason someone opened the page. This is a minor polish, not a safety fix (nothing destructive here) — flagged as lower priority than UX-015/016/017.

**Alternatives considered:**
- *Move it into a secondary location (e.g. below the table, or behind a menu)* — rejected as overcorrection; unlike Accounts' Deactivate, this isn't a risk to guard against, just a visual-weight mismatch. Keeping it in place but lighter preserves discoverability while fixing the prominence mismatch.
- *Leave it as-is* — reasonable minimal alternative; this is explicitly called out as the lowest-priority item in this review and safe to skip if the user prefers to minimize diff.

---

## UX-019 (Flagged): No Free-Text Description Search on Transactions

**Status:** Flagged for architect/business-analyst review (2026-07-11) — not a simple UX fix, not implemented, no react-developer handoff yet.

**Context:** Filtering on this page is limited to date range and CREDIT/DEBIT/ALL type toggles (lines 274–305). There's no way to search "find that Amazon transaction" or "how much did I spend at X merchant this year" by description text. Unlike Accounts' UX-005 search (client-side, trivial, because all 32 accounts are already fully loaded), Transactions is server-side paginated — the frontend only ever holds the current page's 20 rows in memory, so a client-side text filter would only search whatever page is currently on screen, which would be actively misleading (looks like a real search, silently misses every other page). A real fix needs a backend `description`/text-search query parameter threaded through `wealth.yaml` → gateway → `TransactionResource`/repository — a contract change, not a layout tweak.

**Why this isn't a plain UX fix:** It requires a new backend query parameter, a new repository predicate, and contract/gateway mirroring — real scope, not a Tailwind/layout change, and outside what I'm authorized to hand straight to react-developer.

**Recommendation (for architect/business-analyst, not self-authorized):** If the household's real workflow includes "find a specific past transaction by description" often enough to matter (plausible, given real CSV-driven data growing every statement cycle), scope a `description` (or general `q`) query param on `GET /accounts/{id}/transactions`, following the same additive-overload pattern already used for physical-assets pagination (`documents/domain-state/wealth.md`'s Q54 entry) so it doesn't disturb the existing unpaginated call sites.

**Alternatives considered:**
- *Add a client-side search box anyway, scoped to "search this page only"* — rejected; the interaction would look identical to Accounts' real search but behave completely differently (silently incomplete), which is worse than no search at all for a page whose data the user needs to trust.
