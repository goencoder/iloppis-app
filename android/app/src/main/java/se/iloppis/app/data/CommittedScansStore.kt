package se.iloppis.app.data

import android.content.Context
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    private const val FILENAME = "committed_scans.jsonl"
    private val mutex = Mutex()
    private lateinit var file: File

    fun initialize(context: Context, eventId: String) {
        file = JsonlFileOps.createEventFile(context, eventId, FILENAME)
    }

    internal fun initializeForTesting(directory: File) {
        file = File(directory, FILENAME)
    }

    suspend fun appendScan(scan: CommittedScan) {
        mutex.withLock { JsonlFileOps.appendOne(file, scan) }
    }

    /**
     * Check if a ticket has already been scanned (for offline duplicate detection).
     */
    suspend fun hasTicket(ticketId: String): Boolean = mutex.withLock {
        JsonlFileOps.readAll<CommittedScan>(file).any { it.ticketId == ticketId }
    }

    /**
     * Get recent scans for display. Returns most recent scans first.
     */
    suspend fun getRecentScans(limit: Int = 50): List<CommittedScan> = mutex.withLock {
        JsonlFileOps.readAll<CommittedScan>(file).takeLast(limit).reversed()
    }

    /**
     * Count successful scans for a specific event.
     */
    suspend fun countScansForEvent(eventId: String): Int = mutex.withLock {
        JsonlFileOps.readAll<CommittedScan>(file).count { scan ->
            scan.eventId == eventId &&
                (scan.status == "SUCCESS" || scan.status == "OFFLINE_SUCCESS")
        }
    }

    /**
     * Get recent scans for a specific event.
     */
    suspend fun getRecentScansForEvent(eventId: String, limit: Int = 50): List<CommittedScan> =
        mutex.withLock {
            JsonlFileOps.readAll<CommittedScan>(file)
                .filter { it.eventId == eventId }
                .takeLast(limit)
                .reversed()
        }
}
