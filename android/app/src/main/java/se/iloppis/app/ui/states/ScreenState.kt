package se.iloppis.app.ui.states

import se.iloppis.app.navigation.ScreenPage

/**
 * Screen view action
 */
sealed class ScreenAction {
    /**
     * Screen load page action
     */
    data class NavigateToPage(
        val page: ScreenPage
    ) : ScreenAction()

    /**
     * Navigates home
     */
    data object NavigateHome : ScreenAction()
}
