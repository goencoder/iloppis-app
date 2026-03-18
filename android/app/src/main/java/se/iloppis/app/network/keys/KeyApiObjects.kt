package se.iloppis.app.network.keys

import com.google.gson.annotations.SerializedName

/**
 * iLoppis key API response
 */
data class KeyApiResponse(
    /**
     * Event ID of the event that this key belongs to.
     */
    @SerializedName(value = "eventId", alternate = ["event_id"])
    val eventId: String,

    /**
     * Key alias
     */
    val alias: String,

    /**
     * API key
     */
    @SerializedName(value = "apiKey", alternate = ["api_key"])
    val apiKey: String,

    /**
     * Key active status
     */
    @SerializedName(value = "isActive", alternate = ["is_active"])
    val isActive: Boolean,

    /**
     * Key type
     */
    val type: String? = null,

    /**
     * Key tags
     */
    val tags: List<String>? = null,

    /**
     * Key ID
     */
    val id: String? = null
)
