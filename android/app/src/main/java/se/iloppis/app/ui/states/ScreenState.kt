package se.iloppis.app.ui.states

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import se.iloppis.app.navigation.ScreenPage

/**
 * Screen UI state.
 */
data class ScreenState(
    val isLoading: Boolean = false,
    val showNavigator: Boolean = true,
    val borders: PaddingValues = PaddingValues()
)

/**
 * Screen view action
 */
sealed class ScreenAction {
    data class Loading(val status: Boolean) : ScreenAction()

    data class NavigateToPage(
        val page: ScreenPage,
        val navigator: Boolean = true
    ) : ScreenAction()

    data class ShowNavigator(val show: Boolean) : ScreenAction()
    data object NavigateHome : ScreenAction()

    data class SetBorders(val borders: PaddingValues) : ScreenAction()

    data class SetOverlay(val overlay: @Composable () -> Unit) : ScreenAction()
    data object RemoveOverlay : ScreenAction()
}
