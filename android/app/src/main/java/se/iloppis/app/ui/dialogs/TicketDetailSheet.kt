package se.iloppis.app.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import se.iloppis.app.R
import se.iloppis.app.domain.model.VisitorTicket
import se.iloppis.app.domain.model.VisitorTicketStatus
import se.iloppis.app.ui.components.buttons.AppButton
import se.iloppis.app.ui.components.buttons.AppButtonVariant
import se.iloppis.app.ui.theme.AppColors
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val svLocale = Locale.Builder().setLanguage("sv").setRegion("SE").build()
private val dateTimeFormatter = DateTimeFormatter.ofPattern("d MMM HH:mm", svLocale)
    .withZone(ZoneId.systemDefault())

/**
 * Bottom sheet showing ticket details with an optional "Mark as scanned" action.
 * Used by the manual ticket search flow.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicketDetailSheet(
    ticket: VisitorTicket?,
    isProcessing: Boolean,
    onDismiss: () -> Unit,
    onScan: (ticketId: String) -> Unit
) {
    if (ticket == null) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AppColors.CardBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.scanner_ticket_details_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary
            )

            // Status
            val statusText = when (ticket.status) {
                VisitorTicketStatus.SCANNED -> stringResource(R.string.scanner_search_result_scanned)
                VisitorTicketStatus.NOT_SCANNED -> stringResource(R.string.scanner_search_result_not_scanned)
                VisitorTicketStatus.UNSPECIFIED -> stringResource(R.string.scanner_status_unknown)
            }
            val statusColor = when (ticket.status) {
                VisitorTicketStatus.SCANNED -> AppColors.Info
                VisitorTicketStatus.NOT_SCANNED -> AppColors.Success
                VisitorTicketStatus.UNSPECIFIED -> AppColors.TextMuted
            }
            DetailField(
                label = stringResource(R.string.scanner_field_status_label),
                value = statusText,
                valueColor = statusColor
            )

            // Ticket type
            ticket.ticketType?.let { type ->
                DetailField(
                    label = stringResource(R.string.scanner_field_ticket_type_label),
                    value = type
                )
            }

            // Email
            ticket.email?.let { email ->
                DetailField(
                    label = stringResource(R.string.scanner_field_email_label),
                    value = email
                )
            }

            // Scanned at
            ticket.scannedAt?.let { scannedAt ->
                DetailField(
                    label = stringResource(R.string.scanner_field_scanned_at_label),
                    value = dateTimeFormatter.format(scannedAt)
                )
            }

            // Valid window
            val validWindow = buildValidWindowString(ticket)
            if (validWindow != null) {
                DetailField(
                    label = stringResource(R.string.scanner_field_valid_window_label),
                    value = validWindow
                )
            }

            // Ticket ID
            DetailField(
                label = stringResource(R.string.scanner_field_ticket_id_label),
                value = ticket.id
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Primary action: mark as scanned (only for unscanned tickets)
            if (ticket.status == VisitorTicketStatus.NOT_SCANNED) {
                AppButton(
                    text = stringResource(R.string.scanner_button_mark_scanned),
                    onClick = { onScan(ticket.id) },
                    enabled = !isProcessing,
                    variant = AppButtonVariant.Success,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            AppButton(
                text = stringResource(R.string.scanner_button_close),
                onClick = onDismiss,
                variant = AppButtonVariant.Secondary,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun DetailField(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = AppColors.TextPrimary
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = AppColors.Background,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.TextMuted
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = valueColor,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private fun buildValidWindowString(ticket: VisitorTicket): String? {
    val from = ticket.validFrom?.let { dateTimeFormatter.format(it) }
    val until = ticket.validUntil?.let { dateTimeFormatter.format(it) }
    return when {
        from != null && until != null -> "$from – $until"
        from != null -> from
        until != null -> until
        else -> null
    }
}
