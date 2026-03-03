package se.iloppis.app.data

import android.content.Context
import android.util.Log
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
 * File location: <filesDir>/pending_items.jsonl
 * Row existence = pending, deleted row = uploaded successfully.
 *
 * All methods use mutex to ensure only one thread accesses the file at a time.
 * Emits update events via SharedFlow for reactive UI updates.
 */
object PendingItemsStore {
    private const val TAG = "PendingItemsStore"
    private const val FILENAME = "pending_items.jsonl"

    private val mutex = Mutex()
    private val _itemsUpdated = MutableSharedFlow<Unit>(replay = 0)
    val itemsUpdated: SharedFlow<Unit> = _itemsUpdated.asSharedFlow()

    private lateinit var file: File

    // Rate-limit logging of malformed lines (max once per 10 seconds)
    private var lastMalformedLogTime = 0L
    private const val MALFORMED_LOG_INTERVAL_MS = 10_000L

    private fun requireInitialized(): File {
        check(::file.isInitialized) {
            "PendingItemsStore not initialized. Call initialize(context, eventId) first."
        }
        return file
    }

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

    /**
     * Append new items to the file.
     */
    suspend fun appendItems(items: List<PendingItem>) {
        if (items.isEmpty()) return
        val f = requireInitialized()
        mutex.withLock { JsonlFileOps.appendAll(f, items) }
        _itemsUpdated.emit(Unit)
    }

    /**
     * Read all items from the file.
     */
    suspend fun readAll(): List<PendingItem> {
        val f = requireInitialized()
        return mutex.withLock { readLines() }
    }

    /**
     * Update items matching a purchaseId.
     * The updater function returns an updated item or null to delete.
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

    /**
     * Delete all items for a specific purchaseId.
     */
    suspend fun deleteByPurchaseId(purchaseId: String) {
        updateItems(purchaseId) { null }
    }

    /**
     * Get count of items grouped by error severity.
     * @return Triple of (infoCount, warningCount, criticalCount)
     */
    suspend fun getErrorCounts(): Triple<Int, Int, Int> {
        requireInitialized()
        val items = readAll()

        var infoCount = 0
        var warningCount = 0
        var criticalCount = 0

        items.groupBy { it.purchaseId }.forEach { (_, purchaseItems) ->
            val hasServerError = purchaseItems.any {
                it.errorText.contains("serverfel", ignoreCase = true)
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
