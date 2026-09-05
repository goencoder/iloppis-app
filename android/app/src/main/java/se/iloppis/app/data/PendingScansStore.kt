package se.iloppis.app.data

import android.content.Context
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import se.iloppis.app.data.models.PendingScan
import java.io.File

/**
 * Thread-safe storage for pending scans using JSONL format (one JSON object per line).
 *
 * A row remains until its scan has been accepted by the backend. Call [initialize]
 * before accessing the store; operations are serialized by an internal mutex.
 */
object PendingScansStore {
    private const val FILENAME = "pending_scans.jsonl"
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

    /** Appends [scan] atomically to the pending queue. */
    suspend fun appendScan(scan: PendingScan) {
        mutex.withLock { JsonlFileOps.appendOne(file, scan) }
    }

    /** Returns a snapshot of pending scans in insertion order. */
    suspend fun getAllScans(): List<PendingScan> = mutex.withLock {
        JsonlFileOps.readAll(file)
    }

    /** Removes every pending row whose scan ID equals [scanId]. */
    suspend fun removeScan(scanId: String) {
        mutex.withLock {
            val updated = JsonlFileOps.readAll<PendingScan>(file).filter { it.scanId != scanId }
            JsonlFileOps.rewriteAll(file, updated)
        }
    }

    /** Replaces the error text for every pending row identified by [scanId]. */
    suspend fun updateError(scanId: String, errorText: String) {
        mutex.withLock {
            val updated = JsonlFileOps.readAll<PendingScan>(file).map { scan ->
                if (scan.scanId == scanId) scan.copy(errorText = errorText) else scan
            }
            JsonlFileOps.rewriteAll(file, updated)
        }
    }

    /** Returns the current number of pending scan rows. */
    suspend fun count(): Int = getAllScans().size
}
