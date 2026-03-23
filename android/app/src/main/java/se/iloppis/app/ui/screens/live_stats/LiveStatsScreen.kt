package se.iloppis.app.ui.screens.live_stats

import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import coil.compose.AsyncImage
import se.iloppis.app.R
import se.iloppis.app.domain.model.Event
import se.iloppis.app.network.stats.LiveCashierStatus
import se.iloppis.app.network.stats.LiveStatsApiResponse
import se.iloppis.app.ui.components.buttons.AppButton
import se.iloppis.app.ui.components.buttons.AppButtonVariant
import se.iloppis.app.ui.theme.AppColors
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.Locale

private val svLocale = Locale.Builder().setLanguage("sv").setRegion("SE").build()
private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", svLocale)
    .withZone(ZoneId.systemDefault())

@Composable
fun LiveStatsScreen(
    event: Event,
    apiKey: String,
    isActivePage: Boolean = true
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val viewModel: LiveStatsViewModel = viewModel(
        key = liveStatsViewModelKey(event.id, apiKey),
        factory = LiveStatsViewModel.factory(event.id, apiKey)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snapshot = uiState.snapshot

    DisposableEffect(viewModel, lifecycleOwner, isActivePage) {
        if (!isActivePage) {
            viewModel.onScreenStopped()
            onDispose { }
        } else {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_START -> viewModel.onScreenStarted()
                    Lifecycle.Event.ON_STOP -> viewModel.onScreenStopped()
                    else -> Unit
                }
            }

            lifecycleOwner.lifecycle.addObserver(observer)

            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
                viewModel.onScreenStopped()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
    ) {
        when {
            uiState.isLoading && snapshot == null -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = AppColors.Primary
                )
            }

            snapshot == null -> {
                EmptyState(
                    errorKey = uiState.errorKey,
                    onRetry = viewModel::retry,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            else -> {
                LiveStatsContent(
                    event = event,
                    snapshot = snapshot,
                    errorKey = uiState.errorKey
                )
            }
        }
    }
}

@Composable
private fun LiveStatsContent(
    event: Event,
    snapshot: LiveStatsApiResponse,
    errorKey: String?
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val compactLandscape = isLandscape && configuration.screenHeightDp <= 500
    val cashiers = snapshot.cashierStatuses.orEmpty()
    val cashierRows = remember(cashiers) { buildCashierRows(cashiers) }
    val cashierCount = if (cashiers.isNotEmpty()) {
        cashiers.size
    } else {
        (snapshot.cashiers?.openCount ?: 0) +
            (snapshot.cashiers?.processingCount ?: 0) +
            (snapshot.cashiers?.stalledCount ?: 0)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = if (compactLandscape) 32.dp else 16.dp,
            end = if (compactLandscape) 32.dp else 16.dp,
            top = if (compactLandscape) 20.dp else 72.dp,
            bottom = if (compactLandscape) 14.dp else 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(if (compactLandscape) 10.dp else 16.dp)
    ) {
        item {
            SummaryCard(
                event = event,
                eventName = snapshot.eventName?.takeIf { it.isNotBlank() } ?: event.name,
                eventCity = snapshot.eventCity?.takeIf { it.isNotBlank() } ?: event.addressCity.orEmpty(),
                eventImageUrl = snapshot.eventImageUrl,
                generatedAt = snapshot.generatedAt,
                isStale = errorKey != null,
                errorKey = errorKey,
                compactMode = compactLandscape
            )
        }

        if (compactLandscape) {
            item {
                CompactStatGrid(
                    purchases = snapshot.sales?.purchasesTotal?.toString().orEmpty(),
                    items = snapshot.sales?.itemsTotal?.toString().orEmpty(),
                    total = sekFormatter().format(snapshot.sales?.revenueTotalSek ?: 0L),
                    cashiers = cashierCount.toString()
                )
            }
        } else {
            item {
                StatRow(
                    leftTitle = stringResource(R.string.live_stats_purchases_label),
                    leftValue = snapshot.sales?.purchasesTotal?.toString().orEmpty(),
                    rightTitle = stringResource(R.string.live_stats_items_label),
                    rightValue = snapshot.sales?.itemsTotal?.toString().orEmpty(),
                    compactMode = false
                )
            }

            item {
                StatRow(
                    leftTitle = stringResource(R.string.live_stats_total_label),
                    leftValue = sekFormatter().format(snapshot.sales?.revenueTotalSek ?: 0L),
                    rightTitle = stringResource(R.string.live_stats_cashiers_label),
                    rightValue = cashierCount.toString(),
                    compactMode = false
                )
            }
        }

        item {
            Text(
                text = stringResource(R.string.live_stats_cashier_list_title),
                style = MaterialTheme.typography.titleMedium,
                color = AppColors.TextPrimary,
                fontWeight = FontWeight.Bold
            )
        }

        if (cashiers.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = AppColors.CardBackground
                ) {
                    Text(
                        text = stringResource(R.string.live_stats_empty_cashiers),
                        modifier = Modifier.padding(16.dp),
                        color = AppColors.TextSecondary
                    )
                }
            }
        } else {
            items(items = cashierRows, key = { it.key }) { cashierRow ->
                CashierRow(cashier = cashierRow.cashier)
            }
        }
    }
}

@Composable
private fun CompactStatGrid(
    purchases: String,
    items: String,
    total: String,
    cashiers: String
) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Row(
            modifier = Modifier.fillMaxWidth(0.88f),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(
                title = stringResource(R.string.live_stats_purchases_label),
                value = purchases.ifBlank { "0" },
                compactMode = true,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = stringResource(R.string.live_stats_items_label),
                value = items.ifBlank { "0" },
                compactMode = true,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = stringResource(R.string.live_stats_total_label),
                value = total.ifBlank { "0" },
                compactMode = true,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = stringResource(R.string.live_stats_cashiers_label),
                value = cashiers.ifBlank { "0" },
                compactMode = true,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SummaryCard(
    event: Event,
    eventName: String,
    eventCity: String,
    eventImageUrl: String?,
    generatedAt: String?,
    isStale: Boolean,
    errorKey: String?,
    compactMode: Boolean = false
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = AppColors.CardBackground
    ) {
        if (isLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(if (compactMode) 12.dp else 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                EventImageCard(
                    imageUrl = eventImageUrl,
                    modifier = Modifier.weight(0.34f),
                    compactMode = compactMode
                )
                SummaryTextBlock(
                    event = event,
                    eventName = eventName,
                    eventCity = eventCity,
                    generatedAt = generatedAt,
                    isStale = isStale,
                    errorKey = errorKey,
                    compactMode = compactMode,
                    modifier = Modifier.weight(0.38f)
                )
                LiveMetaFlipCard(
                    eventId = event.id,
                    compactMode = compactMode,
                    modifier = Modifier.weight(0.28f)
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(if (compactMode) 12.dp else 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    EventImageCard(
                        imageUrl = eventImageUrl,
                        modifier = Modifier.weight(1f),
                        compactMode = compactMode
                    )
                    LiveMetaFlipCard(
                        eventId = event.id,
                        compactMode = compactMode,
                        modifier = Modifier.weight(1f)
                    )
                }

                SummaryTextBlock(
                    event = event,
                    eventName = eventName,
                    eventCity = eventCity,
                    generatedAt = generatedAt,
                    isStale = isStale,
                    errorKey = errorKey,
                    compactMode = compactMode,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun SummaryTextBlock(
    event: Event,
    eventName: String,
    eventCity: String,
    generatedAt: String?,
    isStale: Boolean,
    errorKey: String?,
    compactMode: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = eventName,
            style = if (compactMode) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineSmall,
            color = AppColors.TextPrimary,
            fontWeight = FontWeight.Bold,
            maxLines = if (compactMode) 2 else 3,
            overflow = TextOverflow.Ellipsis
        )

        if (eventCity.isNotBlank()) {
            Text(
                text = eventCity,
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextSecondary
            )
        }

        Text(
            text = stringResource(R.string.opening_hours_label, eventHoursLabel(event)),
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.TextSecondary
        )

        ConnectionBadge(
            text = stringResource(
                when {
                    isStale && errorKey == "rate_limited" -> R.string.live_stats_connection_rate_limited
                    isStale -> R.string.live_stats_connection_polling
                    else -> R.string.live_stats_connection_live
                }
            ),
            color = if (isStale) AppColors.Warning else AppColors.Success
        )

        generatedAt?.let { timestamp ->
            parseInstant(timestamp)?.let { instant ->
                Text(
                    text = stringResource(
                        R.string.live_stats_updated_at,
                        timeFormatter.format(instant)
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextSecondary
                )
            }
        }
    }
}

@Composable
private fun EventImageCard(
    imageUrl: String?,
    compactMode: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = AppColors.CardBackground
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = stringResource(R.string.live_stats_event_image_content_description),
            placeholder = painterResource(R.drawable.iloppis_logo_black),
            error = painterResource(R.drawable.iloppis_logo_black),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = if (compactMode) 72.dp else 96.dp)
                .aspectRatio(if (compactMode) 2.7f else 2f)
                .clip(RoundedCornerShape(14.dp))
        )
    }
}

@Composable
private fun LiveMetaFlipCard(
    eventId: String,
    compactMode: Boolean,
    modifier: Modifier = Modifier
) {
    var showBack by remember(eventId) { mutableStateOf(true) }

    val flipRotation by animateFloatAsState(
        targetValue = if (showBack) 180f else 0f,
        animationSpec = tween(durationMillis = META_FLIP_ANIM_MS),
        label = "meta-card-flip"
    )
    val density = LocalDensity.current

    TextButton(
        modifier = modifier
            .heightIn(min = if (compactMode) 72.dp else 96.dp)
            .aspectRatio(if (compactMode) 2.7f else 2f),
        shape = RoundedCornerShape(5.dp),
        contentPadding = PaddingValues(0.dp),
        onClick = { showBack = !showBack }
    ) {
        Box(modifier = Modifier) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        rotationY = flipRotation
                        cameraDistance = 14f * density.density
                        alpha = if (flipRotation <= 90f) 1f else 0f
                    },
                shape = RoundedCornerShape(14.dp),
                color = AppColors.CardBackground
            ) {
                MetaQrFace(url = visitUrl(eventId))
            }

            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        rotationY = flipRotation + 180f
                        cameraDistance = 14f * density.density
                        alpha = if (flipRotation > 90f) 1f else 0f
                    },
                shape = RoundedCornerShape(14.dp),
                color = AppColors.CardBackground
            ) {
                MetaBrandFace()
            }
        }
    }
}

@Composable
private fun MetaQrFace(url: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically)
    ) {
        QrCodeImage(
            qrValue = url,
            size = 74.dp,
            modifier = Modifier.clip(RoundedCornerShape(6.dp))
        )
        Text(
            text = stringResource(R.string.live_stats_qr_card_subtitle),
            color = AppColors.TextSecondary,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun MetaBrandFace() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.og_image),
            contentDescription = stringResource(R.string.live_stats_qr_card_content_description),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxSize()
                .clip(RoundedCornerShape(10.dp))
        )
    }
}

@Composable
private fun QrCodeImage(
    qrValue: String,
    size: Dp,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val sizePx = with(density) { size.roundToPx().coerceAtLeast(1) }
    val bitmap by produceState<Bitmap?>(
        initialValue = null,
        key1 = qrValue,
        key2 = sizePx
    ) {
        value = withContext(Dispatchers.Default) {
            createQrBitmap(value = qrValue, sizePx = sizePx)
        }
    }
    val renderedBitmap = bitmap

    if (renderedBitmap != null) {
        Image(
            bitmap = renderedBitmap.asImageBitmap(),
            contentDescription = stringResource(R.string.live_stats_qr_card_content_description),
            modifier = modifier.size(size),
            contentScale = ContentScale.Fit
        )
    } else {
        Box(
            modifier = modifier
                .size(size)
                .background(AppColors.CardBackground, RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.QrCode2,
                contentDescription = stringResource(R.string.live_stats_qr_card_content_description),
                tint = AppColors.TextSecondary
            )
        }
    }
}

@Composable
private fun StatRow(
    leftTitle: String,
    leftValue: String,
    rightTitle: String,
    rightValue: String,
    compactMode: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(
            title = leftTitle,
            value = leftValue.ifBlank { "0" },
            compactMode = compactMode,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            title = rightTitle,
            value = rightValue.ifBlank { "0" },
            compactMode = compactMode,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    compactMode: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = AppColors.CardBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = if (compactMode) 10.dp else 18.dp,
                    vertical = if (compactMode) 8.dp else 20.dp
                ),
            verticalArrangement = Arrangement.spacedBy(if (compactMode) 2.dp else 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = if (compactMode) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineMedium,
                color = AppColors.TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = title,
                style = if (compactMode) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
                color = AppColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CashierRow(cashier: LiveCashierStatus) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = AppColors.CardBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = cashier.displayName?.takeIf { it.isNotBlank() }
                    ?: stringResource(R.string.live_stats_unknown_cashier),
                style = MaterialTheme.typography.titleMedium,
                color = AppColors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = listOf(
                    stateLabel(cashier.state),
                    clientTypeLabel(cashier.clientType),
                    stringResource(R.string.live_stats_pending_count, cashier.pendingPurchasesCount)
                ).filter { it.isNotBlank() }.joinToString(" • "),
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextSecondary
            )

            val purchaseTime = parseInstant(cashier.lastPurchaseAt)?.let(timeFormatter::format)
            if (purchaseTime != null) {
                Text(
                    text = stringResource(R.string.live_stats_updated_at, purchaseTime),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextSecondary
                )
            }
        }
    }
}

@Composable
private fun ConnectionBadge(text: String, color: androidx.compose.ui.graphics.Color) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = color.copy(alpha = 0.14f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelLarge,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun EmptyState(
    errorKey: String?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(
                when (errorKey) {
                    "server" -> R.string.live_stats_error_server
                    "rate_limited" -> R.string.live_stats_error_rate_limited
                    else -> R.string.live_stats_error_network
                }
            ),
            color = AppColors.TextPrimary,
            style = MaterialTheme.typography.bodyLarge
        )
        AppButton(
            text = stringResource(R.string.live_stats_retry),
            onClick = onRetry,
            variant = AppButtonVariant.Primary
        )
    }
}

private data class CashierRowItem(
    val key: String,
    val cashier: LiveCashierStatus
)

private fun liveStatsViewModelKey(eventId: String, apiKey: String): String =
    "live-stats-$eventId-${apiKey.hashCode()}"

private fun buildCashierRows(cashiers: List<LiveCashierStatus>): List<CashierRowItem> {
    val occurrences = mutableMapOf<String, Int>()
    return cashiers.map { cashier ->
        val baseKey = cashierIdentityBase(cashier)
        val occurrence = occurrences.getOrDefault(baseKey, 0) + 1
        occurrences[baseKey] = occurrence
        CashierRowItem(
            key = "$baseKey#$occurrence",
            cashier = cashier
        )
    }
}

private fun cashierIdentityBase(cashier: LiveCashierStatus): String {
    val displayName = cashier.displayName
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?.lowercase()
        ?: "unknown"
    val clientType = cashier.clientType
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?.lowercase()
        ?: "unknown"
    return "cashier-$displayName-$clientType"
}

private fun eventHoursLabel(event: Event): String {
    val datePart = event.dates.takeIf { it.isNotBlank() }
    val hoursPart = listOfNotNull(
        event.startTimeFormatted.takeIf { it.isNotBlank() },
        event.endTimeFormatted.takeIf { it.isNotBlank() }
    ).joinToString("–")

    return listOfNotNull(
        datePart,
        hoursPart.takeIf { it.isNotBlank() }
    ).joinToString(" ")
        .ifBlank { "-" }
}

@Composable
private fun stateLabel(rawState: String?): String = stringResource(
    when (rawState) {
        "STATE_OPEN" -> R.string.live_stats_state_open
        "STATE_PROCESSING" -> R.string.live_stats_state_processing
        "STATE_STALLED" -> R.string.live_stats_state_stalled
        else -> R.string.live_stats_state_offline
    }
)

@Composable
private fun clientTypeLabel(rawClientType: String?): String {
    val normalized = rawClientType?.uppercase().orEmpty()
    val labelRes = when {
        normalized.contains("ANDROID") -> R.string.live_stats_client_type_android
        normalized.contains("IOS") -> R.string.live_stats_client_type_ios
        normalized.contains("WEB") -> R.string.live_stats_client_type_web
        normalized.contains("CASHIER") || normalized.contains("KASSA") -> R.string.live_stats_client_type_cashier
        else -> R.string.live_stats_client_type_unknown
    }
    return stringResource(labelRes)
}

private fun sekFormatter(): NumberFormat =
    NumberFormat.getCurrencyInstance(svLocale).apply {
        currency = Currency.getInstance("SEK")
        maximumFractionDigits = 0
    }

private fun parseInstant(value: String?): Instant? =
    value?.takeIf { it.isNotBlank() }?.let {
        runCatching { Instant.parse(it) }.getOrNull()
    }
