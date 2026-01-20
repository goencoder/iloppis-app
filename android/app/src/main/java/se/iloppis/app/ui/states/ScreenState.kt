package se.iloppis.app.ui.states

/**
 * Screen view state page
 */
sealed class ScreenPage {
    /**
     * Home page
     */
    data object Home : ScreenPage()

    /**
     * Cashier page
     */
    data object Cashier : ScreenPage()

    /**
     * Scanner page
     */
    data object Scanner : ScreenPage()
}



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
    data class LoadPage(
        /**
         * Page to load
         */
        val page: ScreenPage
    ) : ScreenAction()
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
     * State error message
     *
     * Contains an error message
     * if an error has occurred
     */
    val errorMessage: String? = null,

    /**
     * Screen state page
     */
    val page: ScreenPage = ScreenPage.Home,
)
