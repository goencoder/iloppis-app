package se.iloppis.app.ui.screens.user.home

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
import se.iloppis.app.domain.model.Event
import se.iloppis.app.navigation.ScreenPage
import se.iloppis.app.ui.components.buttons.IconButton
import se.iloppis.app.ui.components.dialogs.CodeEntryDialog
import se.iloppis.app.ui.screens.ScreenModel
import se.iloppis.app.ui.screens.events.ILoppisHeader
import se.iloppis.app.ui.screens.screenContext
import se.iloppis.app.ui.states.ScreenAction
import se.iloppis.app.utils.user.codes.CodeStateMode

@Composable
fun HomeScreen() {
    val screen = screenContext()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .statusBarsPadding()
    ) {
        ILoppisHeader()

        Spacer(modifier = Modifier.height(14.dp))
        SelectionScreenButtonsRow(screen) { event, mode ->
            screen.onAction(ScreenAction.SetOverlay {
                CodeEntryDialog(event, mode) {
                    screen.onAction(ScreenAction.RemoveOverlay)
                }
            })
        }

        /*
            Home page content such as the closest event
        */
    }
}



/**
 * Row with selection options buttons
 */
@Composable
private fun SelectionScreenButtonsRow(
    screen: ScreenModel,
    onAction: (Event, CodeStateMode) -> Unit
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
                    ScreenPage.Selection { onAction(it, CodeStateMode.CASHIER) }
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
                    ScreenPage.Selection { onAction(it, CodeStateMode.SCANNER) }
                )
            )
        }
    }
}
