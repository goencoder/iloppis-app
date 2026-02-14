package se.iloppis.app.ui.screens.events

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import se.iloppis.app.R
import se.iloppis.app.domain.model.Event
import se.iloppis.app.ui.components.events.EventList
import se.iloppis.app.ui.components.events.FilterChips
import se.iloppis.app.ui.components.events.SearchBar
import se.iloppis.app.ui.components.navigation.ILoppisHeader
import se.iloppis.app.utils.events.state.EventListState
import se.iloppis.app.utils.events.state.EventListStateAction
import se.iloppis.app.utils.events.state.rememberEventListState

@Composable
fun EventSelectionScreen(onAction: (Event) -> Unit) {
    val state = rememberEventListState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .statusBarsPadding()
    ) {
        ILoppisHeader(R.string.pages_selection)

        SearchBar()

        FilterChips(state.sort, true) {
            state.onAction(EventListStateAction.SetSortingMethod(it))
        }

        ListContent(state, onAction)
    }
}



/**
 * Main list content
 */
@Composable
private fun ListContent(
    state: EventListState,
    onAction: (Event) -> Unit
) {
    PullToRefreshBox(
        isRefreshing = false,
        onRefresh = { state.onAction(EventListStateAction.Reload) }
    ) {
        when {
            state.isLoading -> LoadingState()
            state.errorMessage != null -> ErrorState(state.errorMessage!!)
            state.events.isEmpty() -> EmptyState()
            else -> EventList(state.events, onAction)
        }
    }
}
