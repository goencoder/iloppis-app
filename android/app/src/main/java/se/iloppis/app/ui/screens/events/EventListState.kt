package se.iloppis.app.ui.screens.events

import se.iloppis.app.domain.model.Event

/**
 * Filter chip options for the event list.
 */
enum class EventFilterChip {
    ALL,
    UPCOMING,
    ONGOING,
    PAST,
}

/**
 * UI state for the event list screen.
 */
data class EventListUiState(
    /** Current list of events to display */
    val events: List<Event> = emptyList(),
    /** Whether the list is loading */
    val isLoading: Boolean = true,
    /** Error message if loading failed */
    val errorMessage: String? = null,
    /** Current search query text */
    val searchQuery: String = "",
    /** Currently active filter chip */
    val activeFilter: EventFilterChip = EventFilterChip.ALL,
)

/**
 * Actions that can be performed on the event list screen.
 */
sealed class EventListAction {
    /** Load/reload events with current filter */
    data object LoadEvents : EventListAction()
    /** Update search query text */
    data class UpdateSearch(val query: String) : EventListAction()
    /** Change active filter chip */
    data class SelectFilter(val filter: EventFilterChip) : EventListAction()
}
