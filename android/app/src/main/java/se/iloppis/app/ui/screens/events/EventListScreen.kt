package se.iloppis.app.ui.screens.events

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.dp
import se.iloppis.app.R
import se.iloppis.app.domain.model.CodeEntryMode
import se.iloppis.app.domain.model.Event
import se.iloppis.app.navigation.ScreenPage
import se.iloppis.app.ui.components.buttons.AppButton
import se.iloppis.app.ui.components.buttons.AppButtonSize
import se.iloppis.app.ui.components.buttons.AppButtonVariant
import se.iloppis.app.ui.components.events.SwipeableEventList
import se.iloppis.app.ui.components.navigation.ILoppisHeader
import se.iloppis.app.ui.screens.screenContext
import se.iloppis.app.ui.states.ScreenAction
import se.iloppis.app.ui.theme.AppColors

/**
 * Unified main screen combining Home and Event List.
 *
 * This screen displays:
 * - Quick access buttons for Cashier/Scanner via direct code entry
 * - Functional event search with 300ms debounce
 * - Filter chips mapped to real API calls
 * - List of events with computed status badges
 */
@Composable
fun EventListScreen() {
    val screen = screenContext()
    val viewModel = eventContext()
    val state = viewModel.uiState

    UnifiedEventListContent(
        state = state,
        onReload = {
            if (!viewModel.uiState.isLoading) viewModel.onAction(EventListAction.LoadEvents)
        },
        onSearchChange = { viewModel.onAction(EventListAction.UpdateSearch(it)) },
        onFilterSelect = { viewModel.onAction(EventListAction.SelectFilter(it)) },
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
                    ScreenPage.CodeEntry(CodeEntryMode.CASHIER)
                )
            )
        },
        onScannerClick = {
            screen.onAction(
                ScreenAction.NavigateToPage(
                    ScreenPage.CodeEntry(CodeEntryMode.SCANNER)
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
    onSearchChange: (String) -> Unit,
    onFilterSelect: (EventFilterChip) -> Unit,
    onEventClick: (Event) -> Unit,
    onCashierClick: () -> Unit,
    onScannerClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        // Header with logo
        ILoppisHeader()

        // Scrollable content area
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            // Functional Search bar
            SearchBar(
                query = state.searchQuery,
                onQueryChange = onSearchChange
            )

            // Functional Filter chips
            FilterChips(
                activeFilter = state.activeFilter,
                onSelect = onFilterSelect
            )

            // Content based on state
            EventListBody(
                state = state,
                onReload = onReload,
                onEventClick = onEventClick,
                modifier = Modifier.weight(1f)
            )
        }

        // Sticky footer with tool buttons
        FooterToolButtons(
            onCashierClick = onCashierClick,
            onScannerClick = onScannerClick
        )
    }
}

/**
 * Sticky footer with Cashier and Scanner buttons.
 * Styled identically to EventToolButtons in the event detail screen.
 */
@Composable
private fun FooterToolButtons(
    onCashierClick: () -> Unit,
    onScannerClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.Background)
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        HorizontalDivider(
            color = AppColors.Border,
            thickness = 1.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Cashier button
            AppButton(
                text = stringResource(R.string.home_open_cashier),
                onClick = onCashierClick,
                modifier = Modifier.weight(1f),
                variant = AppButtonVariant.Primary,
                size = AppButtonSize.Large,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Payments,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }
            )

            // Scanner button
            AppButton(
                text = stringResource(R.string.home_open_scanner),
                onClick = onScannerClick,
                modifier = Modifier.weight(1f),
                variant = AppButtonVariant.Success,
                size = AppButtonSize.Large,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.QrCode,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }
            )
        }
    }
}

/**
 * Functional search bar wired to ViewModel.
 */
@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(stringResource(R.string.search_placeholder)) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        shape = RoundedCornerShape(24.dp),
        singleLine = true
    )
}

/**
 * Functional filter chips wired to ViewModel.
 */
@Composable
private fun FilterChips(
    activeFilter: EventFilterChip,
    onSelect: (EventFilterChip) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(bottom = 16.dp)
    ) {
        item {
            FilterChip(
                selected = activeFilter == EventFilterChip.ALL,
                onClick = { onSelect(EventFilterChip.ALL) },
                label = { Text(stringResource(R.string.filter_all)) }
            )
        }
        item {
            FilterChip(
                selected = activeFilter == EventFilterChip.UPCOMING,
                onClick = { onSelect(EventFilterChip.UPCOMING) },
                label = { Text(stringResource(R.string.filter_upcoming)) }
            )
        }
        item {
            FilterChip(
                selected = activeFilter == EventFilterChip.ONGOING,
                onClick = { onSelect(EventFilterChip.ONGOING) },
                label = { Text(stringResource(R.string.filter_ongoing)) }
            )
        }
        item {
            FilterChip(
                selected = activeFilter == EventFilterChip.PAST,
                onClick = { onSelect(EventFilterChip.PAST) },
                label = { Text(stringResource(R.string.filter_past)) }
            )
        }
    }
}

@Composable
private fun EventListBody(
    state: EventListUiState,
    onReload: () -> Unit,
    onEventClick: (Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    PullToRefreshBox(
        modifier = modifier.fillMaxWidth(),
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
                enableStartToEnd = false,
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
