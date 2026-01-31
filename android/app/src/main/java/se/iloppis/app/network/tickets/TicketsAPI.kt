package se.iloppis.app.network.tickets

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import se.iloppis.app.network.iLoppisApiInterface

/**
 * iLoppis Tickets API interface
 */
interface TicketsAPI : iLoppisApiInterface {
    /**
     * Lists different types of tickets available for an event.
     */
    @GET("v1/events/{event_id}/ticket_types")
    suspend fun listTypes(
        @Header("Authorization") authorization: String,
        @Path("event_id") eventId: String
    ) : TicketsResponse
}
