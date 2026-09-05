package se.iloppis.app.network.events

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import se.iloppis.app.network.ILoppisApiInterface

/** Backend operations for reading events. */
interface EventAPI : ILoppisApiInterface {
    /** Returns an unfiltered page of events. Prefer [get] when filtering is possible. */
    @GET("v1/events")
    suspend fun getAll() : ApiEventListResponse

    /** Returns events matching [request]. */
    @POST("v1/events:filter")
    suspend fun get(@Body request: EventFilterRequest) : ApiEventListResponse

    /** Returns events whose IDs are encoded in [ids] for the API query parameter. */
    @GET("v1/events")
    suspend fun get(@Query("eventIds") ids: String) : ApiEventListResponse

    /** Returns events whose market IDs are encoded in [ids] for the API query parameter. */
    @GET("v1/events")
    suspend fun getEventsFromMarkets(@Query("marketIds") ids: String) : ApiEventListResponse

    companion object
}



/**
 * Converts a String collection into a raw string
 *
 * ```kt
 * [one, two, three] // Input
 * one,two,three     // Output
 * ```
 */
fun EventAPI.Companion.convertCollection(collection: Collection<String>) : String = collection.toString()
    .replace(" ", "")
    .drop(1)
    .dropLast(1)
