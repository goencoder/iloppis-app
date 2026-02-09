package se.iloppis.app.ui.components.events

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import se.iloppis.app.R
import se.iloppis.app.domain.model.Event
import se.iloppis.app.ui.screens.screenContext
import se.iloppis.app.utils.events.state.EventListSortType

/**
 * Event list component
 *
 * @see EventCard
 */
@Composable
fun EventList(events: List<Event>, onAction: (Event) -> Unit, screenPadding: Boolean = true) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(events, key = { it.id }) {
            EventCard(it, onClick = { onAction(it) })
        }

        if(screenPadding) item {
            val screen = screenContext()
            Spacer(modifier = Modifier.height(screen.border.calculateBottomPadding()))
        }
    }
}

/**
 * Swipeable events list component
 *
 * @see SwipeToDismissEventCard
 */
@Composable
fun SwipeableEventList(
    events: List<Event>,
    screenPadding: Boolean = true,
    enableEndToStart: Boolean = true,
    onEndToStart: (Event) -> Unit = {},
    enableStartToEnd: Boolean = false,
    onStartToEnd: (Event) -> Unit = {},
    onAction: (Event) -> Unit
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(events, key = { it.id }) {
            SwipeToDismissEventCard(
                event = it,
                enableEndToStart = enableEndToStart,
                onEndToStart = { onEndToStart(it) },
                enableStartToEnd = enableStartToEnd,
                onStartToEnd = { onStartToEnd(it) },
                cardAction = { onAction(it) }
            )
        }

        if(screenPadding) item {
            val screen = screenContext()
            Spacer(modifier = Modifier.height(screen.border.calculateBottomPadding()))
        }
    }
}



/**
 * Event list search bar
 */
@Composable
fun SearchBar() {
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

/**
 * Event list filter chips
 */
@Composable
fun FilterChips(
    sort: EventListSortType,
    allowLocal: Boolean = false,
    onClick: (EventListSortType) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(bottom = 16.dp)
    ) {
        /* Locally stored events filter */
        if(allowLocal) item {
            FilterChip(
                selected = sort == EventListSortType.SAVED,
                onClick = { onClick(EventListSortType.SAVED) },
                label = { Text(stringResource(R.string.filter_saved)) })
        }

        /* Default filters */
        item {
            FilterChip(
                selected = sort == EventListSortType.ALL,
                onClick = { onClick(EventListSortType.ALL) },
                label = { Text(stringResource(R.string.filter_all)) })
        }



        /* To be added */
//        item {
//            FilterChip(
//                selected = false,
//                onClick = {},
//                label = { Text(stringResource(R.string.filter_open)) })
//        }
//        item {
//            FilterChip(
//                selected = false,
//                onClick = {},
//                label = { Text(stringResource(R.string.filter_upcoming)) })
//        }
//        item {
//            FilterChip(
//                selected = false,
//                onClick = {},
//                label = { Text(stringResource(R.string.filter_past)) })
//        }
    }
}
