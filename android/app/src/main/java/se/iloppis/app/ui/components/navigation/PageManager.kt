package se.iloppis.app.ui.components.navigation

import androidx.compose.runtime.Composable
import se.iloppis.app.navigation.ScreenPage
import se.iloppis.app.ui.screens.cashier.CashierScreen
import se.iloppis.app.ui.screens.events.EventSearchScreen
import se.iloppis.app.ui.screens.events.EventSelectionScreen
import se.iloppis.app.ui.screens.events.EventsDetailsScreen
import se.iloppis.app.ui.screens.user.home.HomeScreen
import se.iloppis.app.ui.screens.scanner.ScannerScreen
import se.iloppis.app.ui.screens.screenContext
import se.iloppis.app.ui.screens.user.library.LibraryScreen
import se.iloppis.app.ui.states.ScreenAction

/**
 * Application Page Manager
 *
 * This uses the `ScreenModel` to
 * switch between application pages.
 *
 * @see se.iloppis.app.ui.screens.ScreenModel
 */
@Composable
fun PageManager() {
    val screen = screenContext()

    /* Screen overlays */
    if(screen.overlay != null) screen.overlay!!()

    /* Screen content */
    when (val page = screen.state.page) {
        /* Home screen */
        is ScreenPage.Home -> HomeScreen()

        /* Event screens */
        is ScreenPage.Search -> EventSearchScreen()
        is ScreenPage.Selection -> EventSelectionScreen(page.onAction)
        is ScreenPage.EventsDetailPage -> EventsDetailsScreen(page.event)

        /* Library screen */
        is ScreenPage.Library -> LibraryScreen()

        /* Cashier and Scanner screen */
        is ScreenPage.Cashier -> CashierScreen(
            event = page.event,
            apiKey = page.apiKey,
            onBack = { screen.onAction(ScreenAction.NavigateHome) }
        )
        is ScreenPage.Scanner -> ScannerScreen(
            event = page.event,
            apiKey = page.apiKey,
            onBack = { screen.onAction(ScreenAction.NavigateHome) }
        )
    }
}
