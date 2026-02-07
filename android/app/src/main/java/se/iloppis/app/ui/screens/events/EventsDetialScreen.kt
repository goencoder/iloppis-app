package se.iloppis.app.ui.screens.events

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import se.iloppis.app.domain.model.Event
import se.iloppis.app.ui.components.map.Map

@Composable
fun EventsDetailsScreen(event: Event)  {
    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().height(325.dp)) {
                    Map(
                        event,
                        modifier = Modifier
                    )
                }
            }

            item {
                Text("A test text")
            }
        }
    }
}
