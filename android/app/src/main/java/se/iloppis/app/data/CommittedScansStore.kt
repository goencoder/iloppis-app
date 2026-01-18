package se.iloppis.app.data

import android.content.Context
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import se.iloppis.app.data.models.CommittedScan
import java.io.File

/**
 * Thread-safe storage for committed scans using JSONL format (one JSON object per line).
 * 
 * File location: <filesDir>/committed_scans.jsonl
 * Used for offline duplicate detection and scan history.
 * 
 * All methods use mutex to ensure only one thread accesses the file at a time.
 */
object CommittedScansStore {
    private val mutex = Mutex()
    
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    private lateinit var file: File
    private lateinit var eventId: String
    
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
        file = File(eventDir, "committed_scans.jsonl")
    }
    
    /**
     * Initialize for testing with a custom directory.
     * Internal use only - for unit tests.
     */
    internal fun initializeForTesting(directory: File) {
        file = File(directory, "committed_scans.jsonl")
    }
    
    /**
     * Append a new committed scan to the file.
     * Creates file if it doesn't exist.
     * 
     * @param scan Scan to append
     */
    suspend fun appendScan(scan: CommittedScan) {
        mutex.withLock {
            file.appendText(json.encodeToString(scan) + "\n")
        }
    }
    
    /**
     * Check if a ticket has already been scanned (for offline duplicate detection).
     * 
     * @param ticketId Ticket ID to check
     * @return true if ticket found in committed scans, false otherwise
     */
    suspend fun hasTicket(ticketId: String): Boolean {
        return mutex.withLock {
            if (!file.exists()) return false
            
            file.readLines()
                .filter { it.isNotBlank() }
                .any { line ->
                    try {
                        val scan = json.decodeFromString<CommittedScan>(line)
                        scan.ticketId == ticketId
                    } catch (e: Exception) {
                        false
                    }
                }
        }
    }
    
    /**
     * Get recent scans for display.
     * Returns most recent scans first.
     * 
     * @param limit Maximum number of scans to return
     * @return List of recent committed scans
     */
    suspend fun getRecentScans(limit: Int = 50): List<CommittedScan> {
        return mutex.withLock {
            if (!file.exists()) return emptyList()
            
            file.readLines()
                .filter { it.isNotBlank() }
                .mapNotNull { line ->
                    try {
                        json.decodeFromString<CommittedScan>(line)
                    } catch (e: Exception) {
                        // Skip malformed lines
                        null
                    }
                }
                .takeLast(limit)
                .reversed() // Most recent first
        }
    }

    /**
     * Count total scans for a specific event.
     * 
     * @param eventId Event ID to count scans for
     * @return Number of committed scans for this event
     */
    suspend fun countScansForEvent(eventId: String): Int {
        return mutex.withLock {
            if (!file.exists()) return 0
            
            file.readLines()
                .filter { it.isNotBlank() }
                .count { line ->
                    try {
                        val scan = json.decodeFromString<CommittedScan>(line)
                        // Only count successful admissions (not ALREADY_SCANNED, INVALID_TICKET, etc.)
                        scan.eventId == eventId && 
                            (scan.status == "SUCCESS" || scan.status == "OFFLINE_SUCCESS")
                    } catch (e: Exception) {
                        false
                    }
                }
        }
    }

    /**
     * Get recent scans for a specific event.
     * 
     * @param eventId Event ID to filter by
     * @param limit Maximum number of scans to return
     * @return List of recent committed scans for this event
     */
    suspend fun getRecentScansForEvent(eventId: String, limit: Int = 50): List<CommittedScan> {
        return mutex.withLock {
            if (!file.exists()) return emptyList()
            
            file.readLines()
                .filter { it.isNotBlank() }
                .mapNotNull { line ->
                    try {
                        val scan = json.decodeFromString<CommittedScan>(line)
                        if (scan.eventId == eventId) scan else null
                    } catch (e: Exception) {
                        null
                    }
                }
                .takeLast(limit)
                .reversed()
        }
    }
}
