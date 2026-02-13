package se.iloppis.app.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
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
    var previous by mutableStateOf<ScreenPage?>(null)
        private set

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

            is ScreenAction.NavigateToPage -> navigateToPage(action.page, action.navigator, if(state.page != ScreenPage.Home) state.page else null)
            is ScreenAction.ShowNavigator -> showNavigator(action.show)
            is ScreenAction.NavigateHome -> navigateHome()
            is ScreenAction.NavigateBack -> navigateBack()

            is ScreenAction.SetBorders -> setBorders(action.borders)

            is ScreenAction.SetOverlay -> setScreenOverlay(action.overlay)
            is ScreenAction.RemoveOverlay -> setScreenOverlay(null)
        }
    }



    private fun setLoad(state: Boolean) { this.state = this.state.copy(isLoading = state) }
    private fun navigateToPage(page: ScreenPage, navigator: Boolean, previous: ScreenPage?) {
        this.previous = previous
        state = state.copy(page = page)
        showNavigator(navigator)
    }
    private fun showNavigator(state: Boolean) { this.state = this.state.copy(showNavigator = state) }
    private fun navigateHome() { navigateToPage(ScreenPage.Home, true, null) }
    private fun navigateBack() {
        navigateToPage(
            previous ?: ScreenPage.Home,
            true,
            if(previous != null && previous != ScreenPage.Home) ScreenPage.Home else null
        )
    }
    private fun setBorders(values: PaddingValues) { state = state.copy(borders = values) }
    private fun setScreenOverlay(overlay: (@Composable () -> Unit)?) { this.overlay = overlay }
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
