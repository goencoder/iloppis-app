package se.iloppis.app.utils.events.state

/**
 * Loads events from local storage
 */
internal suspend fun EventListState.loadLocalEvents() {
    isLoading = false
}
