package se.iloppis.app.ui.components.navigation

import androidx.compose.runtime.Composable
import se.iloppis.app.ui.screens.events.EventListScreen
import se.iloppis.app.ui.screens.screenContext
import se.iloppis.app.ui.states.ScreenPage

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
    when (screen.state.page) {
        is ScreenPage.Home -> EventListScreen()
        else -> {}
    }
}
