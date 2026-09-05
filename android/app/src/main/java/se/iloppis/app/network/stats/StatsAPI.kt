package se.iloppis.app.network.stats

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import se.iloppis.app.network.ILoppisApiInterface

interface StatsAPI : ILoppisApiInterface {
    /** Returns the current live-statistics snapshot for an event accessible by [authorization]. */
    @GET("v1/events/{eventId}/stats:live")
    suspend fun getEventLiveStats(
        @Path("eventId") eventId: String,
        @Header("Authorization") authorization: String
    ): LiveStatsApiResponse
}
