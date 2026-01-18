package se.iloppis.app.data

import android.content.Context
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
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
        file = File(eventDir, "pending_scans.jsonl")
    }
    
    /**
     * Initialize for testing with a custom directory.
     * Internal use only - for unit tests.
     */
    internal fun initializeForTesting(directory: File) {
        file = File(directory, "pending_scans.jsonl")
    }
    
    /**
     * Append a new scan to the file.
     * Creates file if it doesn't exist.
     * 
     * @param scan Scan to append
     */
    suspend fun appendScan(scan: PendingScan) {
        mutex.withLock {
            file.appendText(json.encodeToString(scan) + "\n")
        }
    }
    
    /**
     * Read all scans from the file.
     * Returns empty list if file doesn't exist or is empty.
     * 
     * @return List of all pending scans
     */
    suspend fun getAllScans(): List<PendingScan> {
        return mutex.withLock {
            if (!file.exists()) return emptyList()
            
            file.readLines()
                .filter { it.isNotBlank() }
                .mapNotNull { line ->
                    try {
                        json.decodeFromString<PendingScan>(line)
                    } catch (e: Exception) {
                        // Skip malformed lines
                        null
                    }
                }
        }
    }
    
    /**
     * Remove a scan by scanId.
     * 
     * @param scanId Scan ID to remove
     */
    suspend fun removeScan(scanId: String) {
        mutex.withLock {
            if (!file.exists()) return@withLock
            
            val allScans = file.readLines()
                .filter { it.isNotBlank() }
                .mapNotNull { line ->
                    try {
                        json.decodeFromString<PendingScan>(line)
                    } catch (e: Exception) {
                        null
                    }
                }
            
            val updatedScans = allScans.filter { it.scanId != scanId }
            
            // Rewrite entire file without the removed scan
            file.writeText(
                updatedScans.joinToString("\n") { json.encodeToString(it) } +
                    if (updatedScans.isNotEmpty()) "\n" else ""
            )
        }
    }
    
    /**
     * Update error text for a specific scan.
     * 
     * @param scanId Scan ID to update
     * @param errorText Error message to set
     */
    suspend fun updateError(scanId: String, errorText: String) {
        mutex.withLock {
            if (!file.exists()) return@withLock
            
            val allScans = file.readLines()
                .filter { it.isNotBlank() }
                .mapNotNull { line ->
                    try {
                        json.decodeFromString<PendingScan>(line)
                    } catch (e: Exception) {
                        null
                    }
                }
            
            val updatedScans = allScans.map { scan ->
                if (scan.scanId == scanId) {
                    scan.copy(errorText = errorText)
                } else {
                    scan
                }
            }
            
            // Rewrite entire file with updated error
            file.writeText(
                updatedScans.joinToString("\n") { json.encodeToString(it) } +
                    if (updatedScans.isNotEmpty()) "\n" else ""
            )
        }
    }
    
    /**
     * Get count of pending scans.
     * 
     * @return Number of pending scans
     */
    suspend fun count(): Int {
        return getAllScans().size
    }
}
