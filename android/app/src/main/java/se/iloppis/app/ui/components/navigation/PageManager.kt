package se.iloppis.app.ui.components.navigation

import androidx.compose.runtime.Composable
import se.iloppis.app.navigation.ScreenPage
import se.iloppis.app.ui.screens.cashier.CashierSelectionScreen
import se.iloppis.app.ui.screens.events.EventDialogs
import se.iloppis.app.ui.screens.events.EventListScreen
import se.iloppis.app.ui.screens.events.eventContext
import se.iloppis.app.ui.screens.screenContext

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
    val event = eventContext()

    /* Page content helper */
    EventDialogs()

    /* Main page content */
    when (screen.state.page) {
        is ScreenPage.Home -> EventListScreen()
        is ScreenPage.Cashier -> CashierSelectionScreen()
        is ScreenPage.Scanner -> { /* Scanner page */ }
    }
}
