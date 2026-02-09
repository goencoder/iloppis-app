package se.iloppis.app.navigation

import se.iloppis.app.domain.model.Event

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



/**
 * Screen view state page
 */
sealed class ScreenPage {
    /**
     * Home page
     */
    data object Home : ScreenPage()

    /**
     * Search page
     */
    data object Search : ScreenPage()

    /**
     * Event selection page
     */
    data class Selection(
        /**
         * On Action event
         *
         * This is called when an event has been
         * selected on the selection page.
         */
        val onAction: (event: Event) -> Unit
    ) : ScreenPage()

    /**
     * Events detail page
     *
     * Shows details about a specified event
     */
    data class EventsDetailPage(
        /**
         * Event to show details about
         */
        val event: Event
    ) : ScreenPage()

    /**
     * User local library page
     *
     * Stored events will be shown
     * on this page.
     */
    data object Library : ScreenPage()





    /**
     * Cashier selector page
     */
    data object CashierSelector : ScreenPage()

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
     * Scanner selector page
     */
    data object ScannerSelector : ScreenPage()

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
