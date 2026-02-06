package se.iloppis.app.ui.components.navigation

import androidx.compose.runtime.Composable
import se.iloppis.app.navigation.ScreenPage
import se.iloppis.app.ui.screens.cashier.CashierScreen
import se.iloppis.app.ui.screens.cashier.CashierSelectionScreen
import se.iloppis.app.ui.screens.events.EventDialogs
import se.iloppis.app.ui.screens.events.EventSearchScreen
import se.iloppis.app.ui.screens.home.HomeScreen
import se.iloppis.app.ui.screens.scanner.ScannerScreen
import se.iloppis.app.ui.screens.scanner.ScannerSelectionScreen
import se.iloppis.app.ui.screens.screenContext
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

    /* Page content helper */
    EventDialogs()

    /* Main page content */
    when (val page = screen.state.page) {
        is ScreenPage.Home -> HomeScreen()
        is ScreenPage.Search -> EventSearchScreen()



        is ScreenPage.CashierSelector -> CashierSelectionScreen()
        is ScreenPage.Cashier -> CashierScreen(
            event = page.event,
            apiKey = page.apiKey,
            onBack = { screen.onAction(ScreenAction.NavigateToPage(ScreenPage.CashierSelector)) }
        )

        is ScreenPage.ScannerSelector -> ScannerSelectionScreen()
        is ScreenPage.Scanner -> ScannerScreen(
            event = page.event,
            apiKey = page.apiKey,
            onBack = { screen.onAction(ScreenAction.NavigateToPage(ScreenPage.ScannerSelector)) }
        )
    }
}
