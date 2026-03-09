package se.iloppis.app.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import se.iloppis.app.ILoppisAppHolder
import java.io.File

private const val TAG = "SavedCodesStore"
private const val FILE_NAME = "saved_codes.json"
private const val MAX_SAVED_CODES = 20

/**
 * A previously-entered code that resolved successfully.
 *
 * Stored app-wide (not per-event) so codes from all events are available
 * when entering from the main page.
 */
@Serializable
data class SavedCode(
    /** The alias in XXX-YYY format */
    val alias: String,
    /** Event ID this code belongs to */
    val eventId: String,
    /** Human-readable event name for display */
    val eventName: String,
    /** CASHIER or SCANNER */
    val codeType: String,
    /** Epoch millis when the code was saved */
    val savedAt: Long = System.currentTimeMillis()
)

/**
 * App-wide persistent store for previously-entered codes.
 *
 * File location: `<filesDir>/saved_codes.json`
 *
 * Codes are saved after successful verification so users can
 * quickly re-enter a tool without typing the code again.
 * On next entry, codes are validated async against the API.
 */
object SavedCodesStore {

    private val mutex = Mutex()

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private fun file(): File {
        return File(ILoppisAppHolder.appContext.filesDir, FILE_NAME)
    }

    /**
     * Load all saved codes.
     */
    suspend fun loadAll(): List<SavedCode> = mutex.withLock {
        withContext(Dispatchers.IO) {
            try {
                val f = file()
                if (!f.exists()) return@withContext emptyList()
                val text = f.readText()
                if (text.isBlank()) return@withContext emptyList()
                json.decodeFromString<List<SavedCode>>(text)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load saved codes", e)
                emptyList()
            }
        }
    }

    /**
     * Save a code after successful verification.
     * Replaces any existing entry with the same alias.
     */
    suspend fun save(code: SavedCode) = mutex.withLock {
        withContext(Dispatchers.IO) {
            try {
                val existing = readUnsafe().toMutableList()
                existing.removeAll { it.alias == code.alias }
                existing.add(0, code)
                // Cap the list to avoid unbounded growth
                val capped = if (existing.size > MAX_SAVED_CODES) existing.take(MAX_SAVED_CODES) else existing
                writeUnsafe(capped)
                Log.d(TAG, "Saved code ${code.alias} for event ${code.eventId}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save code", e)
            }
        }
    }

    /**
     * Remove a saved code by alias.
     */
    suspend fun remove(alias: String) = mutex.withLock {
        withContext(Dispatchers.IO) {
            try {
                val existing = readUnsafe().toMutableList()
                existing.removeAll { it.alias == alias }
                writeUnsafe(existing)
                Log.d(TAG, "Removed saved code $alias")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to remove code", e)
            }
        }
    }

    // ── Internal helpers (must be called within mutex) ──

    private fun readUnsafe(): List<SavedCode> {
        val f = file()
        if (!f.exists()) return emptyList()
        val text = f.readText()
        if (text.isBlank()) return emptyList()
        return json.decodeFromString(text)
    }

    private fun writeUnsafe(codes: List<SavedCode>) {
        file().writeText(json.encodeToString(codes))
    }
}
