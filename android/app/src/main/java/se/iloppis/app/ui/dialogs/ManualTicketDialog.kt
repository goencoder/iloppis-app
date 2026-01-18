package se.iloppis.app.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import se.iloppis.app.R
import se.iloppis.app.ui.screens.scanner.ManualEntryError
import se.iloppis.app.ui.theme.AppColors

/**
 * Dialog used for manual fallback when scanning QR codes is not possible.
 */
@Composable
fun ManualTicketDialog(
    visible: Boolean,
    eventName: String,
    error: ManualEntryError?,
    isProcessing: Boolean,
    onDismiss: () -> Unit,
    onTextChanged: () -> Unit,
    onSubmit: (String) -> Unit
) {
    if (!visible) return

    var code by rememberSaveable { mutableStateOf("") }

    // Reset input each time the dialog is opened.
    LaunchedEffect(Unit) {
        code = ""
    }

    val errorText = when (error) {
        ManualEntryError.EMPTY_INPUT -> stringResource(R.string.scanner_manual_error_empty)
        ManualEntryError.WRONG_EVENT -> stringResource(R.string.scanner_manual_error_event, eventName)
        ManualEntryError.INVALID_FORMAT -> stringResource(R.string.scanner_manual_error_format)
        null -> null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.scanner_manual_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.scanner_manual_description),
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = code,
                    onValueChange = {
                        code = it
                        onTextChanged()
                    },
                    placeholder = {
                        Text(text = stringResource(R.string.scanner_manual_placeholder))
                    },
                    isError = errorText != null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters)
                )
                errorText?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(code) },
                enabled = code.isNotBlank() && !isProcessing,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.Primary
                )
            ) {
                Text(text = stringResource(R.string.scanner_manual_confirm))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = AppColors.TextSecondary
                )
            ) {
                Text(text = stringResource(R.string.button_cancel))
            }
        }
    )
}
