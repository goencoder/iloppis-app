package se.iloppis.app.ui.components.navigation

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import se.iloppis.app.navigation.ScreenPage
import se.iloppis.app.ui.screens.cashier.CashierScreen
import se.iloppis.app.ui.screens.events.CodeConfirmScreen
import se.iloppis.app.ui.screens.events.CodeEntryScreen
import se.iloppis.app.ui.screens.events.EventListScreen
import se.iloppis.app.ui.screens.events.EventsDetailsScreen
import se.iloppis.app.ui.screens.scanner.ScannerScreen
import se.iloppis.app.ui.screens.screenContext
import se.iloppis.app.ui.screens.splash.SplashScreen
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
                /* Splash screen */
                is ScreenPage.Splash -> NavEntry(page) { SplashScreen() }

                /* Unified event list screen (merged Home + Search) */
                is ScreenPage.EventList -> NavEntry(page) { EventListScreen() }

                /* Event detail screen */
                is ScreenPage.EventsDetailPage -> NavEntry(page) { EventsDetailsScreen(page.event) }

                /* Code entry screen for direct tool access */
                is ScreenPage.CodeEntry -> NavEntry(page) { CodeEntryScreen(mode = page.mode, eventId = page.eventId) }

                /* Code confirmation screen after code resolves */
                is ScreenPage.CodeConfirm -> NavEntry(page) {
                    CodeConfirmScreen(
                        event = page.event,
                        apiKey = page.apiKey,
                        mode = page.mode
                    )
                }

                /* Cashier screen */
                is ScreenPage.Cashier -> NavEntry(page) {
                    CashierScreen(
                        event = page.event,
                        apiKey = page.apiKey,
                        onBack = { screen.onAction(ScreenAction.NavigateHome) }
                    )
                }

                /* Scanner screen */
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
