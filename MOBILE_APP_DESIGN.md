# iLoppis Mobile App - Design Document

**Purpose:** Complete reference for understanding the mobile app architecture, API interactions, and offline support  
**Version:** 1.0  
**Last Updated:** January 18, 2026

---

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Internationalization](#internationalization)
3. [Application Startup](#application-startup)
4. [Cashier View (Kassa)](#cashier-view-kassa)
5. [Scanner View (Biljettskanner)](#scanner-view-biljettskanner)
6. [Offline Support](#offline-support)
7. [Local Storage](#local-storage)
8. [API Reference](#api-reference)

---

## Architecture Overview

The app follows **Clean Architecture + MVVM + Unidirectional Data Flow**:

```
┌─────────────────────────────────────────────┐
│          UI Layer (Screens)                 │
│  EventListScreen | CashierScreen | Scanner  │
└──────────────┬──────────────────────────────┘
               │ immutable state + actions
┌──────────────▼──────────────────────────────┐
│          ViewModels                         │
│  Business Logic + State Management          │
└──────────────┬──────────────────────────────┘
               │ domain models
┌──────────────▼──────────────────────────────┐
│          Domain Layer                       │
│  Pure data models (Event, Vendor, etc.)     │
└──────────────┬──────────────────────────────┘
               │ DTOs
┌──────────────▼──────────────────────────────┐
│          Network + Data Layer               │
│  ApiClient | FileStores | Repositories      │
└─────────────────────────────────────────────┘
```

**Key Principles:**
- **Screens** are stateless, receive data via props
- **ViewModels** handle all business logic, expose immutable state
- **File stores** provide offline-first persistence
- **Repositories** cache data and manage refresh logic

---

## Internationalization

### String Resources (CRITICAL)

**Rule:** ALL user-facing text MUST come from string resources - NEVER hardcode text in UI code.

**Why:** Enables language switching between Swedish (default) and English without code changes.

**Android Implementation:**

```kotlin
// ✅ CORRECT - Use string resources
Text(text = stringResource(R.string.event_list_title))
Button(onClick = { /*...*/ }) {
    Text(stringResource(R.string.button_open_cashier))
}

// ❌ WRONG - Hardcoded text
Text(text = "Öppna kassa")
Text(text = "Open cashier")
```

**File Structure:**
```
app/src/main/res/
├── values/strings.xml           # Swedish (default)
└── values-en/strings.xml        # English
```

**Example (strings.xml - Swedish):**
```xml
<resources>
    <string name="event_list_title">Välj event</string>
    <string name="button_open_cashier">Öppna kassa</string>
    <string name="button_scan_tickets">Scanna biljetter</string>
    <string name="error_invalid_code">Ogiltig kod</string>
</resources>
```

**Example (strings.xml - English):**
```xml
<resources>
    <string name="event_list_title">Select event</string>
    <string name="button_open_cashier">Open cashier</string>
    <string name="button_scan_tickets">Scan tickets</string>
    <string name="error_invalid_code">Invalid code</string>
</resources>
```

**iOS Implementation:**

```swift
// ✅ CORRECT - Use localized strings
Text(LocalizedStringKey("event_list_title"))
Button("button_open_cashier") { /*...*/ }

// ❌ WRONG - Hardcoded text
Text("Öppna kassa")
```

**File Structure:**
```
iLoppis/
├── Localizable.strings          # Swedish (default)
└── Localizable (English).strings # English
```

**Language Detection:**
- System automatically selects language based on device settings
- Fallback to Swedish if device language not supported

**Testing Language Switch:**
```bash
# Android - Change language via adb
adb shell am start -a android.settings.LOCALE_SETTINGS

# iOS - Change in Simulator settings
Device → Language → English/Svenska
```

---

## Application Startup

### Flow Diagram

```
App Launch (MainActivity.kt)
    │
    ├─> EventListViewModel.init()
    │   └─> loadEvents()
    │
    └─> EventListScreen (Compose UI)
        │
        ├─> Show loading spinner
        │
        └─> API: POST /v1/events:filter
            {
              "filter": {
                "dateFrom": "2026-01-18T00:00:00Z",
                "lifecycleStates": ["OPEN"]
              }
            }
            │
            ├─ SUCCESS → Display event list
            │
            └─ ERROR → Show error message
```

### API Interaction

**Endpoint:** `POST /v1/events:filter`  
**Authorization:** None (public endpoint)  
**Request Body:**
```json
{
  "filter": {
    "dateFrom": "2026-01-18T00:00:00Z",
    "lifecycleStates": ["OPEN"]
  },
  "pagination": {}
}
```

**Response:**
```json
{
  "events": [
    {
      "id": "evt_abc123",
      "name": "Stockholms Vinterloppis",
      "startTime": "2026-01-25T10:00:00Z",
      "endTime": "2026-01-25T16:00:00Z",
      "addressCity": "Stockholm",
      "lifecycleState": "OPEN"
    }
  ]
}
```

**Code Location:** `EventListViewModel.kt:50-80`

**User Actions:**
1. User sees list of open events
2. Taps event → Event detail dialog opens
3. Chooses "Öppna kassa" (cashier) or "Biljettskanner" (scanner)
4. Prompted to enter role-specific code (kassakod)

---

## Cashier View (Kassa)

### 1. Code Entry & API Key Exchange

**Flow:**
```
User enters code: "ABC-123"
    │
    └─> API: GET /v1/events/{eventId}/api-keys/alias/ABC-123
        │
        ├─ SUCCESS → Store API key locally
        │            Navigate to CashierScreen
        │
        └─ ERROR → Show "Invalid code" message
```

**API Call:**
- **Endpoint:** `GET /v1/events/{eventId}/api-keys/alias/{code}`
- **Authorization:** None (code exchange is public)
- **Response:**
  ```json
  {
    "apiKey": "test_abc123xyz...",
    "alias": "ABC-123",
    "isActive": true,
    "type": "API_KEY_TYPE_WEB_CASHIER"
  }
  ```

**Storage:** API key stored in memory (not persisted between app restarts)  
**Code Location:** `EventListViewModel.kt:114-160`

---

### 2. Vendor List Fetch

**When:** On CashierViewModel initialization

**API Call:**
- **Endpoint:** `GET /v1/events/{eventId}/vendors?pageSize=100`
- **Authorization:** `Bearer {apiKey}`
- **Pagination:** Automatic (fetches all pages until `nextPageToken` is null)

**Response:**
```json
{
  "vendors": [
    {
      "id": "vnd_001",
      "sellerNumber": 10,
      "firstName": "Anna",
      "lastName": "Andersson",
      "status": "approved"
    }
  ],
  "nextPageToken": "page_token_xyz"
}
```

**Caching:** `VendorRepository` (singleton) caches vendor numbers for validation  
**Code Location:** `CashierViewModel.kt:184-210`

---

### 3. Recording Sales (POST Sold Items)

**Flow:**
```
User enters seller: "42"
User enters prices: "50 75 30"
User presses "Slutför köp" (Complete Purchase)
    │
    ├─> Generate client IDs (crucial for offline support)
    │   - purchaseId = ULID (groups items in same purchase)
    │   - itemId = ULID (unique per item)
    │
    ├─> Validate seller exists (local check via VendorRepository)
    │
    ├─> STEP 1: Save to local JSONL file FIRST
    │   └─> PendingItemsStore.appendItems() [BLOCKS until write completes]
    │       File: context.filesDir/events/{eventId}/pending_items.jsonl
    │
    ├─> STEP 2: Clear UI fields immediately (optimistic UI)
    │   └─> Empty seller/price fields, clear transaction list
    │       Ready for next purchase - no receipt shown
    │
    └─> STEP 3: Trigger background upload (WorkManager)
        └─> Best-effort sync, retries automatically on failure
```

**Critical Fields:**

| Field | Source | Purpose |
|-------|--------|---------|
| `itemId` | **Client-generated ULID** | Unique item identifier for idempotency |
| `purchaseId` | **Client-generated ULID** | Groups items in same purchase |
| `seller` | User input | Seller number (validated locally) |
| `price` | User input | Price in SEK (whole number) |
| `paymentMethod` | User selection | "SWISH" or "KONTANT" |
| `soldTime` | Current timestamp | ISO-8601 format |

**API Call:**
- **Endpoint:** `POST /v1/events/{eventId}/sold-items`
- **Authorization:** `Bearer {apiKey}`
- **Request Body:**
  ```json
  {
    "items": [
      {
        "itemId": "01HW1K2M3N4P5Q6R7S8T9V0WX1",
        "purchaseId": "01HW1K2M3N4P5Q6R7S8T9V0WXY",
        "seller": 42,
        "price": 50,
        "paymentMethod": "SWISH"
      },
      {
        "itemId": "01HW1K2M3N4P5Q6R7S8T9V0WX2",
        "purchaseId": "01HW1K2M3N4P5Q6R7S8T9V0WXY",
        "seller": 42,
        "price": 75,
        "paymentMethod": "SWISH"
      }
    ]
  }
  ```

**Response (Success):**
```json
{
  "acceptedItems": [
    {
      "itemId": "01HW1K2M3N4P5Q6R7S8T9V0WX1",
      "purchaseId": "01HW1K2M3N4P5Q6R7S8T9V0WXY",
      "seller": 42,
      "price": 50,
      "paymentMethod": "SWISH",
      "soldTime": "2026-01-18T14:30:00Z"
    }
  ],
  "rejectedItems": []
}
```

**Response (Partial Failure):**
```json
{
  "acceptedItems": [
    /* items that succeeded */
  ],
  "rejectedItems": [
    {
      "item": {
        "itemId": "01HW1K2M3N4P5Q6R7S8T9V0WX2",
        "seller": 999
      },
      "reason": "seller 999 is not approved",
      "errorCode": "INVALID_SELLER"
    }
  ]
}
```

**Code Location:** `CashierViewModel.kt:600-660`

---

### 4. Re-upload Support & Client-Side Deduplication

**Why Client-Generated IDs?**

1. **Idempotency:** Same item uploaded twice → backend rejects second attempt with "duplicate item"
2. **Offline Support:** Items saved locally before upload, can retry later
3. **Emergency Recovery:** If app crashes, items in local file can be re-uploaded

**ULID Format:**
- 26 uppercase alphanumeric characters (0-9, A-Z excluding I, L, O, U)
- Time-ordered: first 10 chars encode timestamp (millisecond precision)
- Example: `01KF9A3M5GT7F10X0GBT60MPDJ`
- Android library: Add `implementation("com.guepardoapps:kulid:2.0.0.0")` to build.gradle

**Deduplication Strategy:**

```kotlin
// Client generates ULID BEFORE network call
// Use okio-ulid or similar library: https://github.com/guepardoapps/okio-ulid
val itemId = Ulid.randomULID()

// Save to local file
PendingItemsStore.appendItems(listOf(
    PendingItem(itemId, purchaseId, seller, price, ...)
))

// Upload to backend (may fail, can retry)
api.createSoldItems(...)

// If retry: backend checks if itemId exists
// → Already exists? Reject with "duplicate item" (safe to mark uploaded)
// → Doesn't exist? Accept item
```

**Backend Guarantee:** MongoDB unique index on `(event_id, item_id)` prevents duplicates

**Code Location:** `CashierViewModel.kt:620-640`, `SoldItemsSyncWorker.kt:50-100`

---

## Scanner View (Biljettskanner)

### 1. Code Entry (Same as Cashier)

**Flow:** Identical to cashier code entry (see section above)

**API Endpoint:** `GET /v1/events/{eventId}/api-keys/alias/{code}`

**Storage:** API key stored in memory for session

---

### 2. Ticket Scanning

**Visual Feedback (CRITICAL for User Experience):**

Scanner provides instant, clear visual feedback using three elements:

1. **Border Flash (around camera view):**
   - **✅ Green border** (thick ~8dp): Successful scan
   - **❌ Red border** (thick ~8dp): Error/duplicate scan
   - Duration: 500ms flash animation

2. **Party Counter (overlayed on camera view):**
   - Large number (120sp) centered on camera preview
   - Shows ONLY during successful scan (flashes with green border)
   - Displays current party count: "3" = 3rd ticket for this party
   - Party = group of tickets with same email address
   - Semi-transparent (75% opacity) to not block camera view
   - Disappears after border animation completes
   - NOT shown on error/duplicate scans (only red border)

3. **Status Message (below counter):**
   - Success: "Scannad ✓" (green text)
   - Duplicate: "Redan scannad!" (red text)
   - Offline: "Scannad (offline mode)" (yellow text)


**Flow:**
```
User scans QR code with ticket ID: "TKT-12345"
    │
    ├─> Check if already scanned in current group (local dedup)
    │   └─> Duplicate? 
    │       ├─ Flash RED border
    │       ├─ Show "Redan scannad!" 
    │       └─ Keep party counter unchanged
    │
    ├─> ONLINE MODE:
    │   └─> API: POST /v1/events/{eventId}/tickets/{ticketId}/scan
    │       │
    │       ├─ SUCCESS (200):
    │       │  ├─ Save to committed_scans.jsonl (wasOffline=false)
    │       │  ├─ Flash GREEN border (500ms)
    │       │  ├─ Increment party counter if same email
    │       │  └─ Show "Scannad ✓" (green)
    │       │
    │       ├─ ALREADY SCANNED (412):
    │       │  ├─ Flash RED border
    │       │  └─ Show "Redan scannad!" (red)
    │       │
    │       └─ TIMEOUT (5 seconds) or NETWORK ERROR:
    │          └─> Switch to OFFLINE MODE
    │
    └─> OFFLINE MODE:
        ├─> Check if already scanned (read committed_scans.jsonl)
        │   └─> Duplicate? 
        │       ├─ Flash RED border
        │       └─ Show "Redan scannad (offline)" (red)
        │
        └─> New scan:
            ├─> Save to pending_scans.jsonl
            ├─> Save to committed_scans.jsonl (wasOffline=true)
            ├─> Flash GREEN border (500ms)
            ├─> Increment party counter if same email
            ├─> Show "Scannad (offline mode)" (yellow)
            └─> Background sync will upload later
```

**Party Grouping Logic:**
```kotlin
// Group tickets by email for party counter
data class ScannerState(
    val lastScannedEmail: String? = null,
    val currentPartyCount: Int = 0,
    val totalScansToday: Int = 0
)

fun updatePartyCounter(newTicketEmail: String, currentState: ScannerState): ScannerState {
    return if (newTicketEmail == currentState.lastScannedEmail) {
        // Same party - increment counter
        currentState.copy(
            currentPartyCount = currentState.currentPartyCount + 1,
            totalScansToday = currentState.totalScansToday + 1
        )
    } else {
        // New party - reset counter to 1
        currentState.copy(
            lastScannedEmail = newTicketEmail,
            currentPartyCount = 1,
            totalScansToday = currentState.totalScansToday + 1
        )
    }
}
```

**API Call (Online):**
- **Endpoint:** `POST /v1/events/{eventId}/tickets/{ticketId}/scan`
- **Authorization:** `Bearer {apiKey}`
- **Request Body:** (empty)

**Response:**
```json
{
  "ticket": {
    "id": "TKT-12345",
    "ticketType": "Vuxen",
    "email": "customer@example.com",
    "status": "SCANNED"
  }
}
```

**Code Location:** `ScannerViewModel.kt:200-290`

---

### 3. Offline Duplicate Detection

**Problem:** How to detect duplicate scans when offline?

**Solution:** Two-file system:

1. **pending_scans.jsonl** - Scans waiting to be uploaded
2. **committed_scans.jsonl** - All scans (online + offline), used for dedup

**Deduplication Logic:**
```kotlin
suspend fun handleOfflineScan(ticketId: String) {
    // Check committed_scans.jsonl for this ticketId
    if (CommittedScansStore.hasTicket(ticketId)) {
        // Already scanned (even if offline)
        showResult("Redan scannad (offline)")
        return
    }
    
    // New scan: Save to BOTH files
    PendingScansStore.appendScan(scan)
    CommittedScansStore.appendScan(scan.copy(wasOffline = true))
    
    showResult("Scannad offline - synkas automatiskt")
}
```

**Code Location:** `ScannerViewModel.kt:300-340`

---

## Offline Support

### Cashier Offline Support

**Storage:** `context.filesDir/events/{eventId}/pending_items.jsonl`

**Format (JSONL - JSON Lines):**
```jsonl
{"itemId":"01KF9A3M5GT7F10X0GBT60MPDJ","purchaseId":"01KF9A3M5GT7F10X0GBQDGX383","sellerId":42,"price":50,"errorText":"","timestamp":"2026-01-18T14:30:00Z"}
{"itemId":"01KF9A3M5GT7F10X0GBZF5KY1S","purchaseId":"01KF9A3M5GT7F10X0GBV8HN294","sellerId":99,"price":75,"errorText":"seller 99 is not approved","timestamp":"2026-01-18T14:31:00Z"}
```

**Flow:**

```
1. User completes purchase
   └─> Items saved to pending_items.jsonl (errorText="")

2. WorkManager wakes up (constraints: network connected)
   └─> SoldItemsSyncWorker.doWork()
       │
       ├─> Read all items from pending_items.jsonl
       ├─> Group by purchaseId
       ├─> Upload oldest purchase first
       │
       ├─ SUCCESS:
       │  └─> Delete accepted items from JSONL
       │      (read all, filter out, rewrite file)
       │
       ├─ REJECTED (duplicate item):
       │  └─> Delete from JSONL (already in backend, safe)
       │
       ├─ REJECTED (invalid seller):
       │  └─> Update errorText in JSONL
       │      Copy to pending_review.json for user attention
       │
       └─ NETWORK ERROR:
          └─> Keep in JSONL (errorText="")
              Retry automatically on next sync
```

**Retry Strategy:**
- **Immediate sync:** After checkout, if network available
- **Periodic sync:** Every 15 minutes (WorkManager)
- **Exponential backoff:** 5s, 10s, 20s, max 60s
- **Manual trigger:** User can pull-to-refresh

**Code Location:** `SoldItemsSyncWorker.kt:28-120`

---

### Scanner Offline Support

**Storage:** 
- `context.filesDir/events/{eventId}/pending_scans.jsonl` (upload queue)
- `context.filesDir/events/{eventId}/committed_scans.jsonl` (deduplication)

**Format:**
```jsonl
// pending_scans.jsonl
{"scanId":"scan-1","ticketId":"TKT-123","eventId":"evt_abc","scannedAt":"2026-01-18T14:30:00Z"}

// committed_scans.jsonl
{"scanId":"scan-1","ticketId":"TKT-123","eventId":"evt_abc","scannedAt":"2026-01-18T14:30:00Z","committedAt":"2026-01-18T14:30:00Z","wasOffline":true}
```

**Why Two Files?**

| File | Purpose | When Updated |
|------|---------|--------------|
| `pending_scans.jsonl` | Upload queue | Row added on offline scan, deleted after successful upload |
| `committed_scans.jsonl` | Dedup check | Row added on every scan (online + offline), never deleted |

**Offline Scan Flow:**

```
1. Network error detected
   └─> Switch to offline mode

2. User scans ticket "TKT-456"
   │
   ├─> Check committed_scans.jsonl
   │   └─> Already exists? Show "Redan scannad (offline)"
   │
   └─> New scan:
       ├─> Append to pending_scans.jsonl
       ├─> Append to committed_scans.jsonl (wasOffline=true)
       └─> Show yellow "Offline" badge in UI

3. Background sync (ScanSyncWorker)
   │
   ├─> Read pending_scans.jsonl
   ├─> Upload each scan to backend
   │
   ├─ SUCCESS:
   │  └─> Delete from pending_scans.jsonl
   │      Update committed_scans.jsonl (wasOffline=false)
   │
   └─ NETWORK ERROR:
      └─> Keep in pending_scans.jsonl, retry later
```

**Duplicate Detection (Offline):**

```kotlin
// Before scanning
val alreadyScanned = CommittedScansStore.hasTicket(ticketId)

if (alreadyScanned) {
    // Can be online OR offline scan
    showResult("Redan scannad (offline)")
} else {
    // New scan, save to both files
    performOfflineScan(ticketId)
}
```

**Code Location:** `ScannerViewModel.kt:300-350`, `PendingScansStore.kt`, `CommittedScansStore.kt`

---

## Local Storage

### File Layout

```
/data/data/se.iloppis.app/files/
└── events/
    └── {eventId}/
        ├── pending_items.jsonl       # Cashier: Items awaiting upload
        ├── pending_review.json        # Cashier: Items needing manual review
        ├── sold_items.json            # Cashier: Archive (deprecated, kept for migration)
        ├── pending_scans.jsonl        # Scanner: Scans awaiting upload
        └── committed_scans.jsonl      # Scanner: All scans (for deduplication)
```

**Multi-Event Support:** Each event gets its own subdirectory

**File Formats:**

| Format | Files | Reason |
|--------|-------|--------|
| **JSONL** (JSON Lines) | `pending_items.jsonl`, `pending_scans.jsonl`, `committed_scans.jsonl` | Append-only, efficient for queues |
| **JSON** (single object) | `pending_review.json`, `sold_items.json` | Full read/write, small datasets |

**Thread Safety:**
- **Mutex locks** (`kotlinx.coroutines.sync.Mutex`) protect file access
- All writes are atomic (complete or fail, no partial writes)
- Crash-safe: If app crashes during write, file remains in previous valid state

---

### PendingItemsStore (Cashier)

**File:** `context.filesDir/events/{eventId}/pending_items.jsonl`

**Operations:**

```kotlin
// Append items (thread-safe)
suspend fun appendItems(items: List<PendingItem>)

// Read all items
suspend fun readAll(): List<PendingItem>

// Delete specific items
suspend fun deleteItems(itemIds: Set<String>)

// Update errorText for items
suspend fun updateErrorText(itemId: String, errorText: String)
```

**Data Structure:**
```kotlin
data class PendingItem(
    val itemId: String,        // ULID (client-generated)
    val purchaseId: String,    // ULID (groups items in same purchase)
    val sellerId: Int,         // Seller number
    val price: Int,            // Price in SEK
    val errorText: String,     // "" = pending, text = error
    val timestamp: String      // ISO-8601
)
```

**Code Location:** `PendingItemsStore.kt`

---

### PendingScansStore (Scanner)

**File:** `context.filesDir/events/{eventId}/pending_scans.jsonl`

**Operations:**

```kotlin
// Append scan (thread-safe)
suspend fun appendScan(scan: PendingScan)

// Read all scans
suspend fun getAllScans(): List<PendingScan>

// Delete specific scans
suspend fun deleteScans(scanIds: Set<String>)
```

**Data Structure:**
```kotlin
data class PendingScan(
    val scanId: String,    // ULID (client-generated)
    val ticketId: String,  // Ticket identifier from QR code
    val eventId: String,   // Event UUID
    val scannedAt: String  // ISO-8601
)
```

**Code Location:** `PendingScansStore.kt`

---

### CommittedScansStore (Scanner Dedup)

**File:** `context.filesDir/events/{eventId}/committed_scans.jsonl`

**Operations:**

```kotlin
// Append scan (thread-safe)
suspend fun appendScan(scan: CommittedScan)

// Check if ticket already scanned
suspend fun hasTicket(ticketId: String): Boolean

// Get recent scans (last 100)
suspend fun getRecentScans(): List<CommittedScan>
```

**Data Structure:**
```kotlin
data class CommittedScan(
    val scanId: String,
    val ticketId: String,
    val eventId: String,       // Event UUID (server-side)
    val scannedAt: String,
    val committedAt: String,
    val wasOffline: Boolean,   // true = offline check, false = server check
    val ticketType: String?,
    val email: String?
)
```

**Code Location:** `CommittedScansStore.kt`

---

## API Reference

### Base URL
```
Staging: https://iloppis-staging.fly.dev/
All endpoints: /v1/...
```

### Authentication
```
Header: Authorization: Bearer <api_key>
```

### Pagination

All list endpoints (vendors, sold items, tickets) follow the same token-based pagination pattern:

**Initial Request:**
```http
GET /v1/events/{eventId}/vendors?pageSize=100
```

**Response:**
```json
{
  "vendors": [ /* ... */ ],
  "total": 350,
  "nextPageToken": "eyJ...",
  "prevPageToken": ""
}
```

**Next Page Request:**
```http
GET /v1/events/{eventId}/vendors?pageSize=100&nextPageToken=eyJ...
```

**Pattern:**
1. Send `pageSize` (default 100, adjust per endpoint)
2. Receive items + `nextPageToken` (empty = last page)
3. Pass `nextPageToken` in next request
4. Continue until `nextPageToken` is empty

**Android Example:**
```kotlin
class VendorRepository(private val api: VendorApi) {
    suspend fun loadAllVendors(eventId: String): List<Vendor> {
        val allVendors = mutableListOf<Vendor>()
        var nextToken: String? = null
        
        do {
            val response = api.listVendors(
                eventId = eventId,
                pageSize = 100,
                nextPageToken = nextToken
            )
            allVendors.addAll(response.vendors)
            nextToken = response.nextPageToken.takeIf { it.isNotEmpty() }
        } while (nextToken != null)
        
        return allVendors
    }
}
```

**iOS Example:**
```swift
class VendorRepository {
    func loadAllVendors(eventId: String) async throws -> [Vendor] {
        var allVendors: [Vendor] = []
        var nextToken: String? = nil
        
        repeat {
            let response = try await api.listVendors(
                eventId: eventId,
                pageSize: 100,
                nextPageToken: nextToken
            )
            allVendors.append(contentsOf: response.vendors)
            nextToken = response.nextPageToken.isEmpty ? nil : response.nextPageToken
        } while nextToken != nil
        
        return allVendors
    }
}
```

**Notes:**
- Store `nextPageToken` for "Load More" UI patterns
- `total` field available on some endpoints for progress indicators
- Empty `nextPageToken` signals last page reached
- Most endpoints default to 100 items per page

---

### Endpoints Summary

| Endpoint | Method | Auth | Purpose |
|----------|--------|------|---------|
| `/v1/events:filter` | POST | None | List open events |
| `/v1/events/{eventId}/api-keys/alias/{code}` | GET | None | Exchange code for API key |
| `/v1/events/{eventId}/vendors` | GET | Bearer | Fetch vendor list |
| `/v1/events/{eventId}/sold-items` | POST | Bearer | Record sales |
| `/v1/events/{eventId}/tickets/{ticketId}/scan` | POST | Bearer | Scan ticket |

---

### POST /v1/events:filter

**Purpose:** Fetch list of events filtered by date and lifecycle state

**Authorization:** None (public endpoint)

**Request:**
```json
{
  "filter": {
    "dateFrom": "2026-01-18T00:00:00Z",
    "lifecycleStates": ["OPEN"]
  }
}
```

**Response:**
```json
{
  "events": [
    {
      "id": "evt_abc123",
      "name": "Stockholms Vinterloppis",
      "startTime": "2026-01-25T10:00:00Z",
      "endTime": "2026-01-25T16:00:00Z",
      "addressStreet": "Vasagatan 1",
      "addressCity": "Stockholm",
      "lifecycleState": "OPEN"
    }
  ]
}
```

---

### GET /v1/events/{eventId}/api-keys/alias/{code}

**Purpose:** Exchange kassakod for API key

**Authorization:** None

**Path Parameters:**
- `eventId`: Event UUID
- `code`: Kassakod (e.g., "ABC-123")

**Response:**
```json
{
  "apiKey": "test_abc123xyz...",
  "alias": "ABC-123",
  "isActive": true,
  "type": "API_KEY_TYPE_WEB_CASHIER"
}
```

**Error Codes:**
- `404`: Code not found
- `403`: Code inactive or expired

---

### GET /v1/events/{eventId}/vendors

**Purpose:** Fetch approved vendor list

**Authorization:** Bearer token

**Query Parameters:**
- `pageSize`: Number of vendors per page (default: 100)
- `nextPageToken`: Pagination token (optional)

**Response:**
```json
{
  "vendors": [
    {
      "id": "vnd_001",
      "sellerNumber": 10,
      "firstName": "Anna",
      "lastName": "Andersson",
      "status": "approved"
    }
  ],
  "nextPageToken": "page_token_xyz"
}
```

---

### POST /v1/events/{eventId}/sold-items

**Purpose:** Create sold items (bulk)

**Authorization:** Bearer token

**Request:**
```json
{
  "items": [
    {
      "itemId": "01HW1K2M3N4P5Q6R7S8T9V0WX1",
      "purchaseId": "01HW1K2M3N4P5Q6R7S8T9V0WXY",
      "seller": 42,
      "price": 50,
      "paymentMethod": "SWISH"
    }
  ]
}
```

**Response:**
```json
{
  "acceptedItems": [
    {
      "itemId": "01HW1K2M3N4P5Q6R7S8T9V0WX1",
      "purchaseId": "01HW1K2M3N4P5Q6R7S8T9V0WXY",
      "seller": 42,
      "price": 50,
      "paymentMethod": "SWISH",
      "soldTime": "2026-01-18T14:30:00Z"
    }
  ],
  "rejectedItems": [
    {
      "item": {
        "itemId": "01HW1K2M3N4P5Q6R7S8T9V0WX2",
        "seller": 999
      },
      "reason": "seller 999 is not approved",
      "errorCode": "INVALID_SELLER"
    }
  ]
}
```

**Error Codes:**
- `INVALID_SELLER`: Seller not approved
- `DUPLICATE_RECEIPT`: ItemId already exists (idempotency)

---

### POST /v1/events/{eventId}/tickets/{ticketId}/scan

**Purpose:** Scan visitor ticket

**Authorization:** Bearer token

**Request Body:** (empty)

**Response:**
```json
{
  "ticket": {
    "id": "TKT-12345",
    "ticketType": "Vuxen",
    "email": "customer@example.com",
    "status": "SCANNED"
  }
}
```

**Error Codes:**
- `404`: Ticket not found
- `412`: Already scanned (duplicate)

---

## Development Tips

### Testing Offline Mode

**Cashier:**
1. Enable airplane mode on device
2. Complete purchase → Items save to `pending_items.jsonl`
3. Disable airplane mode → WorkManager uploads automatically
4. Verify items removed from JSONL

**Scanner:**
1. Enable airplane mode
2. Scan ticket → Saves to `pending_scans.jsonl` + `committed_scans.jsonl`
3. Scan same ticket again → Shows "Redan scannad (offline)"
4. Disable airplane mode → Background sync uploads
5. Verify scan removed from `pending_scans.jsonl`

### Viewing Local Files (Debug Build)

```bash
# List files
adb exec-out run-as se.iloppis.app ls -la /data/data/se.iloppis.app/files/events/

# Read JSONL file
adb exec-out run-as se.iloppis.app cat /data/data/se.iloppis.app/files/events/{eventId}/pending_items.jsonl

# Pull file to computer
adb exec-out run-as se.iloppis.app cat /data/data/se.iloppis.app/files/events/{eventId}/pending_items.jsonl > local_copy.jsonl
```

### Common Issues

**Issue:** "Purchase not uploading"  
**Solution:** Check `pending_items.jsonl` for `errorText` field. If not empty, check error message.

**Issue:** "Duplicate scans not detected offline"  
**Solution:** Verify `committed_scans.jsonl` contains all scans (both online and offline).

**Issue:** "Items lost after app crash"  
**Solution:** Items in `pending_items.jsonl` survive crashes. Check file still exists.

### Code Review Notes

**API Client:** Base URL currently hardcoded in `ApiClient.kt`. Consider moving to BuildConfig for environment switching.

**Store Initialization:** Stores initialized in CashierViewModel. Consider moving to Application level to avoid implicit coupling between ViewModels.

**Logging:** HttpLoggingInterceptor currently at BODY level. Consider BASIC/NONE for release builds to avoid PII leakage.

**Rejected Items UI:** Current implementation saves rejected items to `pending_review.json` but UI only shows count. Future work should add detailed view/editing capability.

---

## Further Reading

- **Architecture Details:** `.github/copilot-instructions.md`
- **Build Instructions:** `android/README.md`, `ios/README.md`

---

**Last Updated:** January 18, 2026
