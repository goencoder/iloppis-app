package se.iloppis.app.data

import android.content.Context
import se.iloppis.app.utils.AppLog as Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import se.iloppis.app.data.models.PendingItem
import se.iloppis.app.data.models.RejectedItemWithDetails
import se.iloppis.app.data.models.RejectedPurchase
import se.iloppis.app.data.models.SerializableSoldItemErrorCode
import se.iloppis.app.data.models.StoredSoldItem
import se.iloppis.app.network.ILoppisClient
import se.iloppis.app.network.cashier.CashierAPI
import se.iloppis.app.network.cashier.CashierApiResponse
import se.iloppis.app.network.cashier.SoldItemObject
import se.iloppis.app.network.cashier.SoldItemsRequest
import se.iloppis.app.network.config.clientConfig
import retrofit2.HttpException
import java.time.Instant

/**
 * Background sync manager for sold items.
 *
 * Checkout data is durable before the UI clears, uploads use one classification
 * pipeline, and transient network failures are retried without losing local rows.
 */
object BackgroundSyncManager {
    private const val TAG = "BackgroundSyncManager"
    private const val SYNC_INTERVAL_MS = 30_000L
    internal const val UNKNOWN_ERROR_FALLBACK = "Unknown error"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var syncJob: Job? = null
    private var triggerJob: Job? = null

    private var eventId: String? = null
    private var apiKey: String? = null
    private var initialized = false
    private val api: CashierAPI by lazy { ILoppisClient(clientConfig()).create<CashierAPI>() }

    private val _pendingPurchaseCount = MutableStateFlow(0)
    val pendingPurchaseCount: StateFlow<Int> = _pendingPurchaseCount.asStateFlow()

    private val _lastSyncHadNetworkError = MutableStateFlow(false)
    val lastSyncHadNetworkError: StateFlow<Boolean> = _lastSyncHadNetworkError.asStateFlow()

    private val _lastSyncAuthErrorCode = MutableStateFlow<Int?>(null)
    val lastSyncAuthErrorCode: StateFlow<Int?> = _lastSyncAuthErrorCode.asStateFlow()

    private data class RejectedEntry(
        val itemId: String,
        val reason: String,
        val errorCode: String?
    )

    private data class UploadOutcome(
        val acceptedItemIds: Set<String>,
        val duplicateItemIds: Set<String>,
        val rejectedItems: List<RejectedEntry>
    )

    /**
     * Starts periodic synchronization for [eventId], or replaces the credentials of
     * the running event. Switching events also switches all event-scoped stores.
     */
    fun start(context: Context, eventId: String, apiKey: String) {
        val sameEvent = initialized && this.eventId == eventId

        this.eventId = eventId
        this.apiKey = apiKey
        this.initialized = true

        if (!sameEvent) {
            EventStoreManager.initializeForEvent(context, eventId)
        }
        scope.launch { refreshPendingCount() }

        if (!sameEvent || syncJob?.isActive != true) {
            syncJob?.cancel()
            syncJob = scope.launch {
                while (true) {
                    syncOnceSafely()
                    delay(SYNC_INTERVAL_MS)
                }
            }
            Log.d(TAG, "Started for event $eventId (30s sync interval)")
        } else {
            triggerImmediateSync()
            Log.d(TAG, "Updated API key for event $eventId")
        }
    }

    /** Cancels scheduled and immediate synchronization and clears event credentials. */
    fun stop() {
        syncJob?.cancel()
        syncJob = null
        triggerJob?.cancel()
        triggerJob = null
        initialized = false
        eventId = null
        apiKey = null
        Log.d(TAG, "Stopped")
    }

    /** Persists [items] before returning and refreshes [pendingPurchaseCount]. */
    suspend fun enqueueItems(items: List<PendingItem>) {
        if (items.isEmpty()) return
        PendingItemsStore.appendItems(items)
        refreshPendingCount()
    }

    /**
     * Schedules a sync without waiting for the next interval. A previously scheduled
     * immediate sync is cancelled; the periodic worker continues unchanged.
     */
    fun triggerImmediateSync() {
        triggerJob?.cancel()
        triggerJob = scope.launch {
            syncOnceSafely()
        }
    }

    private suspend fun syncOnceSafely() {
        try {
            syncOnce()
        } catch (e: Exception) {
            Log.e(TAG, "Sync cycle failed unexpectedly", e)
        }
    }

    /** Uploads pending purchases in creation order and persists each outcome. */
    private suspend fun syncOnce() {
        val currentApiKey = apiKey ?: return
        val currentEventId = eventId ?: return

        val allItems = PendingItemsStore.readAll()
        if (allItems.isEmpty()) {
            _pendingPurchaseCount.value = 0
            _lastSyncHadNetworkError.value = false
            _lastSyncAuthErrorCode.value = null
            return
        }

        val byPurchase = allItems
            .groupBy { it.purchaseId }
            .toList()
            .sortedBy { (_, items) -> items.minOf { it.timestamp } }

        Log.d(TAG, "Syncing ${byPurchase.size} purchases (${allItems.size} items)")

        var networkError = false
        var authErrorCode: Int? = null

        for ((purchaseId, items) in byPurchase) {
            val uploadable = items.filter { it.errorText.isEmpty() }
            if (uploadable.isEmpty()) continue

            try {
                val outcome = uploadPurchaseGroupWithRetry(
                    currentApiKey = currentApiKey,
                    currentEventId = currentEventId,
                    purchaseItems = uploadable
                )
                val uploadedIds = outcome.acceptedItemIds + outcome.duplicateItemIds
                val rejectedById = outcome.rejectedItems.associateBy { it.itemId }
                val rejectedReasonsById = rejectedById.mapValues { (_, rejected) ->
                    rejected.reason.ifBlank { UNKNOWN_ERROR_FALLBACK }
                }

                if (uploadedIds.isNotEmpty() || rejectedReasonsById.isNotEmpty()) {
                    PendingItemsStore.updateItems(purchaseId) { item ->
                        when {
                            item.itemId in uploadedIds -> null
                            item.itemId in rejectedReasonsById -> {
                                item.copy(errorText = rejectedReasonsById.getValue(item.itemId))
                            }
                            else -> item
                        }
                    }
                }

                updateRejectedPurchaseStore(
                    purchaseId = purchaseId,
                    currentEventId = currentEventId,
                    purchaseItems = items,
                    rejectedById = rejectedById
                )
            } catch (e: HttpException) {
                when (val code = e.code()) {
                    401, 403 -> {
                        authErrorCode = code
                        Log.w(TAG, "Auth error for purchase $purchaseId: HTTP $code")
                        break
                    }
                    in 500..599 -> {
                        networkError = true
                        Log.w(TAG, "Server unavailable for purchase $purchaseId: HTTP $code")
                        break
                    }
                    else -> {
                        Log.w(TAG, "HTTP $code for purchase $purchaseId")
                        PendingItemsStore.updateItems(purchaseId) { item ->
                            if (item.errorText.isEmpty()) {
                                item.copy(errorText = "HTTP $code")
                            } else {
                                item
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Network error for purchase $purchaseId: ${e.message}")
                networkError = true
                break
            }
        }

        _lastSyncHadNetworkError.value = networkError
        _lastSyncAuthErrorCode.value = authErrorCode

        refreshPendingCount()
    }

    private suspend fun uploadPurchaseGroupWithRetry(
        currentApiKey: String,
        currentEventId: String,
        purchaseItems: List<PendingItem>
    ): UploadOutcome {
        val initialOutcome = classifyResponse(
            uploadItems(
                currentApiKey = currentApiKey,
                currentEventId = currentEventId,
                items = purchaseItems
            )
        )

        val hasInvalidSeller = initialOutcome.rejectedItems.any { rejected ->
            rejected.errorCode?.contains("INVALID_SELLER", ignoreCase = true) == true
        }
        if (!hasInvalidSeller) {
            return initialOutcome
        }

        val collateralIds = initialOutcome.rejectedItems
            .filter { rejected ->
                rejected.reason.isBlank() &&
                    (rejected.errorCode == null ||
                        rejected.errorCode.contains("UNSPECIFIED", ignoreCase = true))
            }
            .map { it.itemId }
            .toSet()

        if (collateralIds.isEmpty()) {
            return initialOutcome
        }

        val retryItems = purchaseItems.filter { it.itemId in collateralIds }
        if (retryItems.isEmpty()) {
            return initialOutcome
        }

        val retryOutcome = classifyResponse(
            uploadItems(
                currentApiKey = currentApiKey,
                currentEventId = currentEventId,
                items = retryItems
            )
        )

        return UploadOutcome(
            acceptedItemIds = initialOutcome.acceptedItemIds + retryOutcome.acceptedItemIds,
            duplicateItemIds = initialOutcome.duplicateItemIds + retryOutcome.duplicateItemIds,
            rejectedItems = initialOutcome.rejectedItems
                .filterNot { it.itemId in collateralIds } + retryOutcome.rejectedItems
        )
    }

    private suspend fun uploadItems(
        currentApiKey: String,
        currentEventId: String,
        items: List<PendingItem>
    ): CashierApiResponse {
        return api.createSoldItems(
            authorization = "Bearer $currentApiKey",
            eventId = currentEventId,
            request = SoldItemsRequest(
                items = items.map { pending ->
                    SoldItemObject(
                        itemId = pending.itemId,
                        purchaseId = pending.purchaseId,
                        seller = pending.sellerId,
                        price = pending.price,
                        paymentMethod = pending.paymentMethod
                    )
                }
            )
        )
    }

    private fun classifyResponse(response: CashierApiResponse): UploadOutcome {
        val accepted = response.acceptedItems?.mapNotNull { it.itemId }?.toSet() ?: emptySet()
        val duplicate = mutableSetOf<String>()
        val rejected = mutableListOf<RejectedEntry>()

        response.rejectedItems.orEmpty().forEach { rejectedItem ->
            val itemId = rejectedItem.item.itemId ?: return@forEach
            if (rejectedItem.errorCode?.contains("DUPLICATE", ignoreCase = true) == true) {
                duplicate.add(itemId)
            } else {
                rejected.add(
                    RejectedEntry(
                        itemId = itemId,
                        reason = rejectedItem.reason,
                        errorCode = rejectedItem.errorCode
                    )
                )
            }
        }

        return UploadOutcome(
            acceptedItemIds = accepted,
            duplicateItemIds = duplicate,
            rejectedItems = rejected
        )
    }

    private fun updateRejectedPurchaseStore(
        purchaseId: String,
        currentEventId: String,
        purchaseItems: List<PendingItem>,
        rejectedById: Map<String, RejectedEntry>
    ) {
        if (rejectedById.isEmpty()) {
            RejectedPurchaseStore.removeRejectedPurchase(purchaseId)
            return
        }

        val pendingByItemId = purchaseItems.associateBy { it.itemId }
        val rejectedItems = rejectedById.mapNotNull { (itemId, rejected) ->
            val pending = pendingByItemId[itemId] ?: return@mapNotNull null
            RejectedItemWithDetails(
                item = StoredSoldItem(
                    itemId = pending.itemId,
                    eventId = currentEventId,
                    purchaseId = pending.purchaseId,
                    seller = pending.sellerId,
                    price = pending.price,
                    paymentMethod = pending.paymentMethod,
                    soldTime = parseTimestampToEpochMillis(pending.timestamp),
                    uploaded = false
                ),
                reason = rejected.reason.ifBlank { UNKNOWN_ERROR_FALLBACK },
                errorCode = SerializableSoldItemErrorCode.fromString(rejected.errorCode)
            )
        }

        if (rejectedItems.isEmpty()) {
            return
        }

        val primaryError = rejectedItems.firstOrNull { !it.isCollateralDamage } ?: rejectedItems.first()
        val rejectedPurchase = RejectedPurchase(
            purchaseId = purchaseId,
            items = rejectedItems,
            errorCode = primaryError.errorCode,
            errorMessage = primaryError.reason.ifBlank { UNKNOWN_ERROR_FALLBACK },
            timestamp = Instant.now().toString(),
            autoRecoveryAttempted = true,
            needsManualReview = true
        )
        RejectedPurchaseStore.updateRejectedPurchase(rejectedPurchase)
    }

    private fun parseTimestampToEpochMillis(timestamp: String): Long {
        return try {
            Instant.parse(timestamp).toEpochMilli()
        } catch (_: Exception) {
            System.currentTimeMillis()
        }
    }

    /** Recomputes [pendingPurchaseCount] from distinct locally pending purchases. */
    suspend fun refreshPendingCount() {
        val items = PendingItemsStore.readAll()
        val count = items.map { it.purchaseId }.distinct().size
        _pendingPurchaseCount.value = count
    }
}
