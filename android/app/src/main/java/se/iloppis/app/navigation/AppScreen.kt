package se.iloppis.app.navigation

import se.iloppis.app.domain.model.CodeEntryMode
import se.iloppis.app.domain.model.Event

/**
 * Screen view state page - unified navigation structure
 *
 * Navigation flow:
 * - EventList: Primary screen showing events with search/filters and tool entry buttons
 * - EventsDetailPage: Event details with full information and tool options
 * - CodeEntry: Direct code input for Cashier/Scanner (no event selection needed)
 * - CodeConfirm: Show resolved event before entering tool
 * - Cashier/Scanner/LiveStats: Active tool screens
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
     *
     * @param mode Tool mode (CASHIER or SCANNER)
     * @param eventId Optional event ID filter. When non-null, only saved codes
     *   for this event are shown (navigating from event detail).
     *   When null, all saved codes are shown (navigating from main page).
     */
    data class CodeEntry(
        val mode: CodeEntryMode,
        val eventId: String? = null
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
         * Alias used to resolve the tool.
         */
        val alias: String,

        /**
         * Mode used when the user entered the code.
         */
        val entryMode: CodeEntryMode,

        /**
         * Tool mode (CASHIER, SCANNER, or LIVE_STATS)
         */
        val mode: CodeEntryMode
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
        val apiKey: String,

        /**
         * Cashier display name (alias)
         */
        val alias: String? = null
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

    /**
     * Live stats page
     */
    data class LiveStats(
        /**
         * Event that owns this live stats view
         */
        val event: Event,

        /**
         * API key used to fetch live stats directly from backend
         */
        val apiKey: String
    ) : ScreenPage()
}
