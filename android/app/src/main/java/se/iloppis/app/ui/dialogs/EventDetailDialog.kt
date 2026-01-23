package se.iloppis.app.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import se.iloppis.app.R
import se.iloppis.app.domain.model.Event
import se.iloppis.app.ui.components.StateBadge
import se.iloppis.app.ui.theme.AppColors
import se.iloppis.app.utils.storage.localStorage

/**
 * Dialog showing event details with options to open cashier or scanner mode.
 */
@Composable
fun EventDetailDialog(
    event: Event,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = event.name,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary
                )
                StateBadge(event.state)
            }
        },
        text = {
            EventDetailContent(event = event)
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.button_back),
                    color = AppColors.ButtonDanger
                )
            }
        }
    )
}

@Composable
private fun EventDetailContent(event: Event) {
    Column {
        LazyColumn(modifier = Modifier.heightIn(max = 250.dp)) {
            item {
                // Date
                Text(
                    text = event.dates,
                    color = AppColors.TextMuted
                )
            }


            // Opening hours
            if (event.startTimeFormatted.isNotBlank() || event.endTimeFormatted.isNotBlank()) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    val hours = listOfNotNull(
                        event.startTimeFormatted.takeIf { it.isNotBlank() },
                        event.endTimeFormatted.takeIf { it.isNotBlank() }
                    ).joinToString(" – ")
                    Text(
                        text = stringResource(R.string.opening_hours_label, hours),
                        color = AppColors.TextMuted
                    )
                }
            }

            // Description
            if (event.description.isNotBlank()) {
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = event.description,
                        fontSize = 14.sp,
                        color = AppColors.TextDark
                    )
                }
            }
        }



        //
        // Information that does not change on size
        //

        Spacer(modifier = Modifier.height(20.dp))



        // ======================
        // Save event as interest
        // ======================

        val storage = localStorage()

        val key = "stored-events"
        val storedEvents = remember {
            mutableStateSetOf<String>().apply {
                addAll(storage.getJson<Set<String>>(key, "[]"))
            }
        }

        Button(
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors().copy(
                containerColor = if(!storedEvents.contains(event.id))
                    MaterialTheme.colorScheme.onSecondary
                else MaterialTheme.colorScheme.primary
            ),
            onClick = {
                if(!storedEvents.contains(event.id)) storedEvents.add(event.id)
                else storedEvents.remove(event.id)
                storage.putJson("stored-events", storedEvents.toSet())
            }
        ) {
            Text(
                text = if(!storedEvents.contains(event.id))
                    stringResource(R.string.store_event_locally)
                else
                    stringResource(R.string.remove_event_locally),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// Removed ActionButtons - now integrated into EventDetailContent
