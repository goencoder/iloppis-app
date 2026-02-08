package se.iloppis.app.utils.events.state

/**
 * List sort type
 */
enum class EventListSortType {
    /**
     * Sort after saved events
     *
     * This will only display events
     * that are saved locally on this device.
     */
    SAVED,

    /**
     * No sorting method applies
     *
     * This will show all events and may take
     * unneeded time to finnish. Consider using
     * a sorting method when getting the list.
     */
    ALL,
}
