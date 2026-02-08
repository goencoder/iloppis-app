package se.iloppis.app.utils.events.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import se.iloppis.app.domain.model.Event
import se.iloppis.app.utils.events.LocalEventsListStorage
import se.iloppis.app.utils.events.localEventsStorage

/**
 * Event list state
 *
 * The event list uses both locally stored events
 * and events from the server.
 *
 * @see LocalEventsListStorage
 */
class EventListState(val storage: LocalEventsListStorage, val scope: CoroutineScope, sort: EventListSortType) {
    /**
     * Event list sorting method
     *
     * The way the events are stored
     * and sorted.
     *
     * @see se.iloppis.app.utils.events.state.EventListState.events
     */
    var sort by mutableStateOf(sort)
        internal set

    /**
     * List of events
     *
     * This list will contain events
     * sorted after the [sort] type.
     *
     * @see se.iloppis.app.utils.events.state.EventListState.sort
     */
    var events = mutableStateListOf<Event>()
        internal set

    /**
     * Is event list loading status
     */
    var isLoading by mutableStateOf(false)
        internal set

    /**
     * Event list state error message
     *
     * If there is no error then this will
     * be null
     */
    var errorMessage by mutableStateOf<String?>(null)
        internal set



    init {
        loadEvents()
    }



    /**
     * Handles state actions
     *
     * Using [EventListStateAction] to control
     * the behavior of this state.
     *
     * @see EventListStateAction
     */
    fun onAction(action: EventListStateAction) {
        when(action) {
            is EventListStateAction.SetSortingMethod -> setSortingMethod(action.sort)

            is EventListStateAction.Reload -> reload()
        }
    }



    /**
     * Loads event
     *
     * The way this will load events depends on the
     * [sort] method specified.
     *
     * @see se.iloppis.app.utils.events.state.EventListState.sort
     * @see EventListStateAction.SetSortingMethod
     */
    private fun loadEvents() {
        events.clear() /* Clears all prior events */
        errorMessage = null /* Removes errors */
        isLoading = true /* Sets loading by default for all loading types */

        scope.launch {
            when (sort) {
                EventListSortType.SAVED -> loadLocalEvents()
                EventListSortType.ALL -> {}
            }
        }
    }



    private fun setSortingMethod(sort: EventListSortType) {
        this.sort = sort
        loadEvents()
    }
    private fun reload() = loadEvents()



    /**
     * Static state data
     */
    companion object {
        /**
         * Debug logger tag
         */
        val TAG: String = EventListState::class.java.simpleName
    }
}



/**
 * Creates and [remember] an [EventListState]
 */
@Composable
fun rememberEventListState(
    storage: LocalEventsListStorage = localEventsStorage(),
    scope: CoroutineScope = rememberCoroutineScope(),
    sort: EventListSortType = if(storage.empty()) EventListSortType.ALL else EventListSortType.SAVED
) : EventListState {
    return remember {
        EventListState(storage, scope, sort)
    }
}
