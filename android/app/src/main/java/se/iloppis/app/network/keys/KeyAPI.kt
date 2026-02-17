package se.iloppis.app.network.keys

import retrofit2.http.GET
import retrofit2.http.Path
import se.iloppis.app.network.ILoppisApiInterface

/**
 * iLoppis Key API
 */
interface KeyAPI : ILoppisApiInterface {
    /**
     * Gets iLoppis API key by alias
     */
    @GET("v1/api-keys/alias/{alias}")
    suspend fun getApiKeyByAlias(
        @Path("alias") alias: String
    ) : KeyApiResponse
}
