package se.iloppis.app.navigation

import se.iloppis.app.domain.model.CodeEntryMode
import se.iloppis.app.domain.model.Event

/** Complete navigation state carried by the app's in-memory page stack. */
sealed class ScreenPage {
    /** Initial branding screen. */
    data object Splash : ScreenPage()

    /** Searchable event list and entry point to organizer tools. */
    data object EventList : ScreenPage()

    /** Details and organizer-tool entry points for [event]. */
    data class EventsDetailPage(
        val event: Event
    ) : ScreenPage()

    /**
     * Code entry for direct organizer-tool access.
     *
     * @param mode tool requested by the user.
     * @param eventId limits saved codes to one event; `null` allows all events.
     */
    data class CodeEntry(
        val mode: CodeEntryMode,
        val eventId: String? = null
    ) : ScreenPage()

    /** Confirmation shown after an alias resolves but before the tool opens. */
    data class CodeConfirm(
        val event: Event,
        /** Secret credential resolved from [alias]; never display or persist it. */
        val apiKey: String,
        val alias: String,
        /** Entry context used when returning from confirmation. */
        val entryMode: CodeEntryMode,
        /** Tool authorized by the resolved credential. */
        val mode: CodeEntryMode
    ) : ScreenPage()

    /** Active cashier for [event], authorized by [apiKey]. */
    data class Cashier(
        val event: Event,
        val apiKey: String
    ) : ScreenPage()

    /** Active entrance scanner for [event], authorized by [apiKey]. */
    data class Scanner(
        val event: Event,
        val apiKey: String
    ) : ScreenPage()

    /** Live event statistics authorized by [apiKey]. */
    data class LiveStats(
        val event: Event,
        val apiKey: String
    ) : ScreenPage()
}
