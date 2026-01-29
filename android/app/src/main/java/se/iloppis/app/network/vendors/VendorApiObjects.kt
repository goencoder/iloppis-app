package se.iloppis.app.network.vendors

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

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
