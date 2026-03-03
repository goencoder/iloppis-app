package se.iloppis.app.ui.screens.events

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import se.iloppis.app.R
import se.iloppis.app.domain.model.CodeEntryMode
import se.iloppis.app.navigation.ScreenPage
import se.iloppis.app.ui.components.CodeBox
import se.iloppis.app.ui.components.buttons.AppButton
import se.iloppis.app.ui.components.buttons.AppButtonVariant
import se.iloppis.app.ui.screens.screenContext
import se.iloppis.app.ui.states.ScreenAction
import se.iloppis.app.ui.theme.AppColors

private const val CODE_LENGTH = 6

/**
 * Screen for direct code entry to access Cashier/Scanner modes.
 *
 * User enters code in XXX-YYY format (auto-formatted).
 * On verify: resolves alias via API, validates type/active,
 * then navigates to CodeConfirmScreen on success.
 *
 * Below the code input, previously-used codes are shown in a list.
 * Tapping a saved code re-validates it and navigates automatically.
 *
 * @param mode Cashier or Scanner
 * @param eventId Optional filter — when non-null only codes for this event are shown
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodeEntryScreen(mode: CodeEntryMode, eventId: String? = null) {
    val screen = screenContext()
    val viewModel: CodeEntryViewModel = viewModel(
        key = "code-entry-$mode-${eventId.orEmpty()}",
        factory = CodeEntryViewModel.factory(mode, eventId)
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
        val focusRequester = remember { FocusRequester() }
        LaunchedEffect(Unit) { focusRequester.requestFocus() }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Mode hint ──
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = when (mode) {
                        CodeEntryMode.CASHIER -> stringResource(R.string.code_entry_subtitle_cashier)
                        CodeEntryMode.SCANNER -> stringResource(R.string.code_entry_subtitle_scanner)
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
            }

            // ── 6-box code input (XXX - YYY) ──
            item {
                val hasError = state.errorKey != null
                BasicTextField(
                    value = state.rawCode,
                    onValueChange = { viewModel.onAction(CodeEntryAction.UpdateCode(it)) },
                    keyboardOptions = KeyboardOptions(
                        autoCorrectEnabled = false,
                        capitalization = KeyboardCapitalization.Characters,
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { viewModel.onAction(CodeEntryAction.VerifyCode) }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    decorationBox = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // First 3 boxes
                            repeat(3) { index ->
                                CodeBox(
                                    char = state.rawCode.getOrNull(index)?.toString() ?: "",
                                    isFocused = state.rawCode.length == index,
                                    hasError = hasError && state.isCodeComplete,
                                    modifier = Modifier.weight(1f)
                                )
                                if (index < 2) Spacer(modifier = Modifier.width(6.dp))
                            }
                            // Dash separator
                            Text(
                                text = stringResource(R.string.code_separator),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = AppColors.TextMuted,
                                modifier = Modifier.padding(horizontal = 6.dp)
                            )
                            // Last 3 boxes
                            repeat(3) { index ->
                                CodeBox(
                                    char = state.rawCode.getOrNull(index + 3)?.toString() ?: "",
                                    isFocused = state.rawCode.length == index + 3,
                                    hasError = hasError && state.isCodeComplete,
                                    modifier = Modifier.weight(1f)
                                )
                                if (index < 2) Spacer(modifier = Modifier.width(6.dp))
                            }
                        }
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // ── Error / loading indicator ──
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        state.isLoading -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = AppColors.TextMuted
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.code_validating),
                                    fontSize = 12.sp,
                                    color = AppColors.TextMuted
                                )
                            }
                        }
                        state.errorKey != null -> {
                            val errorText = when (state.errorKey) {
                                "inactive" -> stringResource(R.string.code_entry_error_inactive)
                                "wrong_type_cashier" -> stringResource(R.string.code_entry_error_wrong_type_cashier)
                                "wrong_type_scanner" -> stringResource(R.string.code_entry_error_wrong_type_scanner)
                                "not_found" -> stringResource(R.string.code_entry_error_not_found)
                                "server" -> stringResource(R.string.code_entry_error_server)
                                "network" -> stringResource(R.string.code_entry_error_network)
                                else -> null
                            }
                            if (errorText != null) {
                                Text(
                                    text = errorText,
                                    fontSize = 12.sp,
                                    color = AppColors.TextError,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // ── Verify button ──
            item {
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
            }

            // ── Cancel button ──
            item {
                AppButton(
                    text = stringResource(R.string.button_cancel),
                    onClick = { screen.popPage() },
                    modifier = Modifier.fillMaxWidth(),
                    variant = AppButtonVariant.Secondary,
                    containerColor = AppColors.Background,
                    contentColor = AppColors.TextPrimary
                )
            }

            // ── Saved codes section ──
            val validCodes = state.savedCodes.filter { it.isValid || it.isValidating }
            if (validCodes.isNotEmpty() || state.isSavedCodesLoading) {
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider(color = AppColors.Border)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.saved_codes_header),
                        style = MaterialTheme.typography.titleSmall,
                        color = AppColors.TextSecondary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }

            if (state.isSavedCodesLoading) {
                item {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(16.dp)
                            .size(24.dp),
                        strokeWidth = 2.dp,
                        color = AppColors.Primary
                    )
                }
            }

            items(
                items = validCodes,
                key = { it.savedCode.alias }
            ) { entry ->
                SavedCodeRow(
                    entry = entry,
                    onClick = {
                        viewModel.onAction(CodeEntryAction.SelectSavedCode(entry.savedCode))
                    },
                    onRemove = {
                        viewModel.onAction(CodeEntryAction.RemoveSavedCode(entry.savedCode.alias))
                    }
                )
            }

            // Bottom spacing
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

/**
 * Row displaying a previously-saved code with event name.
 * Tap to re-use, delete icon to remove.
 */
@Composable
private fun SavedCodeRow(
    entry: ValidatedSavedCode,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = entry.isValid && !entry.isValidating, onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Code and event name
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.savedCode.alias,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = if (entry.isValidating) AppColors.TextSecondary else AppColors.TextPrimary
            )
            Text(
                text = entry.savedCode.eventName,
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextSecondary
            )
        }

        // Validating spinner or action icons
        if (entry.isValidating) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = AppColors.TextSecondary
            )
        } else {
            IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = stringResource(R.string.saved_codes_remove),
                    tint = AppColors.TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = AppColors.TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
    HorizontalDivider(color = AppColors.Border.copy(alpha = 0.5f))
}
