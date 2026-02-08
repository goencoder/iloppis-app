package se.iloppis.app.utils.events.state

/**
 * Loads events from local storage
 */
internal suspend fun EventListState.loadLocalEvents() {
    if(storage.empty()) {
        isLoading = false
        return
    }
}
