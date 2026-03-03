package se.iloppis.app.data

import android.content.Context
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import se.iloppis.app.data.models.PendingScan
import java.io.File

/**
 * Thread-safe storage for pending scans using JSONL format (one JSON object per line).
 *
 * File location: <filesDir>/pending_scans.jsonl
 * Row existence = pending, deleted row = uploaded successfully.
 *
 * All methods use mutex to ensure only one thread accesses the file at a time.
 */
object PendingScansStore {
    private const val FILENAME = "pending_scans.jsonl"
    private val mutex = Mutex()
    private lateinit var file: File

    fun initialize(context: Context, eventId: String) {
        file = JsonlFileOps.createEventFile(context, eventId, FILENAME)
    }

    internal fun initializeForTesting(directory: File) {
        file = File(directory, FILENAME)
    }

    suspend fun appendScan(scan: PendingScan) {
        mutex.withLock { JsonlFileOps.appendOne(file, scan) }
    }

    suspend fun getAllScans(): List<PendingScan> = mutex.withLock {
        JsonlFileOps.readAll(file)
    }

    suspend fun removeScan(scanId: String) {
        mutex.withLock {
            val updated = JsonlFileOps.readAll<PendingScan>(file).filter { it.scanId != scanId }
            JsonlFileOps.rewriteAll(file, updated)
        }
    }

    suspend fun updateError(scanId: String, errorText: String) {
        mutex.withLock {
            val updated = JsonlFileOps.readAll<PendingScan>(file).map { scan ->
                if (scan.scanId == scanId) scan.copy(errorText = errorText) else scan
            }
            JsonlFileOps.rewriteAll(file, updated)
        }
    }

    suspend fun count(): Int = getAllScans().size
}
