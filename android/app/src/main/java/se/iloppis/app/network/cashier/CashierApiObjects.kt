package se.iloppis.app.network.cashier

import kotlinx.serialization.Serializable

/**
 * Payment method types enum
 */
@Serializable
enum class PaymentMethod(val value: String) {
    /**
     * Unspecified payment method
     */
    PAYMENT_METHOD_UNSPECIFIED("PAYMENT_METHOD_UNSPECIFIED"),

    /**
     * Swish payment method
     */
    SWISH("SWISH"),

    /**
     * Cash payment method
     */
    CASH("KONTANT")
}

/**
 * iLoppis API item object
 */
data class ApiItem(
    /**
     * Item ID
     */
    val itemId: String?,
    /**
     * Event ID
     *
     * The ID of the event that this item is
     * registered to.
     */
    val eventId: String?,
    /**
     * The alias of the cashier
     */
    val cashierAlias: String?,
    /**
     * The ID of the purchase that this item
     * is registered to.
     */
    val purchaseId: String,
    /**
     * The seller who sold this item
     */
    val seller: Int,
    /**
     * The price of this item
     */
    val price: Int,

    /**
     * The payment method to use
     */
    val paymentMethod: PaymentMethod,

    /**
     * The time this item was sold
     */
    val soldTime: String?,

    /**
     * If this item was collected by seller or not
     */
    val collectedBySeller: Boolean?,
    /**
     * The time this item was collected if it was collected
     */
    val collectedTime: String?,

    /**
     * If this item is archived or not
     */
    val isArchived: Boolean?
)

/**
 * iLoppis API rejected item object
 */
data class RejectedItem(
    /**
     * The rejected item
     */
    val item: ApiItem,
    /**
     * A reason for why the item was rejected
     */
    val reason: String,

    /**
     * An error code if there was an error
     *
     * This maps to [se.iloppis.app.data.models.SerializableSoldItemErrorCode]
     * from proto
     */
    val errorCode: String? = null
)

/**
 * Cashier API response object
 */
data class CashierApiResponse(
    /**
     * List of accepted sold items
     */
    val acceptedItems: List<ApiItem>?,
    /**
     * List of rejected items
     */
    val rejectedItems: List<RejectedItem>?,

    /**
     * List of items
     */
    val items: List<ApiItem>?,
    /**
     * Next page token
     */
    val nextPageToken: String?,
    /**
     * Previous page token
     */
    val prevPageToken: String?,
)



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



//interface SoldItemsApi {
//    @POST("v1/events/{event_id}/sold-items")
//    suspend fun createSoldItems(
//        @Header("Authorization") authorization: String,
//        @Path("event_id") eventId: String,
//        @Body request: CreateSoldItemsRequest
//    ): CreateSoldItemsResponse
//
//    @GET("v1/events/{event_id}/sold-items")
//    suspend fun listSoldItems(
//        @Header("Authorization") authorization: String,
//        @Path("event_id") eventId: String,
//        @Query("purchaseId") purchaseId: String? = null,
//        @Query("pageSize") pageSize: Int = 100
//    ): ListSoldItemsResponse
//}
