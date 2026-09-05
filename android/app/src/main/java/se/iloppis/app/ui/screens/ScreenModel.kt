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

/** Owns the in-memory navigation stack and screen-level presentation state. */
class ScreenModel : ViewModel() {
    /** Current screen-level presentation state. */
    var state by mutableStateOf(ScreenState())
        private set

    /** Overlay rendered above the current page, or `null`. */
    var overlay by mutableStateOf<(@Composable () -> Unit)?>(null)
        private set

    /** Navigation stack, rooted at [ScreenPage.Splash] during startup. */
    var pages = mutableStateListOf<ScreenPage>(ScreenPage.Splash)
        private set

    /** Current page, or `null` if the stack was emptied. */
    val page by derivedStateOf { pages.lastOrNull() }

    /** Page immediately below [page], or `null`. */
    val previous by derivedStateOf { pages.getOrNull(pages.size - 2) }

    /** Padding applied around the active screen. */
    val border by derivedStateOf { state.borders }

    /** Applies a navigation or presentation [action]. */
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
    private fun navigateHome() { navigateToPage(ScreenPage.EventList, true) }

    private fun setBorders(values: PaddingValues) { state = state.copy(borders = values) }

    private fun setScreenOverlay(overlay: (@Composable () -> Unit)?) { this.overlay = overlay }

    /** Removes and returns the current page, or `null` when the stack is empty. */
    fun popPage() : ScreenPage? = pages.removeLastOrNull()

    private fun pushPage(page: ScreenPage) {
        when(page) {
            is ScreenPage.Splash -> {
                pages.clear()
                pages.add(page)
            }
            is ScreenPage.EventList -> {
                pages.clear()
                pages.add(page)
            }
            is ScreenPage.EventsDetailPage -> {
                if(pages.lastOrNull() !is ScreenPage.EventList) {
                    pages.clear()
                    pages.add(ScreenPage.EventList)
                }
                pages.add(page)
            }
            is ScreenPage.CodeEntry -> {
                if(pages.lastOrNull() !is ScreenPage.EventList) {
                    pages.clear()
                    pages.add(ScreenPage.EventList)
                }
                pages.add(page)
            }
            is ScreenPage.CodeConfirm -> {
                if(pages.lastOrNull() !is ScreenPage.CodeEntry) {
                    pages.clear()
                    pages.add(ScreenPage.EventList)
                    pages.add(ScreenPage.CodeEntry(page.entryMode))
                }
                pages.add(page)
            }
            is ScreenPage.Cashier, is ScreenPage.Scanner, is ScreenPage.LiveStats -> {
                if(pages.lastOrNull() !is ScreenPage.CodeConfirm && pages.lastOrNull() !is ScreenPage.EventsDetailPage) {
                    pages.clear()
                    pages.add(ScreenPage.EventList)
                }
                pages.add(page)
            }
        }
    }
}



private val localScreenModel = compositionLocalOf<ScreenModel> {
    error("No screen view model provider present in this context")
}



/** Provides [screen] to [content] through the local Compose context. */
@Composable
fun ScreenModelProvider(screen: ScreenModel = viewModel(), content: @Composable () -> Unit) {
    val view = remember { screen }
    CompositionLocalProvider(localScreenModel provides view, content)
}



/** Returns the [ScreenModel] supplied by the nearest [ScreenModelProvider]. */
@Composable
fun screenContext(): ScreenModel {
    return localScreenModel.current
}
