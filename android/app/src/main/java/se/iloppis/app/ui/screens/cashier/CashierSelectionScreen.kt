package se.iloppis.app.ui.screens.cashier

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import se.iloppis.app.R
import se.iloppis.app.ui.components.EventCard
import se.iloppis.app.ui.screens.events.CodeEntryMode
import se.iloppis.app.ui.screens.events.EventListAction
import se.iloppis.app.ui.screens.events.EventListHeader
import se.iloppis.app.ui.screens.events.EventListViewModel
import se.iloppis.app.utils.localStorage

/**
 * Cashier selection screen
 *
 * This uses the [se.iloppis.app.utils.LocalStorage] bucket
 * to get stored events for cashier selection.
 */
@Composable
fun CashierSelectionScreen(
    viewModel: EventListViewModel = viewModel()
) {
    val storage = localStorage()
    val events = storage.getJson<Set<String>>("stored-events", "[]").toMutableSet()

    Column(modifier = Modifier.fillMaxSize()
        .padding(horizontal = 16.dp)
        .statusBarsPadding()
    ) {
        EventListHeader()
        Text(stringResource(R.string.cashier_selection_header))

        LazyColumn() {
            items(viewModel.uiState.events) {

                /* This only uses the loaded events from EventViewModel - should fetch all events by ID */

                if (events.contains(it.id)) {
                    EventCard(it) {

                        /* Fix to give full control to cashier page */

                        viewModel.onAction(
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
