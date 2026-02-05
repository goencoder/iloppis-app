package se.iloppis.app.ui.components.events

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import se.iloppis.app.R
import se.iloppis.app.domain.model.Event
import se.iloppis.app.ui.components.StateBadge
import se.iloppis.app.ui.theme.AppColors

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
    val scope = rememberCoroutineScope()
    SwipeToDismissBox(
        state = state,
        modifier = modifier.fillMaxSize(),
        enableDismissFromStartToEnd = enableStartToEnd,
        enableDismissFromEndToStart = enableEndToStart,
        backgroundContent = {
            when(state.dismissDirection) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    Icon(
                        imageVector = Icons.Outlined.AddCircleOutline,
                        contentDescription = stringResource(R.string.swipe_box_archive),
                        modifier = Modifier
                            .fillMaxSize()
                            .background(lerp(
                                MaterialTheme.colorScheme.background,
                                MaterialTheme.colorScheme.onSecondary,
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
                                MaterialTheme.colorScheme.onSurfaceVariant,
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
            when (it) {
                SwipeToDismissBoxValue.EndToStart -> onEndToStart()
                SwipeToDismissBoxValue.StartToEnd -> onStartToEnd()
                else -> return@SwipeToDismissBox
            }
            scope.launch { state.reset() }
        }
    ) {
        EventCard(event, cardAction)
    }
}



/**
 * Card displaying event summary information.
 */
@Composable
fun EventCard(event: Event, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.CardBackground)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = event.name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = AppColors.TextPrimary
                )
                StateBadge(event.state)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = event.dates,
                color = AppColors.TextMuted,
                fontSize = 14.sp
            )
            Text(
                text = event.location.ifBlank { stringResource(R.string.location_not_specified) },
                color = AppColors.TextMuted,
                fontSize = 14.sp
            )
        }
    }
}
