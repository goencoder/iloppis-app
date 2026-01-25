package se.iloppis.app.ui.screens.events

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import se.iloppis.app.ui.components.EventCard
import se.iloppis.app.ui.dialogs.CodeEntryDialog
import se.iloppis.app.ui.dialogs.EventDetailDialog
import se.iloppis.app.ui.screens.screenContext
import se.iloppis.app.ui.theme.AppColors

/**
 * Main screen showing list of events (loppisar).
 * Also handles navigation to cashier/scanner screens.
 */
@Composable
fun EventListScreen() {
    val viewModel = eventContext()
    val state = viewModel.uiState

    EventListContent(
        state = state,
        onReload = {
            if(!viewModel.uiState.isLoading) viewModel.onAction(EventListAction.LoadEvents)
        },
        onEventClick = { viewModel.onAction(EventListAction.SelectEvent(it)) }
    )
}

/**
 * Handles dialog display based on current state.
 */
@Composable
fun EventDialogs() {
    val screen = screenContext()
    val events = eventContext()


    // Code entry dialog
    events.uiState.codeEntryState?.let { codeEntry ->
        CodeEntryDialog(
            mode = codeEntry.mode,
            eventName = codeEntry.event.name,
            isValidating = codeEntry.isValidating,
            errorMessage = codeEntry.errorMessage,
            onDismiss = { events.onAction(EventListAction.DismissCodeEntry) },
            onCodeChange = { code -> events.onAction(EventListAction.ValidateCode(screen, code)) },
            onCodeEntered = { code -> events.onAction(EventListAction.SubmitCode(screen, code)) }
        )
    }

    // Event detail dialog
    events.uiState.selectedEvent?.let { event ->
        EventDetailDialog(
            event = event,
            onDismiss = { events.onAction(EventListAction.DismissEventDetail) },
        )
    }
}

/**
 * Main content layout for the event list.
 */
@Composable
private fun EventListContent(
    state: EventListUiState,
    onReload: () -> Unit,
    onEventClick: (Event) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .statusBarsPadding()
    ) {
        // Header
        EventListHeader()

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

@Composable
fun EventListHeader() {
    Text(
        text = stringResource(R.string.app_title),
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        color = AppColors.TextPrimary,
        modifier = Modifier.padding(vertical = 16.dp)
    )
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
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(bottom = 16.dp)
    ) {
        FilterChip(selected = true, onClick = {}, label = { Text(stringResource(R.string.filter_all)) })
        FilterChip(selected = false, onClick = {}, label = { Text(stringResource(R.string.filter_open)) })
        FilterChip(selected = false, onClick = {}, label = { Text(stringResource(R.string.filter_upcoming)) })
        FilterChip(selected = false, onClick = {}, label = { Text(stringResource(R.string.filter_past)) })
    }
}

@Composable
private fun EventListBody(
    state: EventListUiState,
    onReload: () -> Unit,
    onEventClick: (Event) -> Unit
) {
    PullToRefreshBox(
        isRefreshing = false,
        onRefresh = onReload
    ) {
        when {
            state.isLoading -> LoadingState()
            state.errorMessage != null -> ErrorState(state.errorMessage)
            state.events.isEmpty() -> EmptyState()
            else -> EventList(events = state.events, onEventClick = onEventClick)
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

@Composable
private fun EventList(
    events: List<Event>,
    onEventClick: (Event) -> Unit
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(events) { event ->
            EventCard(
                event = event,
                onClick = { onEventClick(event) }
            )
        }
    }
}
