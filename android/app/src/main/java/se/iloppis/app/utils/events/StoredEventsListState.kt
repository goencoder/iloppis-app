package se.iloppis.app.utils.events

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.HttpException
import se.iloppis.app.data.mappers.EventMapper.toDomain
import se.iloppis.app.domain.model.Event
import se.iloppis.app.network.API_URL
import se.iloppis.app.network.ApiClient
import se.iloppis.app.network.EventApi
import se.iloppis.app.network.EventsFromID
import se.iloppis.app.utils.storage.LocalStorage

/**
 * Stored events list state data class
 */
data class StoredEventsListStateData(
    /**
     * Is Loading status
     */
    val isLoading: Boolean = false,

    /**
     * State error message
     */
    val errorMessage: String? = null,

    /**
     * Set of ID:s ( stored locally )
     */
    val ids: Set<String> = mutableSetOf(),
    /**
     * List of events
     */
    val events: List<Event> = mutableListOf(),
)

/**
 * Stored events list state
 *
 * This uses [LocalStorage] to access locally stored
 * events.
 */
class StoredEventsListState(val storage: LocalStorage) {
    /**
     * State data
     *
     * Use data getters for quick access to
     * the states data attributes.
     *
     * @see ids
     * @see events
     * @see isLoading
     * @see errorMessage
     */
    var data by mutableStateOf(StoredEventsListStateData())
        private set

    /**
     * State loading status
     */
    val isLoading by derivedStateOf { data.isLoading }

    /**
     * State error message
     */
    val errorMessage by derivedStateOf { data.errorMessage }

    /**
     * Set of locally stored events ids
     */
    val ids by derivedStateOf { data.ids }
    /**
     * List of events
     */
    val events by derivedStateOf { data.events }



    init {
        data = data.copy(
            ids = storage.getJson<Set<String>>("stored-events", "[]")
        )
        getEvents()
    }



    private fun getEvents() {
        if(ids.isEmpty()) {
            data = data.copy(
                isLoading = false,
                events = emptyList()
            )
            return
        }

        Log.d(TAG, "Loading events from: $API_URL")
        CoroutineScope(Dispatchers.Main).launch {
            data = data.copy(isLoading = true, errorMessage = null)
            try {
                val api = ApiClient.create<EventApi>()

                Log.d(TAG, "Fetching events: [${getIDString()}]")
                val res = api.getEventsByIds(getIDString())
                Log.d(TAG, "Response received, events count: ${res.total}")

                val handled = handleEventsResponse(res)

                data = data.copy(
                    isLoading = false,
                    errorMessage = null,
                    events = handled.first,
                    ids = handled.second
                )

            } catch (e: Exception) {
                val errorMsg = when (e) {
                    is HttpException -> "HTTP ${e.code()}: ${e.message()}"
                    else -> "Network Error: ${e.message}"
                }
                Log.e(TAG, "Error loading events", e)
                data = data.copy(
                    isLoading = false,
                    errorMessage = "API: $API_URL - $errorMsg"
                )
            }
        }
    }



    /**
     * Handles event response
     *
     * This will clear the list if unwanted events
     * and update the local IDs list if there are
     * old events stored but not available.
     */
    private fun handleEventsResponse(response: EventsFromID) : Pair<List<Event>, Set<String>> {
        val idsToKeep = mutableSetOf<String>()
        val result = response.events.mapNotNull {
            if(it.lifecycleState == "OPEN") {
                idsToKeep.add(it.id)
                it.toDomain()
            } else null
        }

        return Pair(
            result,
            data.ids.mapNotNull { if(idsToKeep.contains(it)) it else null }.toSet()
        )
    }



    /**
     * Reloads the events list
     *
     * This will fetch all the open events
     * and sort out events that are not
     * present in the events id list stored locally
     */
    fun reload() { getEvents() }

    /**
     * Gets IDs list as a string
     *
     * The format drops the surrounding square brackets
     * and removes all spaces.
     *
     * #### Example
     * ```kt
     * [one, two, three] // Input
     * one,two,three     // Out
     * ```
     */
    fun getIDString() : String {
        return ids
            .toString()
            .replace(" ", "")
            .drop(1)
            .dropLast(1)
    }

    /**
     * Removes event from list
     */
    fun remove(id: String) {
        data = data.copy(
            ids = data.ids.mapNotNull { if(it == id) null else it }.toSet(),
            events = data.events.mapNotNull { if(it.id == id) null else it }
        )
        storage.putJson("stored-events", data.ids.toSet())
    }



    /**
     * Static state data
     */
    companion object {
        /**
         * Debug logger tag
         */
        val TAG: String = StoredEventsListState::class.java.simpleName
    }
}



/**
 * Stored events list state
 *
 * List of events that is present in
 * a locally saved events id list.
 */
@Composable
fun rememberStoredEventsListState(storage: LocalStorage, key: Any? = Unit) : StoredEventsListState {
    return remember(key1 = key) {
        StoredEventsListState(storage)
    }
}
