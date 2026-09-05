package se.iloppis.app.data.models

import kotlinx.serialization.Serializable
import se.iloppis.app.network.cashier.PaymentMethod

/** A sold item retained locally until its backend upload succeeds. */
@Serializable
data class PendingItem(
    val itemId: String,
    val purchaseId: String,
    val sellerId: Int,
    val price: Int,
    val paymentMethod: PaymentMethod = PaymentMethod.KONTANT,
    val errorText: String = "",
    val timestamp: String
)
