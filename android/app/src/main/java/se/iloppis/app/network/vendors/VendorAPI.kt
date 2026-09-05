package se.iloppis.app.network.vendors

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import se.iloppis.app.network.ILoppisApiInterface

/** Backend operations for reading event sellers. */
interface VendorAPI : ILoppisApiInterface {
    /** Returns one unfiltered seller page for [eventId]. */
    @GET("v1/events/{event_id}/vendors")
    suspend fun getAll(
        @Header("Authorization") authorization: String,
        @Path("event_id") eventId: String,
        @Query("pageSize") pageSize: Int = 100,
        @Query("nextPageToken") nextPageToken: String? = null
    ) : VendorApiResponse

    /** Returns sellers for [eventId] that match [request]. */
    @POST("v1/events/{event_id}/vendors:filter")
    suspend fun get(
        @Header("Authorization") authorization: String,
        @Path("event_id") eventId: String,
        @Body request: VendorFilterRequest
    ) : VendorApiResponse

}
