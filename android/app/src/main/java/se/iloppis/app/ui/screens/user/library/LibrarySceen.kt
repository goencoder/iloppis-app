package se.iloppis.app.ui.screens.user.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import se.iloppis.app.domain.model.Event
import se.iloppis.app.navigation.ScreenPage
import se.iloppis.app.ui.components.events.SwipeableEventList
import se.iloppis.app.ui.screens.events.EmptyState
import se.iloppis.app.ui.screens.events.ErrorState
import se.iloppis.app.ui.screens.events.ILoppisHeader
import se.iloppis.app.ui.screens.events.LoadingState
import se.iloppis.app.ui.screens.screenContext
import se.iloppis.app.ui.states.ScreenAction
import se.iloppis.app.utils.events.localEventsStorage
import se.iloppis.app.utils.events.state.EventListSortType
import se.iloppis.app.utils.events.state.EventListState
import se.iloppis.app.utils.events.state.EventListStateAction
import se.iloppis.app.utils.events.state.rememberEventListState

@Composable
fun LibraryScreen() {
    val screen = screenContext()
    val state = rememberEventListState(sort = EventListSortType.SAVED)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .statusBarsPadding()
    ) {
        ILoppisHeader()
        ListContent(state) {
            screen.onAction(ScreenAction.NavigateToPage(
                ScreenPage.EventsDetailPage(it)
            ))
        }
    }
}


/**
 * List content
 */
@Composable
private fun ListContent(
    state: EventListState,
    onAction: (Event) -> Unit
) {
    val storage = localEventsStorage()
    PullToRefreshBox(
        isRefreshing = false,
        onRefresh = { state.onAction(EventListStateAction.Reload) }
    ) {
        when {
            state.isLoading -> LoadingState()
            state.errorMessage != null -> ErrorState(state.errorMessage!!)
            state.events.isEmpty() -> EmptyState()
            else -> SwipeableEventList(
                state.events,
                onEndToStart = {
                    storage.remove(it.id)
                    state.events.remove(it)
                },
                onAction = onAction
            )
        }
    }
}
