package se.iloppis.app.network.tickets

/**
 * iLoppis ticket object
 */
data class ApiTicketObject(
    /**
     * Ticket ID
     */
    val id: String,
    /**
     * Event ID
     *
     * The event that this ticket belongs to
     */
    val eventId: String,
    /**
     * Ticket type
     */
    val type: String,
    /**
     * Ticket description
     */
    val description: String?,
    /**
     * Max tickets per visitor
     */
    val maxPerVisitor: Int?,
    /**
     * Available amount of tickets
     */
    val available: Int?,
    /**
     * Sold amount of tickets
     */
    val sold: Int?,
    /**
     * Ticket valid start date
     *
     * The date when this ticket becomes valid
     */
    val validFrom: String?,
    /**
     * Ticket valid end date
     *
     * The date when this ticket becomes invalid
     */
    val validUntil: String?
)

/**
 * iLoppis tickets response object
 */
data class TicketsResponse(
    /**
     * List of tickets
     */
    val types: List<ApiTicketObject>
)
