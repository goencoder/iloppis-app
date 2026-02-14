package se.iloppis.app.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import se.iloppis.app.ui.states.ScreenAction
import se.iloppis.app.navigation.ScreenPage
import se.iloppis.app.ui.states.ScreenState

/**
 * Screen view model
 */
class ScreenModel : ViewModel() {
    /**
     * Screen model state
     */
    var state by mutableStateOf(ScreenState())
        private set

    /**
     * Screen overlay
     */
    var overlay by mutableStateOf<(@Composable () -> Unit)?>(null)
        private set

    /**
     * Previous screen page
     */
    var pages = mutableStateListOf<ScreenPage>(ScreenPage.Home)
        private set

    /**
     * Current screen page
     *
     * The page that is currently loaded
     * and viewed in the application.
     */
    val page by derivedStateOf { pages.lastOrNull() }

    /**
     * Previous page
     */
    val previous by derivedStateOf { pages.getOrNull(pages.size - 2) }

    /**
     * Screen border
     *
     * Provides values for the screens border
     * such as the [se.iloppis.app.ui.components.navigation.Navigator]
     * screen borders.
     *
     * @see ScreenState.borders
     */
    val border by derivedStateOf { state.borders }



    /**
     * Sends action to screen view model
     */
    fun onAction(action: ScreenAction) {
        when(action) {
            is ScreenAction.Loading -> setLoad(action.status)

            is ScreenAction.NavigateToPage -> navigateToPage(action.page, action.navigator)
            is ScreenAction.ShowNavigator -> showNavigator(action.show)
            is ScreenAction.NavigateHome -> navigateHome()

            is ScreenAction.SetBorders -> setBorders(action.borders)

            is ScreenAction.SetOverlay -> setScreenOverlay(action.overlay)
            is ScreenAction.RemoveOverlay -> setScreenOverlay(null)
        }
    }



    private fun setLoad(state: Boolean) { this.state = this.state.copy(isLoading = state) }
    private fun showNavigator(state: Boolean) { this.state = this.state.copy(showNavigator = state) }

    private fun navigateToPage(page: ScreenPage, navigator: Boolean) {
        pushPage(page)
        showNavigator(navigator)
    }
    private fun navigateHome() { navigateToPage(ScreenPage.Home, true) }

    private fun setBorders(values: PaddingValues) { state = state.copy(borders = values) }

    private fun setScreenOverlay(overlay: (@Composable () -> Unit)?) { this.overlay = overlay }



    /**
     * Pops previous page from the navigation queue
     */
    fun popPage() : ScreenPage? = pages.removeLastOrNull()

    private fun pushPage(page: ScreenPage) {
        /*
            Needs to have a better way of navigation
            than being hard coded if there are more
            pages to be added.
        */

        val previous = popPage()
        pages.clear()

        if(
            page is ScreenPage.Search ||
            page is ScreenPage.Library ||
            page is ScreenPage.Selection ||
            page is ScreenPage.Cashier ||
            page is ScreenPage.Scanner
        ) {
            pages.add(ScreenPage.Home)
        } else if(page is ScreenPage.EventsDetailPage) {
            pages.add(ScreenPage.Home)
            pages.add(previous ?: ScreenPage.Search)
        }

        pages.add(page)
    }
}



/**
 * Local screen model context
 */
private val localScreenModel = compositionLocalOf<ScreenModel> {
    error("No screen view model provider present in this context")
}



/**
 * Screen view model provider
 *
 * Provides a screen view model for the context
 * where this provider is present
 */
@Composable
fun ScreenModelProvider(screen: ScreenModel = viewModel(), content: @Composable () -> Unit) {
    val view = remember { screen }
    CompositionLocalProvider(localScreenModel provides view, content)
}



/**
 * Gets local screen view model context
 */
@Composable
fun screenContext(): ScreenModel {
    return localScreenModel.current
}
