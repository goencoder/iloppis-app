package se.iloppis.app.data

import android.content.Context
import se.iloppis.app.utils.AppLog as Log
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

/** Thread-safe file storage for rejected purchases that need manual review. */
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
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private val _rejectedPurchaseAdded = MutableSharedFlow<RejectedPurchase>(
        replay = 0,
        extraBufferCapacity = 1
    )
    val rejectedPurchaseAdded: SharedFlow<RejectedPurchase> = _rejectedPurchaseAdded.asSharedFlow()

    /** Selects the event-scoped storage file used by subsequent operations. */
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

    /** Adds or replaces [purchase], then emits it through [rejectedPurchaseAdded]. */
    fun addRejectedPurchase(purchase: RejectedPurchase) {
        lock.withLock {
            try {
                val existing = readPurchasesUnlocked().toMutableList()
                
                existing.removeAll { it.purchaseId == purchase.purchaseId }
                existing.add(purchase)
                
                val jsonString = json.encodeToString(existing)
                requireInitialized().writeText(jsonString)
                
                Log.d(TAG, "Added rejected purchase ${purchase.purchaseId}, total now: ${existing.size}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to add rejected purchase", e)
                throw e
            }
        }
        
        // Emit outside the file lock so slow collectors cannot block persistence.
        scope.launch {
            _rejectedPurchaseAdded.emit(purchase)
            Log.d(TAG, "Emitted rejected purchase event: ${purchase.purchaseId}")
        }
    }

    /** Returns all rejected purchases, or an empty list when none are stored. */
    fun getAllRejectedPurchases(): List<RejectedPurchase> {
        lock.withLock {
            return readPurchasesUnlocked()
        }
    }

    /** Removes [purchaseId] and returns whether it existed. */
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

    /** Updates [purchase], adding it when its ID is not yet stored. */
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
                
                scope.launch {
                    _rejectedPurchaseAdded.emit(purchase)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update rejected purchase", e)
                throw e
            }
        }
    }

    /** Returns the number of stored rejected purchases. */
    fun getCount(): Int {
        lock.withLock {
            return readPurchasesUnlocked().size
        }
    }

    /** Returns [purchaseId], or `null` when it is not stored. */
    fun getRejectedPurchase(purchaseId: String): RejectedPurchase? {
        lock.withLock {
            return readPurchasesUnlocked().firstOrNull { it.purchaseId == purchaseId }
        }
    }

    /** Removes all rejected purchases from the event-scoped file. */
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
