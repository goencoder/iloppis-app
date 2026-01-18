package se.iloppis.app.ui.screens.pending

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import se.iloppis.app.R
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Screen showing all pending purchases with expandable inline editing.
 * 
 * Features:
 * - Badge icon (ℹ️/⚠️/🔴) per purchase based on errorText severity
 * - Expandable list: click to expand/collapse items
 * - Inline editing: Ändra säljare, Ta bort, Radera hela köpet, Försök igen
 * - 5-second spinner on edit operations
 * - Auto-refresh via SharedFlow from PendingItemsStore
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PendingPurchasesScreen(
    apiKey: String,
    eventId: String,
    onNavigateBack: () -> Unit,
    viewModel: PendingPurchasesViewModel = remember(eventId) { PendingPurchasesViewModel(eventId) }
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.pending_purchases_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.button_back))
                    }
                }
            )
        }
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.purchases.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            stringResource(R.string.pending_purchases_empty),
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = uiState.purchases,
                        key = { it.purchaseId }
                    ) { purchase ->
                        PurchaseCard(
                            purchase = purchase,
                            isExpanded = purchase.purchaseId == uiState.expandedPurchaseId,
                            isProcessing = purchase.purchaseId == uiState.processingPurchaseId,
                            onToggleExpand = { viewModel.toggleExpanded(purchase.purchaseId) },
                            onChangeSeller = { itemId, newSeller ->
                                viewModel.changeSeller(purchase.purchaseId, itemId, newSeller)
                            },
                            onDeleteItem = { itemId ->
                                viewModel.deleteItem(purchase.purchaseId, itemId)
                            },
                            onDeletePurchase = {
                                viewModel.deletePurchase(purchase.purchaseId)
                            },
                            onRetry = {
                                viewModel.retryPurchase(purchase.purchaseId, apiKey, eventId, context)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PurchaseCard(
    purchase: PendingPurchaseUi,
    isExpanded: Boolean,
    isProcessing: Boolean,
    onToggleExpand: () -> Unit,
    onChangeSeller: (itemId: String, newSeller: Int) -> Unit,
    onDeleteItem: (itemId: String) -> Unit,
    onDeletePurchase: () -> Unit,
    onRetry: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = !isProcessing) { onToggleExpand() }
                .padding(16.dp)
        ) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Badge icon
                    Text(
                        text = when (purchase.severity) {
                            PurchaseSeverity.CRITICAL -> "🔴"
                            PurchaseSeverity.WARNING -> "⚠️"
                            PurchaseSeverity.INFO -> "ℹ️"
                        },
                        style = MaterialTheme.typography.headlineSmall
                    )
                    
                    Column {
                        Text(
                            text = pluralStringResource(R.plurals.pending_purchases_item_count, purchase.items.size, purchase.items.size),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = formatTimestamp(purchase.timestamp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                if (isProcessing) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Icon(
                        imageVector = if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Dölj" else "Visa"
                    )
                }
            }

            // Expanded content
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HorizontalDivider()

                    // Items list
                    purchase.items.forEach { item ->
                        ItemRow(
                            item = item,
                            onChangeSeller = { newSeller ->
                                onChangeSeller(item.itemId, newSeller)
                            },
                            onDeleteItem = { onDeleteItem(item.itemId) }
                        )
                    }

                    HorizontalDivider()

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onRetry,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.pending_purchases_button_retry))
                        }
                        
                        OutlinedButton(
                            onClick = onDeletePurchase,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.pending_purchases_button_delete_all))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ItemRow(
    item: PendingItemUi,
    onChangeSeller: (Int) -> Unit,
    onDeleteItem: () -> Unit
) {
    var showSellerDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (item.errorText.isNotBlank()) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.pending_purchases_seller_with_number, item.sellerId),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.pending_purchases_price_kr, item.price),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = { showSellerDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.pending_purchases_change_seller))
                    }
                    IconButton(onClick = onDeleteItem) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Ta bort",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            if (item.errorText.isNotBlank()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = item.errorText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }

    if (showSellerDialog) {
        ChangeSellerDialog(
            currentSeller = item.sellerId,
            onDismiss = { showSellerDialog = false },
            onConfirm = { newSeller ->
                onChangeSeller(newSeller)
                showSellerDialog = false
            }
        )
    }
}

@Composable
private fun ChangeSellerDialog(
    currentSeller: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var sellerInput by remember { mutableStateOf(currentSeller.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_change_seller_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.dialog_change_seller_current, currentSeller))
                OutlinedTextField(
                    value = sellerInput,
                    onValueChange = { sellerInput = it.filter { char -> char.isDigit() } },
                    label = { Text(stringResource(R.string.dialog_change_seller_new_label)) },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val newSeller = sellerInput.toIntOrNull()
                    if (newSeller != null && newSeller > 0) {
                        onConfirm(newSeller)
                    }
                },
                enabled = sellerInput.toIntOrNull()?.let { it > 0 } == true
            ) {
                Text(stringResource(R.string.dialog_change_seller_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.button_cancel))
            }
        }
    )
}

private fun formatTimestamp(isoTimestamp: String): String {
    return try {
        val instant = Instant.parse(isoTimestamp)
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.systemDefault())
        formatter.format(instant)
    } catch (e: Exception) {
        isoTimestamp
    }
}
