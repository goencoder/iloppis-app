package se.iloppis.app.data

import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import se.iloppis.app.network.config.clientConfig
import se.iloppis.app.network.ILoppisClient
import se.iloppis.app.network.tickets.TicketsAPI

private const val TAG = "TicketTypeRepository"
private val UUID_PATTERN = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")

/**
 * Global singleton for ticket type name resolution.
 * Maps ticket type UUID → type name (e.g., "heldag").
 */
object TicketTypeRepository {
    private val mutex = Mutex()
    private var ticketTypeMap: Map<String, String> = emptyMap()
    private val api: TicketsAPI = ILoppisClient(clientConfig()).create()

    /**
     * Initialize or refresh ticket types for a specific event.
     */
    suspend fun refresh(eventId: String, apiKey: String) {
        try {
            val response = api.listTypes(
                authorization = "Bearer $apiKey",
                eventId = eventId
            )
            val refreshedMap = response.types.associate { it.id to it.type }
            mutex.withLock {
                ticketTypeMap = refreshedMap
            }
            Log.d(TAG, "Loaded ${refreshedMap.size} ticket types for event $eventId")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            mutex.withLock {
                ticketTypeMap = emptyMap()
            }
            Log.e(TAG, "Failed to load ticket types", e)
        }
    }

    /**
     * Resolve ticket type UUID to display name.
     * Returns the UUID if not found (graceful degradation).
     */
    suspend fun resolveTypeName(ticketTypeId: String): String? {
        return mutex.withLock {
            ticketTypeMap[ticketTypeId] ?: ticketTypeId.takeUnless(::looksLikeOpaqueTypeId)
        }
    }

    /**
     * Check if repository has been initialized.
     */
    suspend fun isInitialized(): Boolean {
        return mutex.withLock {
            ticketTypeMap.isNotEmpty()
        }
    }

    /**
     * Returns all cached ticket type entries as (id, name) pairs.
     */
    suspend fun getAllTypes(): List<Pair<String, String>> {
        return mutex.withLock {
            ticketTypeMap.entries.map { it.key to it.value }
        }
    }

    private fun looksLikeOpaqueTypeId(value: String): Boolean = UUID_PATTERN.matches(value)
}
