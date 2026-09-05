package se.iloppis.app.data.models

import kotlinx.serialization.Serializable

/**
 * A rejected item and its backend error.
 * An unspecified code with an empty reason means another item caused the purchase rejection.
 */
@Serializable
data class RejectedItemWithDetails(
    val item: StoredSoldItem,
    val reason: String,
    val errorCode: SerializableSoldItemErrorCode
) {
    /** Returns whether another item caused this item to be rejected. */
    val isCollateralDamage: Boolean
        get() = errorCode == SerializableSoldItemErrorCode.UNSPECIFIED && reason.isEmpty()
    
    /** Returns whether this item has its own rejection reason. */
    val hasPrimaryError: Boolean
        get() = !isCollateralDamage
}

/** A backend-rejected purchase retained for retry or manual review. */
@Serializable
data class RejectedPurchase(
    val purchaseId: String,
    val items: List<RejectedItemWithDetails>,
    val errorCode: SerializableSoldItemErrorCode,
    val errorMessage: String,
    val timestamp: String,
    val retryAttempts: Int = 0,
    val autoRecoveryAttempted: Boolean = false,
    val needsManualReview: Boolean = false
)
