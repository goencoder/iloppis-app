package se.iloppis.app.ui.components.events

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import se.iloppis.app.domain.model.Event
import se.iloppis.app.ui.components.EventCard

/**
 * Swipe to dismiss event card
 *
 * Application event card with functionality
 * for swipe actions.
 */
@Composable
fun SwipeToDismissEventCard(
    event: Event,
    enableStartToEnd: Boolean = false,
    enableEndToStart: Boolean = true,
    onStartToEnd: () -> Unit = {},
    onEndToStart: () -> Unit,
    cardAction: () -> Unit,
) {
    val state = rememberSwipeToDismissBoxState()
    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = enableStartToEnd,
        enableDismissFromEndToStart = enableEndToStart,
        backgroundContent = {
            val color by animateColorAsState(
                when(state.targetValue) {
                    SwipeToDismissBoxValue.Settled -> MaterialTheme.colorScheme.background
                    SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.primary
                    SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.surface
                }
            )
            Box(modifier = Modifier.fillMaxSize().background(color))
        },
        onDismiss = {
            if(it == SwipeToDismissBoxValue.EndToStart) onEndToStart()
            else if(it == SwipeToDismissBoxValue.StartToEnd) onStartToEnd()
        }
    ) {
        EventCard(event, cardAction)
    }
}
