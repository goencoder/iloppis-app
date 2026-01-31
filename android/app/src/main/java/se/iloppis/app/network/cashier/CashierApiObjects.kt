package se.iloppis.app.network.cashier

/**
 * Payment method types enum
 */
enum class PaymentMethod {
    /**
     * Unspecified payment method
     */
    PAYMENT_METHOD_UNSPECIFIED,

    /**
     * Swish payment method
     */
    SWISH,

    /**
     * Cash payment method
     */
    CASH
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

/**
 * Sold item object for [SoldItemsRequest]
 */
data class SoldItemObject(
    /**
     * Item ID
     */
    val itemId: String,
    /**
     * Purchase ID
     */
    val purchaseId: String,
    /**
     * Seller number
     */
    val seller: Int,
    /**
     * Item price
     */
    val price: Int,
    /**
     * Payment method
     */
    val paymentMethod: PaymentMethod
)

/**
 * Request for sold items
 */
data class SoldItemsRequest(
    /**
     * List of items
     */
    val items: List<SoldItemObject>
)
