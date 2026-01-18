package se.iloppis.app.ui.screens.events

import se.iloppis.app.domain.model.Event
import se.iloppis.app.navigation.AppScreen

/**
 * UI state for the event list screen.
 */
data class EventListUiState(
    val events: List<Event> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val selectedEvent: Event? = null,
    val codeEntryState: CodeEntryState? = null,
    val currentScreen: AppScreen = AppScreen.EventList
)

/**
 * State for code entry dialog.
 */
data class CodeEntryState(
    val mode: CodeEntryMode,
    val event: Event,
    val isValidating: Boolean = false,
    val errorMessage: String? = null
)

/**
 * Code entry mode - cashier or scanner.
 */
enum class CodeEntryMode {
    CASHIER,
    SCANNER
}

/**
 * Actions that can be performed on the event list screen.
 */
sealed class EventListAction {
    data object LoadEvents : EventListAction()
    data class SelectEvent(val event: Event) : EventListAction()
    data object DismissEventDetail : EventListAction()
    data class StartCodeEntry(val mode: CodeEntryMode, val event: Event) : EventListAction()
    data object DismissCodeEntry : EventListAction()
    data class SubmitCode(val code: String) : EventListAction()
    data class ValidateCode(val code: String) : EventListAction()
    data object NavigateBack : EventListAction()
}
