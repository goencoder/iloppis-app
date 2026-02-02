package se.iloppis.app.network.visitor

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import se.iloppis.app.network.ILoppisApiInterface

/**
 * iLoppis visitor API interface
 */
interface VisitorAPI : ILoppisApiInterface {
    /**
     * Scans a visitors ticket and reports it as scanned
     */
    @POST("v1/events/{event_id}/visitor_tickets/{ticket_id}/scan")
    suspend fun scanTicket(
        @Header("Authorization") authorization: String,
        @Path("event_id") eventId: String,
        @Path("ticket_id") ticketId: String,
        @Body body: Map<String, String> = emptyMap()
    ) : VisitorTicketResponse

    /**
     * Gets visitor ticket data
     */
    @GET("v1/events/{event_id}/visitor_tickets/{ticket_id}")
    suspend fun getTicket(
        @Header("Authorization") authorization: String,
        @Path("event_id") eventId: String,
        @Path("ticket_id") ticketId: String
    ) : VisitorTicketResponse
}
