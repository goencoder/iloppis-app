package se.iloppis.app.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.QrCode
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import se.iloppis.app.R
import se.iloppis.app.navigation.ScreenPage
import se.iloppis.app.ui.components.buttons.IconButton
import se.iloppis.app.ui.screens.ScreenModel
import se.iloppis.app.ui.screens.events.CodeEntryMode
import se.iloppis.app.ui.screens.events.EventListAction
import se.iloppis.app.ui.screens.events.EventListViewModel
import se.iloppis.app.ui.screens.events.ILoppisHeader
import se.iloppis.app.ui.screens.events.eventContext
import se.iloppis.app.ui.screens.screenContext
import se.iloppis.app.ui.states.ScreenAction

@Composable
fun HomeScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .statusBarsPadding()
    ) {
        ILoppisHeader()

        Spacer(modifier = Modifier.height(14.dp))
        SelectionScreenButtonsRow()
    }
}



/**
 * Row with selection options buttons
 */
@Composable
private fun SelectionScreenButtonsRow(
    screen: ScreenModel = screenContext(),
    event: EventListViewModel = eventContext()
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        IconButton(
            text = R.string.home_open_cashier,
            icon = Icons.Outlined.Payments
        ) {
            screen.onAction(
                ScreenAction.NavigateToPage(
                    ScreenPage.Selection {

                        /* Event context */
                        event.onAction(
                            EventListAction.StartCodeEntry(
                                CodeEntryMode.CASHIER,
                                it
                            )
                        )

                    }
                )
            )
        }

        IconButton(
            text = R.string.home_open_scanner,
            colors = ButtonDefaults.buttonColors().copy(
                containerColor = MaterialTheme.colorScheme.secondary
            ),
            icon = Icons.Outlined.QrCode
        ) {
            screen.onAction(
                ScreenAction.NavigateToPage(
                    ScreenPage.Selection {

                        /* Event context */
                        event.onAction(
                            EventListAction.StartCodeEntry(
                                CodeEntryMode.SCANNER,
                                it
                            )
                        )

                    }
                )
            )
        }
    }
}
