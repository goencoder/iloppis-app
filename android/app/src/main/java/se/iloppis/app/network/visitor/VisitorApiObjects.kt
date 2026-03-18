package se.iloppis.app.network.visitor

/**
 * iLoppis visitor ticket API object
 */
data class ApiVisitorTicket(
    /**
     * Ticket ID
     */
    val id: String,
    /**
     * Event ID
     *
     * The ID of the event that this ticket belongs to
     */
    val eventId: String,
    /**
     * Ticket type
     */
    val ticketType: String?,
    /**
     * Email address to the owner of the ticket
     *
     * This is not the owner of the event, rather
     * the owner of this ticket.
     */
    val email: String?,
    /**
     * Ticket status
     */
    val status: String?,
    /**
     * The date that this ticket was issued
     *
     * This is the time when the ticket was
     * bout by the owner.
     */
    val issuedAt: String?,
    /**
     * The date that this ticket is valid from
     */
    val validFrom: String?,
    /**
     * The date that this ticket is valid to
     */
    val validUntil: String?,
    /**
     * The date that this ticket was scanned
     */
    val scannedAt: String?
)

/**
 * iLoppis ticket response object
 */
data class VisitorTicketResponse(
    /**
     * Ticket object
     */
    val ticket: ApiVisitorTicket?
)

/**
 * Filter for visitor tickets.
 */
data class VisitorTicketFilter(
    val email: String? = null,
    val ticketType: String? = null,
    val status: String? = null,
    val freeText: String? = null
)

/**
 * Request body for filtering visitor tickets.
 */
data class FilterVisitorTicketsRequest(
    val filter: VisitorTicketFilter? = null,
    val pagination: PaginationRequest? = null
)

/**
 * Pagination parameters for filter requests.
 */
data class PaginationRequest(
    val pageSize: Int? = null,
    val pageToken: String? = null
)

/**
 * Response from filtering visitor tickets.
 */
data class FilterVisitorTicketsResponse(
    val tickets: List<ApiVisitorTicket> = emptyList(),
    val nextPageToken: String? = null,
    val prevPageToken: String? = null,
    val total: Int? = null
)
