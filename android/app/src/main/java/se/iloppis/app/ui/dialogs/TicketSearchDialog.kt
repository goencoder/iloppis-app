package se.iloppis.app.ui.dialogs

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import se.iloppis.app.R
import se.iloppis.app.domain.model.VisitorTicket
import se.iloppis.app.domain.model.VisitorTicketStatus
import se.iloppis.app.ui.components.buttons.AppButton
import se.iloppis.app.ui.components.buttons.AppButtonVariant
import se.iloppis.app.ui.screens.scanner.TicketTypeOption
import se.iloppis.app.ui.theme.AppColors

/**
 * Bottom sheet for searching visitor tickets by email and ticket type.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicketSearchDialog(
    visible: Boolean,
    isSearching: Boolean,
    searchResults: List<VisitorTicket>,
    searchError: String?,
    ticketTypes: List<TicketTypeOption>,
    onDismiss: () -> Unit,
    onSearch: (query: String, ticketTypeId: String?) -> Unit,
    onSelectTicket: (VisitorTicket) -> Unit
) {
    if (!visible) return

    var emailQuery by rememberSaveable { mutableStateOf("") }
    var selectedTypeId by rememberSaveable { mutableStateOf<String?>(null) }
    var dropdownExpanded by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        emailQuery = ""
        selectedTypeId = null
    }

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
                text = stringResource(R.string.scanner_search_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary
            )
            Text(
                text = stringResource(R.string.scanner_search_description),
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextSecondary
            )

            OutlinedTextField(
                value = emailQuery,
                onValueChange = { emailQuery = it },
                placeholder = { Text(stringResource(R.string.scanner_search_email_placeholder)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Ticket type dropdown
            ExposedDropdownMenuBox(
                expanded = dropdownExpanded,
                onExpandedChange = { dropdownExpanded = it }
            ) {
                val selectedName = ticketTypes.find { it.id == selectedTypeId }?.name
                    ?: stringResource(R.string.scanner_search_ticket_type_all)

                OutlinedTextField(
                    value = selectedName,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.scanner_search_ticket_type_all)) },
                        onClick = {
                            selectedTypeId = null
                            dropdownExpanded = false
                        }
                    )
                    ticketTypes.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.name) },
                            onClick = {
                                selectedTypeId = type.id
                                dropdownExpanded = false
                            }
                        )
                    }
                }
            }

            AppButton(
                text = stringResource(R.string.scanner_search_button),
                onClick = { onSearch(emailQuery, selectedTypeId) },
                enabled = emailQuery.isNotBlank() && !isSearching,
                variant = AppButtonVariant.Primary,
                modifier = Modifier.fillMaxWidth()
            )

            // Results
            when {
                isSearching -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            color = AppColors.Primary
                        )
                    }
                }
                searchError != null -> {
                    Text(
                        text = stringResource(R.string.scanner_search_error, searchError),
                        color = AppColors.TextError,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                searchResults.isEmpty() && emailQuery.isNotBlank() && !isSearching -> {
                    Text(
                        text = stringResource(R.string.scanner_search_no_results),
                        color = AppColors.TextSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
                searchResults.isNotEmpty() -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.height(300.dp)
                    ) {
                        items(searchResults, key = { it.id }) { ticket ->
                            TicketSearchResultRow(
                                ticket = ticket,
                                onClick = { onSelectTicket(ticket) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun TicketSearchResultRow(
    ticket: VisitorTicket,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = AppColors.CardBackground,
        tonalElevation = 1.dp,
        shadowElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                ticket.email?.let { email ->
                    Text(
                        text = email,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.TextPrimary
                    )
                }
                ticket.ticketType?.let { type ->
                    Text(
                        text = type,
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextSecondary
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            val statusText = when (ticket.status) {
                VisitorTicketStatus.SCANNED -> stringResource(R.string.scanner_search_result_scanned)
                VisitorTicketStatus.NOT_SCANNED -> stringResource(R.string.scanner_search_result_not_scanned)
                else -> ""
            }
            val statusColor = when (ticket.status) {
                VisitorTicketStatus.SCANNED -> AppColors.Info
                VisitorTicketStatus.NOT_SCANNED -> AppColors.Success
                else -> AppColors.TextMuted
            }
            Text(
                text = statusText,
                style = MaterialTheme.typography.labelSmall,
                color = statusColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
