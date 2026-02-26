package se.iloppis.app.ui.screens.events

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import se.iloppis.app.R
import se.iloppis.app.domain.model.Event
import se.iloppis.app.domain.model.displayStatus
import se.iloppis.app.navigation.ScreenPage
import se.iloppis.app.ui.components.DisplayStatusBadge
import se.iloppis.app.ui.screens.screenContext
import se.iloppis.app.ui.states.ScreenAction
import se.iloppis.app.ui.theme.AppColors

/**
 * Confirmation screen after code entry.
 *
 * Shows which event the code resolved to (name, dates, location, status).
 * User confirms before entering the Cashier/Scanner tool.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodeConfirmScreen(event: Event, apiKey: String, mode: String) {
    val screen = screenContext()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(R.string.confirm_event_title))
                },
                navigationIcon = {
                    IconButton(onClick = { screen.popPage() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Confirmation message
            Text(
                text = stringResource(R.string.confirm_event_message),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                style = MaterialTheme.typography.bodyLarge
            )

            // Event details card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                colors = CardDefaults.cardColors(containerColor = AppColors.CardBackground)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    // Event name + status badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = event.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        DisplayStatusBadge(event.displayStatus())
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Dates
                    if (event.dates.isNotBlank()) {
                        Text(
                            text = event.dates,
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppColors.TextSecondary
                        )
                    }

                    // Time
                    if (event.startTimeFormatted.isNotBlank() || event.endTimeFormatted.isNotBlank()) {
                        val hours = listOfNotNull(
                            event.startTimeFormatted.takeIf { it.isNotBlank() },
                            event.endTimeFormatted.takeIf { it.isNotBlank() }
                        ).joinToString(" – ")
                        Text(
                            text = hours,
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppColors.TextSecondary
                        )
                    }

                    // Location
                    if (event.location.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = event.location,
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppColors.TextSecondary
                        )
                    }
                }
            }

            // Confirm button
            Button(
                onClick = {
                    val toolPage = when (mode) {
                        "CASHIER" -> ScreenPage.Cashier(event, apiKey)
                        "SCANNER" -> ScreenPage.Scanner(event, apiKey)
                        else -> return@Button
                    }
                    screen.onAction(
                        ScreenAction.NavigateToPage(toolPage, true)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Text(
                    stringResource(
                        when (mode) {
                            "CASHIER" -> R.string.open_cashier_button
                            "SCANNER" -> R.string.open_scanner_button
                            else -> R.string.confirm_button
                        }
                    )
                )
            }

            // Cancel button
            Button(
                onClick = { screen.popPage() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.CardBackground
                )
            ) {
                Text(
                    stringResource(R.string.cancel_button),
                    color = AppColors.TextPrimary
                )
            }
        }
    }
}

