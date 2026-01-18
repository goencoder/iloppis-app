package se.iloppis.app.ui.dialogs

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.font.FontWeight
import se.iloppis.app.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Dialog shown when auto-recovery fails for INVALID_SELLER errors.
 * 
 * Display rules:
 * - Show after failed auto-recovery attempt
 * - Only show when kassa fields are empty (idle state)
 * - Non-blocking (can be dismissed immediately)
 * - Minimum 5 minutes between popups
 * 
 * Swedish text as per spec:
 * ⚠️ Köp behöver granskas
 * Ett tidigare köp kunde inte laddas upp eftersom en eller flera
 * säljare inte är godkända för detta event.
 * 
 * Köp: ABC123 (14:23)
 * Problem: Säljare 456 ej godkänd
 * 
 * Du kan fortsätta registrera nya köp.
 */
@Composable
fun InvalidSellerDialog(
    purchaseId: String,
    timestamp: String,
    invalidSellers: List<Int>,
    onDismiss: () -> Unit,
    onReviewNow: () -> Unit
) {
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
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
            TextButton(onClick = onReviewNow) {
                Text(stringResource(R.string.dialog_invalid_seller_review_now))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_invalid_seller_continue))
            }
        }
    )
}
