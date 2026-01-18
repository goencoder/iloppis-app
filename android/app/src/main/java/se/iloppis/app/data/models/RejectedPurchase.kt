package se.iloppis.app.data.models

import kotlinx.serialization.Serializable

/**
 * Represents a single item within a rejected purchase, including its specific error details.
 * 
 * Backend returns rejectedItems with structure:
 * {
 *   "item": { itemId, seller, price, ... },
 *   "reason": "seller 10 is not approved" or "",
 *   "errorCode": "SOLD_ITEM_ERROR_CODE_INVALID_SELLER" or "SOLD_ITEM_ERROR_CODE_UNSPECIFIED"
 * }
 * 
 * Items with errorCode=UNSPECIFIED and empty reason are "collateral damage" - they failed
 * because another item in the same purchase had a primary error.
 * 
 * @property item The sold item data
 * @property reason Human-readable error message (empty for collateral items)
 * @property errorCode Structured error code from backend (serializable wrapper)
 */
@Serializable
data class RejectedItemWithDetails(
    val item: StoredSoldItem,
    val reason: String,
    val errorCode: SerializableSoldItemErrorCode
) {
    /**
     * Returns true if this item is collateral damage (failed due to another item's error).
     */
    val isCollateralDamage: Boolean
        get() = errorCode == SerializableSoldItemErrorCode.UNSPECIFIED && reason.isEmpty()
    
    /**
     * Returns true if this item has a specific error.
     */
    val hasPrimaryError: Boolean
        get() = !isCollateralDamage
}

/**
 * Represents a purchase that was rejected by the backend and needs attention.
 * 
 * @property purchaseId Unique ULID identifier for this purchase
 * @property items List of items with individual error details
 * @property errorCode Overall error code (typically from first item with error)
 * @property errorMessage Overall error message (typically from first item with error)
 * @property timestamp When this rejection occurred (ISO-8601 format)
 * @property retryAttempts Number of retry attempts made
 * @property autoRecoveryAttempted Whether auto-recovery was tried
 * @property needsManualReview Whether this requires manual user intervention
 */
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

/**
 * Result of an auto-recovery attempt.
 */
sealed class RecoveryResult {
    /** Auto-recovery succeeded, purchase was uploaded */
    data object Success : RecoveryResult()
    
    /** Auto-recovery failed with a specific reason */
    data class Failed(val reason: String) : RecoveryResult()
    
    /** Auto-recovery failed, manual review needed */
    data class NeedsManualReview(val invalidSellers: List<Int>) : RecoveryResult()
}
