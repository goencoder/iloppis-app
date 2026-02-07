package se.iloppis.app.ui.screens.events

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import se.iloppis.app.domain.model.Event
import se.iloppis.app.ui.components.map.Map

@Composable
fun EventsDetailsScreen(event: Event)  {
    Box(modifier = Modifier.fillMaxWidth().height(250.dp)) {
        Map(
            event,
            modifier = Modifier
        )
    }
}
