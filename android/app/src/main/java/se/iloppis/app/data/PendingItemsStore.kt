package se.iloppis.app.data

import android.content.Context
import se.iloppis.app.utils.AppLog as Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import se.iloppis.app.data.models.PendingItem
import java.io.File

/**
 * Thread-safe storage for pending items using JSONL format (one JSON object per line).
 *
 * A row remains until it has been accepted by the backend. Mutations are serialized
 * and emit [itemsUpdated]. Call [initialize] before accessing the store.
 */
object PendingItemsStore {
    private const val TAG = "PendingItemsStore"
    private const val FILENAME = "pending_items.jsonl"

    private val mutex = Mutex()
    private val _itemsUpdated = MutableSharedFlow<Unit>(replay = 0)
    val itemsUpdated: SharedFlow<Unit> = _itemsUpdated.asSharedFlow()

    private lateinit var file: File

    private var lastMalformedLogTime = 0L
    private const val MALFORMED_LOG_INTERVAL_MS = 10_000L

    private fun requireInitialized(): File {
        check(::file.isInitialized) {
            "PendingItemsStore not initialized. Call initialize(context, eventId) first."
        }
        return file
    }

    /**
     * Selects the event-scoped backing file used by subsequent operations.
     * Reinitializing switches the store to [eventId] without migrating existing rows.
     */
    fun initialize(context: Context, eventId: String) {
        file = JsonlFileOps.createEventFile(context, eventId, FILENAME)
    }

    private fun logMalformedLine(line: String, error: Exception) {
        val now = System.currentTimeMillis()
        if (now - lastMalformedLogTime > MALFORMED_LOG_INTERVAL_MS) {
            lastMalformedLogTime = now
            Log.w(TAG, "Skipping malformed line (truncated): ${line.take(50)}...", error)
        }
    }

    private fun readLines(): List<PendingItem> =
        JsonlFileOps.readAll(file) { line, e -> logMalformedLine(line, e) }

    /** Appends [items] atomically and emits [itemsUpdated]; an empty list is ignored. */
    suspend fun appendItems(items: List<PendingItem>) {
        if (items.isEmpty()) return
        val f = requireInitialized()
        mutex.withLock { JsonlFileOps.appendAll(f, items) }
        _itemsUpdated.emit(Unit)
    }

    /** Returns a snapshot of pending items in insertion order. */
    suspend fun readAll(): List<PendingItem> {
        val f = requireInitialized()
        return mutex.withLock { readLines() }
    }

    /**
     * Applies [updater] to items belonging to [purchaseId]. Returning `null` deletes
     * an item. Emits [itemsUpdated] after the mutation attempt.
     */
    suspend fun updateItems(purchaseId: String, updater: (PendingItem) -> PendingItem?) {
        val f = requireInitialized()
        mutex.withLock {
            if (!f.exists()) return@withLock
            val updated = readLines().mapNotNull { item ->
                if (item.purchaseId == purchaseId) updater(item) else item
            }
            JsonlFileOps.rewriteAll(f, updated)
        }
        _itemsUpdated.emit(Unit)
    }

    /** Deletes all items belonging to [purchaseId] and emits [itemsUpdated]. */
    suspend fun deleteByPurchaseId(purchaseId: String) {
        updateItems(purchaseId) { null }
    }

    /**
     * Counts purchases by their highest error severity.
     *
     * @return `(waiting, rejected, serverError)` purchase counts.
     */
    suspend fun getErrorCounts(): Triple<Int, Int, Int> {
        requireInitialized()
        val items = readAll()

        var infoCount = 0
        var warningCount = 0
        var criticalCount = 0

        items.groupBy { it.purchaseId }.forEach { (_, purchaseItems) ->
            val hasServerError = purchaseItems.any {
                it.errorText.contains("serverfel", ignoreCase = true) ||
                    it.errorText.startsWith("HTTP 5", ignoreCase = true)
            }
            val hasOtherError = purchaseItems.any {
                it.errorText.isNotBlank() && !it.errorText.contains("serverfel", ignoreCase = true)
            }
            val allWaiting = purchaseItems.all { it.errorText.isBlank() }

            when {
                hasServerError -> criticalCount++
                hasOtherError -> warningCount++
                allWaiting -> infoCount++
            }
        }

        return Triple(infoCount, warningCount, criticalCount)
    }
}
