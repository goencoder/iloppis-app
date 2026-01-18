package se.iloppis.app.ui.screens.cashier

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import se.iloppis.app.R
import se.iloppis.app.domain.model.Event
import se.iloppis.app.ui.dialogs.InvalidSellerDialog
import se.iloppis.app.ui.dialogs.ServerErrorDialog
import se.iloppis.app.ui.screens.pending.PendingPurchasesScreen
import se.iloppis.app.ui.screens.review.PurchaseReviewScreen
import se.iloppis.app.ui.theme.AppColors

/**
 * Main cashier screen with numeric keypad, transaction list, and payment options.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashierScreen(
    event: Event,
    apiKey: String,
    onBack: () -> Unit
) {
    val viewModel = remember(event.id, apiKey) {
        CashierViewModel(
            eventId = event.id,
            eventName = event.name,
            apiKey = apiKey
        )
    }
    val uiState = viewModel.uiState

    var showPendingInfoDialog by remember { mutableStateOf(false) }
    var showReviewScreen by remember { mutableStateOf(false) }
    var showDetailedReview by remember { mutableStateOf<String?>(null) }
    
    // Show detailed purchase review if requested
    if (showDetailedReview != null) {
        val detailedViewModel = remember(showDetailedReview, event.id, apiKey) {
            se.iloppis.app.ui.screens.review.DetailedPurchaseReviewViewModel(
                purchaseId = showDetailedReview!!,
                eventId = event.id,
                apiKey = apiKey
            )
        }
        se.iloppis.app.ui.screens.review.DetailedPurchaseReviewScreen(
            viewModel = detailedViewModel,
            onBack = { 
                showDetailedReview = null
                // Refresh rejected purchases list
                viewModel.refreshRejectedPurchasesCount()
            }
        )
        return
    }
    
    // Show pending purchases screen if requested
    if (showReviewScreen) {
        PendingPurchasesScreen(
            apiKey = apiKey,
            eventId = event.id,
            onNavigateBack = { showReviewScreen = false }
        )
        return
    }

    if (showPendingInfoDialog) {
        AlertDialog(
            onDismissRequest = { showPendingInfoDialog = false },
            confirmButton = {
                TextButton(onClick = { showPendingInfoDialog = false }) {
                    Text("OK")
                }
            },
            title = { Text(stringResource(R.string.cashier_pending_info_title)) },
            text = {
                Text(
                    stringResource(R.string.cashier_pending_info_message)
                )
            }
        )
    }
    
    // Server error dialog
    if (uiState.showServerErrorDialog) {
        ServerErrorDialog(
            pendingPurchasesCount = uiState.rejectedPurchasesCount,
            onDismiss = { viewModel.onAction(CashierAction.DismissServerErrorDialog) }
        )
    }
    
    // Invalid seller dialog
    if (uiState.showInvalidSellerDialog && uiState.invalidSellerDialogData != null) {
        InvalidSellerDialog(
            purchaseId = uiState.invalidSellerDialogData.purchaseId,
            timestamp = uiState.invalidSellerDialogData.timestamp,
            invalidSellers = uiState.invalidSellerDialogData.invalidSellers,
            onDismiss = { viewModel.onAction(CashierAction.DismissInvalidSellerDialog) },
            onReviewNow = { 
                viewModel.onAction(CashierAction.DismissInvalidSellerDialog)
                showReviewScreen = true
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(stringResource(R.string.cashier_title))
                        Text(
                            text = event.name,
                            fontSize = 12.sp,
                            color = AppColors.DialogBackground.copy(alpha = 0.8f)
                        )
                    }
                },
                actions = {
                    // Show rejected purchases badge
                    if (uiState.rejectedPurchasesCount > 0) {
                        TextButton(
                            onClick = { showReviewScreen = true }
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "⚠️",
                                    fontSize = 20.sp
                                )
                                Text(
                                    text = uiState.rejectedPurchasesCount.toString(),
                                    color = AppColors.DialogBackground,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    
                    if (uiState.pendingSoldItemsCount > 0) {
                        IconButton(onClick = { showPendingInfoDialog = true }) {
                            Icon(
                                imageVector = Icons.Filled.Info,
                                contentDescription = "Pending uploads info",
                                tint = AppColors.DialogBackground
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = uiState.pendingSoldItemsCount.toString(),
                            color = AppColors.DialogBackground,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                    } else if (uiState.isProcessingPayment) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = "Processing",
                            tint = AppColors.DialogBackground
                        )
                        Spacer(modifier = Modifier.width(12.dp))
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
                    containerColor = AppColors.ButtonSuccess,
                    titleContentColor = AppColors.DialogBackground,
                    navigationIconContentColor = AppColors.DialogBackground
                )
            )
        },
        snackbarHost = {
            // Show warnings and errors
            uiState.warningMessage?.let { message ->
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    action = {
                        TextButton(onClick = { viewModel.onAction(CashierAction.DismissWarning) }) {
                            Text("OK", color = Color.White)
                        }
                    },
                    containerColor = AppColors.Warning
                ) {
                    Text(message)
                }
            }
            uiState.errorMessage?.let { message ->
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    action = {
                        TextButton(onClick = { viewModel.onAction(CashierAction.DismissError) }) {
                            Text("OK", color = Color.White)
                        }
                    },
                    containerColor = AppColors.Error
                ) {
                    Text(message)
                }
            }
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(color = AppColors.Primary)
                    Text(
                        text = stringResource(R.string.cashier_loading),
                        color = AppColors.TextMuted
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                
                // Input fields for seller number and price
                InputSection(
                    sellerNumber = uiState.sellerNumber,
                    priceString = uiState.priceString,
                    activeField = uiState.activeField,
                    onSellerClick = { viewModel.onAction(CashierAction.SetActiveField(ActiveField.SELLER)) },
                    onPriceClick = { viewModel.onAction(CashierAction.SetActiveField(ActiveField.PRICE)) }
                )

                // Numeric keypad
                NumericKeypad(
                    onDigitPress = { viewModel.onAction(CashierAction.KeypadPress(it)) },
                    onClear = { viewModel.onAction(CashierAction.KeypadClear) },
                    onBackspace = { viewModel.onAction(CashierAction.KeypadBackspace) },
                    onSpace = { viewModel.onAction(CashierAction.KeypadSpace) },
                    onOk = { viewModel.onAction(CashierAction.KeypadOk) }
                )

                // Transaction list
                TransactionList(
                    transactions = uiState.transactions,
                    onRemoveItem = { viewModel.onAction(CashierAction.RemoveItem(it)) },
                    onClearAll = { viewModel.onAction(CashierAction.ClearAllItems) }
                )

                // Payment section
                PaymentSection(
                    total = uiState.total,
                    paidAmount = uiState.paidAmount,
                    change = uiState.change,
                    isProcessing = uiState.isProcessingPayment,
                    onPaidAmountChange = { viewModel.onAction(CashierAction.SetPaidAmount(it)) },
                    onCashPayment = { viewModel.onAction(CashierAction.Checkout(PaymentMethodType.CASH)) },
                    onSwishPayment = { viewModel.onAction(CashierAction.Checkout(PaymentMethodType.SWISH)) }
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun InputSection(
    sellerNumber: String,
    priceString: String,
    activeField: ActiveField,
    onSellerClick: () -> Unit,
    onPriceClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Seller number input
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.cashier_seller_label),
                fontSize = 12.sp,
                color = AppColors.TextMuted,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            InputField(
                value = sellerNumber,
                placeholder = "123",
                isActive = activeField == ActiveField.SELLER,
                onClick = onSellerClick
            )
        }

        // Price input
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.cashier_price_label),
                fontSize = 12.sp,
                color = AppColors.TextMuted,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            InputField(
                value = priceString,
                placeholder = "50 100",
                isActive = activeField == ActiveField.PRICE,
                onClick = onPriceClick
            )
        }
    }
}

@Composable
private fun InputField(
    value: String,
    placeholder: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isActive) AppColors.Primary else AppColors.Border
    val backgroundColor = if (isActive) AppColors.Primary.copy(alpha = 0.05f) else AppColors.CardBackground

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .border(
                width = if (isActive) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        if (value.isEmpty()) {
            Text(
                text = placeholder,
                color = AppColors.TextMuted.copy(alpha = 0.5f),
                fontSize = 16.sp
            )
        } else {
            Text(
                text = value,
                color = AppColors.TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
