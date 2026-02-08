package se.iloppis.app.ui.screens.events

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import se.iloppis.app.domain.model.Event
import se.iloppis.app.ui.components.events.EventList

@Composable
fun EventSelectionScreen(onAction: (event: Event) -> Unit) {
    EventList(
        modifier = Modifier
    )
}
