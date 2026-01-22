package se.iloppis.app.ui.screens.scanner

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import se.iloppis.app.ui.screens.events.EventListHeader

/**
 * Scanner selection screen
 *
 * This uses the [se.iloppis.app.utils.LocalStorage] bucket
 * to get stored events for scanner selection.
 */
@Composable
fun ScannerSelectionScreen() {
    Column(modifier = Modifier.fillMaxSize()
        .padding(horizontal = 16.dp)
        .statusBarsPadding()
    ) {
        EventListHeader()
    }
}
