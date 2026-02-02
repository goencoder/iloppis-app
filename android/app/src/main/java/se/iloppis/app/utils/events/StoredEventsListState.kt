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
import se.iloppis.app.network.config.ClientConfig
import se.iloppis.app.network.config.clientConfig
import se.iloppis.app.network.events.ApiEventListResponse
import se.iloppis.app.network.events.EventAPI
import se.iloppis.app.network.events.EventLifecycle
import se.iloppis.app.network.events.convertCollection
import se.iloppis.app.network.ILoppisClient
import se.iloppis.app.utils.storage.LocalStorage
import se.iloppis.app.utils.storage.localStorage

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
class StoredEventsListState(val config: ClientConfig, val storage: LocalStorage) {
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

        Log.d(TAG, "Loading events from: ${clientConfig().url}")
        CoroutineScope(Dispatchers.Main).launch {
            data = data.copy(isLoading = true, errorMessage = null)
            try {
                val api = ILoppisClient(config).create<EventAPI>()

                Log.d(TAG, "Fetching events: [${EventAPI.convertCollection(ids)}]")
                val res = api.get(EventAPI.convertCollection(ids))
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
                    errorMessage = "API: ${clientConfig().url} - $errorMsg"
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
    private fun handleEventsResponse(response: ApiEventListResponse) : Pair<List<Event>, Set<String>> {
        val idsToKeep = mutableSetOf<String>()
        val result = response.events.mapNotNull {
            if(it.lifecycleState == EventLifecycle.OPEN) {
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
fun rememberStoredEventsListState(
    config: ClientConfig = clientConfig(),
    storage: LocalStorage = localStorage(),
    key: Any? = Unit
) : StoredEventsListState {
    return remember(key1 = key) {
        StoredEventsListState(config, storage)
    }
}
