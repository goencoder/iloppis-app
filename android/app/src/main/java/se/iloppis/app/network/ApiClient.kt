package se.iloppis.app.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit
import kotlin.time.Clock

/**
 * iLoppis API base url
 */
const val API_URL = "https://iloppis-staging.fly.dev/"



/**
 * Gets today time
 */
fun getTodayTime(clock: Clock = Clock.System): String {
    return "${clock.now().toString().split("T")[0]}T00:00:00Z"
}


/**
 * API Client to connect to the iLoppis backend
 */
@Deprecated(
    message = "Old API Client use the new one",
    replaceWith = ReplaceWith("iLoppisApiClient")
)
object ApiClient {
    /**
     * Logging interceptor that:
     * - Uses BODY level only in debug builds (full request/response)
     * - Uses BASIC level in release builds (method + URL only)
     * - Redacts Authorization headers to prevent leaking API keys
     */
    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY // Force BODY logging for debugging
        redactHeader("Authorization")
        redactHeader("X-API-Key")
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(logging)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(API_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    inline fun <reified T> create(): T = retrofit.create(T::class.java)
}

// ============ Events API ============

data class EventFID(
    val id: String,
    val marketId: String,
    val name: String,
    val description: String?,
    val startTime: String?,
    val endTime: String?,
    val addressStreet: String?,
    val addressCity: String?,
    val addressState: String?,
    val addressZip: Int?,
    val latitude: Int?,
    val longitude: Int?,
    val maxVendors: Int?,
    val vendorApplicationStartTime: String?,
    val publishTime: String?,
    val maxTicketsPerVisitor: Int?,
    val availableTickets: Int?,
    val soldTickets: Int?,
    val acceptVendorApplications: Boolean?,
    val ownerEmail: String?,
    val lifecycleState: String?,
    val sellerInfoSummary: String?,
    val sellerLetter: String?
)

data class EventsFromID(
    val events: List<EventFID>,
    val total: Int
)


data class EventDto(
    val id: String,
    val name: String,
    val description: String?,
    val startTime: String?,
    val endTime: String?,
    val addressStreet: String?,
    val addressCity: String?,
    val lifecycleState: String?
)

data class EventListResponse(val events: List<EventDto>)

data class EventFilterRequest(
    val filter: EventFilter,
    val pagination: Map<String, Any> = emptyMap()
)

data class EventFilter(
    val city: String? = null,
    val dateFrom: String? = null,
    val dateTo: String? = null,
    val searchText: String? = null,
    val lifecycleStates: List<String>? = null
)

interface EventApi {
    @GET("v1/events")
    suspend fun listEvents(): EventListResponse

    @POST("v1/events:filter")
    suspend fun filterEvents(@Body request: EventFilterRequest): EventListResponse

    @GET("v1/events")
    suspend fun getEventsByIds(@Query("eventIds") ids: String): EventsFromID

    @GET("v1/events")
    suspend fun getMarketsByIds(@Query("marketIds") ids: String): EventsFromID
}

// ============ API Key API ============

data class ApiKeyResponse(
    val alias: String,
    val apiKey: String,
    val isActive: Boolean,
    val type: String? = null,
    val tags: List<String>? = null,
    val id: String? = null
)

interface ApiKeyApi {
    @GET("v1/events/{event_id}/api-keys/alias/{alias}")
    suspend fun getApiKeyByAlias(
        @Path("event_id") eventId: String,
        @Path("alias") alias: String
    ): ApiKeyResponse
}

// ============ Vendors API ============

data class VendorDto(
    val id: String,
    val sellerNumber: Int,
    val firstName: String?,
    val lastName: String?,
    val email: String?,
    val phone: String?,
    val status: String?
)

data class ListVendorsResponse(
    val vendors: List<VendorDto>,
    val nextPageToken: String?
)

data class VendorFilter(
    val status: String? = null,
    val sellerNumber: Int? = null,
    val email: String? = null,
    val searchText: String? = null
)

data class VendorSortOrder(
    val field: String = "seller_number",
    val ascending: Boolean = true
)

data class VendorPagination(
    val pageSize: Int = 100,
    val nextPageToken: String? = null
)

data class FilterVendorsRequest(
    val filter: VendorFilter = VendorFilter(),
    val sort: VendorSortOrder? = null,
    val pagination: VendorPagination = VendorPagination()
)

data class FilterVendorsResponse(
    val vendors: List<VendorDto>,
    val nextPageToken: String?
)

interface VendorApi {
    @GET("v1/events/{event_id}/vendors")
    suspend fun listVendors(
        @Header("Authorization") authorization: String,
        @Path("event_id") eventId: String,
        @Query("pageSize") pageSize: Int = 100,
        @Query("nextPageToken") nextPageToken: String? = null
    ): ListVendorsResponse

    @POST("v1/events/{event_id}/vendors:filter")
    suspend fun filterVendors(
        @Header("Authorization") authorization: String,
        @Path("event_id") eventId: String,
        @Body request: FilterVendorsRequest
    ): FilterVendorsResponse
}

// ============ Sold Items API ============

enum class PaymentMethod {
    PAYMENT_METHOD_UNSPECIFIED,
    SWISH,
    KONTANT
}

data class SoldItemRequest(
    val itemId: String,
    val purchaseId: String,
    val seller: Int,
    val price: Int,
    val paymentMethod: String  // "SWISH" or "KONTANT"
)

data class CreateSoldItemsRequest(
    val items: List<SoldItemRequest>
)

data class SoldItemDto(
    val itemId: String?,
    val eventId: String?,
    val cashierAlias: String?,
    val purchaseId: String,
    val seller: Int,
    val price: Int,
    val paymentMethod: String?,
    val soldTime: String?,
    val collectedBySeller: Boolean?,
    val collectedTime: String?,
    val isArchived: Boolean?
)

data class RejectedItem(
    val item: SoldItemDto,
    val reason: String,
    val errorCode: String? = null  // Maps to SoldItemErrorCode enum from proto
)

data class CreateSoldItemsResponse(
    val acceptedItems: List<SoldItemDto>?,
    val rejectedItems: List<RejectedItem>?
)

data class ListSoldItemsResponse(
    val items: List<SoldItemDto>?,
    val nextPageToken: String?,
    val prevPageToken: String?
)

interface SoldItemsApi {
    @POST("v1/events/{event_id}/sold-items")
    suspend fun createSoldItems(
        @Header("Authorization") authorization: String,
        @Path("event_id") eventId: String,
        @Body request: CreateSoldItemsRequest
    ): CreateSoldItemsResponse

    @GET("v1/events/{event_id}/sold-items")
    suspend fun listSoldItems(
        @Header("Authorization") authorization: String,
        @Path("event_id") eventId: String,
        @Query("purchaseId") purchaseId: String? = null,
        @Query("pageSize") pageSize: Int = 100
    ): ListSoldItemsResponse
}

// ============ Visitor Tickets API ============

data class VisitorTicketDto(
    val id: String,
    val eventId: String,
    val ticketType: String?,
    val email: String?,
    val status: String?,
    val issuedAt: String?,
    val validFrom: String?,
    val validUntil: String?,
    val scannedAt: String?
)

data class ScanVisitorTicketResponse(
    val ticket: VisitorTicketDto?
)

data class GetVisitorTicketResponse(
    val ticket: VisitorTicketDto?
)

interface VisitorTicketApi {
    @POST("v1/events/{event_id}/visitor_tickets/{ticket_id}/scan")
    suspend fun scanVisitorTicket(
        @Header("Authorization") authorization: String,
        @Path("event_id") eventId: String,
        @Path("ticket_id") ticketId: String,
        @Body body: Map<String, String> = emptyMap()
    ): ScanVisitorTicketResponse

    @GET("v1/events/{event_id}/visitor_tickets/{ticket_id}")
    suspend fun getVisitorTicket(
        @Header("Authorization") authorization: String,
        @Path("event_id") eventId: String,
        @Path("ticket_id") ticketId: String
    ): GetVisitorTicketResponse
}

// ============ Ticket Types API ============

data class TicketTypeDto(
    val id: String,
    val eventId: String,
    val type: String,
    val description: String?,
    val maxPerVisitor: Int?,
    val available: Int?,
    val sold: Int?,
    val validFrom: String?,
    val validUntil: String?
)

data class ListTicketTypesResponse(
    val types: List<TicketTypeDto>
)

interface TicketTypeApi {
    @GET("v1/events/{event_id}/ticket_types")
    suspend fun listTicketTypes(
        @Header("Authorization") authorization: String,
        @Path("event_id") eventId: String
    ): ListTicketTypesResponse
}
