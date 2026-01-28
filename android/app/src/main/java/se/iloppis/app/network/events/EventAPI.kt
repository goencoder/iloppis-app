package se.iloppis.app.network.events

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import se.iloppis.app.network.iLoppisApiInterface

/**
 * iLoppis event API
 */
interface EventAPI : iLoppisApiInterface {
    /**
     * Gets all events without filtering the search
     *
     * This will be slower than searching with a
     * `filter` applied. Therefore it is recommended
     * to use the [get] method instead.
     *
     * @see get
     */
    @GET("v1/events")
    suspend fun getAll() : ApiEventListResponse

    /**
     * Gets events
     *
     * This will apply a `filter` to the
     * event search using [EventFilterRequest].
     *
     * If all events without a filter applied
     * is needed the method [getAll] can be used.
     */
    @POST("v1/events:filter")
    suspend fun get(@Body request: EventFilterRequest) : ApiEventListResponse

    /**
     * Gets a list of events based on their ID
     *
     * This will take a list of IDs as a `String`
     * and return all events that have their ID
     * listed in the IDs string list.
     */
    @GET("v1/events")
    suspend fun get(@Query("eventIds") ids: String) : ApiEventListResponse

    /**
     * Gets a list of events based on their a list of market IDs
     *
     * This works similar to [get] but
     * is based on a list of market IDs.
     *
     * @see get
     */
    @GET("v1/events")
    suspend fun getEventsFromMarkets(@Query("marketIds") ids: String) : ApiEventListResponse



    companion object /* Event API helper methods */
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
