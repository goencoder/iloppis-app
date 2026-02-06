package se.iloppis.app.ui.screens.events

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import se.iloppis.app.domain.model.Event
import se.iloppis.app.ui.components.map.Map

@Composable
fun EventsDetailsScreen(event: Event)  {
    Map(
        event,
        modifier = Modifier
    )
}
