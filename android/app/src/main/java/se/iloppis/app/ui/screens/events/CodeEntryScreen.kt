package se.iloppis.app.ui.screens.events

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import se.iloppis.app.R
import se.iloppis.app.navigation.ScreenPage
import se.iloppis.app.ui.screens.screenContext
import se.iloppis.app.ui.states.ScreenAction
import se.iloppis.app.ui.theme.AppColors

@OptIn(ExperimentalMaterial3Api::class)

/**
 * Screen for direct code entry to access Cashier/Scanner modes.
 *
 * User enters code in XXX-YYY format (auto-formatted).
 * On verify: resolves alias via API, validates type/active,
 * then navigates to CodeConfirmScreen on success.
 */
@Composable
fun CodeEntryScreen(mode: String) {
    val screen = screenContext()
    val viewModel: CodeEntryViewModel = viewModel(factory = CodeEntryViewModel.factory(mode))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.verifiedResult) {
        val verifiedResult = uiState.verifiedResult ?: return@LaunchedEffect
        screen.onAction(
            ScreenAction.NavigateToPage(
                ScreenPage.CodeConfirm(
                    event = verifiedResult.event,
                    apiKey = verifiedResult.apiKey,
                    mode = verifiedResult.mode
                )
            )
        )
        viewModel.onAction(CodeEntryAction.NavigationConsumed)
    }

    val errorText = when (uiState.errorKey) {
        "inactive" -> stringResource(R.string.code_entry_error_inactive)
        "wrong_type_cashier" -> stringResource(R.string.code_entry_error_wrong_type_cashier)
        "wrong_type_scanner" -> stringResource(R.string.code_entry_error_wrong_type_scanner)
        "not_found" -> stringResource(R.string.code_entry_error_not_found)
        "network" -> stringResource(R.string.code_entry_error_network)
        else -> null
    }

    fun verifyCode() {
        viewModel.onAction(CodeEntryAction.VerifyCode)
    }

    fun updateCode(input: String) {
        viewModel.onAction(CodeEntryAction.UpdateCode(input))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(R.string.code_entry_label))
                },
                navigationIcon = {
                    IconButton(onClick = { screen.popPage() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.button_back)
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
            // Mode hint
            Text(
                text = when (mode) {
                    "CASHIER" -> stringResource(R.string.code_entry_cashier_hint)
                    "SCANNER" -> stringResource(R.string.code_entry_scanner_hint)
                    else -> stringResource(R.string.code_entry_label)
                },
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Code input field with XXX-YYY auto-formatting
            OutlinedTextField(
                value = uiState.displayCode,
                onValueChange = { updateCode(it) },
                label = { Text(stringResource(R.string.code_entry_label)) },
                placeholder = { Text(stringResource(R.string.code_entry_placeholder)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                enabled = !uiState.isLoading,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done,
                    capitalization = KeyboardCapitalization.Characters
                ),
                keyboardActions = KeyboardActions(
                    onDone = { verifyCode() }
                ),
                singleLine = true
            )

            // Error message
            if (errorText != null) {
                Text(
                    text = errorText,
                    color = AppColors.TextError,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )
            }

            // Verify button
            Button(
                onClick = { verifyCode() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                enabled = uiState.isCodeComplete && !uiState.isLoading
            ) {
                if (uiState.isLoading) {
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
                onClick = { screen.popPage() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.Background
                )
            ) {
                Text(
                    stringResource(R.string.button_cancel),
                    color = AppColors.TextPrimary
                )
            }
        }
    }
}
