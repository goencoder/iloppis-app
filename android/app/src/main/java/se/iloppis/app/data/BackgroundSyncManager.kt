package se.iloppis.app.data

import android.content.Context
import android.util.Log
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
import se.iloppis.app.network.ILoppisClient
import se.iloppis.app.network.cashier.CashierAPI
import se.iloppis.app.network.cashier.PaymentMethod
import se.iloppis.app.network.cashier.SoldItemObject
import se.iloppis.app.network.cashier.SoldItemsRequest
import se.iloppis.app.network.config.clientConfig
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Single-threaded background sync manager for sold items.
 *
 * Aligned with LoppisKassan's BackgroundSyncManager pattern:
 * - In-memory queue for immediate enqueue (non-blocking checkout)
 * - Single coroutine sync loop (30s interval)
 * - Local-first: items always flushed to disk before upload attempt
 * - Automatic retry on network errors
 * - Exposes pending count via StateFlow for reactive UI
 *
 * ## Data flow
 * ```
 * checkout() → enqueueItems() → ConcurrentLinkedQueue (non-blocking)
 *                                    ↓ (trigger immediate sync)
 *                              syncOnce():
 *                                1. flushQueueToDisk()
 *                                2. readAll pending
 *                                3. group by purchaseId
 *                                4. upload each group
 *                                5. delete accepted / update rejected
 *                                6. update pending count
 *                              (repeats every 30s)
 * ```
 */
object BackgroundSyncManager {
    private const val TAG = "BackgroundSyncManager"
    private const val SYNC_INTERVAL_MS = 30_000L

    // Coroutine scope for the sync loop — survives ViewModel lifecycle
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // In-memory queue: items enqueued here are flushed to disk before upload
    private val pendingQueue = ConcurrentLinkedQueue<PendingItem>()

    // Sync loop job
    private var syncJob: Job? = null
    private var triggerJob: Job? = null

    // Config
    private var eventId: String? = null
    private var apiKey: String? = null
    private var initialized = false

    // Reactive state for UI
    private val _pendingPurchaseCount = MutableStateFlow(0)
    val pendingPurchaseCount: StateFlow<Int> = _pendingPurchaseCount.asStateFlow()

    private val _lastSyncHadNetworkError = MutableStateFlow(false)
    val lastSyncHadNetworkError: StateFlow<Boolean> = _lastSyncHadNetworkError.asStateFlow()

    /**
     * Start the sync manager for an event.
     * Must be called once when entering cashier mode.
     */
    fun start(context: Context, eventId: String, apiKey: String) {
        if (initialized && this.eventId == eventId) return

        this.eventId = eventId
        this.apiKey = apiKey
        this.initialized = true

        // Initialize the store
        EventStoreManager.initializeForEvent(context, eventId)

        // Refresh count immediately from disk
        scope.launch { refreshPendingCount() }

        // Start periodic sync loop
        syncJob?.cancel()
        syncJob = scope.launch {
            while (true) {
                syncOnceSafely()
                delay(SYNC_INTERVAL_MS)
            }
        }

        Log.d(TAG, "Started for event $eventId (30s sync interval)")
    }

    /**
     * Stop the sync manager. Call when leaving cashier mode.
     */
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

    /**
     * Enqueue items for upload. Non-blocking — items are added to in-memory queue
     * and will be flushed to disk + uploaded on next sync cycle.
     *
     * Also triggers an immediate sync.
     */
    fun enqueueItems(items: List<PendingItem>) {
        pendingQueue.addAll(items)
        triggerImmediateSync()
    }

    /**
     * Trigger an immediate sync cycle (e.g., after user retries).
     */
    fun triggerImmediateSync() {
        triggerJob?.cancel()
        triggerJob = scope.launch {
            syncOnceSafely()
        }
    }

    /**
     * Wraps syncOnce in try/catch so the periodic loop never dies.
     */
    private suspend fun syncOnceSafely() {
        try {
            syncOnce()
        } catch (e: Exception) {
            Log.e(TAG, "Sync cycle failed unexpectedly", e)
        }
    }

    /**
     * Single sync cycle — aligned with LoppisKassan's syncOnceInternal().
     *
     * 1. Flush in-memory queue to disk (local-first guarantee)
     * 2. Read all pending items
     * 3. Group by purchaseId, upload each
     * 4. Handle responses: delete accepted, update rejected errorText
     * 5. Refresh pending count
     */
    private suspend fun syncOnce() {
        val currentApiKey = apiKey ?: return
        val currentEventId = eventId ?: return

        // STEP 1: Flush queue to disk (local-first)
        flushQueueToDisk()

        // STEP 2: Read all pending items
        val allItems = PendingItemsStore.readAll()
        if (allItems.isEmpty()) {
            _pendingPurchaseCount.value = 0
            _lastSyncHadNetworkError.value = false
            return
        }

        // STEP 3: Group by purchaseId, process oldest first
        val byPurchase = allItems
            .groupBy { it.purchaseId }
            .toList()
            .sortedBy { (_, items) -> items.minOf { it.timestamp } }

        Log.d(TAG, "Syncing ${byPurchase.size} purchases (${allItems.size} items)")

        val api = ILoppisClient(clientConfig()).create<CashierAPI>()
        var networkError = false

        for ((purchaseId, items) in byPurchase) {
            // Skip items that already have non-empty errorText (need manual intervention)
            val uploadable = items.filter { it.errorText.isEmpty() }
            if (uploadable.isEmpty()) continue

            try {
                val response = api.createSoldItems(
                    authorization = "Bearer $currentApiKey",
                    eventId = currentEventId,
                    request = SoldItemsRequest(
                        uploadable.map {
                            SoldItemObject(
                                itemId = it.itemId,
                                purchaseId = it.purchaseId,
                                seller = it.sellerId,
                                price = it.price,
                                paymentMethod = PaymentMethod.KONTANT
                            )
                        }
                    )
                )

                // Delete accepted items (row deletion = uploaded)
                val acceptedIds = response.acceptedItems
                    ?.mapNotNull { it.itemId }?.toSet() ?: emptySet()

                if (acceptedIds.isNotEmpty()) {
                    Log.d(TAG, "Purchase $purchaseId: ${acceptedIds.size} accepted")
                    PendingItemsStore.updateItems(purchaseId) { item ->
                        if (item.itemId in acceptedIds) null else item
                    }
                }

                // Handle rejected items: check for duplicates (idempotent success)
                response.rejectedItems?.forEach { rejected ->
                    val isDuplicate = rejected.errorCode?.let {
                        it.contains("DUPLICATE", ignoreCase = true)
                    } ?: false

                    if (isDuplicate) {
                        // Duplicate = already uploaded, safe to remove
                        Log.d(TAG, "Item ${rejected.item.itemId}: duplicate (removing)")
                        PendingItemsStore.updateItems(purchaseId) { item ->
                            if (item.itemId == rejected.item.itemId) null else item
                        }
                    } else {
                        // Real rejection — update errorText for manual review
                        val reason = rejected.reason.ifBlank { "Okänt fel" }
                        Log.w(TAG, "Item ${rejected.item.itemId}: $reason")
                        PendingItemsStore.updateItems(purchaseId) { item ->
                            if (item.itemId == rejected.item.itemId) {
                                item.copy(errorText = reason)
                            } else item
                        }
                    }
                }

            } catch (e: retrofit2.HttpException) {
                if (e.code() >= 500) {
                    // Server error — mark items, continue to next purchase
                    Log.e(TAG, "Server error for purchase $purchaseId: ${e.code()}")
                    PendingItemsStore.updateItems(purchaseId) { item ->
                        if (item.errorText.isEmpty()) item.copy(errorText = "serverfel") else item
                    }
                } else {
                    // Client error (4xx) — keep for retry
                    Log.w(TAG, "HTTP ${e.code()} for purchase $purchaseId")
                }
            } catch (e: Exception) {
                // Network error — stop trying further purchases (like LoppisKassan)
                Log.w(TAG, "Network error for purchase $purchaseId: ${e.message}")
                networkError = true
                break
            }
        }

        _lastSyncHadNetworkError.value = networkError

        // STEP 5: Refresh pending count from disk (single source of truth)
        refreshPendingCount()
    }

    /**
     * Flush in-memory queue to disk via PendingItemsStore.
     * This is the local-first guarantee: items hit disk before any network attempt.
     */
    private suspend fun flushQueueToDisk() {
        val batch = mutableListOf<PendingItem>()
        while (true) {
            val item = pendingQueue.poll() ?: break
            batch.add(item)
        }
        if (batch.isNotEmpty()) {
            Log.d(TAG, "Flushing ${batch.size} items to disk")
            PendingItemsStore.appendItems(batch)
        }
    }

    /**
     * Re-read PendingItemsStore and update the pending purchase count.
     * Called internally after sync and externally after manual changes
     * to PendingItemsStore (e.g. delete/retry from review screens).
     */
    suspend fun refreshPendingCount() {
        val items = PendingItemsStore.readAll()
        val count = items.map { it.purchaseId }.distinct().size
        _pendingPurchaseCount.value = count
    }
}
