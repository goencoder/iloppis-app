package se.iloppis.app.network.cashier

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

/**
 * Payment method types enum
 */
@Serializable
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
    KONTANT
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

/**
 * Cashier client activity states for presence heartbeat.
 */
enum class CashierClientState {
    CASHIER_CLIENT_STATE_IDLE,
    CASHIER_CLIENT_STATE_ACTIVE_TRANSACTION,
    CASHIER_CLIENT_STATE_SUBMITTING
}

/**
 * Cashier client type.
 */
enum class CashierClientType {
    CASHIER_CLIENT_TYPE_ANDROID,
    CASHIER_CLIENT_TYPE_IOS
}

/**
 * Register lifecycle event types for cashier session tracking (ILP-003-08).
 */
enum class RegisterLifecycleEventType {
    REGISTER_LIFECYCLE_OPEN,
    REGISTER_LIFECYCLE_SYNC,
    REGISTER_LIFECYCLE_CLOSE_REQUESTED,
    REGISTER_LIFECYCLE_CLOSE_CONFIRMED
}

/**
 * Presence heartbeat request body.
 */
data class CashierPresenceHeartbeatRequest(
    @SerializedName("client_state")
    val clientState: CashierClientState,
    @SerializedName("pending_purchases_count")
    val pendingPurchasesCount: Int,
    @SerializedName("client_type")
    val clientType: CashierClientType,
    @SerializedName("display_name")
    val displayName: String? = null,
    /** ILP-003: lifecycle event to deliver in this heartbeat tick, or null for routine ticks. */
    @SerializedName("lifecycle_event_type")
    val lifecycleEventType: RegisterLifecycleEventType? = null,
    /** ILP-003: register identifier for session tracking. */
    @SerializedName("register_id")
    val registerId: String? = null,
    /** ILP-003: session id assigned when the session was opened. */
    @SerializedName("session_id")
    val sessionId: String? = null
)

/**
 * Presence heartbeat response body.
 */
data class CashierPresenceHeartbeatResponse(
    @SerializedName("display_name")
    val displayName: String? = null
)
