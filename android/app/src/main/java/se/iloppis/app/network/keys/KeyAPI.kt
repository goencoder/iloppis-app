package se.iloppis.app.network.keys

import retrofit2.http.GET
import retrofit2.http.Path
import se.iloppis.app.network.iLoppisApiInterface

/**
 * iLoppis Key API
 */
interface KeyAPI : iLoppisApiInterface {
    /**
     * Gets iLoppis API key by alias
     */
    @GET("v1/events/{event_id}/api-keys/alias/{alias}")
    suspend fun getApiKeyByAlias(
        @Path("event_id") eventId: String,
        @Path("alias") alias: String
    ) : KeyApiResponse
}
