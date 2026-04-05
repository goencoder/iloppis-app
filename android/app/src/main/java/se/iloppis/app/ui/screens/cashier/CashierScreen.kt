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
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import se.iloppis.app.R
import se.iloppis.app.domain.model.Event
import se.iloppis.app.ui.dialogs.InvalidSellerDialog
import se.iloppis.app.ui.dialogs.ServerErrorDialog
import se.iloppis.app.ui.screens.pending.PendingPurchasesScreen
import se.iloppis.app.ui.screens.review.PurchaseReviewScreen
import se.iloppis.app.ui.components.buttons.AppButton
import se.iloppis.app.ui.components.buttons.AppButtonSize
import se.iloppis.app.ui.components.buttons.AppButtonVariant
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
    val viewModel: CashierViewModel = viewModel(
        key = "cashier-${event.id}",
        factory = CashierViewModel.factory(
            eventId = event.id,
            eventName = event.name,
            apiKey = apiKey
        )
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showPendingInfoDialog by remember { mutableStateOf(false) }
    var showClosePendingDialog by remember { mutableStateOf(false) }
    var showReviewScreen by remember { mutableStateOf(false) }
    var showDetailedReview by remember { mutableStateOf<String?>(null) }
    var closeRequested by remember { mutableStateOf(false) }

    if (showClosePendingDialog) {
        AlertDialog(
            onDismissRequest = { showClosePendingDialog = false },
            confirmButton = {
                AppButton(
                    text = stringResource(R.string.cashier_close_pending_confirm),
                    onClick = {
                        showClosePendingDialog = false
                        closeRequested = true
                    },
                    variant = AppButtonVariant.Warning,
                    size = AppButtonSize.Small
                )
            },
            dismissButton = {
                AppButton(
                    text = stringResource(R.string.button_cancel),
                    onClick = { showClosePendingDialog = false },
                    variant = AppButtonVariant.Text,
                    size = AppButtonSize.Small
                )
            },
            title = { Text(stringResource(R.string.cashier_close_pending_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.cashier_close_pending_message,
                        uiState.pendingSoldItemsCount,
                    )
                )
            }
        )
    }

    // Show detailed purchase review if requested
    if (showDetailedReview != null) {
        val detailedViewModel: se.iloppis.app.ui.screens.review.DetailedPurchaseReviewViewModel = viewModel(
            key = "detailed-$showDetailedReview",
            factory = se.iloppis.app.ui.screens.review.DetailedPurchaseReviewViewModel.factory(
                purchaseId = showDetailedReview!!,
                eventId = event.id,
                apiKey = apiKey
            )
        )
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

    LaunchedEffect(closeRequested) {
        if (!closeRequested) return@LaunchedEffect
        val closeSucceeded = viewModel.requestCloseAndFlush()
        if (closeSucceeded) {
            onBack()
        } else {
            closeRequested = false
        }
    }

    BackHandler {
        if (closeRequested) {
            return@BackHandler
        }
        if (uiState.pendingSoldItemsCount > 0) {
            showClosePendingDialog = true
        } else {
            closeRequested = true
        }
    }

    if (showPendingInfoDialog) {
        AlertDialog(
            onDismissRequest = { showPendingInfoDialog = false },
            confirmButton = {
                AppButton(
                    text = stringResource(R.string.common_ok),
                    onClick = { showPendingInfoDialog = false },
                    variant = AppButtonVariant.Text,
                    size = AppButtonSize.Small
                )
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
    val dialogData = uiState.invalidSellerDialogData
    if (uiState.showInvalidSellerDialog && dialogData != null) {
        InvalidSellerDialog(
            purchaseId = dialogData.purchaseId,
            timestamp = dialogData.timestamp,
            invalidSellers = dialogData.invalidSellers,
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
                            color = AppColors.DialogBackground.copy(alpha = 0.8f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                actions = {
                    // Show rejected purchases badge
                    if (uiState.rejectedPurchasesCount > 0) {
                        AppButton(
                            text = uiState.rejectedPurchasesCount.toString(),
                            onClick = { showReviewScreen = true },
                            variant = AppButtonVariant.Text,
                            contentColor = AppColors.DialogBackground,
                            size = AppButtonSize.Small,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = stringResource(R.string.pending_purchases_title)
                                )
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    if (uiState.pendingSoldItemsCount > 0) {
                        IconButton(onClick = { showPendingInfoDialog = true }) {
                            Icon(
                                imageVector = Icons.Filled.Info,
                                contentDescription = stringResource(R.string.content_description_pending_info),
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
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (closeRequested) {
                            return@IconButton
                        }
                        if (uiState.pendingSoldItemsCount > 0) {
                            showClosePendingDialog = true
                        } else {
                            closeRequested = true
                        }
                    }) {
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
                        AppButton(
                            text = stringResource(R.string.common_ok),
                            onClick = { viewModel.onAction(CashierAction.DismissWarning) },
                            variant = AppButtonVariant.Text,
                            contentColor = AppColors.OnButtonPrimary,
                            size = AppButtonSize.Small
                        )
                    },
                    containerColor = AppColors.Warning
                ) {
                    Text(message.asString())
                }
            }
            uiState.errorMessage?.let { message ->
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    action = {
                        AppButton(
                            text = stringResource(R.string.common_ok),
                            onClick = { viewModel.onAction(CashierAction.DismissError) },
                            variant = AppButtonVariant.Text,
                            contentColor = AppColors.OnButtonPrimary,
                            size = AppButtonSize.Small
                        )
                    },
                    containerColor = AppColors.Error
                ) {
                    Text(message.asString())
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
