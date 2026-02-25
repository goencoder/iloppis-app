package se.iloppis.app.ui.screens.events

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import se.iloppis.app.R
import se.iloppis.app.ui.screens.screenContext
import se.iloppis.app.ui.states.ScreenAction
import se.iloppis.app.ui.theme.AppColors
import androidx.compose.material3.TopAppBar

/**
 * Screen for direct code entry to access Cashier/Scanner modes.
 *
 * User enters code XXX-XXX format, which is validated and resolved
 * to an event. After successful entry, user is taken to CodeConfirmScreen
 * to approve the event before entering the tool.
 */
@Composable
fun CodeEntryScreen(mode: String) {
    val screen = screenContext()
    var code by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (mode) {
                            "CASHIER" -> stringResource(R.string.home_open_cashier)
                            "SCANNER" -> stringResource(R.string.home_open_scanner)
                            else -> stringResource(R.string.app_name)
                        }
                    )
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
        CodeEntryContent(
            code = code,
            onCodeChange = {
                code = it
                errorMessage = null
            },
            isLoading = isLoading,
            errorMessage = errorMessage,
            onVerify = {
                isLoading = true
                // TODO: Call API to verify code and get event
                // For now, just show mock confirmation
                errorMessage = null
                isLoading = false
            },
            onBack = { screen.popPage() },
            modifier = Modifier.padding(paddingValues)
        )
    }
}

/**
 * Content for code entry screen
 */
@Composable
private fun CodeEntryContent(
    code: String,
    onCodeChange: (String) -> Unit,
    isLoading: Boolean,
    errorMessage: String?,
    onVerify: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Code input field with XXX-XXX format
        OutlinedTextField(
            value = code,
            onValueChange = onCodeChange,
            label = { Text(stringResource(R.string.code_entry_label)) },
            placeholder = { Text("XXX-XXX") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            enabled = !isLoading,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { if (code.isNotEmpty()) onVerify() }
            ),
            singleLine = true
        )

        // Error message if verification fails
        if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = AppColors.TextError,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )
        }

        // Verify button
        Button(
            onClick = onVerify,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            enabled = code.isNotEmpty() && !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(stringResource(R.string.verify_button))
        }

        // Cancel button
        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors().copy(
                containerColor = AppColors.Background
            )
        ) {
            Text(
                stringResource(R.string.cancel_button),
                color = AppColors.TextPrimary
            )
        }
    }
}
