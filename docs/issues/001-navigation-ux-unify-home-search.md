# Navigation UX: Unify Home + Search and Fix Tool Flows

## Summary
The current navigation splits user tasks across Home, Search, and Library, with a separate event selection flow for Cashier/Scanner. This creates confusing navigation state (bottom bar highlights Search while the user is not on Search) and duplicate list experiences. The UX should converge on a single primary list screen, with direct tool entry for Cashier/Scanner and a clear back behavior.

## Current UX (as implemented)
- Home shows two tool buttons (Cashier/Scanner) and otherwise empty content.
- Search shows a list of events, with search + filter chips.
- Cashier/Scanner entry from Home takes the user to an event selection list (a different list screen) that visually resembles Search but is a different flow.
- Bottom navigation highlights Search while the user is in Selection, and tapping Search again leads to another similar list with different actions.
- Back on Android can exit the app from Home instead of returning to a meaningful list.

## UX Problems
- The app feels empty on landing because Home has little content.
- There are two different list screens that look similar but mean different things, which breaks orientation.
- Bottom navigation does not reflect the actual location in the flow.
- Tool entry (Cashier/Scanner) is indirect and forces an extra decision before code entry.
- Back behavior is surprising; the first back exits rather than returning to a meaningful previous step.

## Proposed UX Direction
1. Single primary list screen
- Merge Home and Search so the app lands on the event list with search.
- Remove the separate Selection list. One list, one set of actions.
- If we keep a bottom bar, it should only reflect real screens (no hidden pages).

2. Direct tool entry (Cashier/Scanner)
- Pressing Cashier/Scanner should immediately ask for a code.
- After code matches, show which event it belongs to and ask for confirmation before entering the tool.
- This avoids two list views with different meanings.
- API note: proper flow needs an endpoint that accepts code without event id. (This now exists.)

3. Favorites as a filter (replace Library view)
- Replace the separate Library page with a single filter (star = show only saved).
- Remove the unused All/Open/Upcoming/Past chips for now (if they are not functional).
- This simplifies navigation and removes the need for a separate Library tab.

4. Event detail presentation
- Event detail can be full screen or a modal sheet, but it must be clearly in the list flow.
- Back or close should return to the list, not exit the app.

5. Back behavior (Android)
- First back returns to the list flow.
- Second back (from list root) exits the app.

## In Scope (Assets)
- Use image resources from `frontend/public` for header/logo needs in this UX work.
- Also reference Android assets in `android/app/src/main/res/drawable` (e.g. `iloppis_logo_black.xml`, `icon_192.xml`) when appropriate.

## Acceptance Criteria
- App lands on a single list-based home (search + list) on both Android and iOS.
- Cashier/Scanner entry starts with code input, then confirms event, then opens tool.
- No separate event selection screen remains in the user-facing flow.
- Bottom navigation (if kept) never highlights a different screen than the one shown.
- Favorites are accessible as a filter on the list, and the Library tab is removed or repurposed.
- Android back button: back from details/tools returns to list; back from list root exits.

## Out of Scope (for this issue)
- Visual styling of the top bar beyond clarifying that header is separate from scrollable list.

## Code Pointers (current behavior)
- Home + tool entry: `android/app/src/main/java/se/iloppis/app/ui/screens/user/home/HomeScreen.kt`
- Search list: `android/app/src/main/java/se/iloppis/app/ui/screens/events/EventListScreen.kt`
- Selection list: `android/app/src/main/java/se/iloppis/app/ui/screens/events/EventSelectionScreen.kt`
- Navigation state + bottom bar logic: `android/app/src/main/java/se/iloppis/app/ui/screens/ScreenModel.kt`, `android/app/src/main/java/se/iloppis/app/ui/components/navigation/Navigator.kt`
- Code entry depends on event id: `android/app/src/main/java/se/iloppis/app/utils/user/codes/CodeState.kt`

## UX → API Use-Case Map (Swagger-Based)
Scope: mobile app UX flows mapped to current Swagger endpoints.
Assumes alias-only API is available (GET `/v1/api-keys/alias/{alias}`), which returns `eventId` + `apiKey`.

### 1) App Landing: Event List (merged Home + Search)
- UX: App opens directly on list with search and filters.
- API:
  - `POST /v1/events:filter`
  - Optional: `GET /v1/events` (no filters) for small datasets.

### 2) Search Query + Filters
- UX: User types in search, toggles “Open/Upcoming/Past” (if enabled), or location filters.
- API:
  - `POST /v1/events:filter` with `filter.searchText` and `filter.lifecycleStates`.

### 3) Favorites / Saved Events Filter
- UX: User taps star filter to show saved events only.
- API:
  - None in current implementation (local storage).
  - Optional future: `GET /v1/preferences` + `PATCH /v1/preferences` if favorites should sync to account.

### 4) Event Details (from list)
- UX: Tap event to view details.
- API:
  - `GET /v1/events/{eventId}`

### 5) Tool Entry: Cashier/Scanner via Code
- UX: Tap “Öppna kassa” or “Öppna skanner” → enter code → app resolves event → user confirms → enter tool.
- API:
  - `GET /v1/api-keys/alias/{alias}` (preferred; alias-only)
  - `GET /v1/events/{eventId}` (to display event name/details before confirm)
- Notes:
  - Legacy still exists: `GET /v1/events/{eventId}/api-keys/alias/{alias}`
  - Response includes `apiKey` + `eventId` + `type` (validate CASHIER/SCANNER)

### 6) Cashier: Validate Sellers (approved list)
- UX: Cashier enters seller number; app validates seller is approved.
- API:
  - `POST /v1/events/{eventId}/vendors:filter` with `filter.status = "approved"`

### 7) Cashier: Submit Purchase (sold items)
- UX: Cashier finalizes purchase, items uploaded (or queued offline).
- API:
  - `POST /v1/events/{eventId}/sold-items`

### 8) Cashier: Review/Retry Failed Uploads
- UX: User opens rejected purchase review and retries upload.
- API:
  - `POST /v1/events/{eventId}/sold-items`

### 9) Scanner: Scan Ticket (camera or manual)
- UX: Scan QR / ticket ID and validate entry.
- API:
  - `POST /v1/events/{eventId}/visitor_tickets/{id}/scan`

### 10) Scanner: Lookup Ticket Details (optional)
- UX: Show details for a scanned ticket or history entry.
- API:
  - `GET /v1/events/{eventId}/visitor_tickets/{id}`

### 11) Scanner: Resolve Ticket Type Labels
- UX: Show ticket type names in scan results.
- API:
  - `GET /v1/events/{eventId}/ticket_types`

### 12) Event List for Saved Events Only (local list sync)
- UX: Show only saved events when star filter is active.
- API:
  - `GET /v1/events?eventIds=...` (optional if you want fresh details for saved list)

### 13) Back Behavior
- UX: Back from detail/tool returns to list; back from list root exits.
- API:
  - None

## Wireframes (UX ↔ API)
Legend: `[API]` indicates a network call triggered by the UI.

### A) Unified Home + Search (List)
```
┌─────────────────────────────────────┐
│ [Logo from frontend/public or       │
│  android/app/src/main/res/drawable] │
├─────────────────────────────────────┤
│ [ Search: "Sök loppis..."        ]  │
│ [ ★ Sparade ] [ Alla ] [ Öppna ]     │
├─────────────────────────────────────┤
│ Event Card                          │
│  Titel               [ÖPPEN]        │
│  Datum / Plats                      │
├─────────────────────────────────────┤
│ Event Card                          │
│  Titel               [ÖPPEN]        │
│  Datum / Plats                      │
└─────────────────────────────────────┘
```
- [API] `POST /v1/events:filter` (initial load + search/filter changes)
- [API] `GET /v1/events/{eventId}` (when opening event detail)

### B) Event Detail (Modal or Full Screen)
```
┌─────────────────────────────────────┐
│  [Map/Media]                         │
│  Event Name                 [★]     │
│  Dates / Location                   │
│  Description...                     │
│  [ Följ / Avfölj ]                  │
│  [ Öppna kassa ] [ Öppna skanner ]  │
└─────────────────────────────────────┘
```
- [API] `GET /v1/events/{eventId}` (if not already loaded)
- Tool buttons go to Code Entry (see C)

### C) Code Entry (Cashier / Scanner)
```
┌─────────────────────────────────────┐
│ Ange kod                             │
│ [ _ _ _  -  _ _ _ ]                  │
│ [ Verifiera ]                        │
└─────────────────────────────────────┘
```
- [API] `GET /v1/api-keys/alias/{alias}`
- Validate `type` matches CASHIER / SCANNER

### D) Confirm Event (after code resolves)
```
┌─────────────────────────────────────┐
│ Kod hittade event:                  │
│  Event Name                         │
│  Datum / Plats                      │
│ [ Avbryt ]         [ Öppna ]        │
└─────────────────────────────────────┘
```
- [API] `GET /v1/events/{eventId}` (for name/details if not cached)

### E) Cashier Screen
```
┌─────────────────────────────────────┐
│ Event Name                          │
│ Säljare: [ 123 ]  Pris: [ 450 ]     │
│ [ Varukorg / Kvitto ]               │
│ [ Betala Kontant ] [ Betala Swish ] │
└─────────────────────────────────────┘
```
- [API] `POST /v1/events/{eventId}/vendors:filter` (load approved sellers)
- [API] `POST /v1/events/{eventId}/sold-items` (checkout upload)

### F) Scanner Screen
```
┌─────────────────────────────────────┐
│ Kamera / Skanna QR                  │
│ Senaste skannade:                   │
│  Ticket ID / Status                 │
│ [ Manuell inmatning ]               │
└─────────────────────────────────────┘
```
- [API] `POST /v1/events/{eventId}/visitor_tickets/{id}/scan`
- [API] `GET /v1/events/{eventId}/visitor_tickets/{id}` (optional detail)
- [API] `GET /v1/events/{eventId}/ticket_types` (optional labels)

### G) Back Behavior (Navigation)
- From Event Detail → returns to List (no API).
- From Cashier/Scanner → returns to List root (no API).
- From List root → exit app (Android).

## Legacy vs New API Notes
- New alias-only endpoint:
  - `GET /v1/api-keys/alias/{alias}` → returns `eventId`, `apiKey`, `type`, `isActive`.
- Legacy endpoint (still available):
  - `GET /v1/events/{eventId}/api-keys/alias/{alias}`
- For new UX flow (code first, event later), the alias-only endpoint is required.

## References
- PR #7 (Navigation new): https://github.com/goencoder/iloppis-app/pull/7
- UX notes from PR comments (Feb 2026)
