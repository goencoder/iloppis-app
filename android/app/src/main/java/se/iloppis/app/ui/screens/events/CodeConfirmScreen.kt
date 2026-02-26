package se.iloppis.app.ui.screens.events

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import se.iloppis.app.R
import se.iloppis.app.domain.model.Event
import se.iloppis.app.navigation.ScreenPage
import se.iloppis.app.ui.screens.screenContext
import se.iloppis.app.ui.states.ScreenAction
import se.iloppis.app.ui.theme.AppColors

/**
 * Confirmation screen after code entry.
 *
 * Shows which event the code belongs to and asks user to confirm
 * before entering the Cashier/Scanner tool.
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
        CodeConfirmContent(
            event = event,
            mode = mode,
            onConfirm = {
                // Navigate to appropriate tool screen
                val toolPage = when (mode) {
                    "CASHIER" -> ScreenPage.Cashier(event, apiKey)
                    "SCANNER" -> ScreenPage.Scanner(event, apiKey)
                    else -> return@CodeConfirmContent
                }
                screen.onAction(
                    ScreenAction.NavigateToPage(toolPage, true)
                )
            },
            onCancel = { screen.popPage() },
            modifier = Modifier.padding(paddingValues)
        )
    }
}

/**
 * Confirmation content showing event details
 */
@Composable
private fun CodeConfirmContent(
    event: Event,
    mode: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Confirmation message
        Text(
            text = stringResource(R.string.confirm_event_message),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            style = MaterialTheme.typography.bodyMedium
        )

        // Event details card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = event.name,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = event.description.orEmpty().take(100),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextMuted
                )

                if (!event.description.isNullOrBlank()) {
                    Text(
                        text = event.description,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }
        }

        // Confirm button
        Button(
            onClick = onConfirm,
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
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors().copy(
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
