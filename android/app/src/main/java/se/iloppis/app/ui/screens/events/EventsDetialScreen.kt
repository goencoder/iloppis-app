package se.iloppis.app.ui.screens.events

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import se.iloppis.app.ui.components.StateBadge
import se.iloppis.app.ui.components.map.Map
import se.iloppis.app.ui.components.navigation.SwipeToNavigate
import se.iloppis.app.ui.components.text.MarkdownText
import se.iloppis.app.ui.screens.screenContext
import se.iloppis.app.ui.states.ScreenAction

@Composable
fun EventsDetailsScreen(event: Event)  {
    val screen = screenContext()

    Column(modifier = Modifier.fillMaxSize()) {
        SwipeToNavigate(
            onNavigate = {
                screen.onAction(
                    ScreenAction.NavigateToPage(
                        ScreenPage.Search
                    )
                )
            }
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(325.dp)) {
                        Map(
                            event,
                            modifier = Modifier
                        )
                    }
                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(color = MaterialTheme.colorScheme.onPrimary)
                    )
                }

                item {
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 15.dp)
                            .statusBarsPadding()
                    ) {
                        Spacer(modifier = Modifier.height(7.dp))
                        EventDetailsContent(event)
                    }
                }

                item { Spacer(modifier = Modifier.height(screen.border.calculateBottomPadding())) }
            }
        }
    }
}



/**
 * Event details main content
 */
@Composable
private fun EventDetailsContent(event: Event) {
    Row(
        modifier = Modifier.fillMaxWidth().statusBarsPadding(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = event.name,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.tertiary
        )
        StateBadge(event.state)
    }

    Text(
        text = event.dates,
        color = MaterialTheme.colorScheme.surfaceDim
    )

    // Opening hours
    if (event.startTimeFormatted.isNotBlank() || event.endTimeFormatted.isNotBlank()) {
        Spacer(modifier = Modifier.height(4.dp))
        val hours = listOfNotNull(
            event.startTimeFormatted.takeIf { it.isNotBlank() },
            event.endTimeFormatted.takeIf { it.isNotBlank() }
        ).joinToString(" – ")
        Text(
            text = stringResource(R.string.opening_hours_label, hours),
            color = MaterialTheme.colorScheme.surfaceDim
        )
    }

    // Description
    if(event.description.isNotBlank()) {
        Spacer(modifier = Modifier.height(12.dp))
        Box(modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(color = MaterialTheme.colorScheme.onPrimary)
        )
        Spacer(modifier = Modifier.height(12.dp))
        MarkdownText(event.description)
    }
}
