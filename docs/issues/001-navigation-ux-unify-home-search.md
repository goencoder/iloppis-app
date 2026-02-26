# iLoppis Mobile – Production UX/UI Specification

> **Version:** 2.0 · **Date:** 2025-07-26 · **Status:** Implementation-ready
> **Platforms:** Android (Kotlin / Jetpack Compose) · iOS (Swift / SwiftUI)

---

## 1  Purpose & Audience

iLoppis Mobile serves two audiences through one unified app:

| Audience | Primary tasks |
|----------|---------------|
| **Public users** | Browse events, search/filter, view details (markdown description, map, links), navigate to Google Maps, open iloppis.se for tickets / seller applications |
| **Event staff** | Enter a code to open Cashier (LoppisKassan) or Scanner (Biljettscanner) |

The app must feel modern, simple, and visually aligned with **iloppis.se**.

---

## 2  Brand & Visual Identity

### 2.1 Logo & Assets

| Asset | Android resource | iOS |
|-------|-----------------|-----|
| Logo (black) | `drawable/iloppis_logo_black` | Asset catalog `iLoppisLogo` |
| App icon 192 | `drawable/icon_192` | AppIcon set |
| App icon 512 | `drawable/icon_512` | — |

### 2.2 Color Palette (from `AppColors`)

| Token | Hex | Usage |
|-------|-----|-------|
| `SplashBackground` | `#4CAF50` | Splash screen background |
| `Background` | `#F5E8E9` | Main background |
| `CardBackground` | `#FAF3F4` | Event cards |
| `TextPrimary` | `#2D3748` | Body text |
| `Success` | `#4CAF50` | Pågående badge, success states |
| `Info` | `#2196F3` | Kommande badge, info states |
| `Error` | `#E53E3E` | Error states |
| `TextSecondary` | `#718096` | Secondary / muted text |

### 2.3 Typography & Spacing

Follow Material3 (Android) and native San Francisco (iOS) type scales.
All dimensions and spacing come from the shared theme files.

---

## 3  Core UX Principles

1. **No dead UI.** Every interactive element maps to a navigation action or a specific API call.
2. **No bottom navigation.** Single-screen architecture with push navigation – no tab bar needed.
3. **"Pågående" is computed, not a lifecycle state.** `startTime <= now <= endTime` → Pågående. The backend `lifecycleState` enum (`OPEN | CLOSED | FINALIZED`) is a separate concern (see §6.2).
4. **Back behavior is predictable.**
   - From detail → list
   - From tool → list root
   - From list root → exit app (Android)
5. **All text from string resources.** Swedish default, English secondary.
6. **All colors from `AppColors`.** No hardcoded hex in UI code.

---

## 4  Navigation Hierarchy

```
Splash Screen
  |  (auto-dismiss 1.5-2 s)
  v
Main Screen  <-- back-stack root
  |-- Event Detail Screen  (push)
  |     |-- [Navigera] -> Google Maps intent (external)
  |     |-- [Besok pa iLoppis.se] -> browser intent (external)
  |     +-- [Oppna kassa / Oppna skanner] -> Code Entry (push)
  |
  |-- Code Entry Screen  (push, from quick-access OR event detail)
  |     +-- Code Confirm Screen  (push, after alias resolve)
  |           +-- Cashier Screen  (replace stack to root + tool)
  |           +-- Scanner Screen  (replace stack to root + tool)
  |
  +-- (Android back from root -> exit app)
```

### 4.1 Back-Stack Behavior

| Current screen | Back action | Result |
|----------------|-------------|--------|
| Main Screen | System back | Exit app (Android) / no-op (iOS) |
| Event Detail | Back / close | Pop to Main Screen |
| Code Entry | Back | Pop to previous (Main or Event Detail) |
| Code Confirm | Back | Pop to Code Entry |
| Cashier | Back | Pop to Main Screen (clear tool stack) |
| Scanner | Back | Pop to Main Screen (clear tool stack) |

---

## 5  Screen Specifications

### 5.1 Splash Screen

**Purpose:** Brand moment, no data loading.

| Property | Value |
|----------|-------|
| Background | `AppColors.SplashBackground` (#4CAF50 green) |
| Content | iLoppis logo (centered) + tagline |
| Tagline | `"Tillsammans gor vi Sveriges basta loppisar!"` |
| Duration | 1.5-2 seconds, then fade-out to Main Screen |
| API calls | None |

The splash visual (logo + green bg at reduced opacity) may be reused as an empty-state background on the Main Screen when no events are loaded.

**String resources:**
- `splash_tagline` = "Tillsammans gor vi Sveriges basta loppisar!"

---

### 5.2 Main Screen (Unified Home + Search + List)

**Purpose:** Root screen. Search, filter, and browse events.

#### Layout (top to bottom)

```
+-------------------------------------+
|  iLoppis logo (compact, top bar)    |
+-------------------------------------+
|  +- Quick-access buttons ---------+ |
|  | [ Oppna kassa ]                | |
|  | [ Oppna skanner ]              | |
|  +--------------------------------+ |
+-------------------------------------+
|  [ Sok loppis...                  ] |
+-------------------------------------+
|  [ Alla ] [ Kommande ] [ Pagaende ] |
|  [ Forflutna ] [ * Sparade ]        |
+-------------------------------------+
|  Event Card                         |
|   Event Name            [PAGAENDE]  |
|   12 mar 2026, 10:00-15:00         |
|   Stockholm                         |
+-------------------------------------+
|  Event Card                         |
|   Event Name            [KOMMANDE]  |
|   20 mar 2026, 09:00-14:00         |
|   Goteborg                          |
+-------------------------------------+
```

#### Components

| Component | Behavior |
|-----------|----------|
| **Logo** | Static brand element in top bar. Not tappable. |
| **Quick-access buttons** | Two prominent buttons: "Oppna kassa" and "Oppna skanner". Navigate to Code Entry with respective `CodeEntryMode`. |
| **Search field** | Functional text input. Debounce 300 ms, then fires filter API. |
| **Filter chips** | Single-select. Active chip is visually highlighted. Triggers re-fetch or client-side filter. |
| **Event list** | Scrollable `LazyColumn` / `List`. Each card shows: name, computed status badge, formatted dates, location. Tap -> Event Detail. |
| **Empty state** | Splash logo at reduced opacity + message "Inga loppisar hittades". |
| **Loading state** | Skeleton cards or centered progress indicator. |
| **Error state** | Inline error message with retry button. |

#### Filter Chip Logic

| Chip | API call | Client filter |
|------|----------|---------------|
| **Alla** | `POST /v1/events:filter` with empty filter | — |
| **Kommande** | `POST /v1/events:filter` with `filter.lifecycleStates = ["OPEN"]` and `filter.dateFrom = now` | `startTime > now` |
| **Pagaende** | `POST /v1/events:filter` with `filter.lifecycleStates = ["OPEN"]` | `startTime <= now <= endTime` |
| **Forflutna** | `POST /v1/events:filter` with `filter.lifecycleStates = ["CLOSED", "FINALIZED"]` | — |
| **Sparade** | Load saved event IDs from local storage, then `GET /v1/events?eventIds={csv}` | Local ID list |

> **Note on Pagaende:** The backend has no "ongoing" lifecycle state. OPEN includes upcoming, ongoing, and ended-but-not-closed events. The client fetches OPEN events and filters to `startTime <= now <= endTime`.

> **Note on Kommande:** Fetch OPEN events with `dateFrom = now` to get events whose `endTime` is in the future, then client-filter to `startTime > now`.

#### Search

| Trigger | API |
|---------|-----|
| User types >= 2 characters (debounced 300 ms) | `POST /v1/events:filter` with `filter.searchText` and active chip's lifecycle filter |
| User clears search | Re-fetch with active chip filter only |

#### API Mapping

| Action | Endpoint | Auth |
|--------|----------|------|
| Initial load | `POST /v1/events:filter` `{ filter: {}, pagination: { pageSize: 50 } }` | None |
| Search | `POST /v1/events:filter` `{ filter: { searchText: "..." } }` | None |
| Filter by lifecycle | `POST /v1/events:filter` `{ filter: { lifecycleStates: [...] } }` | None |
| Load saved events | `GET /v1/events?eventIds=id1,id2,...` | None |

**String resources:**
- `search_placeholder` = "Sok loppis..."
- `filter_all` = "Alla"
- `filter_upcoming` = "Kommande"
- `filter_ongoing` = "Pagaende"
- `filter_past` = "Forflutna"
- `filter_saved` = "Sparade"
- `empty_state_message` = "Inga loppisar hittades"
- `open_cashier` = "Oppna kassa"
- `open_scanner` = "Oppna skanner"

---

### 5.3 Event Detail Screen

**Purpose:** Full-screen event details with public-facing actions.

#### Layout

```
+-------------------------------------+
|  [<-]  Event Name                   |
+-------------------------------------+
|  +-------------------------------+  |
|  |  Event Image (if available)   |  |
|  +-------------------------------+  |
|                                     |
|  Event Name                   [*]   |
|  [PAGAENDE]  (computed badge)       |
|                                     |
|  12 mars 2026, 10:00 - 15:00       |
|  Storgatan 5, Stockholm            |
|                                     |
|  -- Description ----------------    |
|  (Rendered Markdown)                |
|  ...                                |
|                                     |
|  +-------------------------------+  |
|  |  Map preview (static map)     |  |
|  +-------------------------------+  |
|                                     |
|  [ Navigera ]                       |
|  [ Besok pa iLoppis.se ]           |
|                                     |
|  -- Verktyg ---------------------   |
|  [ Oppna kassa ] [ Oppna skanner ] |
+-------------------------------------+
```

#### Data Mapping (from `v1Event`)

| UI element | API field | Notes |
|------------|-----------|-------|
| Event name | `name` | |
| Description | `description` | Render as Markdown if `descriptionIsMarkdown == true` |
| Start time | `startTime` | Format to locale date/time |
| End time | `endTime` | Format to locale date/time |
| Dates display | Computed | "12 mars 2026, 10:00 - 15:00" |
| Location | `addressStreet`, `addressCity` | Concatenated |
| Coordinates | `latitude`, `longitude` | For map + navigation |
| Status badge | Computed | See section 6.2 |
| Event image | `eventImage.imageUrl` | Show if `eventImage.hasImage == true` |

#### Interactions

| Element | Action |
|---------|--------|
| **Star (save toggle)** | Add/remove event ID to local storage. No API call (future: sync to preferences). |
| **Navigera** | Open Google Maps intent: `geo:{lat},{lng}?q={lat},{lng}(Event Name)` |
| **Besok pa iLoppis.se** | Open browser: `https://iloppis.se/?event={eventId}` |
| **Oppna kassa** | Push Code Entry with `mode = CASHIER` |
| **Oppna skanner** | Push Code Entry with `mode = SCANNER` |
| **Back** | Pop to Main Screen |

#### API Mapping

| Action | Endpoint | Auth |
|--------|----------|------|
| Load event | `GET /v1/events/{eventId}` | None |
| Load event image | `GET /v1/resources/event/{eventId}/ATTACHMENT_KIND_EVENT_IMAGE` | None (public download URL in event response) |

**String resources:**
- `event_detail_navigate` = "Navigera"
- `event_detail_website` = "Besok pa iLoppis.se"
- `event_detail_tools_section` = "Verktyg"
- `event_detail_save` = "Spara"
- `event_detail_unsave` = "Ta bort sparad"

---

### 5.4 Code Entry Screen (Tool Entry - Step 1)

**Purpose:** Staff enters a 6-character alias code (format: `XXX-YYY`) to access Cashier or Scanner.

#### Layout

```
+-------------------------------------+
|  [<-]  Ange kod                     |
+-------------------------------------+
|                                     |
|  Mode indicator:                    |
|  "Ange kassans kod" or              |
|  "Ange skannerns kod"              |
|                                     |
|  +- - - -+  -  +- - - -+           |
|  | X X X |  -  | Y Y Y |           |
|  +- - - -+     +- - - -+           |
|                                     |
|  [ Verifiera ]                      |
|                                     |
|  Error message (if any)             |
+-------------------------------------+
```

#### Behavior

1. Code input accepts exactly 6 alphanumeric characters, auto-formatted as `XXX-YYY`.
2. Paste support: strips dashes and whitespace, fills both fields.
3. **Verifiera** button enabled only when 6 characters entered.
4. On submit -> call alias-only endpoint.
5. On success -> validate `type` and `isActive`, then push Code Confirm.
6. On error -> show inline error (invalid code, inactive key, type mismatch).

#### Validation Logic

```
response = GET /v1/api-keys/alias/{alias}

if response.isActive == false:
  show "Koden ar inte aktiv"

if mode == CASHIER and response.type not in [API_KEY_TYPE_WEB_CASHIER, API_KEY_TYPE_DESKTOP_CASHIER]:
  show "Koden ar inte en kassakod"

if mode == SCANNER and response.type != API_KEY_TYPE_SCANNER:
  show "Koden ar inte en skannerkod"

else:
  navigate to CodeConfirm(eventId = response.eventId, apiKey = response.apiKey, mode)
```

#### API Mapping

| Action | Endpoint | Auth |
|--------|----------|------|
| Resolve alias | `GET /v1/api-keys/alias/{alias}` | None (public, rate-limited per IP) |

**Response schema (`v1GetApiKeyByAliasResponse`):**
```json
{
  "eventId": "string (UUID)",
  "alias": "string",
  "apiKey": "string",
  "isActive": true,
  "type": "API_KEY_TYPE_WEB_CASHIER | API_KEY_TYPE_SCANNER | API_KEY_TYPE_DESKTOP_CASHIER",
  "tags": ["string"],
  "id": "string"
}
```

**String resources:**
- `code_entry_title` = "Ange kod"
- `code_entry_cashier_hint` = "Ange kassans kod"
- `code_entry_scanner_hint` = "Ange skannerns kod"
- `code_entry_verify` = "Verifiera"
- `code_entry_error_inactive` = "Koden ar inte aktiv"
- `code_entry_error_wrong_type_cashier` = "Koden ar inte en kassakod"
- `code_entry_error_wrong_type_scanner` = "Koden ar inte en skannerkod"
- `code_entry_error_not_found` = "Koden hittades inte"

---

### 5.5 Code Confirm Screen (Tool Entry - Step 2)

**Purpose:** Confirm the resolved event before entering the tool.

#### Layout

```
+-------------------------------------+
|  [<-]  Bekrafta                     |
+-------------------------------------+
|                                     |
|  Kod hittade event:                 |
|                                     |
|  Event Name                         |
|  12 mars 2026, 10:00 - 15:00       |
|  Stockholm                          |
|                                     |
|  [ Avbryt ]       [ Oppna kassa ]   |
|                or [ Oppna skanner ] |
+-------------------------------------+
```

#### Behavior

1. Fetch event details to display name, dates, location.
2. User confirms -> navigate to Cashier or Scanner (replace back-stack: root + tool).
3. User cancels -> pop back to Code Entry.

#### API Mapping

| Action | Endpoint | Auth |
|--------|----------|------|
| Fetch event | `GET /v1/events/{eventId}` | None |

**String resources:**
- `code_confirm_title` = "Bekrafta"
- `code_confirm_found` = "Kod hittade event:"
- `code_confirm_cancel` = "Avbryt"
- `code_confirm_open_cashier` = "Oppna kassa"
- `code_confirm_open_scanner` = "Oppna skanner"

---

### 5.6 Cashier Screen

**Purpose:** Register sales for an event. Authenticated via API key from code entry.

#### Core Functionality

1. **Seller validation** - Cashier enters seller number, app validates against approved vendors.
2. **Item entry** - Seller number + price per item, grouped into purchases.
3. **Checkout** - Submit purchase (ULID-based `itemId` and `purchaseId`).
4. **Offline queue** - If network fails, items are queued locally and retried.
5. **Rejected purchase review** - Show rejected items and allow retry.

#### API Mapping

| Action | Endpoint | Auth | Notes |
|--------|----------|------|-------|
| Load approved vendors | `POST /v1/events/{eventId}/vendors:filter` `{ filter: { status: "approved" } }` | API key | Cached locally per session |
| Submit purchase | `POST /v1/events/{eventId}/sold-items` `{ items: [...] }` | API key | Bulk upload, idempotent via ULID |
| Retry rejected | `POST /v1/events/{eventId}/sold-items` | API key | `DUPLICATE_RECEIPT` = already uploaded (safe) |

**Request body (`SoldItemsServiceCreateSoldItemsBody`):**
```json
{
  "items": [
    {
      "itemId": "01KF9A3M5GT7F10X0GBT60MPDJ",
      "purchaseId": "01KF9A3M5GT7F10X0GBT60MPDK",
      "seller": 42,
      "price": 150,
      "paymentMethod": "KONTANT",
      "soldTime": "2026-03-12T11:30:00Z"
    }
  ]
}
```

**Response (`v1CreateSoldItemsResponse`):**
```json
{
  "acceptedItems": ["..."],
  "rejectedItems": [
    {
      "item": {"..."},
      "reason": "string",
      "errorCode": "SOLD_ITEM_ERROR_CODE_INVALID_SELLER | SOLD_ITEM_ERROR_CODE_DUPLICATE_RECEIPT"
    }
  ]
}
```

#### Back Behavior

Back from Cashier -> Pop to Main Screen (clear tool stack entirely).

---

### 5.7 Scanner Screen

**Purpose:** Scan visitor tickets (QR code or manual entry). Authenticated via API key from code entry.

#### Core Functionality

1. **Camera scan** - Read QR code containing ticket ID.
2. **Manual entry** - Text field for ticket ID.
3. **Scan result** - Show ticket status, type label, scan timestamp.
4. **History** - Recent scans in current session.

#### API Mapping

| Action | Endpoint | Auth | Notes |
|--------|----------|------|-------|
| Scan ticket | `POST /v1/events/{eventId}/visitor_tickets/{id}/scan` | API key | Body is empty `{}` |
| Get ticket details | `GET /v1/events/{eventId}/visitor_tickets/{id}` | API key | Optional, for detailed view |
| Load ticket types | `GET /v1/events/{eventId}/ticket_types` | None | For label resolution |

**Scan response (`v1ScanVisitorTicketResponse`):**
```json
{
  "ticket": {
    "id": "string",
    "eventId": "string",
    "ticketType": "Standard",
    "email": "visitor@example.com",
    "status": "TICKET_STATUS_SCANNED",
    "issuedAt": "2026-03-01T10:00:00Z",
    "scannedAt": "2026-03-12T10:05:00Z"
  }
}
```

#### Back Behavior

Back from Scanner -> Pop to Main Screen (clear tool stack entirely).

---

## 6  State Management

### 6.1 Architecture

| Platform | Pattern | State holder |
|----------|---------|-------------|
| Android | MVVM + `StateFlow` | `ViewModel` per screen, immutable `UiState` data class |
| iOS | MVVM + `@Published` | `ObservableObject` per screen, immutable state struct |

Unidirectional data flow: **UI -> Action/Intent -> ViewModel -> new State -> UI**.

### 6.2 Computed Event Display Status

The backend `lifecycleState` values are: `OPEN`, `CLOSED`, `FINALIZED`.
These represent **administrative** state, not temporal state.

The **display status** shown to users is computed client-side:

```kotlin
enum class EventDisplayStatus {
    ONGOING,    // "Pagaende"   - startTime <= now <= endTime AND lifecycleState == OPEN
    UPCOMING,   // "Kommande"   - now < startTime AND lifecycleState == OPEN
    PAST,       // "Forfluten"  - now > endTime OR lifecycleState in [CLOSED, FINALIZED]
}

fun Event.displayStatus(): EventDisplayStatus {
    val now = Instant.now()
    return when {
        lifecycleState in listOf(CLOSED, FINALIZED) -> PAST
        startTime != null && endTime != null
            && now >= startTime && now <= endTime -> ONGOING
        startTime != null && now < startTime -> UPCOMING
        else -> PAST
    }
}
```

### 6.3 State Files

Each screen has an associated state file:

| Screen | State file | Key state properties |
|--------|-----------|---------------------|
| Main | `EventListState` | `events: List<Event>`, `isLoading: Boolean`, `error: String?`, `searchQuery: String`, `activeFilter: FilterChip`, `savedEventIds: Set<String>` |
| Event Detail | `EventDetailState` | `event: Event?`, `isLoading: Boolean`, `isSaved: Boolean` |
| Code Entry | `CodeEntryState` | `code: String`, `mode: CodeEntryMode`, `isLoading: Boolean`, `error: String?` |
| Code Confirm | `CodeConfirmState` | `event: Event?`, `apiKey: String`, `mode: CodeEntryMode`, `isLoading: Boolean` |
| Cashier | `CashierState` | `event: Event`, `apiKey: String`, `approvedSellers: List<Vendor>`, `cart: List<SoldItem>`, `pendingUploads: List<SoldItem>`, `rejectedItems: List<RejectedItem>` |
| Scanner | `ScannerState` | `event: Event`, `apiKey: String`, `ticketTypes: List<TicketType>`, `recentScans: List<ScanResult>` |

### 6.4 Local Storage

| Data | Storage | Scope |
|------|---------|-------|
| Saved event IDs | SharedPreferences / UserDefaults | Persistent |
| Pending sold items (offline) | JSONL file per event | Persistent until uploaded |
| Rejected purchases | JSONL file per event | Persistent until resolved |
| Approved vendor cache | In-memory | Per Cashier session |

---

## 7  UI Component Library

### 7.1 Shared Components

| Component | Purpose | Platforms |
|-----------|---------|-----------|
| `EventCard` | List item showing event name, dates, location, status badge | Android + iOS |
| `StatusBadge` | Pill-shaped label: Pagaende (green), Kommande (blue), Forfluten (gray) | Android + iOS |
| `FilterChipRow` | Horizontally scrollable single-select chip group | Android + iOS |
| `SearchBar` | Text input with search icon and clear button | Android + iOS |
| `QuickAccessButton` | Prominent action button with icon (Cashier / Scanner) | Android + iOS |
| `CodeInputField` | 6-char input with auto-dash formatting and paste support | Android + iOS |
| `ConfirmCard` | Event summary card used in Code Confirm screen | Android + iOS |
| `MapPreview` | Static map image or embedded map showing event location | Android + iOS |
| `MarkdownRenderer` | Renders event description from Markdown to native rich text | Android + iOS |
| `ErrorBanner` | Inline error message with optional retry action | Android + iOS |
| `LoadingIndicator` | Centered circular progress or skeleton placeholder | Android + iOS |
| `EmptyState` | Centered message with faded splash logo | Android + iOS |

### 7.2 Screen-Specific Components

| Component | Screen | Purpose |
|-----------|--------|---------|
| `SellerInput` | Cashier | Numeric input for seller number with validation feedback |
| `PriceInput` | Cashier | Numeric input for item price |
| `CartList` | Cashier | Scrollable list of items in current purchase |
| `PaymentMethodSelector` | Cashier | Toggle: Kontant / Swish |
| `ScanResultCard` | Scanner | Shows ticket ID, type, status, scan time |
| `CameraScanView` | Scanner | Camera viewfinder for QR scanning |
| `ManualTicketInput` | Scanner | Text field for manual ticket ID entry |

---

## 8  Complete API Mapping

### 8.1 Public Endpoints (no auth)

| Endpoint | Method | Used by | Trigger |
|----------|--------|---------|---------|
| `/v1/events:filter` | POST | Main Screen | Initial load, search, filter chips |
| `/v1/events` | GET | Main Screen | Load saved events by CSV IDs |
| `/v1/events/{eventId}` | GET | Event Detail, Code Confirm | Tap event card, after alias resolve |
| `/v1/api-keys/alias/{alias}` | GET | Code Entry | Verify code button |
| `/v1/events/{eventId}/ticket_types` | GET | Scanner | On scanner session start |

### 8.2 Authenticated Endpoints (API key from alias resolution)

| Endpoint | Method | Used by | Trigger |
|----------|--------|---------|---------|
| `/v1/events/{eventId}/vendors:filter` | POST | Cashier | Session start (load approved sellers) |
| `/v1/events/{eventId}/sold-items` | POST | Cashier | Checkout / retry |
| `/v1/events/{eventId}/visitor_tickets/{id}/scan` | POST | Scanner | QR scan / manual entry |
| `/v1/events/{eventId}/visitor_tickets/{id}` | GET | Scanner | Ticket detail (optional) |

### 8.3 Legacy Endpoint (not used in new flow)

| Endpoint | Status |
|----------|--------|
| `GET /v1/events/{eventId}/api-keys/alias/{alias}` | **Deprecated.** Use alias-only `GET /v1/api-keys/alias/{alias}` instead. |

---

## 9  Removed Elements

The following elements from the previous implementation are explicitly removed:

| Element | Reason |
|---------|--------|
| Static "Home" label/screen | Merged into Main Screen |
| Bottom navigation bar | Single-screen root makes tab bar unnecessary |
| Separate EventSelectionScreen | Tool entry now uses direct code input |
| Non-functional search field | Search is now fully wired to `POST /v1/events:filter` |
| Non-functional filter chips | All chips mapped to real API calls or client-side filters |
| Empty Library tab | Replaced by "Sparade" filter chip |
| "OPEN" as a display status | Replaced by computed Pagaende / Kommande / Forfluten |

---

## 10  Error Handling

| Scenario | Behavior |
|----------|----------|
| Network error (event list) | Show `ErrorBanner` with retry button. Cached data stays visible if available. |
| Network error (code verify) | Show inline error on Code Entry screen. |
| Invalid alias code | Show "Koden hittades inte" error. |
| Inactive API key | Show "Koden ar inte aktiv" error. |
| Type mismatch (cashier code on scanner) | Show "Koden ar inte en skannerkod/kassakod" error. |
| Invalid seller number | Cashier shows inline warning, prevents adding to cart. |
| Duplicate receipt | Silently accept (idempotent). Log for audit. |
| Offline (Cashier) | Queue items to JSONL. Show pending count. Auto-retry on reconnect. |

---

## 11  Platform-Specific Notes

### 11.1 Android

- **Navigation:** Navigation3 (`NavDisplay` / `NavEntry`) with `ScreenPage` sealed class.
- **Splash:** Use `SplashScreen` API (Android 12+) or custom composable for older versions.
- **Markdown:** Use `compose-markdown` library or custom `AnnotatedString` renderer.
- **Maps:** Google Maps SDK `MapView` composable for preview; `Intent(ACTION_VIEW, Uri.parse("geo:..."))` for navigation.
- **QR:** CameraX + ML Kit barcode scanner.
- **ULID:** `com.guepardoapps:kulid:2.0.0.0` for generating item/purchase IDs.

### 11.2 iOS

- **Navigation:** `NavigationStack` with typed destinations.
- **Splash:** Custom `View` with `onAppear` timer -> navigation to main.
- **Markdown:** Native `Text` with `AttributedString` from Markdown (iOS 15+).
- **Maps:** `MapKit` for preview; `MKMapItem.openInMaps()` for navigation.
- **QR:** `AVCaptureSession` with `AVMetadataObjectTypeQRCode`.
- **ULID:** Swift ULID package or custom implementation.

---

## 12  Testing Scenarios

| # | Scenario | Expected |
|---|----------|----------|
| 1 | App launch | Splash -> fade to Main Screen with event list loaded |
| 2 | Search "Stockholm" | Events filtered, cards update in real-time |
| 3 | Tap "Pagaende" chip | Only events where `startTime <= now <= endTime` shown |
| 4 | Tap event card | Push to Event Detail with full data |
| 5 | Tap "Navigera" | Google Maps opens with event coordinates |
| 6 | Tap "Besok pa iLoppis.se" | Browser opens `https://iloppis.se/?event={id}` |
| 7 | Tap "Oppna kassa" -> enter valid code | Code Entry -> Code Confirm -> Cashier |
| 8 | Enter invalid code | Error: "Koden hittades inte" |
| 9 | Enter scanner code in cashier mode | Error: "Koden ar inte en kassakod" |
| 10 | Cashier: submit purchase offline | Items queued, auto-submitted on reconnect |
| 11 | Scanner: scan valid QR | Ticket marked as scanned, result displayed |
| 12 | Back from Event Detail | Returns to Main Screen with scroll position preserved |
| 13 | Back from Cashier | Returns to Main Screen (stack cleared) |
| 14 | Orientation change | All screens adapt; no data loss |

---

## 13  File Mapping

| Spec section | Android files | iOS files |
|-------------|---------------|-----------|
| Splash | `ui/screens/splash/SplashScreen.kt` | `UI/Screens/Splash/SplashScreen.swift` |
| Main Screen | `ui/screens/events/EventListScreen.kt`, `EventListState.kt`, `EventListViewModel.kt` | `UI/Screens/Events/EventListScreen.swift`, `EventListState.swift`, `EventListViewModel.swift` |
| Event Detail | `ui/screens/events/EventDetailScreen.kt`, `EventDetailState.kt`, `EventDetailViewModel.kt` | `UI/Screens/Events/EventDetailScreen.swift`, etc. |
| Code Entry | `ui/screens/events/CodeEntryScreen.kt`, `CodeEntryState.kt`, `CodeEntryViewModel.kt` | `UI/Screens/Events/CodeEntryScreen.swift`, etc. |
| Code Confirm | `ui/screens/events/CodeConfirmScreen.kt`, `CodeConfirmState.kt` | `UI/Screens/Events/CodeConfirmScreen.swift`, etc. |
| Cashier | `ui/screens/cashier/CashierScreen.kt`, `CashierState.kt`, `CashierViewModel.kt` | `UI/Screens/Cashier/CashierScreen.swift`, etc. |
| Scanner | `ui/screens/scanner/ScannerScreen.kt`, `ScannerState.kt`, `ScannerViewModel.kt` | `UI/Screens/Scanner/ScannerScreen.swift`, etc. |
| Components | `ui/components/` | `UI/Components/` |
| Theme | `ui/theme/AppColors.kt` | `UI/Theme/AppColors.swift` |
| Domain | `domain/model/Event.kt`, `domain/mapper/EventMapper.kt` | `Domain/Model/Event.swift`, `Domain/Mapper/EventMapper.swift` |
| Network | `network/events/EventApiObjects.kt`, `network/ApiClient.kt` | `Network/ApiClient.swift` |

---

## References

- PR #7 (Navigation): https://github.com/goencoder/iloppis-app/pull/7
- OpenAPI spec: https://iloppis.se/api/openapi.json
- Backend lifecycle states: `OPEN -> CLOSED -> FINALIZED` (administrative, not temporal)
- ULID spec: https://github.com/ulid/spec
