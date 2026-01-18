# iLoppis Mobile - GitHub Copilot Instructions

## CRITICAL: Always Follow These Instructions First

**ALWAYS reference these instructions first and only fallback to search or bash commands when you encounter unexpected information that does not match the info here.**

iLoppis Mobile is the native mobile application for organizing digital flea markets (loppisar). The app supports both Android (Kotlin/Jetpack Compose) and iOS (Swift/SwiftUI) platforms with a shared architecture: Clean Architecture + MVVM + unidirectional data flow (immutable UI state + events) and mirrored folder layout.

## Prerequisites & Environment

### Java Version (Android Development)
**Required Java Version**: Java 17 or 21 (NOT Java 25+)
- ❌ Java 25.0.1+ causes Kotlin compilation errors
- ✅ Install compatible version: `brew install openjdk@21`
- ✅ Makefile auto-detects compatible Java in mobile/android/

**Android Setup**:
- ANDROID_HOME: `/opt/homebrew/share/android-commandlinetools`
- Connected device or running emulator required
- Use `make devices` to verify device connection

## Dependency Version Management (CRITICAL)

### Version Update Strategy

**Proactive updates prevent painful migrations.** Don't wait for security warnings or ecosystem bans.

#### Update Frequency
| Update Type | Frequency | Action |
|-------------|-----------|--------|
| **Patch** (x.x.PATCH) | Weekly | Auto-apply, test, commit |
| **Minor** (x.MINOR.x) | Bi-weekly | Review changelog, test, commit |
| **Major** (MAJOR.x.x) | Monthly review | Plan migration within 1-2 sprints |

#### Version Check Commands
```bash
# Android - Check for outdated dependencies
cd mobile/android
./gradlew dependencyUpdates

# Or manually check libs.versions.toml against:
# - https://developer.android.com/jetpack/androidx/versions
# - https://github.com/JetBrains/kotlin/releases
# - https://square.github.io/retrofit/
```

#### Update Process
1. **Check** - Run dependency update check
2. **Review** - Read changelogs for breaking changes
3. **Update** - Bump version in `libs.versions.toml`
4. **Test** - Build and run manual test scenarios
5. **Commit** - `chore(deps): bump [library] from x.y.z to a.b.c`

#### Major Version Policy
- **Track release candidates** - Test RCs in a branch before stable release
- **Don't skip majors** - If on v2, don't jump directly to v4
- **Budget time** - Allocate 1-2 days per quarter for major upgrades
- **Document breaking changes** - Note any code changes needed in commit

#### Current Version Targets (libs.versions.toml)
Keep these reasonably current:
```toml
[versions]
agp = "8.7.x"              # Android Gradle Plugin - check quarterly
kotlin = "2.0.x"           # Kotlin - follow stable releases
composeBom = "2024.xx.xx"  # Compose BOM - update monthly
retrofit = "2.x.x"         # Retrofit - stable, update on security
```

#### Red Flags - Immediate Action Required
- Security advisory on any dependency
- Deprecation warning in build output
- Google Play Console policy warning
- Library marked as abandoned/unmaintained

## Architecture Overview

### Cross-Platform Parity (CRITICAL)
- Keep screens, flows, and surface area identical unless platform UX conventions require changes; prefer feature parity first.
- Mirror naming across platforms: `EventListScreen`/`EventListViewModel`/`EventListState` (Android ↔ iOS). If Android naming drifts, update it to match iOS for symmetry.
- Share design patterns: Clean Architecture + MVVM + unidirectional data flow (state + actions/intents). Android uses Kotlin coroutines/StateFlow; iOS uses Swift Concurrency (`async`/`await`) and `ObservableObject`.
- UI composition rules mirror: stateless components, screen-level orchestration, side-effects isolated in ViewModels.
- Styling and copy centralized: colors from `AppColors`, text from localized string resources, spacing/typography from theme files.
- Navigation should mirror route names/arguments and entry points (e.g., Event List → Event Detail Dialog → Mode Selection).

The mobile app follows a clean architecture with clear separation of concerns and mirrored folder names per platform:

```
app/
├── domain/              # Business logic layer
│   ├── model/           # Domain models (Event, EventState, etc.)
│   └── mapper/          # DTO → Domain mappers
├── network/             # API/networking layer
│   └── ApiClient        # Retrofit/URLSession clients
└── ui/                  # Presentation layer
    ├── theme/           # Colors, typography, styling
    │   └── AppColors    # ★ SINGLE color palette source
    ├── components/      # Reusable UI components
    ├── dialogs/         # Dialog/sheet composables
    └── screens/         # Screen composables with ViewModels
        └── [feature]/
            ├── *Screen.kt / *Screen.swift    # UI composition
            ├── *State.kt / *State.swift      # UI state + actions
            └── *ViewModel.kt / *ViewModel.swift  # Business logic
```

### Layer Responsibilities

| Layer | Responsibility | Example |
|-------|---------------|---------|
| **domain/model** | Pure data classes, enums | `Event`, `EventState`, `CodeEntryMode` |
| **domain/mapper** | Transform DTOs to domain models | `EventMapper.toDomain()` |
| **network** | API calls, DTOs, client config | `ApiClient`, `EventDto`, `EventApi` |
| **ui/theme** | Colors, typography, dimensions | `AppColors.Background` |
| **ui/components** | Reusable UI building blocks | `EventCard`, `StateBadge`, `CodeBox` |
| **ui/dialogs** | Modal dialogs/sheets | `CodeEntryDialog`, `EventDetailDialog` |
| **ui/screens** | Full screen compositions | `EventListScreen` |

## Development Guidelines

### Styling Rules (CRITICAL)

- **ALL colors MUST come from `AppColors.kt`** - Never use hardcoded hex values in UI code
- **NO inline styling** - Extract to theme or component-level styling
- Keep styling separate from logic code
- Use semantic color names (`TextPrimary`, `ButtonSuccess`) not literal names (`Blue`, `Red`)

```kotlin
// ✅ CORRECT - Use centralized colors
Text(color = AppColors.TextPrimary)
Button(colors = ButtonDefaults.buttonColors(containerColor = AppColors.ButtonSuccess))

// ❌ WRONG - Hardcoded colors
Text(color = Color(0xFF2D3748))
Button(colors = ButtonDefaults.buttonColors(containerColor = Color.Green))
```

### Internationalization Rules (CRITICAL)

- **NO hardcoded user-facing text** - All text must come from string resources/dictionaries
- Use string resource files for all user-visible text
- Support Swedish (default) and English languages

**Android:**
```kotlin
// ✅ CORRECT - Use string resources
Text(text = stringResource(R.string.event_list_title))

// ❌ WRONG - Hardcoded text
Text(text = "iLoppis")
```

**iOS:**
```swift
// ✅ CORRECT - Use localized strings
Text(LocalizedStringKey("event_list_title"))

// ❌ WRONG - Hardcoded text
Text("iLoppis")
```

### String Resource Organization
- **Android**: `res/values/strings.xml`, `res/values-sv/strings.xml`
- **iOS**: `Localizable.strings`, `Localizable (Swedish).strings`

### Code Organization Rules

1. **Screens (Android/iOS)** - Compose UI from state only; delegate actions to ViewModel; avoid side-effects in views.
2. **ViewModels** - Handle all business logic, expose immutable UI state + single-event channels; Android: `StateFlow`/`Flow`; iOS: `@Published` with `ObservableObject`.
3. **State files** - Define immutable state data classes/structs and sealed action/intent types; prefer explicit events over ad-hoc callbacks.
4. **Components** - Pure UI, receive data via parameters, emit events via callbacks; keep component props aligned across platforms.
5. **Navigation** - Centralize navigation targets and argument contracts; keep route names and deep links mirrored between platforms.

```kotlin
// Screen pattern
@Composable
fun EventListScreen(viewModel: EventListViewModel = viewModel()) {
    val state = viewModel.uiState
    
    // Delegate dialogs to separate composable
    EventDialogs(state = state, onAction = viewModel::onAction)
    
    // Main content
    EventListContent(state = state, onEventClick = { ... })
}
```

### Asset Management

- **NEVER commit binary assets** - Use icon libraries at runtime
- Align with frontend icon usage patterns
- Use vector drawables (Android) / SF Symbols (iOS) when possible
- For custom icons, use SVG sources and generate platform-specific assets

**Android icon libraries:**
```kotlin
implementation("androidx.compose.material:material-icons-extended")
```

**iOS:**
- Use SF Symbols for system icons
- Asset catalogs for custom icons (generate from SVG)

## Android-Specific Guidelines

### Build Commands
```bash
cd mobile/android

# Build debug APK
./gradlew assembleDebug

# Install and run on connected device/emulator
make install
make run

# Or combined
make build && make install && make run
```

### Dependencies (libs.versions.toml)
- Kotlin 2.0+
- Jetpack Compose with Material3
- Retrofit + OkHttp for networking
- ViewModel from lifecycle-viewmodel-compose

### Project Structure
```
mobile/android/
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/se/iloppis/app/
│       │   ├── MainActivity.kt
│       │   ├── domain/
│       │   ├── network/
│       │   └── ui/
│       └── res/
│           ├── values/strings.xml      # Swedish (default)
│           ├── values-en/strings.xml   # English
│           └── ...
├── gradle/
├── Makefile
└── README.md
```

## iOS-Specific Guidelines

### Project Structure
```
mobile/ios/
├── iLoppis/
│   ├── iLoppisApp.swift
│   ├── Domain/
│   ├── Network/
│   └── UI/
│       ├── Theme/
│       ├── Components/
│       └── Screens/
├── iLoppis.xcodeproj
└── README.md
```

### SwiftUI Patterns
- Use `@StateObject` for ViewModels and `@Published` for observable state.
- Follow the same layer separation and naming as Android (Domain/Network/UI with Theme, Components, Screens, Dialogs if needed).
- Use async/await for networking and side-effects; keep reducers/intents in the ViewModel to mirror Android event handling.

### Build & Test
- Open `mobile/ios` in Xcode; keep a shared scheme name (`iLoppis`) across developers.
- Prefer command-line CI builds: `xcodebuild -scheme iLoppis -destination 'platform=iOS Simulator,name=iPhone 15' clean build` (adjust destination as needed).
- Run SwiftLint (if configured) before commits; align rule set with Android lint expectations where possible.

## API Integration

### Base URL
- Production: `https://iloppis.fly.dev`
- All endpoints under `/v1/`

### Authentication
- API keys passed via `Authorization: Bearer <key>` header
- Code exchange: `GET /v1/events/{eventId}/api-keys/alias/{code}`

### Key Endpoints
| Endpoint | Method | Description |
|----------|--------|-------------|
| `/v1/events` | GET | List all events |
| `/v1/events/{id}` | GET | Get event details |
| `/v1/events/{id}/api-keys/alias/{code}` | GET | Exchange code for API key |
| `/v1/events/{id}/sold-items` | POST | Record sale (cashier) |
| `/v1/events/{id}/scans` | POST | Record ticket scan |

## Quality Checks

### Before Committing
```bash
# Android
cd mobile/android
./gradlew assembleDebug    # Must succeed
./gradlew lint             # Check for issues
```

### Manual Testing Scenarios
1. **Event List Load** - Should fetch and display events from API
2. **Event Detail Dialog** - Tap event → show details with mode buttons
3. **Code Entry** - Enter XXX-XXX code, supports paste in both formats
4. **Orientation Change** - UI should adapt properly
5. **Network Error** - Should show error message gracefully

## Known Implementation Details

### ID Format: ULID (Not UUID)
**CRITICAL:** The backend uses **ULID** (Universally Unique Lexicographically Sortable Identifier) for `item_id`, `purchase_id`, and `scan_id` - NOT UUID.

**ULID Characteristics:**
- 26 uppercase alphanumeric characters (0-9, A-Z excluding I, L, O, U)
- Time-ordered: first 10 characters encode timestamp (millisecond precision)
- Lexicographically sortable
- 80 bits of randomness (collision-safe even with hundreds of concurrent cashiers)
- Example: `01KF9A3M5GT7F10X0GBT60MPDJ`

**Collision Safety:**
- Safe for parallel use with many cashiers (tested with 10+ concurrent users)
- Collision probability: ~0.00000000000000004% even if generated at same millisecond
- MongoDB unique index (event_id, item_id) provides additional safety net
- Any theoretical duplicate → backend rejects with DUPLICATE_RECEIPT error code

**Android Library:**
```gradle
implementation("com.guepardoapps:kulid:2.0.0.0")
```

**Usage:**
```kotlin
import com.guepardoapps.kulid.ULID

val itemId = ULID.random()      // Generate new ULID
val purchaseId = ULID.random()  // Different ULID for purchase grouping
```

**Note:** `eventId` uses standard UUID format with hyphens (e.g., `d50e8356-8deb-428a-a588-afaa0d4f1214`)

### API Client Configuration
**File:** `android/app/.../network/ApiClient.kt`
- Base URL hardcoded: `https://iloppis.fly.dev/`
- **Future improvement:** Move to BuildConfig/gradle.properties for environment switching
- HttpLoggingInterceptor: Currently BODY level (consider BASIC/NONE for release builds)

### Store Initialization
- PendingItemsStore, SoldItemFileStore, RejectedPurchaseStore initialized in `CashierViewModel`
- **Note:** Other ViewModels depend on this initialization (implicit coupling)
- **Future improvement:** Move to Application-level initialization

### Proto Integration (Future Work)
- Currently uses manual string parsing for backend error codes
- Proto files exist in `/proto/iloppis/v1/sold_items.proto`
- **Blocked by:** External proto dependencies (buf/validate, google/api annotations)
- **Future improvement:** Generate Kotlin stubs via protobuf-gradle-plugin for type safety

## Troubleshooting

### Emulator Network Issues
```bash
# Restart emulator with explicit DNS
emulator -avd <avd_name> -dns-server 8.8.8.8,8.8.4.4

# Toggle airplane mode via adb
adb shell settings put global airplane_mode_on 1
adb shell am broadcast -a android.intent.action.AIRPLANE_MODE
adb shell settings put global airplane_mode_on 0
adb shell am broadcast -a android.intent.action.AIRPLANE_MODE
```

### Build Cache Issues
```bash
# Clean and rebuild
cd mobile/android
./gradlew clean
./gradlew assembleDebug
```

### Viewing Local Files (Debug)
```bash
# List files for specific event
adb exec-out run-as se.iloppis.app ls -la /data/data/se.iloppis.app/files/events/{eventId}/

# Read JSONL file
adb exec-out run-as se.iloppis.app cat /data/data/se.iloppis.app/files/events/{eventId}/pending_items.jsonl

# Pull file to computer
adb exec-out run-as se.iloppis.app cat /data/data/se.iloppis.app/files/events/{eventId}/pending_items.jsonl > local_copy.jsonl
```

## Alignment with Frontend

The mobile app should maintain visual and functional parity with the web frontend where applicable:

| Aspect | Frontend | Mobile |
|--------|----------|--------|
| Colors | `src/styles/Styles.js` | `ui/theme/AppColors.kt` / `AppColors.swift` |
| Strings | `src/locales/*.json` | `res/values*/strings.xml` / `Localizable*.strings` |
| Icons | Icon libraries (runtime) | Material Icons / SF Symbols |
| Components | `src/components/` | `ui/components/` |

### Color Palette Reference
The mobile `AppColors.kt` should use the same color values as the frontend:
- Background pink: `#F5E8E9`
- Card background: `#FAF3F4`
- Text primary: `#2D3748`
- Success green: `#4CAF50`
- Info blue: `#2196F3`
- Error red: `#E53E3E`

## File Naming Conventions

| Type | Android (Kotlin) | iOS (Swift) |
|------|-----------------|-------------|
| Screens | `EventListScreen.kt` | `EventListScreen.swift` |
| ViewModels | `EventListViewModel.kt` | `EventListViewModel.swift` |
| State | `EventListState.kt` | `EventListState.swift` |
| Components | `CommonComponents.kt` | `CommonComponents.swift` |
| Colors | `AppColors.kt` | `AppColors.swift` |

## Summary Checklist

Before submitting code changes, verify:

- [ ] All colors from `AppColors` - no hardcoded hex values (Android + iOS)
- [ ] All user-facing text from string resources - no hardcoded strings (Android `strings.xml`, iOS `Localizable.strings`)
- [ ] Styling separated from logic; components stateless and reused across screens
- [ ] State and actions are handled in ViewModels; screens are passive
- [ ] Navigation routes/arguments and screen naming stay mirrored between Android and iOS
- [ ] Build succeeds: Android `./gradlew assembleDebug`, iOS `xcodebuild -scheme iLoppis -destination 'platform=iOS Simulator,name=iPhone 15' build`
- [ ] Manual testing scenarios pass on both platforms (event list load, detail dialog, code entry, rotation/adaptive layout, graceful network error)
