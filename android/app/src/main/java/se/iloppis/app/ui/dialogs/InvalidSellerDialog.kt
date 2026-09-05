package se.iloppis.app.ui.dialogs

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import se.iloppis.app.ui.components.buttons.AppButton
import se.iloppis.app.ui.components.buttons.AppButtonSize
import se.iloppis.app.ui.components.buttons.AppButtonVariant
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.font.FontWeight
import se.iloppis.app.R
import java.text.SimpleDateFormat
import java.util.Date

/** Displays a dismissible summary of a purchase rejected for invalid sellers. */
@Composable
fun InvalidSellerDialog(
    purchaseId: String,
    timestamp: String,
    invalidSellers: List<Int>,
    onDismiss: () -> Unit,
    onReviewNow: () -> Unit
) {
    val timeFormat = SimpleDateFormat("HH:mm", LocalConfiguration.current.locales[0])
    val timestampMillis = try {
        java.time.Instant.parse(timestamp).toEpochMilli()
    } catch (e: Exception) {
        System.currentTimeMillis()
    }
    val timeString = timeFormat.format(Date(timestampMillis))
    
    val sellerText = when {
        invalidSellers.size == 1 -> stringResource(R.string.dialog_invalid_seller_problem_single, invalidSellers[0])
        invalidSellers.size <= 3 -> stringResource(R.string.dialog_invalid_seller_problem_multiple, invalidSellers.joinToString(", "))
        else -> pluralStringResource(R.plurals.dialog_invalid_seller_problem_many, invalidSellers.size, invalidSellers.size)
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.dialog_invalid_seller_title),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = buildString {
                    append(stringResource(R.string.dialog_invalid_seller_message_intro))
                    append("\n\n")
                    append(stringResource(R.string.dialog_invalid_seller_purchase_info, purchaseId.takeLast(6), timeString))
                    append("\n")
                    append(stringResource(R.string.cashier_seller_header))
                    append(": $sellerText\n\n")
                    append(stringResource(R.string.dialog_invalid_seller_continue_work))
                }
            )
        },
        confirmButton = {
            AppButton(
                text = stringResource(R.string.dialog_invalid_seller_review_now),
                onClick = onReviewNow,
                variant = AppButtonVariant.Primary
            )
        },
        dismissButton = {
            AppButton(
                text = stringResource(R.string.dialog_invalid_seller_continue),
                onClick = onDismiss,
                variant = AppButtonVariant.Text,
                size = AppButtonSize.Small
            )
        }
    )
}
