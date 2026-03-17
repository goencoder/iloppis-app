package se.iloppis.app.network.stats

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import se.iloppis.app.network.ILoppisApiInterface

interface StatsAPI : ILoppisApiInterface {
    @GET("v1/events/{eventId}/stats:live")
    suspend fun getEventLiveStats(
        @Path("eventId") eventId: String,
        @Header("Authorization") authorization: String
    ): LiveStatsApiResponse
}
