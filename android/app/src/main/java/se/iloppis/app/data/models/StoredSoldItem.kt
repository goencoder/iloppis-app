package se.iloppis.app.data.models

import kotlinx.serialization.Serializable
import se.iloppis.app.network.cashier.PaymentMethod

/** A sold item in the local persistence format shared with the cashier workflow. */
@Serializable
data class StoredSoldItem(
    val itemId: String,
    val eventId: String,
    val purchaseId: String,
    val seller: Int,
    val price: Int,
    val paymentMethod: PaymentMethod,
    val soldTime: Long,
    val uploaded: Boolean = false
)
