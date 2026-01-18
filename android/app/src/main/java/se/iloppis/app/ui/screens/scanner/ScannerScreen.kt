package se.iloppis.app.ui.screens.scanner

import android.Manifest
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import kotlinx.coroutines.delay
import se.iloppis.app.R
import se.iloppis.app.domain.model.Event
import se.iloppis.app.ui.components.CameraScanner
import se.iloppis.app.ui.dialogs.ManualTicketDialog
import se.iloppis.app.ui.theme.AppColors
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val svLocale = Locale.Builder().setLanguage("sv").setRegion("SE").build()
private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss", svLocale)
    .withZone(ZoneId.systemDefault())
private val windowFormatter = DateTimeFormatter.ofPattern("d MMM HH:mm", svLocale)
    .withZone(ZoneId.systemDefault())

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun ScannerScreen(
    event: Event,
    apiKey: String,
    onBack: () -> Unit
) {
    val viewModel = remember(event.id, apiKey) {
        ScannerViewModel(
            eventId = event.id,
            eventName = event.name,
            apiKey = apiKey
        )
    }
    val uiState = viewModel.uiState
    
    // Camera permission state
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    var isScanningActive by remember { mutableStateOf(true) } // Always active for continuous scanning

    // Auto-dismiss after 5 blinks (1500ms)
    uiState.activeResult?.let { result ->
        LaunchedEffect(result) {
            delay(1500)
            viewModel.onAction(ScannerAction.DismissResult)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.scanner_title))
                        Text(
                            text = event.name,
                            fontSize = 12.sp,
                            color = AppColors.DialogBackground.copy(alpha = 0.8f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.button_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppColors.Primary,
                    titleContentColor = AppColors.DialogBackground,
                    navigationIconContentColor = AppColors.DialogBackground
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (uiState.isProcessing) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = AppColors.Primary
                )
            }

            if (uiState.pendingCount > 0) {
                OfflineBanner(pendingCount = uiState.pendingCount)
            }

            // Current group card
            if (uiState.currentGroupCount > 0) {
                CurrentGroupCard(
                    groupEmail = uiState.currentGroupEmail,
                    groupTicketType = uiState.currentGroupTicketType,
                    groupCount = uiState.currentGroupCount,
                    groupTickets = uiState.currentGroup,
                    isExpanded = uiState.isGroupExpanded,
                    onToggleExpanded = { viewModel.onAction(ScannerAction.ToggleGroupExpanded) },
                    onCommitGroup = { viewModel.onAction(ScannerAction.CommitCurrentGroup) },
                    onRemoveTicket = { ticketId -> viewModel.onAction(ScannerAction.RemoveFromGroup(ticketId)) }
                )
            }

            // Camera preview area
            ScannerPreview(
                uiState = uiState,
                cameraPermissionGranted = cameraPermissionState.status.isGranted,
                isScanningActive = isScanningActive,
                onBarcodeScanned = { rawValue ->
                    if (isScanningActive) {
                        viewModel.onAction(ScannerAction.SubmitCode(rawValue))
                    }
                    false // Continue scanning (don't stop camera)
                },
                onDismissResult = { viewModel.onAction(ScannerAction.DismissResult) }
            )

            // Action buttons
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when {
                    cameraPermissionState.status.isGranted -> {
                        // Camera permission granted - show scan toggle
                        Button(
                            onClick = { isScanningActive = !isScanningActive },
                            enabled = !uiState.isProcessing,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isScanningActive) AppColors.Warning else AppColors.Primary
                            )
                        ) {
                            Text(
                                if (isScanningActive) 
                                    stringResource(R.string.scanner_button_pause) 
                                else 
                                    stringResource(R.string.scanner_button_scan)
                            )
                        }
                    }
                    cameraPermissionState.status.shouldShowRationale -> {
                        // Permission denied but can ask again
                        Button(
                            onClick = { cameraPermissionState.launchPermissionRequest() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary)
                        ) {
                            Text(stringResource(R.string.scanner_button_grant_permission))
                        }
                        Text(
                            text = stringResource(R.string.scanner_permission_rationale),
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.TextSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    else -> {
                        // First time or permission permanently denied
                        Button(
                            onClick = { cameraPermissionState.launchPermissionRequest() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary)
                        ) {
                            Text(stringResource(R.string.scanner_button_enable_camera))
                        }
                    }
                }

                // Manual entry is always available as fallback
                OutlinedButton(
                    onClick = { 
                        viewModel.onAction(ScannerAction.RequestManualEntry) 
                    },
                    enabled = !uiState.isProcessing,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = AppColors.Primary
                    )
                ) {
                    Text(stringResource(R.string.scanner_button_manual_entry))
                }
            }

            // Total scans counter
            if (uiState.totalScansCount > 0) {
                Surface(
                    color = AppColors.CardBackground,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.scanner_total_scans_label),
                            style = MaterialTheme.typography.bodyLarge,
                            color = AppColors.TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = uiState.totalScansCount.toString(),
                            style = MaterialTheme.typography.headlineSmall,
                            color = AppColors.Primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Scan history
            ScanHistory(
                groupedHistory = uiState.groupedHistory,
                hasMoreHistory = uiState.hasMoreHistory,
                isLoadingHistory = uiState.isLoadingHistory,
                showErrorScans = uiState.showErrorScans,
                onItemClick = { result ->
                    viewModel.onAction(ScannerAction.ShowTicketDetails(result))
                },
                onLoadMore = { viewModel.onAction(ScannerAction.LoadMoreHistory) },
                onToggleErrors = { viewModel.onAction(ScannerAction.ToggleErrorScans) }
            )
        }
    }

    ManualTicketDialog(
        visible = uiState.manualEntryVisible,
        eventName = event.name,
        error = uiState.manualEntryError,
        isProcessing = uiState.isProcessing,
        onDismiss = { viewModel.onAction(ScannerAction.DismissManualEntry) },
        onTextChanged = { viewModel.onAction(ScannerAction.ClearManualError) },
        onSubmit = { code -> viewModel.onAction(ScannerAction.SubmitCode(code)) }
    )

    // Ticket details dialog
    uiState.ticketDetailsResult?.let { result ->
        TicketDetailsDialog(
            result = result,
            eventName = event.name,
            onDismiss = { viewModel.onAction(ScannerAction.DismissTicketDetails) }
        )
    }
}

@Composable
private fun ScannerPreview(
    uiState: ScannerUiState,
    cameraPermissionGranted: Boolean,
    isScanningActive: Boolean,
    onBarcodeScanned: (String) -> Boolean,
    onDismissResult: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(AppColors.CardBackground)
    ) {
        if (cameraPermissionGranted && isScanningActive) {
            // Show live camera feed only when scanning is active
            CameraScanner(
                onBarcodeScanned = onBarcodeScanned,
                modifier = Modifier.fillMaxSize()
            )
        } else if (cameraPermissionGranted && !isScanningActive) {
            // Show paused state
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "⏸",
                    fontSize = 40.sp
                )
                Text(
                    text = stringResource(R.string.scanner_paused),
                    color = AppColors.TextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            }
        } else {
            // Show placeholder when camera is not active
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.scanner_preview_icon),
                    fontSize = 40.sp
                )
                Text(
                    text = if (cameraPermissionGranted) 
                        stringResource(R.string.scanner_tap_to_scan)
                    else 
                        stringResource(R.string.scanner_permission_needed),
                    color = AppColors.TextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            }
        }

        // Blinking border feedback (always on top)
        uiState.activeResult?.let { result ->
            val borderColor = when (result.status) {
                ScanStatus.SUCCESS, ScanStatus.OFFLINE_SUCCESS -> AppColors.Success
                else -> AppColors.Error
            }
            BlinkingBorder(
                color = borderColor,
                modifier = Modifier.fillMaxSize()
            )
            
            // Show group position number in center
            if (result.status == ScanStatus.SUCCESS || result.status == ScanStatus.OFFLINE_SUCCESS) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${uiState.currentGroupCount}",
                        fontSize = 120.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.Success.copy(alpha = 0.75f)
                    )
                }
            }
        }
    }
}

@Composable
private fun BlinkingBorder(
    color: Color,
    modifier: Modifier = Modifier
) {
    var isVisible by remember { mutableStateOf(true) }
    
    LaunchedEffect(Unit) {
        // Blink 5 times: on-off-on-off-on-off-on-off-on-off (150ms each)
        repeat(5) {
            isVisible = true
            delay(150)
            isVisible = false
            delay(150)
        }
    }
    
    if (isVisible) {
        Box(
            modifier = modifier
                .border(16.dp, color, RoundedCornerShape(24.dp))
        )
    }
}

@Composable
private fun OfflineBanner(pendingCount: Int) {
    Surface(
        color = AppColors.Warning,
        contentColor = AppColors.DialogBackground,
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 0.dp,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = pluralStringResource(R.plurals.scanner_offline_banner, pendingCount, pendingCount),
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ResultSheet(
    result: ScanResult,
    eventName: String,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit
) {
    val (containerColor, titleRes, defaultMessageRes) = when (result.status) {
        ScanStatus.SUCCESS -> Triple(AppColors.Success, R.string.scanner_result_title_success, R.string.scanner_result_message_success)
        ScanStatus.DUPLICATE -> Triple(AppColors.Warning, R.string.scanner_result_title_duplicate, R.string.scanner_result_message_duplicate)
        ScanStatus.INVALID -> Triple(AppColors.Error, R.string.scanner_result_title_invalid, R.string.scanner_result_message_invalid)
        ScanStatus.OFFLINE -> Triple(AppColors.Warning, R.string.scanner_result_title_offline, R.string.scanner_result_message_offline)
        ScanStatus.OFFLINE_SUCCESS -> Triple(AppColors.Success, R.string.scanner_result_title_offline, R.string.scanner_result_message_offline)
        ScanStatus.ERROR -> Triple(AppColors.Error, R.string.scanner_result_title_error, R.string.scanner_result_message_error)
    }

    Surface(
        color = containerColor,
        contentColor = AppColors.DialogBackground,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 0.dp,
        shadowElevation = 6.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(titleRes),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.scanner_button_close_result),
                        tint = AppColors.DialogBackground
                    )
                }
            }

            val message = result.message ?: stringResource(defaultMessageRes)
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium
            )

            result.ticket?.let { ticket ->
                val details = listOfNotNull(
                    ticket.ticketType?.let { type ->
                        stringResource(R.string.scanner_field_ticket_type, type)
                    },
                    ticket.email?.takeIf { it.isNotBlank() }?.let { email ->
                        stringResource(R.string.scanner_field_email, email)
                    },
                    ticket.scannedAt?.let { scanned ->
                        stringResource(R.string.scanner_field_scanned_at, timeFormatter.format(scanned))
                    },
                    formatValidWindow(ticket)?.let { window ->
                        stringResource(R.string.scanner_field_valid_window, window)
                    },
                    stringResource(R.string.scanner_field_event, eventName)
                )
                if (details.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        details.forEach { line ->
                            Text(text = line, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScanHistory(
    groupedHistory: List<HistoryItem>,
    hasMoreHistory: Boolean,
    isLoadingHistory: Boolean,
    showErrorScans: Boolean,
    onItemClick: (ScanResult) -> Unit,
    onLoadMore: () -> Unit,
    onToggleErrors: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.scanner_history_title),
                style = MaterialTheme.typography.titleMedium,
                color = AppColors.TextPrimary
            )
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.clickable(onClick = onToggleErrors)
            ) {
                Checkbox(
                    checked = showErrorScans,
                    onCheckedChange = { onToggleErrors() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = AppColors.Primary,
                        uncheckedColor = AppColors.TextSecondary
                    )
                )
                Text(
                    text = stringResource(R.string.scanner_history_show_errors),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextSecondary
                )
            }
        }

        if (groupedHistory.isEmpty()) {
            Text(
                text = stringResource(R.string.scanner_history_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextMuted
            )
        } else {
            // Use LazyColumn for scrollable history
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(groupedHistory.size) { index ->
                    val item = groupedHistory[index] as HistoryItem.Group
                    // All items are now groups (even single scans)
                    HistoryGroupRow(
                        email = item.email,
                        ticketType = item.ticketType,
                        count = item.count,
                        successCount = item.successCount,
                        errorCount = item.errorCount,
                        hasErrors = item.hasErrors,
                        timestamp = item.timestamp,
                        scans = item.scans,
                        onItemClick = onItemClick
                    )
                }
                
                // Load more button
                if (hasMoreHistory) {
                    item {
                        Button(
                            onClick = onLoadMore,
                            enabled = !isLoadingHistory,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AppColors.Primary.copy(alpha = 0.1f),
                                contentColor = AppColors.Primary
                            )
                        ) {
                            if (isLoadingHistory) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = AppColors.Primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(stringResource(R.string.scanner_history_load_more))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryGroupRow(
    email: String?,
    ticketType: String?,
    count: Int,
    successCount: Int,
    errorCount: Int,
    hasErrors: Boolean,
    timestamp: java.time.Instant,
    scans: List<ScanResult>,
    onItemClick: (ScanResult) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Group header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .background(
                    if (hasErrors) {
                        AppColors.Error.copy(alpha = 0.1f)
                    } else {
                        AppColors.Success.copy(alpha = 0.1f)
                    },
                    RoundedCornerShape(8.dp)
                )
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(
                        if (hasErrors) AppColors.Error else AppColors.Success,
                        CircleShape
                    )
            )

            Column(modifier = Modifier.weight(1f)) {
                val emailText = email ?: stringResource(R.string.scanner_group_no_email)
                val groupLabel = if (ticketType != null) {
                    "$emailText, $ticketType"
                } else {
                    emailText
                }
                
                // Show count breakdown if there are errors
                val countText = if (hasErrors) {
                    "($successCount ok, $errorCount fel)"
                } else {
                    "($count)"
                }
                
                Text(
                    text = "$groupLabel $countText",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = timeFormatter.format(timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextSecondary
                )
            }
            
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = AppColors.TextSecondary
            )
        }
        
        // Expanded individual scans
        if (isExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                scans.forEach { scan ->
                    HistoryRow(
                        result = scan,
                        onClick = { onItemClick(scan) }
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(
    result: ScanResult,
    onClick: () -> Unit
) {
    val indicatorColor = when (result.status) {
        ScanStatus.SUCCESS -> AppColors.Success
        ScanStatus.DUPLICATE -> AppColors.Warning
        ScanStatus.INVALID -> AppColors.Error
        ScanStatus.OFFLINE -> AppColors.Warning
        ScanStatus.OFFLINE_SUCCESS -> AppColors.Success
        ScanStatus.ERROR -> AppColors.Error
    }

    val titleRes = when (result.status) {
        ScanStatus.SUCCESS -> R.string.scanner_result_title_success
        ScanStatus.DUPLICATE -> R.string.scanner_result_title_duplicate
        ScanStatus.INVALID -> R.string.scanner_result_title_invalid
        ScanStatus.OFFLINE -> R.string.scanner_result_title_offline
        ScanStatus.OFFLINE_SUCCESS -> R.string.scanner_result_title_offline
        ScanStatus.ERROR -> R.string.scanner_result_title_error
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(indicatorColor, CircleShape)
        )

        Column(modifier = Modifier.fillMaxWidth()) {
            // Show ticket type if available, otherwise show title
            val displayText = result.ticket?.ticketType ?: result.ticket?.id ?: stringResource(titleRes)
            Text(
                text = displayText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            val detailParts = buildList {
                add(timeFormatter.format(result.timestamp))
                result.ticket?.email?.takeIf { it.isNotBlank() }?.let { add(it) }
                result.message?.takeIf { it.isNotBlank() }?.let { add(it) }
            }

            Text(
                text = detailParts.joinToString(separator = " • "),
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun formatValidWindow(ticket: se.iloppis.app.domain.model.VisitorTicket): String? {
    val start = ticket.validFrom?.let { windowFormatter.format(it) }
    val end = ticket.validUntil?.let { windowFormatter.format(it) }
    return when {
        start != null && end != null -> "$start – $end"
        start != null -> start
        end != null -> end
        else -> null
    }
}

@Composable
private fun TicketDetailsDialog(
    result: ScanResult,
    eventName: String,
    onDismiss: () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = AppColors.DialogBackground,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.scanner_ticket_details_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.scanner_button_close_result),
                            tint = AppColors.TextSecondary
                        )
                    }
                }

                result.ticket?.let { ticket ->
                    // Ticket type
                    ticket.ticketType?.let { type ->
                        DetailRow(
                            label = stringResource(R.string.scanner_field_ticket_type_label),
                            value = type
                        )
                    }

                    // Email
                    ticket.email?.takeIf { it.isNotBlank() }?.let { email ->
                        DetailRow(
                            label = stringResource(R.string.scanner_field_email_label),
                            value = email
                        )
                    }

                    // Status
                    ticket.status.let { status ->
                        DetailRow(
                            label = stringResource(R.string.scanner_field_status_label),
                            value = status.name
                        )
                    }

                    // Scanned at
                    ticket.scannedAt?.let { scanned ->
                        DetailRow(
                            label = stringResource(R.string.scanner_field_scanned_at_label),
                            value = timeFormatter.format(scanned)
                        )
                    }

                    // Valid window
                    formatValidWindow(ticket)?.let { window ->
                        DetailRow(
                            label = stringResource(R.string.scanner_field_valid_window_label),
                            value = window
                        )
                    }

                    // Event
                    DetailRow(
                        label = stringResource(R.string.scanner_field_event_label),
                        value = eventName
                    )

                    // Ticket ID
                    DetailRow(
                        label = stringResource(R.string.scanner_field_ticket_id_label),
                        value = ticket.id
                    )
                } ?: run {
                    // No ticket info available
                    result.message?.let { msg ->
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppColors.TextSecondary
                        )
                    }
                }

                // Close button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.Primary
                    )
                ) {
                    Text(stringResource(R.string.scanner_button_close))
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = AppColors.TextSecondary,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = AppColors.TextPrimary
        )
    }
}

@Composable
private fun CurrentGroupCard(
    groupEmail: String?,
    groupTicketType: String?,
    groupCount: Int,
    groupTickets: List<ScanResult>,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    onCommitGroup: () -> Unit,
    onRemoveTicket: (String) -> Unit
) {
    Surface(
        color = AppColors.Primary,
        contentColor = AppColors.DialogBackground,
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 4.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggleExpanded)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header with email and ticket type
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    val emailText = groupEmail ?: stringResource(R.string.scanner_group_no_email)
                    val groupLabel = if (groupTicketType != null) {
                        "$emailText - $groupTicketType"
                    } else {
                        emailText
                    }
                    Text(
                        text = groupLabel,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    // Show ticket type summary if available
                    val ticketTypeSummary = groupTickets
                        .mapNotNull { it.ticket?.ticketType }
                        .groupingBy { it }
                        .eachCount()
                        .entries
                        .joinToString(", ") { "${it.key} (${it.value})" }
                    
                    if (ticketTypeSummary.isNotEmpty()) {
                        Text(
                            text = ticketTypeSummary,
                            fontSize = 14.sp,
                            color = AppColors.DialogBackground.copy(alpha = 0.9f)
                        )
                    }
                }
                
                Text(
                    text = if (isExpanded) "▲" else "▼",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            // Expanded content
            if (isExpanded) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // List all tickets in the group
                    groupTickets.forEach { result ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    AppColors.DialogBackground.copy(alpha = 0.1f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = result.ticket?.ticketType ?: stringResource(R.string.scanner_group_unknown_type),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = timeFormatter.format(result.timestamp),
                                    fontSize = 12.sp,
                                    color = AppColors.DialogBackground.copy(alpha = 0.7f)
                                )
                            }
                            
                            // Remove button
                            IconButton(
                                onClick = { result.ticket?.id?.let { onRemoveTicket(it) } },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.scanner_group_remove_ticket),
                                    tint = AppColors.DialogBackground
                                )
                            }
                        }
                    }
                    
                    // Commit button
                    Button(
                        onClick = onCommitGroup,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.Success,
                            contentColor = AppColors.DialogBackground
                        )
                    ) {
                        Text(stringResource(R.string.scanner_group_commit_button))
                    }
                }
            }
        }
    }
}
