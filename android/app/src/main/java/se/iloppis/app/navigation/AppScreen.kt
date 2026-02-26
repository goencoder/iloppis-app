package se.iloppis.app.navigation

import se.iloppis.app.domain.model.Event

/**
 * Represents the current navigation destination in the app.
 */
@Deprecated("use new screen model system")
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



/**
 * Screen view state page - unified navigation structure
 * 
 * Navigation flow:
 * - EventList: Primary screen showing events with search/filters and tool entry buttons
 * - EventsDetailPage: Event details with full information and tool options
 * - CodeEntry: Direct code input for Cashier/Scanner (no event selection needed)
 * - CodeConfirm: Show resolved event before entering tool
 * - Cashier/Scanner: Active tool screens
 */
sealed class ScreenPage {
    /**
     * Splash screen - shown on app launch for brand moment
     */
    data object Splash : ScreenPage()

    /**
     * Unified event list screen (merged Home + Search)
     * 
     * Shows:
     * - Event search and filters
     * - List of events
     * - Quick access buttons for Cashier/Scanner entry
     */
    data object EventList : ScreenPage()

    /**
     * Events detail page
     *
     * Shows details about a specified event and provides
     * access to Cashier/Scanner tools
     */
    data class EventsDetailPage(
        /**
         * Event to show details about
         */
        val event: Event
    ) : ScreenPage()

    /**
     * Code entry screen for direct tool access
     *
     * Shows code input field with mode (Cashier or Scanner).
     * No event selection needed - code resolves the event.
     */
    data class CodeEntry(
        /**
         * Tool mode (CASHIER or SCANNER)
         */
        val mode: String
    ) : ScreenPage()

    /**
     * Code confirmation screen
     *
     * After code is validated, show which event it belongs to
     * and ask user to confirm before entering the tool.
     */
    data class CodeConfirm(
        /**
         * Event that the code belongs to
         */
        val event: Event,

        /**
         * API key for the tool
         */
        val apiKey: String,

        /**
         * Tool mode (CASHIER or SCANNER)
         */
        val mode: String
    ) : ScreenPage()

    /**
     * Cashier page
     */
    data class Cashier(
        /**
         * Event that owns this cashier
         */
        val event: Event,

        /**
         * API key
         */
        val apiKey: String
    ) : ScreenPage()

    /**
     * Scanner page
     */
    data class Scanner(
        /**
         * Event that owns this scanner
         */
        val event: Event,

        /**
         * API key
         */
        val apiKey: String
    ) : ScreenPage()
}
