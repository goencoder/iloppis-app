# Android Architecture Alignment with Desktop Design Patterns

> **Version:** 1.0 · **Date:** 2026-02-26 · **Status:** Proposal
> **Platform:** Android (Kotlin / Jetpack Compose)
> **Reference codebase:** LoppisKassan (Java 21 / Swing desktop app)

---

## 1  Motivation

The LoppisKassan desktop app has matured into a well-structured codebase with strong architectural patterns: interface-driven MVC, centralized design system (`AppColors` + `AppButton` factory), structured state management (PropertyChangeSupport), Strategy pattern for mode polymorphism, and strict localization discipline.

The Android app shares the same product domain but has grown organically, resulting in inconsistent patterns across screens. Aligning the Android app with the desktop's design principles — adapted to Compose/MVVM idioms — will improve:

- **Maintainability** — fewer one-off patterns to reason about
- **Cross-platform parity** — developers moving between codebases find familiar structure
- **Onboarding** — clear conventions reduce decision fatigue
- **Quality** — centralized tokens eliminate styling drift

This issue proposes concrete refactoring tasks, grouped by priority.

---

## 2  Gap Analysis Summary

| Area | Desktop (LoppisKassan) | Android (iLoppis) | Severity |
|------|----------------------|-------------------|----------|
| **Screen architecture (MVC/MVVM)** | **Consistent MVC: interface contract + controller + state for every screen** | **4+ different patterns; 3 screens lack ViewModels entirely** | **Critical** |
| **Color palette discipline** | **`AppColors` is the single source; zero raw colors anywhere** | **~81 violations: `Color.White`, `MaterialTheme.colorScheme.*`, duplicate `Color.kt`** | **Critical** |
| Button components | `AppButton.create(text, Variant, Size)` factory | 3 overlapping components; 30+ ad-hoc `Button()` | **High** |
| Spacing & typography | `xs=4, sm=8, md=16, lg=24, xl=32` tokens | Literal `dp` values everywhere; default `Typography()` | **High** |
| ViewModel lifecycle | Singleton controllers (Swing-appropriate) | `remember {}` anti-pattern; 4 creation styles | **High** |
| Store initialization | `ConfigurationStore` template method; central init | Scattered `object` singletons; fragile implicit coupling | **High** |
| Configuration persistence | `ConfigurationStore` + `config.properties` | **None** — no settings/prefs layer exists | **High** |
| Localization | `LocalizationManager.tr()` universally enforced | 15+ hardcoded Swedish strings; `PurchaseReviewScreen` mostly untranslated | **High** |
| Strategy / mode logic | Explicit Strategy interfaces (`CashierStrategy`, `HistoryOperations`) | Implicit via `sealed class` + `when`; no formal abstraction | Low |

---

## 3  Detailed Findings

### 3.1 Screen Architecture: The MVC/MVVM Contract (MOST IMPORTANT)

This is the **single most important architectural alignment**. The desktop app's greatest strength is that every screen follows exactly the same structural contract. A developer opening any screen immediately knows where to find the state, how actions flow, and how the view communicates with the controller. The Android app lacks this consistency entirely.

#### Desktop pattern (reference) — strict MVC triple:

Every screen in LoppisKassan follows the same four-file structure:

```
*PanelInterface     → contract defining what the view can show/do
*TabPanel           → JPanel implementing the interface (pure UI)
*TabController      → singleton owning all business logic + state
*State              → observable model with PropertyChangeSupport
```

The wiring is **identical across all screens**:
```java
// 1. View registers with controller
controller.registerView(this);

// 2. View delegates user actions to controller
submitButton.addActionListener(e -> controller.onPricesSubmitted());

// 3. Controller pushes state changes back through the interface
view.addSoldItem(item);
view.enableCheckoutButtons(true);
```

Key properties of this pattern:
- **Interface contracts enforce separation** — the controller never touches Swing widgets directly; the view never touches business logic
- **State is observable** — `PropertyChangeSupport` fires change events; views register as listeners
- **Uniform wiring** — `registerView()` + action methods + interface callbacks is the same mechanism everywhere
- **Testable** — controllers can be tested with mock view interfaces
- **Discoverable** — any developer knows exactly where to look for any piece of logic

#### Android equivalent — MVVM with unidirectional data flow:

Compose replaces the need for explicit interface contracts (recomposition handles the view→state binding). But the Android equivalent of the desktop's MVC consistency is the **State + Action + ViewModel** triple, which some screens already follow:

```kotlin
// Equivalent four-part structure
data class FeatureUiState(...)                // ↔ *State.java
sealed class FeatureAction { ... }            // ↔ *ControllerInterface methods
class FeatureViewModel : ViewModel() {        // ↔ *TabController.java
    var uiState by mutableStateOf(State())
        private set
    fun onAction(action: FeatureAction) { ... }
}
@Composable
fun FeatureScreen(vm: FeatureViewModel) {     // ↔ *TabPanel.java
    val state = vm.uiState
    Content(state, onAction = vm::onAction)
}
```

#### Current Android state — inconsistent:

| Screen | Has ViewModel? | Has State class? | Has Action sealed class? | Follows pattern? |
|--------|:-:|:-:|:-:|:-:|
| `CashierScreen` | ✅ | ✅ | ✅ | ✅ |
| `ScannerScreen` | ✅ | ✅ | ✅ | ✅ |
| `EventListScreen` | ✅ | ✅ | ✅ | ✅ |
| `DetailedPurchaseReviewScreen` | ✅ | ✅ | ✅ | ✅ |
| `PendingPurchasesScreen` | ✅ | ✅ | ❌ Direct method calls | ⚠️ Partial |
| `PurchaseReviewScreen` | ❌ | ❌ | ❌ Direct callbacks | ❌ |
| `CodeEntryScreen` | ❌ | ❌ | ❌ Inline logic + local vars | ❌ |

**3 out of 7 screens violate the pattern.** This means a developer working on `CodeEntryScreen` encounters a completely different architecture than `CashierScreen` — business logic mixed into composables, state scattered across local `var`s, API calls in `rememberCoroutineScope` blocks.

#### Why this matters most:

1. **Consistency is the foundation** — Design system tokens, button components, and store patterns all benefit from a consistent screen structure. Without it, there's no reliable place to apply those improvements.
2. **Debugging is unpredictable** — When each screen has a different architecture, developers waste time understanding *how* the screen works before they can fix *what* is wrong.
3. **Feature additions drift further** — New screens will copy whichever existing screen the developer happens to look at, amplifying inconsistency.
4. **The desktop proves it works** — LoppisKassan has 5 screens, all following the exact same MVC pattern. Despite Swing being more verbose than Compose, the codebase is navigable because the structure is predictable.

#### Required actions:

1. **Extract ViewModels** for `CodeEntryScreen` and `PurchaseReviewScreen`
2. **Add sealed Action class** to `PendingPurchasesViewModel` (replace direct method calls with `onAction()`)
3. **Document the pattern** as a mandatory convention (developer guide or README section)
4. **Lint/review gate** — new screens that bypass `State + Action + ViewModel` should be flagged in code review

---

### 3.2 Color Palette Discipline (CRITICAL)

#### The principle

Both the desktop and Android apps define a centralized `AppColors` object as the **single source of truth** for all colors. The rule is absolute:

> **Every color reference in UI code must go through `AppColors`.** No `Color(0xFF...)`, no `Color.White`, no `MaterialTheme.colorScheme.*` in screens, components, or dialogs.

The desktop enforces this strictly — there are zero raw hex values or `Color.YELLOW` constants in any UI file. The Android app has **~81 violations** across three categories.

#### Desktop enforcement (reference):
```java
// LoppisKassan: AppColors.java — the ONLY place colors are defined
public static final Color TEXT_PRIMARY = Color.decode("#2D3748");
public static final Color SUCCESS = Color.decode("#4CAF50");
public static final Color DANGER = Color.decode("#E53E3E");

// Usage — always via AppColors
text.setForeground(AppColors.TEXT_PRIMARY);      // ✅ CORRECT
text.setForeground(Color.decode("#2D3748"));     // ❌ NEVER
text.setForeground(Color.BLACK);                 // ❌ NEVER
```

#### Android violations by category:

**Category 1: Direct `Color.White` / `Color.Black` usage (8 instances)**

These bypass `AppColors` with raw Material Color constants:

| File | Line(s) | Expression |
|------|---------|------------|
| `PaymentSection.kt` | L140, L146, L167, L173 | `contentColor = Color.White`, `color = Color.White` |
| `CashierScreen.kt` | L212, L225 | `Text("OK", color = Color.White)` |
| `SplashScreen.kt` | L77 | `color = Color.White` |
| `NumericKeypad.kt` | L116 | `if (isPrimary) Color.White else …` |

**Category 2: `MaterialTheme.colorScheme.*` bypasses AppColors (~57 instances)**

This is the largest source of violations. Screens reference `MaterialTheme.colorScheme.error`, `.primary`, `.secondary`, etc. — which are populated from a **separate `Color.kt` file** that duplicates and diverges from `AppColors`:

| File | Count | Examples |
|------|-------|----------|
| `DetailedPurchaseReviewScreen.kt` | 11 | `.error`, `.surface`, `.onPrimary`, `.primaryContainer`, `.tertiary` |
| `PendingPurchasesScreen.kt` | 8 | `.primary`, `.error`, `.errorContainer`, `.surfaceVariant` |
| `MarkdownText.kt` | 7 | `.onSurfaceVariant` (×7) |
| `EventDetailDialog.kt` | 9 | `.onSurfaceVariant` (×7), `.onSecondary`, `.primary` |
| `PurchaseReviewScreen.kt` | 6 | `.onSurfaceVariant`, `.error`, `.tertiary`, `.errorContainer` |
| `EventCards.kt` | 6 | `.background`, `.secondary`, `.error` |
| `Navigator.kt` | 3 | `.onSurface`, `.background` |
| `EventsDetailsScreen.kt` | 3 | `.onPrimary`, `.secondary` |
| `EventListScreen.kt` | 1 | `.secondary` |
| `HomeScreen.kt` | 1 | `.secondary` |
| `CodeEntryScreen.kt` | 1 | `.onPrimary` |
| `CommonComponents.kt` | 1 | `.onTertiaryFixed` |

**Category 3: Duplicate `Color.kt` file (11 constants)**

`ui/theme/Color.kt` defines 11 loose color constants that feed `Theme.kt`'s `lightColorScheme()`. This creates a **parallel color system** that diverges from `AppColors`:

| `Color.kt` constant | Hex | `AppColors` equivalent | Match? |
|---------------------|-----|----------------------|--------|
| `PinkBackground` | `0xFFF5E8E9` | `AppColors.Background` | ✅ |
| `PinkCard` | `0xFFFAF3F4` | `AppColors.CardBackground` | ✅ |
| `PinkAccent` | `0xFFE8C8CA` | `AppColors.BadgeUpcomingBackground` | ✅ |
| `GreenBadge` | `0xFFC8E6C9` | `AppColors.BadgeOpenBackground` | ✅ |
| `Green` | `0xFF388E3C` | `AppColors.BadgeOpenText` | ✅ |
| `Blue` | `0xFF228CCA` | `AppColors.Info` (`0xFF2196F3`) | ❌ **Different blues** |
| `Red` | `0xFFB71C1C` | `AppColors.Error` (`0xFFE53E3E`) | ❌ **Different reds** |
| `GrayBadge` | `0xFFE0E0E0` | `AppColors.BadgeDefaultBackground` | ✅ |
| `GrayText` | `0xFF757575` | `AppColors.BadgeDefaultText` | ✅ |
| `Text` | `0xFF2D3748` | `AppColors.TextPrimary` | ✅ |
| `Gold` | `0xFFF59E0B` | **Missing from AppColors** | ❌ |

The value mismatches for Blue and Red mean the same semantic concept (e.g., "error") renders as **different colors** depending on whether code goes through `AppColors.Error` or `MaterialTheme.colorScheme.error`.

**Category 4: `Theme.kt` references (5 instances)**

`Theme.kt`'s `lightColorScheme()` call uses `Color.White`, `Color.Black`, `Color.DarkGray`, `Color.Gray` directly instead of `AppColors.*`.

#### Required actions:

1. **Add missing semantic colors** to `AppColors.kt`: `OnButton` (white on colored buttons), `Secondary` (green), `Star`/`Accent` (gold), `NavBarBackground`
2. **Replace all `Color.White`/`Color.Black`** in UI files with `AppColors.*` equivalents
3. **Replace all `MaterialTheme.colorScheme.*`** references with direct `AppColors.*` usage
4. **Rewrite `Theme.kt`** to use `AppColors.*` for its `lightColorScheme()` definition
5. **Delete `Color.kt`** entirely — it becomes unnecessary once Theme.kt references AppColors
6. **Code review rule:** any PR introducing `Color(0x`, `Color.White`, or `MaterialTheme.colorScheme.*` outside of `AppColors.kt` or `Theme.kt` must be rejected

---

### 3.3 Button Component Fragmentation

**Desktop pattern (reference):**
```java
// LoppisKassan: AppButton.java
AppButton.create("Öppna kassa", Variant.PRIMARY, Size.LARGE);
AppButton.applyStyle(existingButton, Variant.DANGER, Size.MEDIUM);
// Variants: PRIMARY, SECONDARY, OUTLINE, DANGER, GHOST
// Sizes: SMALL (28px), MEDIUM (36px), LARGE (44px), XLARGE (50px)
```

**Android current state:**

Three overlapping component definitions exist:

| Component | File | Usage count |
|-----------|------|-------------|
| `PrimaryButton` | `ui/components/CommonComponents.kt` | ~2 call sites |
| `IconButton` | `ui/components/buttons/Buttons.kt` | ~3 call sites |
| `CancelTextButton` | `ui/components/CommonComponents.kt` | ~2 call sites |

The vast majority of buttons (~30+) use raw `Button()` with inline `ButtonDefaults.buttonColors(containerColor = ...)` in:
- `ScannerScreen.kt` (5+ instances)
- `PaymentSection.kt` (2 instances)
- `CodeEntryScreen.kt`, `CodeConfirmScreen.kt` (2 each)
- `EventsDetailsScreen.kt` (4 instances)
- `DetailedPurchaseReviewScreen.kt`, `PurchaseReviewScreen.kt`

**Impact:** Color and sizing drift across screens; no single place to update button styling globally.

### 3.4 Missing Spacing & Typography Tokens

**Desktop pattern (reference):**
```java
// LoppisKassan: structured spacing
xs=4, sm=8, md=16, lg=24, xl=32  // pixel tokens
// Typography: 20px titles, 16px section headers, 13–14px body, 11px help, 28–36px totals
```

**Android current state:**

No `Dimens` or `Spacing` object exists. All values are literal:
```kotlin
// Scattered across every screen file
padding(horizontal = 16.dp)
spacedBy(16.dp)
height(8.dp)
padding(bottom = 12.dp)
fontSize = 12.sp
fontSize = 20.sp
```

`Theme.kt` uses default `Typography()` with zero customization.

**Impact:** Spacing and font sizes vary subtly across screens; changing the design scale requires editing dozens of files.

### 3.5 ViewModel Lifecycle Anti-Pattern

**Critical issue:** Three screens create ViewModels with `remember {}`:

```kotlin
// CashierScreen.kt, ScannerScreen.kt, PendingPurchasesScreen.kt
val viewModel = remember(event.id, apiKey) { CashierViewModel(eventId, eventName, apiKey) }
```

`remember {}` ties the ViewModel to composition scope — it does **not** survive configuration changes (screen rotation). The standard Compose approach is `viewModel()` with a `ViewModelProvider.Factory`.

Additionally, `CodeEntryScreen` has no ViewModel at all; business logic (API calls, error handling) runs inline in the composable via `rememberCoroutineScope()`.

### 3.6 Fragile Store/Repository Initialization

**Desktop pattern (reference):**
```java
// LoppisKassan: ConfigurationStore<T> — Template Method
abstract class ConfigurationStore<T> {
    abstract Path getConfigPath();
    abstract T createDefaultConfig();
    // Template: load() reads JSON, save() writes JSON — one pattern for all stores
}
```

**Android current state:**

All data stores are `object` singletons with `lateinit var` properties and manual `initialize()` calls:

| Store | Initialized from |
|-------|-----------------|
| `PendingItemsStore` | `CashierViewModel`, `PendingPurchasesViewModel`, `SoldItemsSyncWorker` (3 places!) |
| `SoldItemFileStore` | `CashierViewModel` only |
| `RejectedPurchaseStore` | `CashierViewModel` only |
| `VendorRepository` | `CashierViewModel` only |
| `PendingScansStore` | `ScannerViewModel`, `ScanSyncWorker` |
| `CommittedScansStore` | `ScannerViewModel`, `ScanSyncWorker` |

**Problems:**
- Calling any store method before `initialize()` crashes with `UninitializedPropertyAccessException`
- `DetailedPurchaseReviewViewModel` uses `VendorRepository` and `RejectedPurchaseStore` **without initializing them** — relies on CashierViewModel having run first
- Multiple init sites for the same store can race with different `eventId` values

### 3.7 No Configuration Persistence

The desktop app persists user preferences (language, event selection, API keys, mode) via `ConfigurationStore` with JSON files. The Android app has **no equivalent**:

- No `SharedPreferences` usage
- No `DataStore` usage
- No settings screen
- Base URL is hardcoded in `ApiClient`

### 3.8 Localization Violations

**Desktop pattern (reference):**
```java
// LoppisKassan: strictly enforced
LocalizationManager.tr("cashier.checkout.success")
// Never: new JLabel("Checkout successful")
```

**Android violations found:**

| File | String | Severity |
|------|--------|----------|
| `PurchaseReviewScreen.kt` | `"🟡 KRÄVER GRANSKNING (${needsReview.size} köp)"` | **High** |
| `PurchaseReviewScreen.kt` | `"📦 Köp ${purchase.purchaseId.takeLast(6)} - $timeString"` | **High** |
| `PurchaseReviewScreen.kt` | `"❌ ${purchase.errorMessage}\nSynkar automatiskt..."` | **High** |
| `PurchaseReviewScreen.kt` | `"Säljare ${it.item.seller}: ${it.reason}"` | **High** |
| `PurchaseReviewScreen.kt` | `"Väntar på server..."` | **High** |
| `DetailedPurchaseReviewScreen.kt` | `Text("Fel")` | **High** |
| `CommonComponents.kt` | `CancelTextButton(text = "Avbryt")` | **High** |
| `ScannerScreen.kt` | `"($successCount ok, $errorCount fel)"` | **High** |
| `CashierScreen.kt` | `Text("OK")` (3 instances) | Medium |
| `PendingPurchasesScreen.kt` | `"Dölj"`, `"Visa"`, `"Ta bort"` | Medium |

---

## 4  Proposed Refactoring Plan

### Phase 1 — Design System Foundation (Priority: Highest)

#### 4.1.1 Create `AppButton` composable library

Mirroring the desktop's `AppButton` factory, create a centralized button system:

```kotlin
// ui/components/AppButton.kt

enum class ButtonVariant { PRIMARY, SECONDARY, OUTLINE, DANGER, GHOST }
enum class ButtonSize { SMALL, MEDIUM, LARGE, XLARGE }

@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    variant: ButtonVariant = ButtonVariant.PRIMARY,
    size: ButtonSize = ButtonSize.MEDIUM,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
)
```

**Variant → color mapping (aligned with desktop):**

| Variant | Container | Text | Border |
|---------|-----------|------|--------|
| `PRIMARY` | `AppColors.ButtonPrimary` | `AppColors.OnButton` | none |
| `SECONDARY` | `AppColors.DialogBackground` | `AppColors.TextPrimary` | `AppColors.BorderLight` |
| `OUTLINE` | `Color.Transparent` | `AppColors.Primary` | `AppColors.Primary` |
| `DANGER` | `Color.Transparent` | `AppColors.Error` | `AppColors.Error` |
| `GHOST` | `Color.Transparent` | `AppColors.TextSecondary` | none |

**Size → dimensions mapping:**

| Size | Height | Font | Padding |
|------|--------|------|---------|
| `SMALL` | 32.dp | 12.sp | 12.dp horizontal |
| `MEDIUM` | 40.dp | 14.sp | 16.dp horizontal |
| `LARGE` | 48.dp | 16.sp | 20.dp horizontal |
| `XLARGE` | 56.dp | 16.sp (bold) | 24.dp horizontal |

**Migration:** Replace all raw `Button()` calls with `AppButton()`. Retire `PrimaryButton`, `CancelTextButton`, and the custom `IconButton` — fold their behavior into `AppButton` variants.

**Files to update:** `CashierScreen.kt`, `ScannerScreen.kt`, `PaymentSection.kt`, `CodeEntryScreen.kt`, `CodeConfirmScreen.kt`, `EventsDetailsScreen.kt`, `DetailedPurchaseReviewScreen.kt`, `PurchaseReviewScreen.kt`, `CommonComponents.kt`, `Buttons.kt`

#### 4.1.2 Consolidate color palette — eliminate `Color.kt` and `MaterialTheme.colorScheme` bypass

**This is the highest-priority design system task.** The ~81 color violations undermine the entire purpose of having `AppColors`.

**Step 1 — Extend `AppColors.kt` with missing semantic constants:**
```kotlin
object AppColors {
    // ... existing colors ...

    // NEW — fill gaps found in audit
    val OnButton = Color.White              // text/icon on colored buttons
    val Secondary = Color(0xFF388E3C)       // green (scanner, confirm actions)
    val Star = Color(0xFFF59E0B)            // gold accent (favorites, ratings)
    val NavBarBackground = Color(0xFF2D3748) // bottom nav dark surface
}
```

**Step 2 — Rewrite `Theme.kt` to source from `AppColors`:**
```kotlin
private val LightColorScheme = lightColorScheme(
    primary = AppColors.Info,
    onPrimary = AppColors.OnButton,
    secondary = AppColors.Secondary,
    background = AppColors.Background,
    surface = AppColors.CardBackground,
    error = AppColors.Error,
    onBackground = AppColors.TextPrimary,
    onSurface = AppColors.TextPrimary,
    onSurfaceVariant = AppColors.TextDark,
    // ... all slots reference AppColors
)
```

**Step 3 — Delete `Color.kt`** — all its constants are now redundant.

**Step 4 — Replace all `MaterialTheme.colorScheme.*` in screens/components/dialogs** with direct `AppColors.*` references. This is a mechanical find-and-replace across ~57 call sites.

**Step 5 — Replace all `Color.White` / `Color.Black`** with `AppColors.OnButton` / `AppColors.TextPrimary` as appropriate (~8 call sites).

**Migration files (by violation count):**
| File | Violations | Primary replacements |
|------|-----------|---------------------|
| `DetailedPurchaseReviewScreen.kt` | 11 | `.error` → `AppColors.Error`, `.surface` → `AppColors.CardBackground` |
| `EventDetailDialog.kt` | 9 | `.onSurfaceVariant` → `AppColors.TextDark`, `.primary` → `AppColors.Info` |
| `PendingPurchasesScreen.kt` | 8 | `.error` → `AppColors.Error`, `.primary` → `AppColors.Info` |
| `MarkdownText.kt` | 7 | `.onSurfaceVariant` → `AppColors.TextDark` (×7) |
| `PurchaseReviewScreen.kt` | 6 | `.onSurfaceVariant` → `AppColors.TextDark`, `.error` → `AppColors.Error` |
| `EventCards.kt` | 6 | `.background` → `AppColors.Background`, `.error` → `AppColors.Error` |
| `PaymentSection.kt` | 4 | `Color.White` → `AppColors.OnButton` |
| `Navigator.kt` | 3 | `.background` → `AppColors.Background` |
| `EventsDetailsScreen.kt` | 3 | `.secondary` → `AppColors.Secondary` |
| Other files | ~7 | Various |

---

#### 4.1.3 Create `Spacing` and `AppTypography` token objects

```kotlin
// ui/theme/Spacing.kt
object Spacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
}

// ui/theme/AppTypography.kt
object AppTypography {
    val title = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary)
    val sectionHeader = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = AppColors.TextPrimary)
    val body = TextStyle(fontSize = 14.sp, color = AppColors.TextPrimary)
    val bodySmall = TextStyle(fontSize = 13.sp, color = AppColors.TextSecondary)
    val help = TextStyle(fontSize = 11.sp, color = AppColors.TextSecondary)
    val total = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary)
    val totalLarge = TextStyle(fontSize = 36.sp, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary)
}
```

**Migration:** Replace all literal `dp` spacing values with `Spacing.*` tokens. Replace inline `fontSize` / `fontWeight` with `AppTypography.*` styles. Update `Theme.kt` to wire `AppTypography` into the Material3 `Typography`.

---

### Phase 2 — Screen Architecture Standardization (Priority: High)

#### 4.2.1 Standardize on `sealed class Action` + `onAction()` for all screens

Every screen with business logic must follow the pattern already used by `CashierScreen` and `ScannerScreen`:

```kotlin
// pattern: State + Action + ViewModel
data class FeatureUiState(...)
sealed class FeatureAction {
    data class SomeInput(val value: String) : FeatureAction()
    data object Submit : FeatureAction()
}
class FeatureViewModel : ViewModel() {
    var uiState by mutableStateOf(FeatureUiState())
        private set
    fun onAction(action: FeatureAction) { when (action) { ... } }
}
```

**Screens requiring refactoring:**

| Screen | Current state | Target |
|--------|--------------|--------|
| `PendingPurchasesScreen` | Direct method calls on ViewModel | `sealed class PendingPurchasesAction` + `onAction()` |
| `PurchaseReviewScreen` | No ViewModel; direct callbacks | Extract `PurchaseReviewViewModel` + `PurchaseReviewAction` |
| `CodeEntryScreen` | No ViewModel; inline logic + local vars | Extract `CodeEntryViewModel` + `CodeEntryAction` |

#### 4.2.2 Fix ViewModel lifecycle — replace `remember {}` with `viewModel()`

All ViewModels must be created with `viewModel()` using a `ViewModelProvider.Factory` to survive configuration changes:

```kotlin
// BEFORE (anti-pattern)
val viewModel = remember(event.id, apiKey) { CashierViewModel(eventId, eventName, apiKey) }

// AFTER (lifecycle-safe)
val viewModel: CashierViewModel = viewModel(
    key = "${event.id}-$apiKey",
    factory = CashierViewModel.Factory(event.id, event.name, apiKey)
)
```

Each ViewModel gets a companion `Factory`:
```kotlin
class CashierViewModel(eventId: String, eventName: String, apiKey: String) : ViewModel() {
    class Factory(
        private val eventId: String,
        private val eventName: String,
        private val apiKey: String
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return CashierViewModel(eventId, eventName, apiKey) as T
        }
    }
}
```

**Affected screens:** `CashierScreen`, `ScannerScreen`, `PendingPurchasesScreen`, `DetailedPurchaseReviewScreen`

#### 4.2.3 Create a shared screen scaffold

Extract a reusable screen wrapper that provides consistent structure:

```kotlin
@Composable
fun AppScreenScaffold(
    title: String,
    onBack: (() -> Unit)? = null,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onDismissError: (() -> Unit)? = null,
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
)
```

This mirrors the desktop's uniform `Scaffold + TopAppBar + error handling` structure that each screen currently rebuilds independently.

#### 4.2.4 Standardize state holder type

All ViewModels should use the same state mechanism. Standardize on `mutableStateOf` (already dominant):

- **Migrate** `PendingPurchasesViewModel` from `MutableStateFlow<UiState>` → `var uiState by mutableStateOf(UiState())`
- This eliminates the need for `collectAsState()` in the Screen composable

---

### Phase 3 — Store & Configuration (Priority: High)

#### 4.3.1 Centralize store initialization at Application level

Move all store `initialize()` calls out of ViewModels and into a central coordinator, similar to the desktop's central init:

```kotlin
// data/StoreInitializer.kt
object StoreInitializer {
    fun initializeForEvent(context: Context, eventId: String, apiKey: String) {
        PendingItemsStore.initialize(context, eventId)
        SoldItemFileStore.initialize(context, eventId)
        RejectedPurchaseStore.initialize(context, eventId)
        PendingScansStore.initialize(context, eventId)
        CommittedScansStore.initialize(context, eventId)
        VendorRepository.initialize(apiKey, eventId)
    }
}
```

Call this **once** when the user enters a cashier/scanner session (after code confirmation), not inside each ViewModel.

**Benefits:**
- Eliminates the implicit dependency where `DetailedPurchaseReviewViewModel` assumes CashierViewModel ran first
- Prevents multiple initializations with potentially different `eventId` values
- Single point for lifecycle management

#### 4.3.2 Introduce `AppConfigStore` for user preferences

Create a configuration persistence layer inspired by the desktop's `ConfigurationStore`:

```kotlin
// config/AppConfigStore.kt
class AppConfigStore(private val context: Context) {
    private val prefs = context.getSharedPreferences("iloppis_config", Context.MODE_PRIVATE)

    var language: String
        get() = prefs.getString("language", "sv") ?: "sv"
        set(value) = prefs.edit().putString("language", value).apply()

    var lastEventId: String?
        get() = prefs.getString("last_event_id", null)
        set(value) = prefs.edit().putString("last_event_id", value).apply()

    var apiBaseUrl: String
        get() = prefs.getString("api_base_url", DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL
        set(value) = prefs.edit().putString("api_base_url", value).apply()

    companion object {
        private const val DEFAULT_BASE_URL = "https://iloppis.fly.dev/"
    }
}
```

Provide via CompositionLocal or as a singleton in `ILoppisApp.kt`.

---

### Phase 4 — Localization Compliance (Priority: High)

#### 4.4.1 Extract all hardcoded strings to `strings.xml`

All 15+ violations listed in §3.8 must be migrated to string resources.

**New string resource entries needed:**

```xml
<!-- res/values/strings.xml (Swedish default) -->
<string name="review_needs_attention">🟡 Kräver granskning (%1$d köp)</string>
<string name="review_purchase_label">📦 Köp %1$s - %2$s</string>
<string name="review_sync_auto">❌ %1$s\nSynkar automatiskt…</string>
<string name="review_seller_reason">Säljare %1$d: %2$s</string>
<string name="review_waiting_server">Väntar på server…</string>
<string name="label_error">Fel</string>
<string name="action_cancel">Avbryt</string>
<string name="action_ok">OK</string>
<string name="scan_summary">(%1$d ok, %2$d fel)</string>
<string name="action_hide">Dölj</string>
<string name="action_show">Visa</string>
<string name="action_delete">Ta bort</string>
<string name="pending_uploads_info">Information om väntande uppladdningar</string>
<string name="label_processing">Bearbetar</string>

<!-- res/values-en/strings.xml (English) -->
<string name="review_needs_attention">🟡 Needs review (%1$d purchases)</string>
<string name="review_purchase_label">📦 Purchase %1$s - %2$s</string>
<string name="review_sync_auto">❌ %1$s\nSyncing automatically…</string>
<string name="review_seller_reason">Seller %1$d: %2$s</string>
<string name="review_waiting_server">Waiting for server…</string>
<string name="label_error">Error</string>
<string name="action_cancel">Cancel</string>
<string name="action_ok">OK</string>
<string name="scan_summary">(%1$d ok, %2$d errors)</string>
<string name="action_hide">Hide</string>
<string name="action_show">Show</string>
<string name="action_delete">Delete</string>
<string name="pending_uploads_info">Pending uploads info</string>
<string name="label_processing">Processing</string>
```

#### 4.4.2 Remove hardcoded default parameters

The `CancelTextButton` composable has a hardcoded Swedish default:
```kotlin
// BEFORE
fun CancelTextButton(text: String = "Avbryt", ...)

// AFTER — no default; callers must provide a stringResource
fun CancelTextButton(text: String, ...)
```

---

### Phase 5 — Future Considerations (Priority: Low)

#### 4.5.1 Strategy pattern for mode abstraction

The desktop uses explicit Strategy interfaces for mode-specific behavior:
```java
interface CashierStrategy {
    void validateSeller(int seller);
    void persistItem(SoldItem item);
}
class LocalCashierStrategy implements CashierStrategy { ... }
class IloppisCashierStrategy implements CashierStrategy { ... }
```

The Android app currently only supports iLoppis mode (online), so this is not needed yet. However, if local/offline mode is ever added, the desktop's Strategy pattern should be adopted rather than scattering `if (isLocal)` checks throughout ViewModels.

#### 4.5.2 Dependency injection

Both apps currently use manual wiring (singletons + constructor params). If the Android app's dependency graph grows significantly, consider adopting Hilt/Koin. For now, the `CompositionLocal` + `StoreInitializer` approach from Phase 3 is sufficient.

---

## 5  Implementation Order & Effort Estimates

| Phase | Task | Effort | Risk |
|-------|------|--------|------|
| **1** | 4.1.1 `AppButton` component library | 1 day | Low — additive, then migrate callsites |
| **1** | 4.1.2 Color palette consolidation (delete `Color.kt`, fix ~81 violations) | 1.5 days | Medium — many files but mechanical |
| **1** | 4.1.3 `Spacing` + `AppTypography` tokens | 0.5 day | Low — additive, then migrate callsites |
| **2** | 4.2.1 Standardize action pattern (3 screens) | 1 day | Medium — logic extraction for `CodeEntryScreen` |
| **2** | 4.2.2 Fix ViewModel lifecycle | 0.5 day | Low — mechanical factory replacement |
| **2** | 4.2.3 Shared screen scaffold | 0.5 day | Low — extract common Scaffold wrapper |
| **2** | 4.2.4 Standardize state holder | 0.5 day | Low — `PendingPurchasesViewModel` only |
| **3** | 4.3.1 Centralize store init | 0.5 day | Medium — must verify no ordering dependency |
| **3** | 4.3.2 `AppConfigStore` preferences | 0.5 day | Low — additive |
| **4** | 4.4.1 Extract hardcoded strings | 1 day | Low — mechanical but tedious |
| **4** | 4.4.2 Remove default params | 0.25 day | Low |

**Total estimated effort:** ~8 days

**Suggested execution order:** Phase 2 (§4.2.1 only: screen architecture) → Phase 1 → Phase 4 → Phase 2 (remaining) → Phase 3

The screen architecture standardization (§4.2.1) should come **first** because it is the most impactful structural change and establishes the pattern that all other improvements build on. Phase 1 (design system tokens and color consolidation) and Phase 4 (localization) are high-impact and can be done incrementally. Phase 3 (stores/config) can follow.

---

## 6  Acceptance Criteria

- [ ] **All screens follow State + Action + ViewModel pattern** — zero screens with inline business logic or missing ViewModels
- [ ] **All colors reference `AppColors.*`** — zero `Color(0x...)`, `Color.White`, `Color.Black`, or `MaterialTheme.colorScheme.*` in screens/components/dialogs
- [ ] **`Color.kt` deleted** — `Theme.kt` sources all colors from `AppColors`
- [ ] All buttons use `AppButton` — zero raw `Button()` with inline colors outside of `AppButton` internals
- [ ] All spacing uses `Spacing.*` tokens — zero literal `dp` values for padding/gaps in screen files
- [ ] All font styles use `AppTypography.*` — zero inline `fontSize` in screen files
- [ ] All ViewModels created via `viewModel()` factory — zero `remember { ViewModel() }` patterns
- [ ] All stores initialized via `StoreInitializer` — zero `*.initialize()` calls in ViewModels
- [ ] `AppConfigStore` exists and persists at least language preference
- [ ] Zero hardcoded user-facing strings — all text from `stringResource()`
- [ ] Build succeeds: `./gradlew assembleDebug`
- [ ] Manual test: cashier flow, scanner flow, event list, code entry all functional

---

## 7  Cross-Platform Alignment Reference

The following table maps equivalent architectural concepts across the three codebases:

| Concept | Desktop (LoppisKassan) | Android (iLoppis) Current | Android (iLoppis) Target |
|---------|----------------------|--------------------------|--------------------------|
| Color palette | `AppColors.java` — **single source, strictly enforced** | `AppColors.kt` exists but bypassed ~81 times via `Color.kt` + `MaterialTheme.colorScheme` | Delete `Color.kt`; migrate all refs to `AppColors.*` |
| Button factory | `AppButton.create(text, Variant, Size)` | Ad-hoc `Button()` calls | `AppButton(text, variant, size)` composable |
| Spacing tokens | `xs/sm/md/lg/xl` constants | Literal dp values | `Spacing.xs/sm/md/lg/xl` object |
| Typography scale | 20/16/14/11/28-36px layers | Default Material3 | `AppTypography` object |
| View contract | `*PanelInterface` (Java interface) | N/A (Compose reactive) | Not needed — Compose handles this ✓ |
| State model | `*State` + `PropertyChangeSupport` | `data class *UiState` + `mutableStateOf` | Standardize across all screens |
| Action dispatch | View calls controller method | `sealed class *Action` + `onAction()` | Standardize across all screens |
| Controller lifecycle | Singleton (`getInstance()`) | `remember {}` or `viewModel()` | `viewModel()` with Factory |
| Mode strategy | `CashierStrategy` interface | N/A (online only) | Adopt if offline mode is added |
| Config persistence | `ConfigurationStore<T>` + JSON files | None | `AppConfigStore` + SharedPreferences |
| Localization | `LocalizationManager.tr("key")` + JSON | `stringResource(R.string.key)` + XML | Fix violations; no system change needed ✓ |
| Store init | Central startup in controller | Scattered across ViewModels | `StoreInitializer.initializeForEvent()` |
| Async work | `SwingWorker` / threads | Kotlin coroutines + WorkManager | No change needed ✓ |
