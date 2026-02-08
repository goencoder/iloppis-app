package se.iloppis.app.utils.events.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
class EventListState(val storage: LocalEventsListStorage, sort: EventListSortType) {
    /**
     * Event list sorting method
     *
     * The way the events are stored
     * and sorted.
     *
     * @see se.iloppis.app.utils.events.state.EventListState.events
     */
    var sort by mutableStateOf(sort)
        private set

    /**
     * List of events
     *
     * This list will contain events
     * sorted after the [sort] type.
     *
     * @see se.iloppis.app.utils.events.state.EventListState.sort
     */
    var events = mutableStateListOf<Event>()
        private set



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
        }
    }



    private fun setSortingMethod(sort: EventListSortType) {
        this.sort = sort
        /* Reload the list */
    }
}



/**
 * Creates and [remember] an [EventListState]
 */
@Composable
fun rememberEventListState(
    storage: LocalEventsListStorage = localEventsStorage(),
    sort: EventListSortType = EventListSortType.SAVED
) : EventListState {
    return remember {
        EventListState(storage, sort)
    }
}
