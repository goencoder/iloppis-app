package se.iloppis.app.ui.screens.events

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import se.iloppis.app.domain.model.Event
import se.iloppis.app.utils.events.state.rememberEventListState

@Composable
fun EventSelectionScreen(onAction: (event: Event) -> Unit) {
    val state = rememberEventListState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .statusBarsPadding()
    ) {

        /* One page with saved events */

        Text("Sort: ${state.sort}")

        /* One page with all events ( load this is saved is 0 ) */

    }
}
