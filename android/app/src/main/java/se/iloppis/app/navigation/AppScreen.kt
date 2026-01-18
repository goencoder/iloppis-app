package se.iloppis.app.navigation

import se.iloppis.app.domain.model.Event
import se.iloppis.app.ui.screens.events.CodeEntryMode

/**
 * Represents the current navigation destination in the app.
 */
sealed class AppScreen {
    /** Event list / home screen */
    data object EventList : AppScreen()
    
    /** Cashier mode screen */
    data class Cashier(
        val event: Event,
        val apiKey: String
    ) : AppScreen()
    
    /** Scanner mode screen */
    data class Scanner(
        val event: Event,
        val apiKey: String
    ) : AppScreen()
}
