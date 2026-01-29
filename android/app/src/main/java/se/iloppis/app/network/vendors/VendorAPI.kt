package se.iloppis.app.network.vendors

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import se.iloppis.app.network.iLoppisApiInterface

/**
 * iLoppis vendor API interface
 */
interface VendorAPI : iLoppisApiInterface {
    /**
     * Gets all vendors of event of [eventId]
     *
     * This will not filter out any vendors
     * and therefore it is recommended to use
     * the [get] method instead.
     *
     * @see get
     */
    @GET("v1/events/{event_id}/vendors")
    suspend fun getAll(
        @Header("Authorization") authorization: String,
        @Path("event_id") eventId: String,
        @Query("pageSize") pageSize: Int = 100,
        @Query("nextPageToken") nextPageToken: String? = null
    ) : VendorApiResponse

    /**
     * Gets vendors that match the filter applied
     *
     * This will get all vendors that match with
     * the [request] filter applied to the search.
     *
     * To get all vendors of an event use [getAll]
     *
     * @see getAll
     */
    @POST("v1/events/{event_id}/vendors:filter")
    suspend fun get(
        @Header("Authorization") authorization: String,
        @Path("event_id") eventId: String,
        @Body request: VendorFilterRequest
    ) : VendorApiResponse

}
