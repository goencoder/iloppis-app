package se.iloppis.app.network.keys

/**
 * iLoppis key API response
 */
data class KeyApiResponse(
    /**
     * Key alias
     */
    val alias: String,

    /**
     * API key
     */
    val apiKey: String,

    /**
     * Key active status
     */
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
