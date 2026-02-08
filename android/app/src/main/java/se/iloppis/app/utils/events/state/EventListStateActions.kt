package se.iloppis.app.utils.events.state

/**
 * Event list state action
 *
 * Controls specified [EventListState]
 */
sealed class EventListStateAction {
    /**
     * Sets sorting method for the [EventListState]
     */
    data class SetSortingMethod(
        /**
         * Sort method to use
         *
         * Default is [EventListSortType.SAVED]
         *
         * @see EventListState.sort
         * @see EventListState.events
         * @see EventListSortType.SAVED
         * @see EventListSortType
         */
        val sort: EventListSortType = EventListSortType.SAVED
    ) : EventListStateAction()
}
