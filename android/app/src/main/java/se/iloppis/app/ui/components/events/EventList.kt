package se.iloppis.app.ui.components.events

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import se.iloppis.app.domain.model.Event

/**
 * Swipeable events list component
 *
 * @see SwipeToDismissEventCard
 */
@Composable
fun SwipeableEventList(
    events: List<Event>,
    enableEndToStart: Boolean = true,
    onEndToStart: (Event) -> Unit = {},
    enableStartToEnd: Boolean = false,
    onStartToEnd: (Event) -> Unit = {},
    onAction: (Event) -> Unit
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(events, key = { it.id }) {
            SwipeToDismissEventCard(
                event = it,
                enableEndToStart = enableEndToStart,
                onEndToStart = { onEndToStart(it) },
                enableStartToEnd = enableStartToEnd,
                onStartToEnd = { onStartToEnd(it) },
                cardAction = { onAction(it) }
            )
        }
    }
}
