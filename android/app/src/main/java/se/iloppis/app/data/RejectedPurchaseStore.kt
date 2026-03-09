package se.iloppis.app.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import se.iloppis.app.data.models.RejectedPurchase
import java.io.File
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Thread-safe file-based storage for rejected purchases that need manual review.
 *
 * File location: context.filesDir/events/{eventId}/pending_review.json
 *
 * Works alongside PendingItemsStore (pending_items.jsonl):
 * - PendingItemsStore: items awaiting upload (row exists = pending, deleted = uploaded)
 * - RejectedPurchaseStore: purchases that failed with errors needing user attention
 *
 * BackgroundSyncManager uploads from PendingItemsStore; if a purchase is rejected
 * with a non-recoverable error, the items stay in PendingItemsStore with errorText set
 * and may also be tracked here for detailed per-item review.
 *
 * This store persists purchases that:
 * - Failed with rejection errors (e.g., INVALID_SELLER)
 * - Need manual intervention to resolve
 * - Should be shown in the Purchase Review Screen
 */
object RejectedPurchaseStore {
    private const val TAG = "RejectedPurchaseStore"
    private const val FILE_NAME = "pending_review.json"

    private val lock = ReentrantLock()
    private var file: File? = null
    private lateinit var eventId: String
    private val json = Json {
        prettyPrint = false
        ignoreUnknownKeys = true
    }
    
    // Coroutine scope for emitting events
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // SharedFlow to notify observers when a rejected purchase is added
    // replay = 0: Only new events (no buffering of past events)
    // extraBufferCapacity = 1: Buffer one event if no collectors are active
    private val _rejectedPurchaseAdded = MutableSharedFlow<RejectedPurchase>(
        replay = 0,
        extraBufferCapacity = 1
    )
    val rejectedPurchaseAdded: SharedFlow<RejectedPurchase> = _rejectedPurchaseAdded.asSharedFlow()

    /**
     * Initialize the store with the application context and event ID.
     * Must be called before any other operations.
     * 
     * @param context Application context
     * @param eventId Event ID for data isolation
     */
    fun initialize(context: Context, eventId: String) {
        this.eventId = eventId
        val eventDir = File(context.filesDir, "events/$eventId")
        eventDir.mkdirs()
        file = File(eventDir, FILE_NAME)
        Log.d(TAG, "Initialized with file: ${file?.absolutePath}")
    }

    /**
     * Initialize for testing with a custom directory.
     * Internal use only - for unit tests.
     */
    internal fun initializeForTesting(directory: File) {
        file = File(directory, FILE_NAME)
        Log.d(TAG, "Initialized for testing with file: ${file?.absolutePath}")
    }

    /**
     * Check if the store is initialized.
     */
    private fun requireInitialized(): File {
        return file ?: throw IllegalStateException(
            "RejectedPurchaseStore not initialized. Call initialize(context) first."
        )
    }

    /**
     * Private unlocked read method for internal use.
     */
    private fun readPurchasesUnlocked(): List<RejectedPurchase> {
        val f = requireInitialized()
        if (!f.exists()) {
            return emptyList()
        }

        return try {
            val content = f.readText()
            if (content.isBlank()) {
                emptyList()
            } else {
                json.decodeFromString<List<RejectedPurchase>>(content)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse rejected purchases", e)
            emptyList()
        }
    }

    /**
     * Add a rejected purchase to the store.
     * If a purchase with the same ID exists, it will be updated.
     * 
     * Emits an event via SharedFlow for reactive UI updates.
     */
    fun addRejectedPurchase(purchase: RejectedPurchase) {
        lock.withLock {
            try {
                val existing = readPurchasesUnlocked().toMutableList()
                
                // Remove existing purchase with same ID (update scenario)
                existing.removeAll { it.purchaseId == purchase.purchaseId }
                
                // Add the new/updated purchase
                existing.add(purchase)
                
                val jsonString = json.encodeToString(existing)
                requireInitialized().writeText(jsonString)
                
                Log.d(TAG, "Added rejected purchase ${purchase.purchaseId}, total now: ${existing.size}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to add rejected purchase", e)
                throw e
            }
        }
        
        // Emit event outside of lock to avoid blocking file operations
        // This allows UI to react immediately to new rejected purchases
        scope.launch {
            _rejectedPurchaseAdded.emit(purchase)
            Log.d(TAG, "Emitted rejected purchase event: ${purchase.purchaseId}")
        }
    }

    /**
     * Get all rejected purchases.
     */
    fun getAllRejectedPurchases(): List<RejectedPurchase> {
        lock.withLock {
            return readPurchasesUnlocked()
        }
    }

    /**
     * Remove a rejected purchase by ID.
     * Returns true if a purchase was removed, false if not found.
     */
    fun removeRejectedPurchase(purchaseId: String): Boolean {
        lock.withLock {
            try {
                val existing = readPurchasesUnlocked().toMutableList()
                val removed = existing.removeAll { it.purchaseId == purchaseId }
                
                if (removed) {
                    val jsonString = json.encodeToString(existing)
                    requireInitialized().writeText(jsonString)
                    Log.d(TAG, "Removed rejected purchase $purchaseId, total now: ${existing.size}")
                }
                
                return removed
            } catch (e: Exception) {
                Log.e(TAG, "Failed to remove rejected purchase", e)
                throw e
            }
        }
    }

    /**
     * Update an existing rejected purchase with new data.
     * If the purchase doesn't exist, it will be added.
     */
    fun updateRejectedPurchase(purchase: RejectedPurchase) {
        lock.withLock {
            try {
                val existing = readPurchasesUnlocked().toMutableList()
                val index = existing.indexOfFirst { it.purchaseId == purchase.purchaseId }
                
                if (index >= 0) {
                    existing[index] = purchase
                    Log.d(TAG, "Updated rejected purchase ${purchase.purchaseId}")
                } else {
                    existing.add(purchase)
                    Log.d(TAG, "Added new rejected purchase ${purchase.purchaseId}")
                }
                
                val jsonString = json.encodeToString(existing)
                requireInitialized().writeText(jsonString)
                
                // Emit event for new/updated purchase
                scope.launch {
                    _rejectedPurchaseAdded.emit(purchase)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update rejected purchase", e)
                throw e
            }
        }
    }

    /**
     * Get count of rejected purchases.
     */
    fun getCount(): Int {
        lock.withLock {
            return readPurchasesUnlocked().size
        }
    }

    /**
     * Get a specific rejected purchase by ID.
     */
    fun getRejectedPurchase(purchaseId: String): RejectedPurchase? {
        lock.withLock {
            return readPurchasesUnlocked().firstOrNull { it.purchaseId == purchaseId }
        }
    }

    /**
     * Clear all rejected purchases.
     */
    fun clear() {
        lock.withLock {
            try {
                requireInitialized().writeText("[]")
                Log.d(TAG, "Cleared all rejected purchases")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear rejected purchases", e)
                throw e
            }
        }
    }

    /**
     * Get the file reference (for testing purposes).
     */
    internal fun getFile(): File = requireInitialized()
}
