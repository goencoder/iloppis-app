package se.iloppis.app.network.events

/**
 * Events lifecycle enum types
 */
enum class EventLifecycle {
    /**
     * Lifecycle open
     */
    OPEN,
    /**
     * Lifecycle state open
     *
     * @see OPEN
     */
    LIFECYCLE_STATE_OPEN,

    /**
     * Lifecycle closed
     */
    CLOSED,
    /**
     * Lifecycle state closed
     *
     * @see CLOSED
     */
    LIFECYCLE_STATE_CLOSED,

    /**
     * Lifecycle finalized
     */
    FINALIZED,

    /**
     * Lifecycle pending
     */
    PENDING,
    /**
     * Lifecycle state pending
     *
     * @see PENDING
     */
    LIFECYCLE_STATE_PENDING,
}

/**
 * Event filter object
 */
data class EventFilter(
    /**
     * Filter after city
     */
    val city: String? = null,

    /**
     * Date interval filter
     *
     * Filters events starting from this date
     * and ending at [dateTo]
     */
    val dateFrom: String? = null,
    /**
     * Date interval filter
     *
     * Filters events ending on this date
     * and starting from [dateFrom]
     */
    val dateTo: String? = null,

    /**
     * Filter events to contain a search term
     */
    val searchText: String? = null,

    /**
     * Filter after events lifecycle state
     */
    val lifecycleStates: List<EventLifecycle>? = null
)

/**
 * Event filter request
 */
data class EventFilterRequest(
    /**
     * Events search filter
     */
    val filter: EventFilter,

    /**
     * Request pagination
     */
    val pagination: Map<String, Any> = emptyMap()
)

/**
 * Event seller letter
 */
data class EventSellerLetter(
    val pdfUrl: String?,
    val hasLetter: Boolean?,
    val uploadedAt: String?,
    val expiresAt: String?,
    val infoSummary: String?
)



/**
 * API Event object
 */
data class ApiEvent(
    /**
     * Event ID
     */
    val id: String,

    /**
     * Market ID
     */
    val marketId: String?,

    /**
     * Event name
     */
    val name: String,

    /**
     * Event description
     */
    val description: String?,

    /**
     * The time that this event starts as a raw string
     */
    val startTime: String?,
    /**
     * The time that this event ends as a raw string
     */
    val endTime: String?,

    /**
     * The address of the street where the event is located
     */
    val addressStreet: String?,
    /**
     * The ity where the event is located
     */
    val addressCity: String?,
    /**
     * The state where the event is located
     */
    val addressState: String?,
    /**
     * The event address zip code
     */
    val addressZip: Int?,
    /**
     * Event latitude coordinates
     */
    val latitude: Double?,
    /**
     * Event longitude coordinates
     */
    val longitude: Double?,

    /**
     * Events max vendors
     */
    val maxVendors: Int?,
    /**
     * Vendor application starting time
     *
     * The starting time of when vendors
     * can sign up for this event.
     */
    val vendorApplicationStartTime: String?,
    /**
     * The time when this event was published
     */
    val publishTime: String?,
    /**
     * Max tickets per visitor
     */
    val maxTicketsPerVisitor: Int?,
    /**
     * The amount of available tickets in the event
     */
    val availableTickets: Int?,
    /**
     * The amount of sold tickets
     */
    val soldTickets: Int?,
    /**
     * The amount of vendor applications
     * that have been accepted.
     */
    val acceptVendorApplications: Boolean?,

    /**
     * Event owner email
     */
    val ownerEmail: String?,

    /**
     * Event lifecycle state
     */
    val lifecycleState: EventLifecycle?,

    /**
     * Seller information summary
     */
    val sellerInfoSummary: String?,
    /**
     * Seller letter
     */
    val sellerLetter: EventSellerLetter?
)

/**
 * API Event object list response
 */
data class ApiEventListResponse(
    /**
     * List of events
     */
    val events: List<ApiEvent>,

    /**
     * Total amount of events
     */
    val total: Int = events.size
)
