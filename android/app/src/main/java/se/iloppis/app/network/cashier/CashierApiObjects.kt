package se.iloppis.app.network.cashier

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

/** Payment methods supported by the cashier API. */
@Serializable
enum class PaymentMethod {
    PAYMENT_METHOD_UNSPECIFIED,
    SWISH,
    KONTANT
}

/** Sold-item representation returned by the cashier API. */
data class ApiItem(
    val itemId: String?,
    val eventId: String?,
    val cashierAlias: String?,
    val purchaseId: String,
    val seller: Int,
    val price: Int,
    val paymentMethod: PaymentMethod,
    val soldTime: String?,
    val collectedBySeller: Boolean?,
    val collectedTime: String?,
    val isArchived: Boolean?
)

/** Rejected sold item with its human-readable and machine-readable reasons. */
data class RejectedItem(
    val item: ApiItem,
    val reason: String,
    /** Wire value mapped to `SerializableSoldItemErrorCode` when recognized. */
    val errorCode: String? = null
)

/** Response envelope shared by cashier upload and list operations. */
data class CashierApiResponse(
    val acceptedItems: List<ApiItem>?,
    val rejectedItems: List<RejectedItem>?,
    val items: List<ApiItem>?,
    val nextPageToken: String?,
    val prevPageToken: String?,
)

/** Sold item submitted as part of [SoldItemsRequest]. */
data class SoldItemObject(
    val itemId: String,
    val purchaseId: String,
    val seller: Int,
    val price: Int,
    val paymentMethod: PaymentMethod
)

/** Batch submitted atomically to the cashier endpoint. */
data class SoldItemsRequest(
    val items: List<SoldItemObject>
)

/** Cashier activity reported by presence heartbeats. */
enum class CashierClientState {
    CASHIER_CLIENT_STATE_IDLE,
    CASHIER_CLIENT_STATE_ACTIVE_TRANSACTION,
    CASHIER_CLIENT_STATE_SUBMITTING
}

/** Platform that produced a cashier heartbeat. */
enum class CashierClientType {
    CASHIER_CLIENT_TYPE_ANDROID,
    CASHIER_CLIENT_TYPE_IOS
}

/** State transition carried by the next cashier heartbeat. */
enum class RegisterLifecycleEventType {
    REGISTER_LIFECYCLE_OPEN,
    REGISTER_LIFECYCLE_SYNC,
    REGISTER_LIFECYCLE_CLOSE_REQUESTED,
    REGISTER_LIFECYCLE_CLOSE_CONFIRMED
}

/** Presence and register-session state sent on one heartbeat tick. */
data class CashierPresenceHeartbeatRequest(
    @SerializedName("client_state")
    val clientState: CashierClientState,
    @SerializedName("pending_purchases_count")
    val pendingPurchasesCount: Int,
    @SerializedName("client_type")
    val clientType: CashierClientType,
    @SerializedName("display_name")
    val displayName: String? = null,
    /** Lifecycle event to deliver on this tick, or `null` for routine presence. */
    @SerializedName("lifecycle_event_type")
    val lifecycleEventType: RegisterLifecycleEventType? = null,
    /** Stable register identifier within the event. */
    @SerializedName("register_id")
    val registerId: String? = null,
    /** Session ID assigned when the register was opened. */
    @SerializedName("session_id")
    val sessionId: String? = null
)

/** Server-authoritative display data returned by a presence heartbeat. */
data class CashierPresenceHeartbeatResponse(
    @SerializedName("display_name")
    val displayName: String? = null
)
