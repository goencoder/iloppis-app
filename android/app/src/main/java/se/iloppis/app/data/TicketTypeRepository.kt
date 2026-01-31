package se.iloppis.app.data

import android.util.Log
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import se.iloppis.app.network.config.clientConfig
import se.iloppis.app.network.iLoppisClient
import se.iloppis.app.network.tickets.TicketsAPI

private const val TAG = "TicketTypeRepository"

/**
 * Global singleton for ticket type name resolution.
 * Maps ticket type UUID → type name (e.g., "heldag").
 */
object TicketTypeRepository {
    private val mutex = Mutex()
    private var ticketTypeMap: Map<String, String> = emptyMap()
    private val api: TicketsAPI = iLoppisClient(clientConfig()).create()

    /**
     * Initialize or refresh ticket types for a specific event.
     */
    suspend fun refresh(eventId: String, apiKey: String) {
        mutex.withLock {
            try {
                val response = api.listTypes(
                    authorization = "Bearer $apiKey",
                    eventId = eventId
                )
                ticketTypeMap = response.types.associate { it.id to it.type }
                Log.d(TAG, "Loaded ${ticketTypeMap.size} ticket types for event $eventId")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load ticket types", e)
            }
        }
    }

    /**
     * Resolve ticket type UUID to display name.
     * Returns the UUID if not found (graceful degradation).
     */
    suspend fun resolveTypeName(ticketTypeId: String): String {
        return mutex.withLock {
            ticketTypeMap[ticketTypeId] ?: ticketTypeId
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
}
