package se.iloppis.app.utils.events.state

import android.util.Log
import retrofit2.HttpException
import se.iloppis.app.data.mappers.EventMapper.toDomain
import se.iloppis.app.domain.model.Event
import se.iloppis.app.network.ILoppisClient
import se.iloppis.app.network.events.ApiEventListResponse
import se.iloppis.app.network.events.EventAPI
import se.iloppis.app.network.events.EventFilter
import se.iloppis.app.network.events.EventFilterRequest
import se.iloppis.app.network.events.EventLifecycle
import se.iloppis.app.network.events.convertCollection
import java.time.LocalDate

/**
 * Loads events from local storage
 */
internal suspend fun EventListState.loadLocalEvents() {
    if(storage.empty()) {
        isLoading = false
        return
    }

    Log.d(EventListState.TAG, "Loading events from: ${config.url}")
    try {
        val api = ILoppisClient(config).create<EventAPI>()

        val ids = EventAPI.convertCollection(storage.getData())
        Log.d(EventListState.TAG, "Fetching events: [$ids]")
        val res = api.get(ids)
        Log.d(EventListState.TAG, "Response received, events count: ${res.total}")

        val handled = handleResponseForLocalEvents(res)
        events.addAll(handled) /* Adds all events to an empty events list */

    } catch (e: Exception) {
        val message = e.message()
        Log.e(EventListState.TAG, "Error loading events", e)
        errorMessage = "API: ${config.url} - $message"
    }
    isLoading = false
}

/**
 * Loads events without any specific sorting
 * method applied.
 *
 * @see EventListSortType.ALL
 */
internal suspend fun EventListState.loadAllEvents() {
    Log.d(EventListState.TAG, "Loading events from: ${config.url}")
    try {
        val api = ILoppisClient(config).create<EventAPI>()
        Log.d(EventListState.TAG, "API client created, making filterEvents request")
        val today = "${LocalDate.now()}T00:00:00Z"
        val filterRequest = EventFilterRequest(
            filter = EventFilter(
                dateFrom = today,
                lifecycleStates = listOf(EventLifecycle.OPEN)
            )
        )
        Log.d(EventListState.TAG, "Filter request: dateFrom=$today, states=[OPEN]")
        val response = api.get(filterRequest)
        Log.d(EventListState.TAG, "Response received, events count: ${response.events.size}")
        events.addAll(response.events.map { it.toDomain() })

    }catch (e: Exception) {
        val message = e.message()
        Log.e(EventListState.TAG, "Error loading events", e)
        errorMessage = "API: ${config.url} - $message"
    }
    isLoading = false
}





/**
 * Exception message handler for API errors
 */
private fun Exception.message() = when (this) {
    is HttpException -> "HTTP ${this.code()}: ${this.message()}"
    else -> "Network Error: ${this.message}"
}



/**
 * Handles response for local events
 *
 * This takes an API response and
 * removes all ID's that are not
 * in the stored events list.
 *
 * If there is any ID's stored locally
 * that does not have an event on the
 * server it will be removed.
 *
 * @see se.iloppis.app.utils.events.LocalEventsListStorage
 */
private fun EventListState.handleResponseForLocalEvents(response: ApiEventListResponse) : List<Event> {
    val idsToKeep = mutableSetOf<String>()

    val result = response.events.mapNotNull {
        if(it.lifecycleState == EventLifecycle.OPEN) {
            idsToKeep.add(it.id)
            it.toDomain()
        } else null
    }

    storage.set(idsToKeep)
    return result
}
