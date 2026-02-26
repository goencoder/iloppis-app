package se.iloppis.app.ui.screens.review

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import se.iloppis.app.R
import se.iloppis.app.data.models.RejectedItemWithDetails
import se.iloppis.app.ui.components.buttons.AppButton
import se.iloppis.app.ui.components.buttons.AppButtonSize
import se.iloppis.app.ui.components.buttons.AppButtonVariant
import se.iloppis.app.ui.theme.AppColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Detailed purchase review screen showing individual items with edit/remove actions.
 * 
 * Allows user to:
 * - View all items in a rejected purchase with individual error messages
 * - Edit seller numbers on specific items
 * - Remove items from the purchase
 * - Delete the entire purchase
 * - Retry upload with modified data
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailedPurchaseReviewScreen(
    viewModel: DetailedPurchaseReviewViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    // Handle navigation on success/deletion
    LaunchedEffect(uiState.uploadSuccess, uiState.purchaseDeleted) {
        if (uiState.uploadSuccess || uiState.purchaseDeleted) {
            onBack()
        }
    }

    // Show success message
    if (uiState.successMessage != null) {
        LaunchedEffect(uiState.successMessage) {
            kotlinx.coroutines.delay(2000)
            onBack()
        }
    }

    // Seller editor dialog
    val editingIndex = uiState.editingItemIndex
    if (uiState.showSellerEditor && editingIndex != null) {
        SellerEditorDialog(
            currentSeller = uiState.editingSellerNumber,
            validSellers = uiState.validSellers,
            onSellerChange = { newSeller ->
                viewModel.onAction(DetailedPurchaseAction.EditSeller(
                    itemIndex = editingIndex,
                    newSeller = newSeller
                ))
            },
            onDismiss = {
                viewModel.onAction(DetailedPurchaseAction.CloseSellerEditor)
            }
        )
    }

    // Delete confirmation dialog
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text(stringResource(R.string.detailed_review_delete_confirmation_title)) },
            text = { 
                Text(stringResource(R.string.detailed_review_delete_confirmation_message))
            },
            confirmButton = {
                AppButton(
                    text = stringResource(R.string.detailed_review_delete_confirm),
                    onClick = {
                        showDeleteConfirmation = false
                        viewModel.onAction(DetailedPurchaseAction.DeletePurchase)
                    },
                    variant = AppButtonVariant.Text,
                    contentColor = AppColors.Error,
                    size = AppButtonSize.Small
                )
            },
            dismissButton = {
                AppButton(
                    text = stringResource(R.string.button_cancel),
                    onClick = { showDeleteConfirmation = false },
                    variant = AppButtonVariant.Text,
                    size = AppButtonSize.Small
                )
            }
        )
    }

    // Error dialog
    val currentError = uiState.error
    if (currentError != null) {
        AlertDialog(
            onDismissRequest = { viewModel.onAction(DetailedPurchaseAction.DismissError) },
            title = { Text(stringResource(R.string.common_error_title)) },
            text = { Text(currentError.asString()) },
            confirmButton = {
                AppButton(
                    text = stringResource(R.string.common_ok),
                    onClick = { viewModel.onAction(DetailedPurchaseAction.DismissError) },
                    variant = AppButtonVariant.Text,
                    size = AppButtonSize.Small
                )
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    val currentPurchase = uiState.purchase
                    Text(
                        text = if (currentPurchase != null) {
                            stringResource(R.string.detailed_review_title_with_id, currentPurchase.purchaseId.takeLast(6))
                        } else {
                            stringResource(R.string.detailed_review_title_generic)
                        }
                    )
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
                    containerColor = AppColors.Background
                )
            )
        },
        bottomBar = {
            if (!uiState.isLoading && !uiState.purchaseNotFound) {
                BottomAppBar(
                    containerColor = AppColors.CardBackground,
                    tonalElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        AppButton(
                            text = stringResource(R.string.detailed_review_button_delete_purchase),
                            onClick = { showDeleteConfirmation = true },
                            variant = AppButtonVariant.Danger,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )

                        AppButton(
                            text = if (uiState.isUploading) {
                                stringResource(R.string.detailed_review_button_retry_uploading)
                            } else {
                                stringResource(R.string.detailed_review_button_retry)
                            },
                            onClick = { viewModel.onAction(DetailedPurchaseAction.RetryUpload) },
                            enabled = !uiState.isUploading,
                            loading = uiState.isUploading,
                            variant = AppButtonVariant.Primary
                        )
                    }
                }
            }
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
            uiState.purchaseNotFound -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.icon_error),
                            fontSize = 48.sp
                        )
                        Text(
                            text = stringResource(R.string.detailed_review_not_found),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
            else -> {
                val purchase = uiState.purchase
                if (purchase != null) {
                    PurchaseDetailContent(
                        purchase = purchase,
                        items = uiState.items,
                        validSellers = uiState.validSellers,
                        hasUnsavedChanges = uiState.hasUnsavedChanges,
                        onEditSeller = { index ->
                            viewModel.onAction(DetailedPurchaseAction.OpenSellerEditor(index))
                        },
                        onRemoveItem = { index ->
                            viewModel.onAction(DetailedPurchaseAction.RemoveItem(index))
                        },
                        modifier = Modifier.padding(paddingValues)
                    )
                }
            }
        }
    }
}

@Composable
private fun PurchaseDetailContent(
    purchase: se.iloppis.app.data.models.RejectedPurchase,
    items: List<RejectedItemWithDetails>,
    validSellers: Set<Int>,
    hasUnsavedChanges: Boolean,
    onEditSeller: (Int) -> Unit,
    onRemoveItem: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val timestamp = try {
        java.time.Instant.parse(purchase.timestamp).toEpochMilli()
    } catch (e: Exception) {
        System.currentTimeMillis()
    }
    val timeString = timeFormat.format(Date(timestamp))
    val total = items.sumOf { it.item.price }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header with purchase info
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = AppColors.BadgeUpcomingBackground
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.detailed_review_purchase_info, purchase.purchaseId.takeLast(6), timeString),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = pluralStringResource(R.plurals.detailed_review_purchase_summary, items.size, items.size, total),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (hasUnsavedChanges) {
                        Text(
                            text = stringResource(R.string.detailed_review_unsaved_changes),
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Items list
        item {
            Text(
                text = stringResource(R.string.detailed_review_items_header),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        itemsIndexed(items) { index, rejectedItem ->
            ItemCard(
                itemIndex = index + 1,
                rejectedItem = rejectedItem,
                validSellers = validSellers,
                canRemove = items.size > 1,
                onEditSeller = { onEditSeller(index) },
                onRemoveItem = { onRemoveItem(index) }
            )
        }
    }
}

@Composable
private fun ItemCard(
    itemIndex: Int,
    rejectedItem: RejectedItemWithDetails,
    validSellers: Set<Int>,
    canRemove: Boolean,
    onEditSeller: () -> Unit,
    onRemoveItem: () -> Unit
) {
    val item = rejectedItem.item
    val isSellerValid = validSellers.isEmpty() || item.seller in validSellers
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.detailed_review_item_number, itemIndex) + 
                        if (rejectedItem.isCollateralDamage) " " + stringResource(R.string.detailed_review_item_collateral) else "",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            // Item details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    val statusIcon = stringResource(
                        if (isSellerValid) R.string.icon_success else R.string.icon_error
                    )
                    Text(
                        text = stringResource(
                            R.string.detailed_review_seller_with_status,
                            item.seller,
                            statusIcon
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = stringResource(R.string.detailed_review_price, item.price),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Error message
            if (rejectedItem.hasPrimaryError) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = AppColors.ErrorContainer,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.review_error_prefix, rejectedItem.reason),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(8.dp),
                        color = AppColors.OnErrorContainer
                    )
                }
            } else if (rejectedItem.isCollateralDamage) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = AppColors.SurfaceVariant,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.detailed_review_collateral_message),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppButton(
                    text = stringResource(R.string.detailed_review_button_change_seller),
                    onClick = onEditSeller,
                    modifier = Modifier.weight(1f),
                    variant = AppButtonVariant.Outlined,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )

                if (canRemove) {
                    AppButton(
                        text = stringResource(R.string.detailed_review_button_remove),
                        onClick = onRemoveItem,
                        modifier = Modifier.weight(1f),
                        variant = AppButtonVariant.Danger,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SellerEditorDialog(
    currentSeller: String,
    validSellers: Set<Int>,
    onSellerChange: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var sellerInput by remember { mutableStateOf(currentSeller) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_seller_editor_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = sellerInput,
                    onValueChange = { sellerInput = it },
                    label = { Text(stringResource(R.string.dialog_seller_editor_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (validSellers.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.dialog_seller_editor_valid_sellers, 
                            validSellers.sorted().take(10).joinToString(", ") + if (validSellers.size > 10) "..." else ""),
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextSecondary
                    )
                }
            }
        },
        confirmButton = {
            AppButton(
                text = stringResource(R.string.dialog_seller_editor_save),
                onClick = { onSellerChange(sellerInput) },
                variant = AppButtonVariant.Primary
            )
        },
        dismissButton = {
            AppButton(
                text = stringResource(R.string.button_cancel),
                onClick = onDismiss,
                variant = AppButtonVariant.Text,
                size = AppButtonSize.Small
            )
        }
    )
}
