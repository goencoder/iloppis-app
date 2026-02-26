package se.iloppis.app.ui.screens.events

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.QrCode
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import se.iloppis.app.R
import se.iloppis.app.domain.model.Event
import se.iloppis.app.domain.model.displayStatus
import se.iloppis.app.navigation.ScreenPage
import se.iloppis.app.ui.components.DisplayStatusBadge
import se.iloppis.app.ui.components.map.Map
import se.iloppis.app.ui.components.text.MarkdownText
import se.iloppis.app.ui.screens.screenContext
import se.iloppis.app.ui.states.ScreenAction
import se.iloppis.app.ui.theme.AppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventsDetailsScreen(event: Event) {
    val screen = screenContext()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = event.name,
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { screen.popPage() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.button_back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Map
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                ) {
                    Map(event, modifier = Modifier)
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(color = AppColors.Border)
                )
            }

            // Event details content
            item {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    EventDetailsContent(event)
                }
            }

            // Action buttons
            item {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    EventActionButtons(event)
                }
            }

            // Tools section
            item {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    EventToolButtons(event)
                }
            }

            // Bottom spacing
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}



/**
 * Event details main content
 */
@Composable
private fun EventDetailsContent(event: Event) {
    // Name row with status badge
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = event.name,
            modifier = Modifier.widthIn(max = 225.dp),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = AppColors.TextPrimary
        )
        DisplayStatusBadge(event.displayStatus())
    }

    Spacer(modifier = Modifier.height(4.dp))

    // Dates
    Text(
        text = event.dates,
        color = AppColors.TextSecondary
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
            color = AppColors.TextSecondary
        )
    }

    // Location
    if (event.location.isNotBlank()) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = event.location,
            color = AppColors.TextSecondary
        )
    }

    // Description
    if (event.description.isNotBlank()) {
        Spacer(modifier = Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(color = AppColors.Border)
        )
        Spacer(modifier = Modifier.height(12.dp))
        MarkdownText(event.description)
    }
}



/**
 * Navigation and website action buttons.
 */
@Composable
private fun EventActionButtons(event: Event) {
    val context = LocalContext.current

    // Navigate button (Google Maps intent)
    if (event.latitude != null && event.longitude != null) {
        Button(
            onClick = {
                val uri = Uri.parse(
                    "geo:${event.latitude},${event.longitude}?q=${event.latitude},${event.longitude}(${Uri.encode(event.name)})"
                )
                val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                    setPackage("com.google.android.apps.maps")
                }
                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                } else {
                    // Fallback to browser
                    context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AppColors.Info)
        ) {
            Icon(
                imageVector = Icons.Outlined.Map,
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(stringResource(R.string.event_detail_navigate))
        }
    }

    // Website button (browser intent)
    Button(
        onClick = {
            val url = "https://iloppis.se/?event=${event.id}"
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = AppColors.ButtonSecondary)
    ) {
        Icon(
            imageVector = Icons.Outlined.OpenInBrowser,
            contentDescription = null,
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(stringResource(R.string.event_detail_website))
    }
}



/**
 * Tool access buttons: Cashier and Scanner.
 */
@Composable
private fun EventToolButtons(event: Event) {
    val screen = screenContext()

    // Section header
    Text(
        text = stringResource(R.string.event_detail_tools_section),
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        color = AppColors.TextPrimary,
        modifier = Modifier.padding(bottom = 8.dp)
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Cashier button
        Button(
            onClick = {
                screen.onAction(
                    ScreenAction.NavigateToPage(ScreenPage.CodeEntry("CASHIER"))
                )
            },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = AppColors.ButtonPrimary)
        ) {
            Icon(
                imageVector = Icons.Outlined.Payments,
                contentDescription = null,
                modifier = Modifier.padding(end = 4.dp)
            )
            Text(stringResource(R.string.home_open_cashier))
        }

        // Scanner button
        Button(
            onClick = {
                screen.onAction(
                    ScreenAction.NavigateToPage(ScreenPage.CodeEntry("SCANNER"))
                )
            },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = AppColors.ButtonSecondary)
        ) {
            Icon(
                imageVector = Icons.Outlined.QrCode,
                contentDescription = null,
                modifier = Modifier.padding(end = 4.dp)
            )
            Text(stringResource(R.string.home_open_scanner))
        }
    }
}

