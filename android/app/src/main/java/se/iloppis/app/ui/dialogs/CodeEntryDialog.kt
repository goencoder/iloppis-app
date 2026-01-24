package se.iloppis.app.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import se.iloppis.app.R
import se.iloppis.app.ui.components.CancelTextButton
import se.iloppis.app.ui.components.CodeBox
import se.iloppis.app.ui.components.PrimaryButton
import se.iloppis.app.ui.screens.events.CodeEntryMode
import se.iloppis.app.ui.theme.AppColors

/**
 * Dialog for entering cashier or scanner codes.
 * Supports paste with both XXXXXX and XXX-XXX formats.
 * Validates code implicitly when complete.
 */
@Composable
fun CodeEntryDialog(
    mode: CodeEntryMode,
    eventName: String,
    isValidating: Boolean = false,
    errorMessage: String? = null,
    onDismiss: () -> Unit,
    onCodeChange: (String) -> Unit,
    onCodeEntered: (String) -> Unit
) {
    var code by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val maxLength = 6

    val title = when (mode) {
        CodeEntryMode.CASHIER -> stringResource(R.string.code_entry_title_cashier)
        CodeEntryMode.SCANNER -> stringResource(R.string.code_entry_title_scanner)
    }
    val subtitle = when (mode) {
        CodeEntryMode.CASHIER -> stringResource(R.string.code_entry_subtitle_cashier)
        CodeEntryMode.SCANNER -> stringResource(R.string.code_entry_subtitle_scanner)
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = AppColors.DialogBackground)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Title
                Text(
                    text = title,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Event name
                Text(
                    text = eventName,
                    fontSize = 14.sp,
                    color = AppColors.TextMuted
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Subtitle
                Text(
                    text = subtitle,
                    fontSize = 14.sp,
                    color = AppColors.TextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Code input
                CodeInput(
                    code = code,
                    onCodeChange = { newCode ->
                        code = newCode
                        onCodeChange(newCode)
                    },
                    maxLength = maxLength,
                    focusRequester = focusRequester,
                    hasError = errorMessage != null
                )

                // Error message or validating indicator
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isValidating -> {
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
                        errorMessage != null -> {
                            Text(
                                text = stringResource(R.string.code_invalid),
                                fontSize = 12.sp,
                                color = AppColors.TextError
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Submit button
                PrimaryButton(
                    text = stringResource(R.string.button_verify_continue),
                    onClick = { onCodeEntered(code) },
                    enabled = code.length == maxLength && !isValidating
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Cancel button
                CancelTextButton(onClick = onDismiss)
            }
        }
    }
}

/**
 * Code input field with 6 boxes in XXX-XXX format.
 */
@Composable
private fun CodeInput(
    code: String,
    onCodeChange: (String) -> Unit,
    maxLength: Int,
    focusRequester: FocusRequester,
    hasError: Boolean = false
) {
    BasicTextField(
        value = code,
        onValueChange = { newValue ->
            // Handle paste: remove dashes/spaces, filter alphanumeric, uppercase
            val filtered = newValue
                .replace("-", "")
                .replace(" ", "")
                .filter { it.isLetterOrDigit() }
                .take(maxLength)
                .uppercase()
            onCodeChange(filtered)
        },
        keyboardOptions = KeyboardOptions(
            autoCorrectEnabled = false,
            capitalization = KeyboardCapitalization.Characters,
            keyboardType = KeyboardType.Password
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
                        char = code.getOrNull(index)?.toString() ?: "",
                        isFocused = code.length == index,
                        hasError = hasError && code.length == maxLength,
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
                        char = code.getOrNull(index + 3)?.toString() ?: "",
                        isFocused = code.length == index + 3,
                        hasError = hasError && code.length == maxLength,
                        modifier = Modifier.weight(1f)
                    )
                    if (index < 2) Spacer(modifier = Modifier.width(6.dp))
                }
            }
        }
    )
}
