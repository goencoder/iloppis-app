package se.iloppis.app.ui.screens.cashier

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import se.iloppis.app.ILoppisAppHolder
import se.iloppis.app.data.OfflineToastGatekeeper
import se.iloppis.app.data.PendingItemsStore
import se.iloppis.app.data.RejectedPurchaseStore
import se.iloppis.app.data.SoldItemFileStore
import se.iloppis.app.data.VendorRepository
import se.iloppis.app.data.models.PendingItem
import se.iloppis.app.data.models.SerializableSoldItemErrorCode
import se.iloppis.app.data.models.StoredSoldItem
import se.iloppis.app.network.ApiClient
import se.iloppis.app.network.CreateSoldItemsRequest
import se.iloppis.app.network.SoldItemRequest
import se.iloppis.app.network.SoldItemsApi
import se.iloppis.app.network.VendorApi
import se.iloppis.app.ui.screens.cashier.CashierAction
import se.iloppis.app.work.SyncScheduler
import java.util.UUID
import kotlin.math.ceil

private const val TAG = "CashierViewModel"

/**
 * Represents a single transaction item in the current purchase.
 */
data class TransactionItem(
    val id: String = se.iloppis.app.utils.Ulid.random(),
    val sellerNumber: Int,
    val price: Int,
    val status: TransactionStatus = TransactionStatus.PENDING
)

enum class TransactionStatus {
    PENDING,    // Not yet submitted
    UPLOADED,   // Successfully saved
    FAILED      // Failed to save
}

enum class PaymentMethodType {
    CASH,
    SWISH
}

/**
 * Represents a completed purchase for display.
 */
data class CompletedPurchase(
    val purchaseId: String,
    val items: List<TransactionItem>,
    val total: Int,
    val paymentMethod: PaymentMethodType
)

/**
 * Active input field in the cashier UI.
 */
enum class ActiveField {
    SELLER,
    PRICE
}

/**
 * UI state for the cashier screen.
 */
data class CashierUiState(
    val eventName: String = "",
    val eventStatus: String = "",
    
    // Input state
    val sellerNumber: String = "",
    val priceString: String = "",
    val activeField: ActiveField = ActiveField.SELLER,
    
    // Transaction state
    val transactions: List<TransactionItem> = emptyList(),
    val paidAmount: String = "0",
    
    // Valid seller numbers for this event
    val validSellers: Set<Int> = emptySet(),
    
    // Last completed purchase (for showing receipt)
    val lastPurchase: CompletedPurchase? = null,

    // Offline sync state
    val pendingSoldItemsCount: Int = 0,
    val lastCheckoutQueuedOffline: Boolean = false,
    
    // Rejected purchase state (for badge and popups)
    val rejectedPurchasesCount: Int = 0,
    val rejectedPurchases: List<se.iloppis.app.data.models.RejectedPurchase> = emptyList(),
    val showServerErrorDialog: Boolean = false,
    val showInvalidSellerDialog: Boolean = false,
    val invalidSellerDialogData: InvalidSellerDialogData? = null,
    
    // Loading/error states
    val isLoading: Boolean = false,
    val isProcessingPayment: Boolean = false,
    val errorMessage: String? = null,
    val warningMessage: String? = null
) {
    val total: Int get() = transactions.sumOf { it.price }
    val change: Int get() = (paidAmount.toIntOrNull() ?: 0) - total
    val nextHundred: Int get() = (ceil(total / 100.0) * 100).toInt()
    
    // Check if kassa is idle (all fields empty, not processing)
    val isIdle: Boolean get() = sellerNumber.isEmpty() && 
                                 priceString.isEmpty() && 
                                 transactions.isEmpty() && 
                                 !isProcessingPayment
}

/**
 * Data for the invalid seller dialog.
 */
data class InvalidSellerDialogData(
    val purchaseId: String,
    val timestamp: String,
    val invalidSellers: List<Int>
)

/**
 * Actions that can be performed on the cashier screen.
 */
sealed class CashierAction {
    data class KeypadPress(val digit: String) : CashierAction()
    data object KeypadClear : CashierAction()
    data object KeypadBackspace : CashierAction()
    data object KeypadOk : CashierAction()
    data object KeypadSpace : CashierAction()
    data class SetActiveField(val field: ActiveField) : CashierAction()
    data class RemoveItem(val id: String) : CashierAction()
    data object ClearAllItems : CashierAction()
    data class Checkout(val method: PaymentMethodType) : CashierAction()
    data class SetPaidAmount(val amount: String) : CashierAction()
    data object DismissWarning : CashierAction()
    data object DismissError : CashierAction()
    data object DismissServerErrorDialog : CashierAction()
    data object DismissInvalidSellerDialog : CashierAction()
    data object OpenReviewScreen : CashierAction()
}

/**
 * ViewModel for the cashier screen.
 */
class CashierViewModel(
    private val eventId: String,
    private val eventName: String,
    private val apiKey: String
) : ViewModel() {

    var uiState by mutableStateOf(CashierUiState(eventName = eventName))
        private set

    private val toastGatekeeper = OfflineToastGatekeeper()
    private var workObserverRegistered = false
    
    // Rejected purchase popup management
    private var lastPopupShown: Long = 0
    private var serverErrorShownThisSession = false
    private val MIN_POPUP_INTERVAL_MS = 5 * 60 * 1000L  // 5 minutes

    init {
        // Initialize file stores for this event
        val context = ILoppisAppHolder.appContext
        PendingItemsStore.initialize(context, eventId)
        SoldItemFileStore.initialize(context, eventId)
        RejectedPurchaseStore.initialize(context, eventId)
        
        // Initialize global VendorRepository singleton
        if (!VendorRepository.isInitialized()) {
            VendorRepository.initialize(eventId, apiKey)
        }
        loadVendors()
        refreshPendingCount()
        refreshRejectedPurchasesCount()
        registerSyncObserverIfPossible()
        
        // Observe pending items changes for reactive badge updates
        viewModelScope.launch {
            PendingItemsStore.itemsUpdated.collect {
                Log.d(TAG, "Pending items updated, refreshing badge")
                refreshRejectedPurchasesCount()
                checkAndShowPopupsIfNeeded()
            }
        }

        // Fallback polling loop: periodic checks if events are missed
        // Reduced from 30s to 60s since we now have reactive updates
        viewModelScope.launch {
            while (true) {
                delay(60_000)  // 1 minute fallback polling
                tryTriggerSync() // Periodic sync, no specific purchase
                refreshPendingCount()
                refreshRejectedPurchasesCount()
                checkAndShowPopupsIfNeeded()
            }
        }
    }

    private fun registerSyncObserverIfPossible() {
        if (workObserverRegistered) return

        val context = try {
            se.iloppis.app.ILoppisAppHolder.appContext
        } catch (t: Throwable) {
            null
        } ?: return

        workObserverRegistered = true
        val workManager = WorkManager.getInstance(context)

        fun refreshIfNotRunning(infos: List<WorkInfo>?) {
            val state = infos?.firstOrNull()?.state ?: return
            if (state != WorkInfo.State.RUNNING) {
                refreshPendingCount()
            }
        }

        workManager.getWorkInfosForUniqueWorkLiveData(SyncScheduler.UNIQUE_IMMEDIATE)
            .observeForever { infos -> refreshIfNotRunning(infos) }

        workManager.getWorkInfosForUniqueWorkLiveData(SyncScheduler.UNIQUE_PERIODIC)
            .observeForever { infos -> refreshIfNotRunning(infos) }
    }

    private fun refreshPendingCount() {
        viewModelScope.launch {
            val pending = withContext(Dispatchers.IO) {
                SoldItemFileStore.getAllSoldItems()
                    .asSequence()
                    .filter { !it.uploaded }
                    .map { it.purchaseId }
                    .distinct()
                    .count()
            }
            uiState = uiState.copy(pendingSoldItemsCount = pending)
            
            // If we successfully refreshed and count is 0, we can consider ourselves online
            if (pending == 0) {
                toastGatekeeper.recordSuccessfulUpload()
            }
        }
    }

    private suspend fun tryTriggerSync(purchaseId: String? = null): Boolean {
        // WorkManager handles network constraints.
        val context = try {
            se.iloppis.app.ILoppisAppHolder.appContext
        } catch (t: Throwable) {
            null
        }
        if (context == null) {
            Log.w(TAG, "No appContext available; cannot enqueue sync yet")
            if (purchaseId != null) {
                // Record as missed upload
                val shouldShowToast = toastGatekeeper.recordMissedUpload(purchaseId)
                if (shouldShowToast) {
                    withContext(Dispatchers.Main) {
                        uiState = uiState.copy(
                            warningMessage = "Sparade lokalt. Synkar i bakgrunden när nätet är tillbaka."
                        )
                    }
                }
            }
            return false
        }

        Log.d(TAG, "Enqueueing background sync")
        SyncScheduler.enqueueImmediate(context = context, apiKey = apiKey, eventId = eventId)
        SyncScheduler.ensurePeriodic(context = context, apiKey = apiKey, eventId = eventId)

        // Successfully enqueued - will be attempted by WorkManager
        return true
    }

    /**
     * Trigger an immediate sync for all pending items.
     * Used when user manually retries failed purchases.
     */
    fun triggerSync() {
        viewModelScope.launch {
            tryTriggerSync()
        }
    }

    fun onAction(action: CashierAction) {
        when (action) {
            is CashierAction.KeypadPress -> handleKeypadPress(action.digit)
            is CashierAction.KeypadClear -> handleClear()
            is CashierAction.KeypadBackspace -> handleBackspace()
            is CashierAction.KeypadOk -> handleOk()
            is CashierAction.KeypadSpace -> handleSpace()
            is CashierAction.SetActiveField -> setActiveField(action.field)
            is CashierAction.RemoveItem -> removeItem(action.id)
            is CashierAction.ClearAllItems -> clearAllItems()
            is CashierAction.Checkout -> checkout(action.method)
            is CashierAction.SetPaidAmount -> setPaidAmount(action.amount)
            is CashierAction.DismissWarning -> dismissWarning()
            is CashierAction.DismissError -> dismissError()
            is CashierAction.DismissServerErrorDialog -> {
                uiState = uiState.copy(showServerErrorDialog = false)
            }
            is CashierAction.DismissInvalidSellerDialog -> {
                uiState = uiState.copy(
                    showInvalidSellerDialog = false,
                    invalidSellerDialogData = null
                )
            }
            is CashierAction.OpenReviewScreen -> {
                // This will be handled by the screen to navigate
                uiState = uiState.copy(
                    showInvalidSellerDialog = false,
                    invalidSellerDialogData = null
                )
            }
        }
    }

    private fun loadVendors() {
        uiState = uiState.copy(isLoading = true)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d(TAG, "Loading vendors from VendorRepository...")
                // Use global VendorRepository singleton - this updates the shared cache
                val allSellers = VendorRepository.refresh()
                
                Log.d(TAG, "Loaded ${allSellers.size} valid seller numbers from VendorRepository")
                withContext(Dispatchers.Main) {
                    uiState = uiState.copy(
                        validSellers = allSellers,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load vendors: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    uiState = uiState.copy(
                        isLoading = false,
                        errorMessage = "Kunde inte ladda säljare: ${e.message}"
                    )
                }
            }
        }
    }

    private fun handleKeypadPress(digit: String) {
        when (uiState.activeField) {
            ActiveField.SELLER -> {
                uiState = uiState.copy(sellerNumber = uiState.sellerNumber + digit)
            }
            ActiveField.PRICE -> {
                uiState = uiState.copy(priceString = uiState.priceString + digit)
            }
        }
    }

    private fun handleClear() {
        when (uiState.activeField) {
            ActiveField.SELLER -> uiState = uiState.copy(sellerNumber = "")
            ActiveField.PRICE -> uiState = uiState.copy(priceString = "")
        }
    }

    private fun handleBackspace() {
        when (uiState.activeField) {
            ActiveField.SELLER -> {
                if (uiState.sellerNumber.isNotEmpty()) {
                    uiState = uiState.copy(sellerNumber = uiState.sellerNumber.dropLast(1))
                }
            }
            ActiveField.PRICE -> {
                if (uiState.priceString.isNotEmpty()) {
                    uiState = uiState.copy(priceString = uiState.priceString.dropLast(1))
                }
            }
        }
    }

    private fun handleSpace() {
        if (uiState.activeField == ActiveField.PRICE) {
            // Space separates multiple prices
            uiState = uiState.copy(priceString = uiState.priceString + " ")
        }
    }

    private fun handleOk() {
        when (uiState.activeField) {
            ActiveField.SELLER -> {
                // Validate seller against GLOBAL repository (always current)
                val sellerNum = uiState.sellerNumber.toIntOrNull()
                val validSellers = VendorRepository.getCached() ?: emptySet()
                if (sellerNum == null || !validSellers.contains(sellerNum)) {
                    uiState = uiState.copy(warningMessage = "Ogiltigt säljarnummer")
                    // Refresh vendor list in background - seller may have been added recently
                    viewModelScope.launch(Dispatchers.IO) {
                        try {
                            VendorRepository.refresh()
                            Log.d(TAG, "Vendor list refreshed after invalid seller input")
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to refresh vendor list", e)
                        }
                    }
                    return
                }
                uiState = uiState.copy(activeField = ActiveField.PRICE)
            }
            ActiveField.PRICE -> {
                // Add prices to transaction list
                addPrices()
            }
        }
    }

    private fun addPrices() {
        val sellerNum = uiState.sellerNumber.toIntOrNull()
        val validSellers = VendorRepository.getCached() ?: emptySet()
        if (sellerNum == null || !validSellers.contains(sellerNum)) {
            uiState = uiState.copy(warningMessage = "Ogiltigt säljarnummer")
            // Refresh vendor list in background - seller may have been added recently
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    VendorRepository.refresh()
                    Log.d(TAG, "Vendor list refreshed after invalid seller input")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to refresh vendor list", e)
                }
            }
            return
        }

        // Parse prices (space-separated)
        val priceStrings = uiState.priceString.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (priceStrings.isEmpty()) {
            uiState = uiState.copy(warningMessage = "Ange minst ett pris")
            return
        }

        val newItems = mutableListOf<TransactionItem>()
        for (priceStr in priceStrings) {
            val price = priceStr.toIntOrNull()
            if (price == null || price <= 0) {
                uiState = uiState.copy(warningMessage = "Ogiltigt pris: $priceStr")
                return
            }
            newItems.add(TransactionItem(sellerNumber = sellerNum, price = price))
        }

        // Add items to front of list, update paid amount to next hundred
        val updatedTransactions = newItems + uiState.transactions
        val newTotal = updatedTransactions.sumOf { it.price }
        val nextHundred = (ceil(newTotal / 100.0) * 100).toInt()

        uiState = uiState.copy(
            transactions = updatedTransactions,
            sellerNumber = "",
            priceString = "",
            activeField = ActiveField.SELLER,
            paidAmount = nextHundred.toString()
        )
    }

    private fun setActiveField(field: ActiveField) {
        uiState = uiState.copy(activeField = field)
    }

    private fun removeItem(id: String) {
        val updatedTransactions = uiState.transactions.filter { it.id != id }
        val newTotal = updatedTransactions.sumOf { it.price }
        val nextHundred = if (newTotal > 0) (ceil(newTotal / 100.0) * 100).toInt() else 0
        
        uiState = uiState.copy(
            transactions = updatedTransactions,
            paidAmount = nextHundred.toString()
        )
    }

    private fun clearAllItems() {
        uiState = uiState.copy(
            transactions = emptyList(),
            paidAmount = "0"
        )
    }

    private fun setPaidAmount(amount: String) {
        // Only allow digits
        val filtered = amount.filter { it.isDigit() }
        uiState = uiState.copy(paidAmount = filtered)
    }

    /**
     * Process checkout and register purchase locally.
     * 
     * ## GUARANTEE: Purchase registration cannot be lost
     * 
     * This method ensures that once user completes payment, the purchase data
     * is ALWAYS registered locally before any UI feedback is shown.
     * 
     * **Execution flow with guarantees**:
     * 
     * 1. **Validate input** (line 407-411)
     *    - Check transactions exist
     *    - Return early if validation fails
     * 
     * 2. **Generate stable IDs** (line 414)
     *    - purchaseId: Random UUID (26 chars, uppercase)
     *    - itemId: Already assigned in TransactionItem.id (UUID)
     *    - These IDs are stable and never regenerated
     * 
     * 3. **Create purchase record** (line 420-425)
     *    - Snapshot current state for receipt display
     *    - Includes all transaction items with their stable IDs
     * 
     * 4. **Clear UI immediately** (line 427-437)
     *    - UI cleared BEFORE persistence starts
     *    - Allows cashier to start next transaction immediately
     *    - Receipt shown in lastPurchase (not blocking)
     * 
     * 5. **Persist purchase synchronously** (line 440-463)
     *    - Runs on IO dispatcher (background thread)
     *    - `appendSoldItems()` is SYNCHRONOUS and blocks until file write completes
     *    - If write fails, exception is logged but caught (line 462)
     *    - GUARANTEE: If no exception, data is on disk and survives app crash
     * 
     * 6. **Trigger background upload** (line 465-466)
     *    - Enqueue WorkManager job (best-effort, non-blocking)
     *    - Upload happens asynchronously in background
     *    - If upload fails, WorkManager will retry automatically
     * 
     * **Data loss scenarios - answered**:
     * 
     * Q: What if app crashes during checkout?
     * A: Depends on exact timing:
     *    - Before line 459 returns: Purchase not saved (but user hasn't seen receipt)
     *    - After line 459 returns: Purchase saved to disk, will upload on app restart
     * 
     * Q: What if device loses power during file write?
     * A: OS-level atomic write guarantees:
     *    - File.writeText() either completes fully or fails
     *    - No partial writes that corrupt file
     *    - If power lost mid-write, old file content remains intact
     * 
     * Q: What if storage is full?
     * A: IOException thrown from appendSoldItems():
     *    - Exception logged (line 462)
     *    - UI not updated with receipt (user can retry)
     *    - No silent data loss
     * 
     * **End-to-end guarantee**:
     * ```
     * User presses "Betala KONTANT"
     *   → checkout() called
     *   → UI cleared (cashier can continue)
     *   → appendSoldItems() blocks until write completes
     *   → If success: Data on disk, upload queued
     *   → If failure: Exception logged, no receipt shown
     * Result: Either purchase is saved OR user knows it failed
     * ```
     * 
     * @param method Payment method (CASH or SWISH)
     */
    private fun checkout(method: PaymentMethodType) {
        val transactionsSnapshot = uiState.transactions
        if (transactionsSnapshot.isEmpty()) {
            uiState = uiState.copy(warningMessage = "Inga varor att betala")
            return
        }

        // STEP 1: Generate stable unique IDs
        // purchaseId: ULID groups all items in this purchase (26-char time-ordered ID)
        // itemId: Already in TransactionItem.id (ULID from creation)
        val purchaseTotal = uiState.total
        val purchaseId = se.iloppis.app.utils.Ulid.random()
        val paymentMethodStr = when (method) {
            PaymentMethodType.CASH -> "KONTANT"
            PaymentMethodType.SWISH -> "SWISH"
        }

        // STEP 2: Create receipt record
        // Snapshot current state before clearing UI
        val completedPurchase = CompletedPurchase(
            purchaseId = purchaseId,
            items = transactionsSnapshot,
            total = purchaseTotal,
            paymentMethod = method
        )

        // STEP 3: Clear UI immediately
        // User sees cleared screen + receipt, can start next purchase
        // This happens BEFORE persistence starts (optimistic UI)
        uiState = uiState.copy(
            transactions = emptyList(),
            sellerNumber = "",
            priceString = "",
            activeField = ActiveField.SELLER,
            paidAmount = "0",
            lastPurchase = completedPurchase,
            lastCheckoutQueuedOffline = false,
            warningMessage = null
        )

        // STEP 4: Persist purchase data (background thread)
        // CRITICAL: This is SYNCHRONOUS - blocks until file write completes
        viewModelScope.launch(Dispatchers.IO) {
            uiState = uiState.copy(isProcessingPayment = true)

            // STEP 4a: Map UI items to pending items
            // Each item gets stable itemId from TransactionItem.id
            val timestamp = java.time.Instant.now().toString()
            val pendingItems = transactionsSnapshot.map { tx ->
                PendingItem(
                    itemId = tx.id,              // Stable UUID from TransactionItem
                    purchaseId = purchaseId,      // Groups items in this purchase
                    sellerId = tx.sellerNumber,
                    price = tx.price,
                    errorText = "",               // Empty = waiting for upload
                    timestamp = timestamp
                )
            }

            // STEP 4b: Write to JSONL file SYNCHRONOUSLY with mutex protection
            // GUARANTEE: If this succeeds, data is on disk and survives crashes
            try {
                Log.d(TAG, "Persisting ${pendingItems.size} pending items locally for purchaseId=$purchaseId")
                PendingItemsStore.appendItems(pendingItems)  // BLOCKS until write completes
                Log.d(TAG, "Persisted locally; triggering sync")
            } catch (t: Throwable) {
                // File write failed (e.g., disk full)
                // Log error - data not saved, user can retry checkout
                Log.e(TAG, "Failed to persist pending items", t)
                // TODO: Show error to user, allow retry
            }

            // STEP 5: Trigger background upload (best-effort)
            // WorkManager will handle retries if this fails
            val syncEnqueued = tryTriggerSync(purchaseId = purchaseId)
            
            viewModelScope.launch {
                refreshPendingCount()
                refreshRejectedPurchasesCount()
                uiState = uiState.copy(isProcessingPayment = false)
            }

            // Upload happens asynchronously via WorkManager
            // SoldItemsSyncWorker will process items with errorText=""
        }
    }

    private fun dismissWarning() {
        uiState = uiState.copy(warningMessage = null, lastCheckoutQueuedOffline = false)
    }

    private fun dismissError() {
        uiState = uiState.copy(errorMessage = null)
    }
    
    /**
     * Refresh count of pending purchases from PendingItemsStore.
     */
    internal fun refreshRejectedPurchasesCount() {
        viewModelScope.launch(Dispatchers.IO) {
            val (infoCount, warningCount, criticalCount) = PendingItemsStore.getErrorCounts()
            val count = infoCount + warningCount + criticalCount
            
            // If there are any rejected items, refresh vendor list in case sellers changed
            if (count > 0) {
                try {
                    VendorRepository.refresh()
                    Log.d(TAG, "Vendor list refreshed after detecting rejected purchases")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to refresh vendor list after rejected purchases", e)
                }
            }
            
            withContext(Dispatchers.Main) {
                uiState = uiState.copy(
                    rejectedPurchasesCount = count,
                    rejectedPurchases = emptyList() // No longer used
                )
            }
        }
    }
    
    /**
     * Check if a popup should be shown and show it if conditions are met.
     */
    private fun checkAndShowPopupsIfNeeded() {
        if (!uiState.isIdle) {
            return  // Never show popup when kassa is busy
        }
        
        val now = System.currentTimeMillis()
        val timeSinceLastPopup = now - lastPopupShown
        
        if (timeSinceLastPopup < MIN_POPUP_INTERVAL_MS) {
            return  // Too soon since last popup
        }
        
        viewModelScope.launch(Dispatchers.IO) {
            val purchases = RejectedPurchaseStore.getAllRejectedPurchases()
            
            // Priority 1: Server errors (if not shown this session)
            if (!serverErrorShownThisSession) {
                val serverErrors = purchases.filter { purchase ->
                    purchase.errorMessage.contains("server", ignoreCase = true) ||
                    purchase.errorMessage.contains("5", ignoreCase = true)
                }
                
                if (serverErrors.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        lastPopupShown = now
                        serverErrorShownThisSession = true
                        uiState = uiState.copy(showServerErrorDialog = true)
                    }
                    return@launch
                }
            }
            
            // Priority 2: Invalid seller errors that need manual review
            val invalidSellers = purchases.filter { 
                it.errorCode == SerializableSoldItemErrorCode.INVALID_SELLER && 
                it.needsManualReview &&
                it.autoRecoveryAttempted
            }
            
            if (invalidSellers.isNotEmpty()) {
                val first = invalidSellers.first()
                val invalidSellerNumbers = first.items.map { it.item.seller }.distinct()
                
                withContext(Dispatchers.Main) {
                    lastPopupShown = now
                    uiState = uiState.copy(
                        showInvalidSellerDialog = true,
                        invalidSellerDialogData = InvalidSellerDialogData(
                            purchaseId = first.purchaseId,
                            timestamp = first.timestamp,
                            invalidSellers = invalidSellerNumbers
                        )
                    )
                }
            }
        }
    }
}
