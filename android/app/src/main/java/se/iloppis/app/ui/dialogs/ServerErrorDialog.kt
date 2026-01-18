package se.iloppis.app.ui.dialogs

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import se.iloppis.app.R

/**
 * Dialog shown when server errors (5xx) occur during purchase uploads.
 * 
 * Display rules:
 * - Show max once per server error session
 * - Only show when kassa fields are empty (idle state)
 * - Non-blocking (can be dismissed immediately)
 * 
 * Swedish text as per spec:
 * ⚠️ Serverproblem
 * iLoppis-servern svarar inte. Kassa fungerar i offline-läge.
 * Köp sparas lokalt och laddas upp automatiskt när problemet är löst.
 * 
 * Detta kräver åtgärd från iLoppis support.
 * Antal väntande köp: X
 */
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
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_server_error_confirm))
            }
        }
    )
}
