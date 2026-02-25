package se.iloppis.app.ui.screens.events

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.QrCode
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import se.iloppis.app.R
import se.iloppis.app.domain.model.Event
import se.iloppis.app.navigation.ScreenPage
import se.iloppis.app.ui.components.buttons.IconButton
import se.iloppis.app.ui.components.events.SwipeableEventList
import se.iloppis.app.ui.components.navigation.ILoppisHeader
import se.iloppis.app.ui.screens.screenContext
import se.iloppis.app.ui.states.ScreenAction
import se.iloppis.app.ui.theme.AppColors
import se.iloppis.app.utils.events.localEventsStorage

/**
 * Unified main screen combining Home and Event List.
 * 
 * This screen displays:
 * - Quick access buttons for Cashier/Scanner via direct code entry
 * - Event search and filters
 * - List of events
 */
@Composable
fun EventListScreen() {
    val screen = screenContext()
    val viewModel = eventContext()
    val state = viewModel.uiState

    UnifiedEventListContent(
        state = state,
        onReload = {
            if(!viewModel.uiState.isLoading) viewModel.onAction(EventListAction.LoadEvents)
        },
        onEventClick = {
            screen.onAction(
                ScreenAction.NavigateToPage(
                    ScreenPage.EventsDetailPage(it)
                )
            )
        },
        onCashierClick = {
            screen.onAction(
                ScreenAction.NavigateToPage(
                    ScreenPage.CodeEntry("CASHIER")
                )
            )
        },
        onScannerClick = {
            screen.onAction(
                ScreenAction.NavigateToPage(
                    ScreenPage.CodeEntry("SCANNER")
                )
            )
        }
    )
}

/**
 * Unified content layout combining Home buttons and Event List.
 */
@Composable
private fun UnifiedEventListContent(
    state: EventListUiState,
    onReload: () -> Unit,
    onEventClick: (Event) -> Unit,
    onCashierClick: () -> Unit,
    onScannerClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        // Header
        ILoppisHeader(R.string.pages_home)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Quick access buttons for Cashier/Scanner
            ToolAccessButtons(
                onCashierClick = onCashierClick,
                onScannerClick = onScannerClick
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Search bar
            SearchBar()

            // Filter chips
            FilterChips()

            // Content based on state
            EventListBody(
                state = state,
                onReload = onReload,
                onEventClick = onEventClick
            )
        }
    }
}

/**
 * Row with quick access buttons for Cashier and Scanner
 */
@Composable
private fun ToolAccessButtons(
    onCashierClick: () -> Unit,
    onScannerClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        IconButton(
            text = R.string.home_open_cashier,
            icon = Icons.Outlined.Payments
        ) { onCashierClick() }

        IconButton(
            text = R.string.home_open_scanner,
            colors = ButtonDefaults.buttonColors().copy(
                containerColor = MaterialTheme.colorScheme.secondary
            ),
            icon = Icons.Outlined.QrCode
        ) { onScannerClick() }
    }
}

@Composable
private fun SearchBar() {
    OutlinedTextField(
        value = "",
        onValueChange = {},
        placeholder = { Text(stringResource(R.string.search_placeholder)) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        shape = RoundedCornerShape(24.dp),
        singleLine = true
    )
}

@Composable
private fun FilterChips() {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(bottom = 16.dp)
    ) {
        item {
            FilterChip(
                selected = true,
                onClick = {},
                label = { Text(stringResource(R.string.filter_all)) })
        }
        item {
            FilterChip(
                selected = false,
                onClick = {},
                label = { Text(stringResource(R.string.filter_open)) })
        }
        item {
            FilterChip(
                selected = false,
                onClick = {},
                label = { Text(stringResource(R.string.filter_upcoming)) })
        }
        item {
            FilterChip(
                selected = false,
                onClick = {},
                label = { Text(stringResource(R.string.filter_past)) })
        }
    }
}

@Composable
private fun EventListBody(
    state: EventListUiState,
    onReload: () -> Unit,
    onEventClick: (Event) -> Unit
) {
    val storage = localEventsStorage()
    PullToRefreshBox(
        isRefreshing = false,
        onRefresh = onReload
    ) {
        when {
            state.isLoading -> LoadingState()
            state.errorMessage != null -> ErrorState(state.errorMessage)
            state.events.isEmpty() -> EmptyState()
            else -> SwipeableEventList(
                events = state.events,
                enableEndToStart = false,
                enableStartToEnd = true,
                onStartToEnd = {
                    if(storage.contains(it.id)) storage.remove(it.id)
                    else storage.add(it.id)
                },
                onAction = onEventClick
            )
        }
    }
}

@Composable
fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
fun ErrorState(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.error_prefix, message),
            color = AppColors.TextError
        )
    }
}

@Composable
fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.no_events_found),
            color = AppColors.TextMuted
        )
    }
}
