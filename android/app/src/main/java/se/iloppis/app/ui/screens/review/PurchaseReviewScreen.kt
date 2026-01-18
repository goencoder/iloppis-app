package se.iloppis.app.ui.screens.review

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import se.iloppis.app.R
import se.iloppis.app.data.models.RejectedPurchase
import se.iloppis.app.data.models.SerializableSoldItemErrorCode
import se.iloppis.app.ui.theme.AppColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Purchase Review Screen showing all rejected purchases that need attention.
 * 
 * Groups purchases by error type:
 * - Server errors (automatic retry, show status)
 * - Invalid sellers (needs manual review)
 * - Other validation errors
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseReviewScreen(
    rejectedPurchases: List<RejectedPurchase>,
    onBack: () -> Unit,
    onRetryPurchase: (String) -> Unit,
    onReviewPurchase: (String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.review_title)) },
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
        }
    ) { paddingValues ->
        if (rejectedPurchases.isEmpty()) {
            // Empty state
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
                        text = "✅",
                        fontSize = 48.sp
                    )
                    Text(
                        text = stringResource(R.string.review_empty),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = stringResource(R.string.review_all_uploaded),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            // Group purchases by error type
            // SERVER ERROR = needsManualReview=false (auto-retry enabled)
            // NEEDS REVIEW = needsManualReview=true (business logic errors)
            val serverErrors = rejectedPurchases.filter { !it.needsManualReview }
            val needsReview = rejectedPurchases.filter { it.needsManualReview }
            
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Server Errors Section (auto-retry)
                if (serverErrors.isNotEmpty()) {
                    item {
                        Text(
                            text = "🔴 SERVERFEL (${serverErrors.size} köp)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    
                    items(serverErrors) { purchase ->
                        PurchaseCard(
                            purchase = purchase,
                            type = PurchaseCardType.ServerError,
                            onRetry = null // Auto-retry, no manual button needed
                        )
                    }
                    
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                }
                
                // Needs Manual Review Section
                if (needsReview.isNotEmpty()) {
                    item {
                        Text(
                            text = "🟡 KRÄVER GRANSKNING (${needsReview.size} köp)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                    
                    items(needsReview) { purchase ->
                        PurchaseCard(
                            purchase = purchase,
                            type = PurchaseCardType.NeedsReview,
                            onReview = { onReviewPurchase(purchase.purchaseId) }
                        )
                    }
                }
            }
        }
    }
}

enum class PurchaseCardType {
    ServerError,
    NeedsReview
}

@Composable
private fun PurchaseCard(
    purchase: RejectedPurchase,
    type: PurchaseCardType,
    onRetry: (() -> Unit)? = null,
    onReview: (() -> Unit)? = null
) {
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val timestamp = try {
        java.time.Instant.parse(purchase.timestamp).toEpochMilli()
    } catch (e: Exception) {
        System.currentTimeMillis()
    }
    val timeString = timeFormat.format(Date(timestamp))
    
    val total = purchase.items.sumOf { it.item.price }
    val itemCount = purchase.items.size
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "📦 Köp ${purchase.purchaseId.takeLast(6)} - $timeString",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            
            // Details
            Text(
                text = "$itemCount artiklar, $total kr",
                style = MaterialTheme.typography.bodyMedium
            )
            
            // Error message
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = when (type) {
                    PurchaseCardType.ServerError -> MaterialTheme.colorScheme.errorContainer
                    PurchaseCardType.NeedsReview -> MaterialTheme.colorScheme.tertiaryContainer
                },
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = when (type) {
                        PurchaseCardType.ServerError -> "❌ ${purchase.errorMessage}\nSynkar automatiskt när servern svarar."
                        PurchaseCardType.NeedsReview -> {
                            // Show item-specific errors
                            val errorsWithItems = purchase.items
                                .filter { !it.isCollateralDamage }
                                .map { "Säljare ${it.item.seller}: ${it.reason}" }
                            
                            if (errorsWithItems.isNotEmpty()) {
                                "❌ " + errorsWithItems.joinToString("\n")
                            } else {
                                "❌ ${purchase.errorMessage}"
                            }
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(8.dp)
                )
            }
            
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                when (type) {
                    PurchaseCardType.ServerError -> {
                        // No button - auto-retry
                        Text(
                            text = "Väntar på server...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    PurchaseCardType.NeedsReview -> {
                        if (onReview != null) {
                            Button(onClick = onReview) {
                                Text(stringResource(R.string.review_navigate_arrow))
                            }
                        }
                    }
                }
            }
        }
    }
}
