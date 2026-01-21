package se.iloppis.app.ui.screens.cashier

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import se.iloppis.app.ui.components.EventCard
import se.iloppis.app.ui.screens.events.CodeEntryMode
import se.iloppis.app.ui.screens.events.EventListAction
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

    LazyColumn() {
        items(viewModel.uiState.events) {

            /* This only uses the loaded events from EventViewModel - should fetch all events by ID */

            if(events.contains(it.id)) {
                EventCard(it) {

                    /* Fix to give full control to cashier page */

                    viewModel.onAction(EventListAction.StartCodeEntry(CodeEntryMode.CASHIER, it))
                }
            }
        }
    }
}
