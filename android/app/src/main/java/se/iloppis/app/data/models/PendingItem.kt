package se.iloppis.app.data.models

import kotlinx.serialization.Serializable

/**
 * Represents a single item pending upload to the backend.
 * 
 * This item is stored in pending_items.jsonl (one JSON object per line).
 * Row existence = pending, deleted row = uploaded successfully.
 * 
 * @property itemId Unique identifier for this item (UUID)
 * @property purchaseId Common identifier for all items in same purchase (UUID)
 * @property sellerId Seller number who owns this item
 * @property price Price in SEK (whole number, no decimals)
 * @property errorText Error message from backend/server. Empty = waiting/retry, text = has error
 * @property timestamp ISO-8601 timestamp when item was created
 */
@Serializable
data class PendingItem(
    val itemId: String,
    val purchaseId: String,
    val sellerId: Int,
    val price: Int,
    val errorText: String = "",
    val timestamp: String
)
