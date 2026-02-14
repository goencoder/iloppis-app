package se.iloppis.app.ui.components.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import se.iloppis.app.domain.model.Event
import se.iloppis.app.ui.components.CancelTextButton
import se.iloppis.app.ui.components.CodeBox
import se.iloppis.app.ui.components.PrimaryButton
import se.iloppis.app.ui.theme.AppColors
import se.iloppis.app.utils.user.codes.CodeState
import se.iloppis.app.utils.user.codes.CodeStateMode
import se.iloppis.app.utils.user.codes.rememberCodeState

/**
 * Code entry dialog
 */
@Composable
fun CodeEntryDialog(event: Event, mode: CodeStateMode, onDismiss: () -> Unit) {
    val state = rememberCodeState(event, mode)
    DialogContent(state, onDismiss)
}



/**
 * Dialog for entering cashier or scanner codes.
 * Supports paste with both XXXXXX and XXX-XXX formats.
 * Validates code implicitly when complete.
 */
@Composable
private fun DialogContent(
    state: CodeState,
    onDismiss: () -> Unit,
) {
    var code by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val maxLength = 6

    val title = when (state.mode) {
        CodeStateMode.CASHIER -> stringResource(R.string.code_entry_title_cashier)
        CodeStateMode.SCANNER -> stringResource(R.string.code_entry_title_scanner)
    }
    val subtitle = when (state.mode) {
        CodeStateMode.CASHIER -> stringResource(R.string.code_entry_subtitle_cashier)
        CodeStateMode.SCANNER -> stringResource(R.string.code_entry_subtitle_scanner)
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
                    text = state.event.name,
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
                        state.validate(newCode)
                    },
                    focusRequester = focusRequester,
                    hasError = state.errorMessage != null
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
                        state.isValidating -> {
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
                        state.errorMessage != null -> {
                            Text(
                                text = stringResource(R.string.code_invalid),
                                fontSize = 12.sp,
                                color = AppColors.TextError
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                PrimaryButton( // Submit button
                    text = stringResource(R.string.button_verify_continue),
                    onClick = { state.validate(code) },
                    enabled = code.length == maxLength && !state.isValidating
                )

                Spacer(modifier = Modifier.height(16.dp))
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
                .take(CodeState.CODE_LENGTH)
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
                        hasError = hasError && code.length == CodeState.CODE_LENGTH,
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
                        hasError = hasError && code.length == CodeState.CODE_LENGTH,
                        modifier = Modifier.weight(1f)
                    )
                    if (index < 2) Spacer(modifier = Modifier.width(6.dp))
                }
            }
        }
    )
}
