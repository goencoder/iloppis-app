package se.iloppis.app.network.cashier

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import se.iloppis.app.network.ILoppisApiInterface

/**
 * Cashier API interface
 */
interface CashierAPI : ILoppisApiInterface {
    /**
     * Sends sold items create request
     */
    @POST("v1/events/{event_id}/sold-items")
    suspend fun createSoldItems(
        @Header("Authorization") authorization: String,
        @Path("event_id") eventId: String,
        @Body request: SoldItemsRequest
    ) : CashierApiResponse

    /**
     * Lists sold items
     */
    @GET("v1/events/{event_id}/sold-items")
    suspend fun listSoldItems(
        @Header("Authorization") authorization: String,
        @Path("event_id") eventId: String,
        @Query("purchaseId") purchaseId: String? = null,
        @Query("pageSize") pageSize: Int = 100
    ) : CashierApiResponse
}
