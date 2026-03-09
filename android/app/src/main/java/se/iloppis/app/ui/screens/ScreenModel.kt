package se.iloppis.app.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import se.iloppis.app.navigation.ScreenPage
import se.iloppis.app.ui.states.ScreenAction

/**
 * Screen view model
 */
class ScreenModel : ViewModel() {
    /**
     * Previous screen page
     */
    var pages = mutableStateListOf<ScreenPage>(ScreenPage.Splash)
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
     * Sends action to screen view model
     */
    fun onAction(action: ScreenAction) {
        when(action) {
            is ScreenAction.NavigateToPage -> pushPage(action.page)
            is ScreenAction.NavigateHome -> pushPage(ScreenPage.EventList)
        }
    }



    /**
     * Pops previous page from the navigation queue
     */
    fun popPage() : ScreenPage? = pages.removeLastOrNull()

    private fun pushPage(page: ScreenPage) {
        /*
            Navigation hierarchy:
            - EventList is root screen
            - EventsDetailPage: EventList > EventsDetailPage
            - CodeEntry & CodeConfirm: EventList > CodeEntry > CodeConfirm
            - Cashier/Scanner: Can be reached from CodeConfirm or EventsDetailPage
        */

        when(page) {
            is ScreenPage.Splash -> {
                // Splash replaces everything
                pages.clear()
                pages.add(page)
            }
            is ScreenPage.EventList -> {
                // Reset to root
                pages.clear()
                pages.add(page)
            }
            is ScreenPage.EventsDetailPage -> {
                // Keep: EventList > EventsDetailPage
                if(pages.lastOrNull() !is ScreenPage.EventList) {
                    pages.clear()
                    pages.add(ScreenPage.EventList)
                }
                pages.add(page)
            }
            is ScreenPage.CodeEntry -> {
                // Keep: EventList > CodeEntry
                // or:   EventList > EventsDetailPage > CodeEntry
                if(pages.lastOrNull() !is ScreenPage.EventList && pages.lastOrNull() !is ScreenPage.EventsDetailPage) {
                    pages.clear()
                    pages.add(ScreenPage.EventList)
                }
                pages.add(page)
            }
            is ScreenPage.CodeConfirm -> {
                // Keep: EventList > CodeEntry > CodeConfirm
                if(pages.lastOrNull() !is ScreenPage.CodeEntry) {
                    pages.clear()
                    pages.add(ScreenPage.EventList)
                    pages.add(ScreenPage.CodeEntry(page.mode))
                }
                pages.add(page)
            }
            is ScreenPage.Cashier, is ScreenPage.Scanner -> {
                // Navigate from CodeConfirm or EventsDetailPage
                // Keep existing path and add tool page
                if(pages.lastOrNull() !is ScreenPage.CodeConfirm && pages.lastOrNull() !is ScreenPage.EventsDetailPage) {
                    pages.clear()
                    pages.add(ScreenPage.EventList)
                }
                pages.add(page)
            }
        }
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
