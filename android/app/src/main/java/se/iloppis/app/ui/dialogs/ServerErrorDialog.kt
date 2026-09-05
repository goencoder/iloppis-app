package se.iloppis.app.ui.dialogs

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import se.iloppis.app.ui.components.buttons.AppButton
import se.iloppis.app.ui.components.buttons.AppButtonSize
import se.iloppis.app.ui.components.buttons.AppButtonVariant
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import se.iloppis.app.R

/** Displays the number of purchases waiting locally after a server failure. */
@Composable
fun ServerErrorDialog(
    pendingPurchasesCount: Int,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.dialog_server_error_title),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = stringResource(R.string.dialog_server_error_message, pendingPurchasesCount)
            )
        },
        confirmButton = {
            AppButton(
                text = stringResource(R.string.dialog_server_error_confirm),
                onClick = onDismiss,
                variant = AppButtonVariant.Text,
                size = AppButtonSize.Small
            )
        }
    )
}
