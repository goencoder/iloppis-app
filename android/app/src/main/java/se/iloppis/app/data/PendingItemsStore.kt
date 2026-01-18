package se.iloppis.app.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
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
    
    private val mutex = Mutex()
    private val _itemsUpdated = MutableSharedFlow<Unit>(replay = 0)
    val itemsUpdated: SharedFlow<Unit> = _itemsUpdated.asSharedFlow()
    
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    private lateinit var file: File
    private lateinit var eventId: String
    
    // Rate-limit logging of malformed lines (max once per 10 seconds)
    private var lastMalformedLogTime = 0L
    private const val MALFORMED_LOG_INTERVAL_MS = 10_000L
    
    /**
     * Check that the store has been initialized.
     * @throws IllegalStateException if not initialized
     */
    private fun requireInitialized(): File {
        check(::file.isInitialized) {
            "PendingItemsStore not initialized. Call initialize(context, eventId) first."
        }
        return file
    }
    
    /**
     * Initialize the store with application context and event ID.
     * Must be called once before any other operations.
     * 
     * @param context Application context
     * @param eventId Event ID for data isolation
     */
    fun initialize(context: Context, eventId: String) {
        this.eventId = eventId
        val eventDir = File(context.filesDir, "events/$eventId")
        eventDir.mkdirs()
        file = File(eventDir, "pending_items.jsonl")
    }
    
    /**
     * Append new items to the file.
     * Creates file if it doesn't exist.
     * 
     * @param items List of items to append
     * @throws IllegalStateException if store is not initialized
     */
    suspend fun appendItems(items: List<PendingItem>) {
        if (items.isEmpty()) return
        val f = requireInitialized()
        
        mutex.withLock {
            f.appendText(
                items.joinToString("\n") { json.encodeToString(it) } + "\n"
            )
        }
        
        _itemsUpdated.emit(Unit)
    }
    
    /**
     * Log a malformed line warning (rate-limited to avoid log spam).
     */
    private fun logMalformedLine(line: String, error: Exception) {
        val now = System.currentTimeMillis()
        if (now - lastMalformedLogTime > MALFORMED_LOG_INTERVAL_MS) {
            lastMalformedLogTime = now
            Log.w(TAG, "Skipping malformed line (truncated): ${line.take(50)}...", error)
        }
    }
    
    /**
     * Read all items from the file.
     * Returns empty list if file doesn't exist or is empty.
     * Malformed lines are skipped and logged (rate-limited).
     * 
     * @return List of all pending items
     * @throws IllegalStateException if store is not initialized
     */
    suspend fun readAll(): List<PendingItem> {
        val f = requireInitialized()
        return mutex.withLock {
            if (!f.exists()) return emptyList()
            
            f.readLines()
                .filter { it.isNotBlank() }
                .mapNotNull { line ->
                    try {
                        json.decodeFromString<PendingItem>(line)
                    } catch (e: Exception) {
                        logMalformedLine(line, e)
                        null
                    }
                }
        }
    }
    
    /**
     * Update items matching a purchaseId.
     * The updater function receives each matching item and returns:
     * - Updated item to keep (with modifications)
     * - null to delete the item
     * 
     * @param purchaseId Purchase ID to filter items
     * @param updater Function that transforms or deletes items
     * @throws IllegalStateException if store is not initialized
     */
    suspend fun updateItems(purchaseId: String, updater: (PendingItem) -> PendingItem?) {
        val f = requireInitialized()
        mutex.withLock {
            if (!f.exists()) return@withLock
            
            val allItems = f.readLines()
                .filter { it.isNotBlank() }
                .mapNotNull { line ->
                    try {
                        json.decodeFromString<PendingItem>(line)
                    } catch (e: Exception) {
                        logMalformedLine(line, e)
                        null
                    }
                }
            
            val updatedItems = allItems.mapNotNull { item ->
                if (item.purchaseId == purchaseId) {
                    updater(item)
                } else {
                    item
                }
            }
            
            // Rewrite entire file with updated items
            f.writeText(
                updatedItems.joinToString("\n") { json.encodeToString(it) } +
                    if (updatedItems.isNotEmpty()) "\n" else ""
            )
        }
        
        _itemsUpdated.emit(Unit)
    }
    
    /**
     * Delete all items for a specific purchaseId.
     * 
     * @param purchaseId Purchase ID to delete
     */
    suspend fun deleteByPurchaseId(purchaseId: String) {
        updateItems(purchaseId) { null }
    }
    
    /**
     * Get count of items grouped by error severity.
     * 
     * @return Triple of (infoCount, warningCount, criticalCount)
     * @throws IllegalStateException if store is not initialized
     */
    suspend fun getErrorCounts(): Triple<Int, Int, Int> {
        requireInitialized() // Validate init before delegating to readAll
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
