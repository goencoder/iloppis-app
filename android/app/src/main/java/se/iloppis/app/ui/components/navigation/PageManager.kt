package se.iloppis.app.ui.components.navigation

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
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

    NavDisplay(
        backStack = screen.pages,
        onBack = { screen.popPage() },
        transitionSpec = animateSlideOut(),
        popTransitionSpec = animateSlideOut(),
        predictivePopTransitionSpec = animatePredictiveSlideOut(),
        entryProvider = { page ->
            when (page) {
                /* Home screen */
                is ScreenPage.Home -> NavEntry(page) { HomeScreen() }

                /* Event screens */
                is ScreenPage.Search -> NavEntry(page) { EventSearchScreen() }
                is ScreenPage.Selection -> NavEntry(page) { EventSelectionScreen(page.onAction) }
                is ScreenPage.EventsDetailPage -> NavEntry(page) { EventsDetailsScreen(page.event) }

                /* Library screen */
                is ScreenPage.Library -> NavEntry(page) { LibraryScreen() }

                /* Cashier and Scanner screen */
                is ScreenPage.Cashier -> NavEntry(page) {
                    CashierScreen(
                        event = page.event,
                        apiKey = page.apiKey,
                        onBack = { screen.onAction(ScreenAction.NavigateHome) }
                    )
                }
                is ScreenPage.Scanner -> NavEntry(page) {
                    ScannerScreen(
                        event = page.event,
                        apiKey = page.apiKey,
                        onBack = { screen.onAction(ScreenAction.NavigateHome) }
                    )
                }
            }
        }
    )
}
