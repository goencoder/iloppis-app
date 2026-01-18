package se.iloppis.app.data.models

import kotlinx.serialization.Serializable

/**
 * StoredSoldItem represents a sold item that is persisted to local storage.
 * Matches Desktop (SoldItem.java) and Backend (SoldItem proto) terminology.
 *
 * @property itemId UUID - matches Desktop/Backend
 * @property eventId Event identifier
 * @property purchaseId ULID that groups items in the same purchase
 * @property seller Seller number (matches backend)
 * @property price Price in SEK
 * @property paymentMethod "KONTANT" or "SWISH"
 * @property soldTime Epoch millis
 * @property uploaded Upload status flag (matches Desktop)
 */
@Serializable
data class StoredSoldItem(
    val itemId: String,
    val eventId: String,
    val purchaseId: String,
    val seller: Int,
    val price: Int,
    val paymentMethod: String,
    val soldTime: Long,
    val uploaded: Boolean = false
)
