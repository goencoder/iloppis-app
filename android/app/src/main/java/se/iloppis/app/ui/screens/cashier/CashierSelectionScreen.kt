package se.iloppis.app.ui.screens.cashier

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import se.iloppis.app.R
import se.iloppis.app.ui.components.events.SwipeToDismissEventCard
import se.iloppis.app.ui.screens.events.CodeEntryMode
import se.iloppis.app.ui.screens.events.EmptyState
import se.iloppis.app.ui.screens.events.ErrorState
import se.iloppis.app.ui.screens.events.EventListAction
import se.iloppis.app.ui.screens.events.EventListHeader
import se.iloppis.app.ui.screens.events.LoadingState
import se.iloppis.app.ui.screens.events.eventContext
import se.iloppis.app.utils.events.StoredEventsListState
import se.iloppis.app.utils.events.rememberStoredEventsListState
import se.iloppis.app.utils.storage.LocalStorage
import se.iloppis.app.utils.storage.localStorage

/**
 * Cashier selection screen
 *
 * This uses the [LocalStorage] bucket
 * to get stored events for cashier selection.
 */
@Composable
fun CashierSelectionScreen() {
    val storage = localStorage()
    val state = rememberStoredEventsListState(storage)

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 16.dp)
        .statusBarsPadding()
    ) {
        EventListHeader()
        Text(
            modifier = Modifier,
            text = stringResource(R.string.cashier_selection_header),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 18.sp
        )
        Spacer(modifier = Modifier.height(4.dp))

        when {
            state.isLoading -> LoadingState()
            state.errorMessage != null -> ErrorState(state.errorMessage!!)
            state.events.isEmpty() -> EmptyState()
            else -> Content(state)
        }
    }
}



@Composable
private fun Content(
    state: StoredEventsListState
) {
    val event = eventContext()

    PullToRefreshBox(
        isRefreshing = false,
        onRefresh = { if(!state.isLoading) state.reload() }
    ) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(state.events) {
                SwipeToDismissEventCard(
                    event = it,
                    modifier = Modifier.animateItem(),
                    onEndToStart = {
                        state.remove(it.id)
                    }
                ) {
                    event.onAction(
                        EventListAction.StartCodeEntry(
                            CodeEntryMode.CASHIER,
                            it
                        )
                    )
                }
            }
        }
    }
}
