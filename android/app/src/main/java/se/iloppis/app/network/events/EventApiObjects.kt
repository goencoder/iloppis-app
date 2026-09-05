package se.iloppis.app.network.events

/** Lifecycle values accepted from current and legacy event payloads. */
enum class EventLifecycle {
    OPEN,
    LIFECYCLE_STATE_OPEN,
    CLOSED,
    LIFECYCLE_STATE_CLOSED,
    FINALIZED,
    PENDING,
    LIFECYCLE_STATE_PENDING,
}

/** Optional criteria sent to the event-filter endpoint. Dates use the API wire format. */
data class EventFilter(
    val city: String? = null,
    /**
     * Inclusive lower bound for event start dates.
     */
    val dateFrom: String? = null,
    /** Inclusive upper bound for event end dates. */
    val dateTo: String? = null,
    val query: String? = null,
    val lifecycleStates: List<EventLifecycle>? = null
)

/** Request envelope for filtered event searches. */
data class EventFilterRequest(
    val filter: EventFilter,
    val pagination: Map<String, Any> = emptyMap()
)

/** Seller-letter metadata returned with an event. */
data class EventSellerLetter(
    val pdfUrl: String?,
    val hasLetter: Boolean?,
    val uploadedAt: String?,
    val expiresAt: String?,
    val infoSummary: String?
)

/** Event representation used on the API wire. */
data class ApiEvent(
    val id: String,
    val marketId: String?,
    val name: String,
    val description: String?,
    val startTime: String?,
    val endTime: String?,
    val addressStreet: String?,
    val addressCity: String?,
    val addressState: String?,
    val addressZip: String?,
    val latitude: Double?,
    val longitude: Double?,
    val maxVendors: Int?,
    val vendorApplicationStartTime: String?,
    val publishTime: String?,
    val maxTicketsPerVisitor: Int?,
    val availableTickets: Int?,
    val soldTickets: Int?,
    val acceptVendorApplications: Boolean?,
    val ownerEmail: String?,
    val lifecycleState: EventLifecycle?,
    val sellerInfoSummary: String?,
    val sellerLetter: EventSellerLetter?
)

/** Paginated event result. [total] is the server total, not necessarily this page size. */
data class ApiEventListResponse(
    val events: List<ApiEvent>,
    val total: Int = events.size
)
