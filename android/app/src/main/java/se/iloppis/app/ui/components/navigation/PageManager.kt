package se.iloppis.app.ui.components.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import se.iloppis.app.navigation.ScreenPage
import se.iloppis.app.ui.screens.cashier.CashierSelectionScreen
import se.iloppis.app.ui.screens.events.EventListScreen
import se.iloppis.app.ui.screens.events.EventListViewModel
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

    val viewModel: EventListViewModel = viewModel()

    when (screen.state.page) {
        is ScreenPage.Home -> EventListScreen(viewModel)
        is ScreenPage.Cashier -> CashierSelectionScreen(viewModel)
        else -> {}
    }
}
