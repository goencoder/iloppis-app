package se.iloppis.app.ui.states

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import se.iloppis.app.navigation.ScreenPage

/**
 * Screen view action
 */
sealed class ScreenAction {
    /**
     * Screen loading action
     */
    data class Loading(
        /**
         * Loading status
         */
        val status: Boolean
    ) : ScreenAction()

    /**
     * Screen load page action
     */
    data class NavigateToPage(
        /**
         * Page to load
         */
        val page: ScreenPage,
        /**
         * Show navigator
         */
        val navigator: Boolean = true
    ) : ScreenAction()

    /**
     * Show navigator action
     */
    data class ShowNavigator(
        /**
         * Show status
         */
        val show: Boolean
    ) : ScreenAction()

    /**
     * Sets screen border values
     */
    data class SetBorders(
        /**
         * Sets screen border values
         */
        val borders: PaddingValues
    ) : ScreenAction()

    /**
     * Sets screen overlay
     */
    data class SetOverlay(
        /**
         * Screen overlay
         */
        val overlay: @Composable () -> Unit
    ) : ScreenAction()

    /**
     * Removes screen overlay
     */
    data object RemoveOverlay : ScreenAction()

    /**
     * Navigates home and enables the navigator if disabled
     */
    data object NavigateHome : ScreenAction()
}



/**
 * Screen view state
 */
data class ScreenState(
    /**
     * State loading status
     *
     * This is true if the
     * state is loading data
     */
    val isLoading: Boolean = true,

    /**
     * Show app navigator status
     */
    val showNavigator: Boolean = true,

    /**
     * State error message
     *
     * Contains an error message
     * if an error has occurred
     */
    val errorMessage: String? = null,

    /**
     * Screen borders
     *
     * This provides values for different
     * screen borders such as the [se.iloppis.app.ui.components.navigation.Navigator]
     */
    val borders: PaddingValues = PaddingValues(),
)
