package se.iloppis.app.ui.components.events

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import se.iloppis.app.R
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
    modifier: Modifier = Modifier,
    enableStartToEnd: Boolean = false,
    enableEndToStart: Boolean = true,
    onStartToEnd: () -> Unit = {},
    onEndToStart: () -> Unit = {},
    cardAction: () -> Unit,
) {
    val state = rememberSwipeToDismissBoxState()
    SwipeToDismissBox(
        state = state,
        modifier = modifier.fillMaxSize(),
        enableDismissFromStartToEnd = enableStartToEnd,
        enableDismissFromEndToStart = enableEndToStart,
        backgroundContent = {
            when(state.dismissDirection) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    Icon(
                        imageVector = Icons.Outlined.CheckBox,
                        contentDescription = stringResource(R.string.swipe_box_archive),
                        modifier = Modifier
                            .fillMaxSize()
                            .background(lerp(
                                MaterialTheme.colorScheme.background,
                                MaterialTheme.colorScheme.secondary,
                                state.progress
                            ), shape = RoundedCornerShape(16.dp))
                            .wrapContentSize(Alignment.CenterStart)
                            .padding(12.dp),
                        tint = MaterialTheme.colorScheme.background
                    )
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = stringResource(R.string.swipe_box_remove),
                        modifier = Modifier
                            .fillMaxSize()
                            .background(lerp(
                                MaterialTheme.colorScheme.background,
                                MaterialTheme.colorScheme.error,
                                state.progress
                            ), shape = RoundedCornerShape(16.dp))
                            .wrapContentSize(Alignment.CenterEnd)
                            .padding(12.dp),
                        tint = MaterialTheme.colorScheme.background
                    )
                }
                SwipeToDismissBoxValue.Settled -> {}
            }
        },
        onDismiss = {
            if(it == SwipeToDismissBoxValue.EndToStart) onEndToStart()
            else if(it == SwipeToDismissBoxValue.StartToEnd) onStartToEnd()
        }
    ) {
        EventCard(event, cardAction)
    }
}
