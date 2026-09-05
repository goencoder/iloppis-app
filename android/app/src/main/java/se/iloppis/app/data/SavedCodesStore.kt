package se.iloppis.app.data

import se.iloppis.app.utils.AppLog as Log
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
 * Codes are app-wide so entries from different events can appear on the main page.
 */
@Serializable
data class SavedCode(
    /** Alias in `XXX-XXX` format. */
    val alias: String,
    /** Event that owns the code. */
    val eventId: String,
    /** Human-readable event name. */
    val eventName: String,
    /** Tool type such as `CASHIER`, `SCANNER`, or `LIVE_STATS`. */
    val codeType: String,
    /** Save time in Unix epoch milliseconds. */
    val savedAt: Long = System.currentTimeMillis()
)

/**
 * App-wide persistent store for previously-entered codes.
 *
 * Codes are persisted only after successful verification and must be revalidated
 * against the API before reuse.
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

    /** Returns saved codes newest first, or an empty list if storage cannot be read. */
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

    /** Saves [code] first, replacing its alias and retaining at most 20 entries. */
    suspend fun save(code: SavedCode) = mutex.withLock {
        withContext(Dispatchers.IO) {
            try {
                val existing = readUnsafe().toMutableList()
                existing.removeAll { it.alias == code.alias }
                existing.add(0, code)
                val capped = if (existing.size > MAX_SAVED_CODES) existing.take(MAX_SAVED_CODES) else existing
                writeUnsafe(capped)
                Log.d(TAG, "Saved code ${code.alias} for event ${code.eventId}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save code", e)
            }
        }
    }

    /** Removes every saved entry matching [alias]; storage failures are logged. */
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
