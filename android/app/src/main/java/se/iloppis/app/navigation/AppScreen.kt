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
}
