package se.iloppis.app.data

import android.content.Context
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import se.iloppis.app.data.models.CommittedScan
import java.io.File

/**
 * Thread-safe storage for committed scans using JSONL format (one JSON object per line).
 *
 * The store backs offline duplicate detection and scan history. Call [initialize]
 * before accessing it; operations are serialized by an internal mutex.
 */
object CommittedScansStore {
    private const val FILENAME = "committed_scans.jsonl"
    private val mutex = Mutex()
    private lateinit var file: File

    /**
     * Selects the event-scoped backing file used by subsequent operations.
     * Reinitializing switches the store to [eventId] without migrating existing rows.
     */
    fun initialize(context: Context, eventId: String) {
        file = JsonlFileOps.createEventFile(context, eventId, FILENAME)
    }

    internal fun initializeForTesting(directory: File) {
        file = File(directory, FILENAME)
    }

    /** Appends a successfully committed [scan] atomically. */
    suspend fun appendScan(scan: CommittedScan) {
        mutex.withLock { JsonlFileOps.appendOne(file, scan) }
    }

    /** Returns whether [ticketId] occurs in the committed history. */
    suspend fun hasTicket(ticketId: String): Boolean = mutex.withLock {
        JsonlFileOps.readAll<CommittedScan>(file).any { it.ticketId == ticketId }
    }

    /** Returns at most [limit] scans, newest first. */
    suspend fun getRecentScans(limit: Int = 50): List<CommittedScan> = mutex.withLock {
        JsonlFileOps.readAll<CommittedScan>(file).takeLast(limit).reversed()
    }

    /** Counts successful and offline-successful scans belonging to [eventId]. */
    suspend fun countScansForEvent(eventId: String): Int = mutex.withLock {
        JsonlFileOps.readAll<CommittedScan>(file).count { scan ->
            scan.eventId == eventId &&
                (scan.status == "SUCCESS" || scan.status == "OFFLINE_SUCCESS")
        }
    }

    /** Returns at most [limit] scans for [eventId], newest first. */
    suspend fun getRecentScansForEvent(eventId: String, limit: Int = 50): List<CommittedScan> =
        mutex.withLock {
            JsonlFileOps.readAll<CommittedScan>(file)
                .filter { it.eventId == eventId }
                .takeLast(limit)
                .reversed()
        }
}
