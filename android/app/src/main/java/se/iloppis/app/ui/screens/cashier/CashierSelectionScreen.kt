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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import se.iloppis.app.R
import se.iloppis.app.ui.components.events.SwipeToDismissEventCard
import se.iloppis.app.ui.screens.events.CodeEntryMode
import se.iloppis.app.ui.screens.events.EmptyState
import se.iloppis.app.ui.screens.events.EventListAction
import se.iloppis.app.ui.screens.events.EventListHeader
import se.iloppis.app.ui.screens.events.eventContext
import se.iloppis.app.utils.localStorage

/**
 * Cashier selection screen
 *
 * This uses the [se.iloppis.app.utils.LocalStorage] bucket
 * to get stored events for cashier selection.
 */
@Composable
fun CashierSelectionScreen() {
    val event = eventContext()
    val storage = localStorage()
    val list = remember { mutableStateListOf<String>().apply {
        addAll(
            storage.getJson<List<String>>("stored-events", "[]").toMutableSet()
        )
    }}

    Column(modifier = Modifier.fillMaxSize()
        .padding(horizontal = 16.dp)
        .statusBarsPadding()
    ) {
        EventListHeader()
        Text(
            modifier = Modifier,
            text = stringResource(R.string.cashier_selection_header),
            color = MaterialTheme.colorScheme.tertiary,
            fontSize = 18.sp
        )
        Spacer(modifier = Modifier.height(4.dp))

        if(list.isEmpty()) EmptyState() /* Empty list state */
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(event.uiState.events) {

                /* This only uses the loaded events from EventViewModel - should fetch all events by ID */

                if (list.contains(it.id)) {
                    SwipeToDismissEventCard(
                        event = it,
                        modifier = Modifier.animateItem(),
                        onEndToStart = {
                            list.remove(it.id)
                            storage.putJson("stored-events", list.toSet())
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
}
