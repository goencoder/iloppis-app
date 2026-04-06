package se.iloppis.app.ui.screens.cashier

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import se.iloppis.app.ILoppisAppHolder
import se.iloppis.app.R
import se.iloppis.app.data.BackgroundSyncManager
import se.iloppis.app.data.PendingItemsStore
import se.iloppis.app.data.RejectedPurchaseStore
import se.iloppis.app.data.RegisterSessionManager
import se.iloppis.app.data.VendorRepository
import se.iloppis.app.data.models.PendingItem
import se.iloppis.app.data.models.SerializableSoldItemErrorCode
import se.iloppis.app.network.ILoppisClient
import se.iloppis.app.network.cashier.CashierAPI
import se.iloppis.app.network.cashier.CashierClientType
import se.iloppis.app.network.cashier.CashierPresenceHeartbeatRequest
import se.iloppis.app.network.cashier.PaymentMethod
import se.iloppis.app.network.cashier.RegisterLifecycleEventType
import se.iloppis.app.network.config.clientConfig
import se.iloppis.app.ui.util.UiText
import java.util.UUID
import java.security.MessageDigest
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
    val isOffline: Boolean = false,

    // Rejected purchase state (for badge and popups)
    val rejectedPurchasesCount: Int = 0,
    val rejectedPurchases: List<se.iloppis.app.data.models.RejectedPurchase> = emptyList(),
    val showServerErrorDialog: Boolean = false,
    val showInvalidSellerDialog: Boolean = false,
    val invalidSellerDialogData: InvalidSellerDialogData? = null,

    // Loading/error states
    val isLoading: Boolean = false,
    val isProcessingPayment: Boolean = false,
    val heartbeatDisplayName: String? = null,
    val errorMessage: UiText? = null,
    val warningMessage: UiText? = null
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
    private val apiKey: String,
    private val cashierAlias: String? = null
) : ViewModel() {

    companion object {
        private val KASSA_CODE_REGEX = Regex("^[A-Z0-9]{3}-[A-Z0-9]{3}$")
        fun factory(eventId: String, eventName: String, apiKey: String, cashierAlias: String? = null) =
            object : androidx.lifecycle.ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    CashierViewModel(eventId, eventName, apiKey, cashierAlias) as T
            }
    }

    private val _uiState = MutableStateFlow(
        CashierUiState(
            eventName = eventName,
            heartbeatDisplayName = cashierAlias?.takeIf { it.isNotBlank() }
        )
    )
    val uiState: StateFlow<CashierUiState> = _uiState.asStateFlow()
    private val cashierApi: CashierAPI = ILoppisClient(clientConfig()).create()
    private val registerSessionManager = RegisterSessionManager.getInstance(ILoppisAppHolder.appContext)
    private var rawPendingPurchasesCount: Int = 0
    private val heartbeatCoordinator = CashierHeartbeatCoordinator(
        scope = viewModelScope,
        shouldRun = { eventId.isNotBlank() && apiKey.isNotBlank() },
        requestFactory = { buildHeartbeatRequest() },
        sendHeartbeat = { request ->
            withContext(Dispatchers.IO) {
                cashierApi.updateCashierPresence(
                    authorization = "Bearer $apiKey",
                    eventId = eventId,
                    request = request
                )
            }
        },
        onHeartbeatResponse = { response ->
            val responseName = response.displayName
            _uiState.value = _uiState.value.copy(
                heartbeatDisplayName = responseName?.takeIf { it.isNotBlank() }
                    ?: _uiState.value.heartbeatDisplayName,
                isOffline = false
            )
        },
        onHeartbeatFailure = { throwable ->
            Log.w(TAG, "Cashier heartbeat failed", throwable)
            val isAuthError = (throwable as? retrofit2.HttpException)?.code() in listOf(401, 403)
            if (!isAuthError) {
                _uiState.value = _uiState.value.copy(isOffline = true)
            }
        },
        sessionManager = registerSessionManager
    )

    // Rejected purchase popup management
    private var lastPopupShown: Long = 0
    private var serverErrorShownThisSession = false
    private val MIN_POPUP_INTERVAL_MS = 5 * 60 * 1000L  // 5 minutes
    private var lastHandledAuthErrorCode: Int? = null

    init {
        ensureRegisterSessionInitialized()

        // Start BackgroundSyncManager (single sync loop, like LoppisKassan)
        val context = ILoppisAppHolder.appContext
        BackgroundSyncManager.start(context, eventId, apiKey)
        rawPendingPurchasesCount = BackgroundSyncManager.pendingPurchaseCount.value
        heartbeatCoordinator.start()

        // Always initialize with current event/api key (event can change during app session)
        VendorRepository.initialize(eventId, apiKey)
        loadVendors()

        // Observe pending count from BackgroundSyncManager (single source of truth).
        // Debounce: only show the badge after a 2s grace period so that successful
        // uploads don't cause a brief flicker of the warning icon.
        viewModelScope.launch {
            var pendingJob: kotlinx.coroutines.Job? = null
            BackgroundSyncManager.pendingPurchaseCount.collect { count ->
                rawPendingPurchasesCount = count
                pendingJob?.cancel()
                if (count == 0) {
                    // Clear immediately — no reason to delay hiding it
                    _uiState.value = _uiState.value.copy(pendingSoldItemsCount = 0)
                } else {
                    // Delay showing the badge; if sync succeeds within the
                    // grace period the count drops to 0 and the badge never appears.
                    pendingJob = launch {
                        delay(2_000L)
                        _uiState.value = _uiState.value.copy(pendingSoldItemsCount = count)
                    }
                }
            }
        }

        viewModelScope.launch {
            BackgroundSyncManager.lastSyncAuthErrorCode.collect { code ->
                val authExpiredMessage = UiText.StringResource(R.string.cashier_error_auth_expired)
                if (code == null) {
                    lastHandledAuthErrorCode = null
                    if (_uiState.value.errorMessage == authExpiredMessage) {
                        _uiState.value = _uiState.value.copy(errorMessage = null)
                    }
                    return@collect
                }
                if (lastHandledAuthErrorCode == code) {
                    return@collect
                }
                lastHandledAuthErrorCode = code
                _uiState.value = _uiState.value.copy(
                    errorMessage = authExpiredMessage
                )
            }
        }

        // Observe pending items changes for rejected count + popups
        viewModelScope.launch {
            PendingItemsStore.itemsUpdated.collect {
                Log.d(TAG, "Pending items updated, refreshing rejected count")
                refreshRejectedPurchasesCount()
                checkAndShowPopupsIfNeeded()
            }
        }

        // Fallback: check rejected counts periodically
        viewModelScope.launch {
            while (true) {
                delay(60_000)
                refreshRejectedPurchasesCount()
                checkAndShowPopupsIfNeeded()
            }
        }
    }

    override fun onCleared() {
        heartbeatCoordinator.stop()
        super.onCleared()
    }

    /**
     * Trigger an immediate sync for all pending items.
     * Used when user manually retries failed purchases.
     */
    fun triggerSync() {
        BackgroundSyncManager.triggerImmediateSync()
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
                _uiState.value = _uiState.value.copy(showServerErrorDialog = false)
            }
            is CashierAction.DismissInvalidSellerDialog -> {
                _uiState.value = _uiState.value.copy(
                    showInvalidSellerDialog = false,
                    invalidSellerDialogData = null
                )
            }
            is CashierAction.OpenReviewScreen -> {
                // This will be handled by the screen to navigate
                _uiState.value = _uiState.value.copy(
                    showInvalidSellerDialog = false,
                    invalidSellerDialogData = null
                )
            }
        }
    }

    private fun loadVendors() {
        _uiState.value = _uiState.value.copy(isLoading = true)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d(TAG, "Loading vendors from VendorRepository...")
                // Use global VendorRepository singleton - this updates the shared cache
                val allSellers = VendorRepository.refresh()

                Log.d(TAG, "Loaded ${allSellers.size} valid seller numbers from VendorRepository")
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(
                        validSellers = allSellers,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load vendors: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = UiText.StringResource(R.string.cashier_error_load_sellers, listOf(e.message ?: ""))
                    )
                }
            }
        }
    }

    private fun handleKeypadPress(digit: String) {
        when (_uiState.value.activeField) {
            ActiveField.SELLER -> {
                _uiState.value = _uiState.value.copy(sellerNumber = _uiState.value.sellerNumber + digit)
            }
            ActiveField.PRICE -> {
                _uiState.value = _uiState.value.copy(priceString = _uiState.value.priceString + digit)
            }
        }
    }

    private fun handleClear() {
        when (_uiState.value.activeField) {
            ActiveField.SELLER -> _uiState.value = _uiState.value.copy(sellerNumber = "")
            ActiveField.PRICE -> _uiState.value = _uiState.value.copy(priceString = "")
        }
    }

    private fun handleBackspace() {
        when (_uiState.value.activeField) {
            ActiveField.SELLER -> {
                if (_uiState.value.sellerNumber.isNotEmpty()) {
                    _uiState.value = _uiState.value.copy(sellerNumber = _uiState.value.sellerNumber.dropLast(1))
                }
            }
            ActiveField.PRICE -> {
                if (_uiState.value.priceString.isNotEmpty()) {
                    _uiState.value = _uiState.value.copy(priceString = _uiState.value.priceString.dropLast(1))
                }
            }
        }
    }

    private fun handleSpace() {
        if (_uiState.value.activeField == ActiveField.PRICE) {
            // Space separates multiple prices
            _uiState.value = _uiState.value.copy(priceString = _uiState.value.priceString + " ")
        }
    }

    private fun handleOk() {
        when (_uiState.value.activeField) {
            ActiveField.SELLER -> {
                // Validate seller against GLOBAL repository (always current)
                val sellerNum = _uiState.value.sellerNumber.toIntOrNull()
                val validSellers = VendorRepository.getCached() ?: emptySet()
                if (sellerNum == null || !validSellers.contains(sellerNum)) {
                    _uiState.value = _uiState.value.copy(warningMessage = UiText.StringResource(R.string.cashier_warning_invalid_seller))
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
                _uiState.value = _uiState.value.copy(activeField = ActiveField.PRICE)
            }
            ActiveField.PRICE -> {
                // Add prices to transaction list
                addPrices()
            }
        }
    }

    private fun addPrices() {
        val sellerNum = _uiState.value.sellerNumber.toIntOrNull()
        val validSellers = VendorRepository.getCached() ?: emptySet()
        if (sellerNum == null || !validSellers.contains(sellerNum)) {
            _uiState.value = _uiState.value.copy(warningMessage = UiText.StringResource(R.string.cashier_warning_invalid_seller))
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
        val priceStrings = _uiState.value.priceString.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (priceStrings.isEmpty()) {
            _uiState.value = _uiState.value.copy(warningMessage = UiText.StringResource(R.string.cashier_warning_enter_price))
            return
        }

        val newItems = mutableListOf<TransactionItem>()
        for (priceStr in priceStrings) {
            val price = priceStr.toIntOrNull()
            if (price == null || price <= 0) {
                _uiState.value = _uiState.value.copy(warningMessage = UiText.StringResource(R.string.cashier_warning_invalid_price, listOf(priceStr)))
                return
            }
            newItems.add(TransactionItem(sellerNumber = sellerNum, price = price))
        }

        // Add items to front of list, update paid amount to next hundred
        val updatedTransactions = newItems + _uiState.value.transactions
        val newTotal = updatedTransactions.sumOf { it.price }
        val nextHundred = (ceil(newTotal / 100.0) * 100).toInt()

        _uiState.value = _uiState.value.copy(
            transactions = updatedTransactions,
            sellerNumber = "",
            priceString = "",
            activeField = ActiveField.SELLER,
            paidAmount = nextHundred.toString()
        )
    }

    private fun setActiveField(field: ActiveField) {
        _uiState.value = _uiState.value.copy(activeField = field)
    }

    private fun removeItem(id: String) {
        val updatedTransactions = _uiState.value.transactions.filter { it.id != id }
        val newTotal = updatedTransactions.sumOf { it.price }
        val nextHundred = if (newTotal > 0) (ceil(newTotal / 100.0) * 100).toInt() else 0

        _uiState.value = _uiState.value.copy(
            transactions = updatedTransactions,
            paidAmount = nextHundred.toString()
        )
    }

    private fun clearAllItems() {
        _uiState.value = _uiState.value.copy(
            transactions = emptyList(),
            paidAmount = "0"
        )
    }

    private fun setPaidAmount(amount: String) {
        // Only allow digits
        val filtered = amount.filter { it.isDigit() }
        _uiState.value = _uiState.value.copy(paidAmount = filtered)
    }

    /**
     * Process checkout and register purchase locally.
     *
     * ## Local-first guarantee (aligned with LoppisKassan)
     *
     * 1. Generate stable IDs (purchaseId + itemIds)
     * 2. Persist pending items to local disk
     * 3. Clear UI and show receipt only after local persistence succeeds
     * 4. Trigger background sync to upload pending purchases
     * 5. On success: items deleted from PendingItemsStore
     * 6. On failure: items remain on disk, retried every 30s
     *
     * @param method Payment method (CASH or SWISH)
     */
    private fun checkout(method: PaymentMethodType) {
        val transactionsSnapshot = _uiState.value.transactions
        if (transactionsSnapshot.isEmpty()) {
            _uiState.value = _uiState.value.copy(warningMessage = UiText.StringResource(R.string.cashier_warning_no_items))
            return
        }

        val purchaseTotal = _uiState.value.total
        val purchaseId = se.iloppis.app.utils.Ulid.random()
        val paymentMethod = when (method) {
            PaymentMethodType.CASH -> PaymentMethod.KONTANT
            PaymentMethodType.SWISH -> PaymentMethod.SWISH
        }
        val timestamp = java.time.Instant.now().toString()

        val pendingItems = transactionsSnapshot.map { tx ->
            PendingItem(
                itemId = tx.id,
                purchaseId = purchaseId,
                sellerId = tx.sellerNumber,
                price = tx.price,
                paymentMethod = paymentMethod,
                errorText = "",
                timestamp = timestamp
            )
        }

        val completedPurchase = CompletedPurchase(
            purchaseId = purchaseId,
            items = transactionsSnapshot,
            total = purchaseTotal,
            paymentMethod = method
        )

        _uiState.value = _uiState.value.copy(
            isProcessingPayment = true,
            warningMessage = null,
            errorMessage = null
        )

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Durability first: write pending items to disk before clearing cashier UI.
                BackgroundSyncManager.enqueueItems(pendingItems)
                BackgroundSyncManager.triggerImmediateSync()

                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(
                        transactions = emptyList(),
                        sellerNumber = "",
                        priceString = "",
                        activeField = ActiveField.SELLER,
                        paidAmount = "0",
                        lastPurchase = completedPurchase,
                        lastCheckoutQueuedOffline = false,
                        warningMessage = null,
                        isProcessingPayment = false
                    )
                }
            } catch (t: Throwable) {
                Log.e(TAG, "Failed to persist pending items", t)
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(
                        isProcessingPayment = false,
                        errorMessage = UiText.StringResource(
                            R.string.cashier_error_save_local,
                            listOf(t.message ?: "")
                        )
                    )
                }
            }

            refreshRejectedPurchasesCount()
        }
    }

    private fun dismissWarning() {
        _uiState.value = _uiState.value.copy(warningMessage = null, lastCheckoutQueuedOffline = false)
    }

    private fun dismissError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    private fun buildHeartbeatRequest(): CashierPresenceHeartbeatRequest {
        val snapshot = _uiState.value.toCashierPresenceSnapshot(
            rawPendingPurchasesCount = rawPendingPurchasesCount
        )
        val sanitizedSnapshot = snapshot.copy(
            displayName = sanitizeHeartbeatDisplayName(snapshot.displayName)
        )

        return sanitizedSnapshot.toHeartbeatRequest(
            clientType = CashierClientType.CASHIER_CLIENT_TYPE_ANDROID,
            sessionManager = registerSessionManager
        )
    }

    private fun ensureRegisterSessionInitialized() {
        val current = registerSessionManager.getCurrent()
        val expectedRegisterId = deriveRegisterId()
        val canReuse = current != null &&
            current.eventId == eventId &&
            current.registerId == expectedRegisterId &&
            current.state != RegisterSessionManager.State.CLOSED
        if (canReuse) {
            return
        }

        registerSessionManager.openSession(
            eventId = eventId,
            registerId = expectedRegisterId
        )
    }

    private fun deriveRegisterId(): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
                .digest(apiKey.toByteArray(Charsets.UTF_8))
            val hashedSuffix = digest.joinToString(separator = "") { byte ->
                "%02x".format(byte.toInt() and 0xff)
            }.take(16)
            "android-$hashedSuffix"
        } catch (e: Exception) {
            "android-${getOrCreateRegisterIdSeed()}"
        }
    }

    private fun sanitizeHeartbeatDisplayName(displayName: String?): String? {
        val trimmed = displayName?.trim().orEmpty()
        if (trimmed.isBlank()) {
            return null
        }
        if (KASSA_CODE_REGEX.matches(trimmed.uppercase())) {
            return null
        }
        return trimmed
    }

    private fun getOrCreateRegisterIdSeed(): String {
        val prefs = ILoppisAppHolder.appContext.getSharedPreferences("register_id", Context.MODE_PRIVATE)
        val existing = prefs.getString("seed", null)
        if (!existing.isNullOrBlank()) {
            return existing
        }
        val seed = UUID.randomUUID().toString().replace("-", "").take(16)
        prefs.edit().putString("seed", seed).apply()
        return seed
    }

    suspend fun requestCloseAndFlush(): Boolean {
        heartbeatCoordinator.stop()
        val closeSucceeded = when (registerSessionManager.getCurrent()?.state) {
            RegisterSessionManager.State.OPEN -> {
                if (!registerSessionManager.requestClose()) {
                    false
                } else {
                    flushRequestedClose()
                }
            }
            RegisterSessionManager.State.CLOSE_REQUESTED -> flushRequestedClose()
            RegisterSessionManager.State.CLOSED -> flushConfirmedClose()
            else -> false
        }

        if (!closeSucceeded) {
            heartbeatCoordinator.start()
        }

        return closeSucceeded
    }

    fun requestCloseIfIdle() {
        if (_uiState.value.pendingSoldItemsCount > 0) {
            return
        }
        val state = registerSessionManager.getCurrent()?.state ?: return
        if (state != RegisterSessionManager.State.OPEN &&
            state != RegisterSessionManager.State.CLOSE_REQUESTED
        ) {
            return
        }
        viewModelScope.launch {
            requestCloseAndFlush()
        }
    }

    private suspend fun flushRequestedClose(): Boolean {
        val current = registerSessionManager.getCurrent() ?: return false
        if (current.state != RegisterSessionManager.State.CLOSE_REQUESTED) {
            return false
        }
        if (
            current.pendingLifecycleEvent == RegisterLifecycleEventType.REGISTER_LIFECYCLE_CLOSE_REQUESTED &&
            !sendHeartbeatOnce()
        ) {
            return false
        }
        if (!registerSessionManager.confirmClose()) {
            return false
        }
        return flushConfirmedClose()
    }

    private suspend fun flushConfirmedClose(): Boolean {
        val current = registerSessionManager.getCurrent() ?: return false
        if (
            current.state != RegisterSessionManager.State.CLOSED &&
            current.state != RegisterSessionManager.State.CLOSE_REQUESTED
        ) {
            return false
        }
        if (
            current.pendingLifecycleEvent != null &&
            current.pendingLifecycleEvent != RegisterLifecycleEventType.REGISTER_LIFECYCLE_CLOSE_CONFIRMED
        ) {
            return false
        }
        if (
            current.pendingLifecycleEvent ==
            RegisterLifecycleEventType.REGISTER_LIFECYCLE_CLOSE_CONFIRMED &&
            !sendHeartbeatOnce()
        ) {
            return false
        }
        val updated = registerSessionManager.getCurrent() ?: return false
        return updated.state == RegisterSessionManager.State.CLOSED &&
            updated.pendingLifecycleEvent == null
    }

    private suspend fun sendHeartbeatOnce(): Boolean {
        if (eventId.isBlank() || apiKey.isBlank()) return false
        val request = buildHeartbeatRequest()
        try {
            withContext(Dispatchers.IO) {
                cashierApi.updateCashierPresence(
                    authorization = "Bearer $apiKey",
                    eventId = eventId,
                    request = request
                )
            }
            registerSessionManager.clearPendingLifecycleEvent(
                expectedLifecycleEvent = request.lifecycleEventType,
                expectedSessionId = request.sessionId
            )
            return true
        } catch (t: Throwable) {
            Log.w(TAG, "Cashier heartbeat flush failed", t)
            return false
        }
    }

    /**
     * Refresh count of pending purchases from PendingItemsStore.
     */
    internal fun refreshRejectedPurchasesCount() {
        viewModelScope.launch(Dispatchers.IO) {
            val (_, warningCount, criticalCount) = PendingItemsStore.getErrorCounts()
            // Only count purchases with actual errors (warning/critical).
            // Items just waiting for upload (infoCount) are covered by
            // BackgroundSyncManager.pendingPurchaseCount with its own debounce.
            val count = warningCount + criticalCount

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
                _uiState.value = _uiState.value.copy(
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
        if (!_uiState.value.isIdle) {
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
                        _uiState.value = _uiState.value.copy(showServerErrorDialog = true)
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
                    _uiState.value = _uiState.value.copy(
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
