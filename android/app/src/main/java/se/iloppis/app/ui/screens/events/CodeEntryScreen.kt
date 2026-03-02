package se.iloppis.app.ui.screens.events

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
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
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import se.iloppis.app.R
import se.iloppis.app.domain.model.CodeEntryMode
import se.iloppis.app.navigation.ScreenPage
import se.iloppis.app.ui.components.buttons.AppButton
import se.iloppis.app.ui.components.buttons.AppButtonVariant
import se.iloppis.app.ui.screens.screenContext
import se.iloppis.app.ui.states.ScreenAction
import se.iloppis.app.ui.theme.AppColors

/**
 * Screen for direct code entry to access Cashier/Scanner modes.
 *
 * User enters code in XXX-YYY format (auto-formatted).
 * On verify: resolves alias via API, validates type/active,
 * then navigates to CodeConfirmScreen on success.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodeEntryScreen(mode: CodeEntryMode) {
    val screen = screenContext()
    val viewModel: CodeEntryViewModel = viewModel(
        key = "code-entry-$mode",
        factory = CodeEntryViewModel.factory(mode)
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // React to successful verification → navigate to CodeConfirm
    LaunchedEffect(state.verifiedResult) {
        val result = state.verifiedResult ?: return@LaunchedEffect
        screen.onAction(
            ScreenAction.NavigateToPage(
                ScreenPage.CodeConfirm(
                    event = result.event,
                    apiKey = result.apiKey,
                    mode = result.mode
                )
            )
        )
        viewModel.onAction(CodeEntryAction.NavigationConsumed)
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
                    CodeEntryMode.CASHIER -> stringResource(R.string.code_entry_cashier_hint)
                    CodeEntryMode.SCANNER -> stringResource(R.string.code_entry_scanner_hint)
                },
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Code input field with XXX-YYY auto-formatting
            OutlinedTextField(
                value = state.displayCode,
                visualTransformation = CodeEntryFormatTransform(),
                onValueChange = { viewModel.onAction(CodeEntryAction.UpdateCode(it)) },
                label = { Text(stringResource(R.string.code_entry_label)) },
                placeholder = { Text(stringResource(R.string.code_entry_placeholder)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                enabled = !state.isLoading,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done,
                    capitalization = KeyboardCapitalization.Characters
                ),
                keyboardActions = KeyboardActions(
                    onDone = { viewModel.onAction(CodeEntryAction.VerifyCode) }
                ),
                singleLine = true
            )

            // Error message
            val errorText = when (state.errorKey) {
                "inactive" -> stringResource(R.string.code_entry_error_inactive)
                "wrong_type_cashier" -> stringResource(R.string.code_entry_error_wrong_type_cashier)
                "wrong_type_scanner" -> stringResource(R.string.code_entry_error_wrong_type_scanner)
                "not_found" -> stringResource(R.string.code_entry_error_not_found)
                "network" -> stringResource(R.string.code_entry_error_network)
                else -> null
            }
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
            AppButton(
                text = stringResource(R.string.verify_button),
                onClick = { viewModel.onAction(CodeEntryAction.VerifyCode) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                enabled = state.isCodeComplete,
                loading = state.isLoading,
                variant = AppButtonVariant.Primary
            )

            // Cancel button
            AppButton(
                text = stringResource(R.string.button_cancel),
                onClick = { screen.popPage() },
                modifier = Modifier.fillMaxWidth(),
                variant = AppButtonVariant.Secondary,
                containerColor = AppColors.Background,
                contentColor = AppColors.TextPrimary
            )
        }
    }
}
