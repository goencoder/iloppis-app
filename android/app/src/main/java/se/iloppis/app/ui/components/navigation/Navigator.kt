package se.iloppis.app.ui.components.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import se.iloppis.app.R
import se.iloppis.app.ui.screens.screenContext
import se.iloppis.app.ui.states.ScreenAction
import se.iloppis.app.navigation.ScreenPage

/**
 * Application navigator
 *
 * This depends on a `screen view model context`
 * to navigate to different pages.
 *
 * This creates a bottom aligned page
 * navigator with a full width.
 *
 * @see se.iloppis.app.ui.screens.ScreenModel
 */
@Composable
fun Navigator(
    height: Dp = 120.dp,
    paddingValues: PaddingValues = PaddingValues(
        start = 25.dp,
        top = 5.dp,
        end = 25.dp
    ),
    alpha: Float = 0.2f,
    buttonSize: Dp = 75.dp,
    buttonCorner: Dp = 5.dp,
    buttonSpacing: Dp = 15.dp,
    iconSize: Dp = 35.dp
) {
    val screen = screenContext()
    if(!screen.state.showNavigator) return /* Hides navigator if requested */

    NavigationBar(
        modifier = Modifier.height(height),
        containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha)
    ) {
        Row(modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
            horizontalArrangement = Arrangement.Center
        ) {
            NavigatorButton(
                if(screen.state.page == ScreenPage.Home)
                    Icons.Filled.Home
                else Icons.Outlined.Home,
                stringResource(R.string.nav_home),
                buttonSize,
                iconSize,
                buttonCorner,
                buttonSpacing,
                screen.state.page == ScreenPage.Home
            ) { screen.onAction(ScreenAction.NavigateToPage(ScreenPage.Home)) }

            NavigatorButton(
                if(screen.state.page == ScreenPage.CashierSelector)
                    Icons.Filled.Payments
                else Icons.Outlined.Payments,
                stringResource(R.string.nav_cashier),
                buttonSize,
                iconSize,
                buttonCorner,
                buttonSpacing,
                screen.state.page == ScreenPage.CashierSelector
            ) { screen.onAction(ScreenAction.NavigateToPage(ScreenPage.CashierSelector)) }

            NavigatorButton(
                if(screen.state.page == ScreenPage.ScannerSelector)
                    Icons.Filled.QrCodeScanner
                else Icons.Outlined.QrCodeScanner,
                stringResource(R.string.nav_scanner),
                buttonSize,
                iconSize,
                buttonCorner,
                buttonSpacing,
                screen.state.page == ScreenPage.ScannerSelector
            ) { screen.onAction(ScreenAction.NavigateToPage(ScreenPage.ScannerSelector)) }
        }
    }
}



/**
 * Navigator button component
 */
@Composable
fun NavigatorButton(
    image: ImageVector,
    description: String,
    size: Dp,
    icon: Dp,
    rounded: Dp,
    space: Dp,
    enable: Boolean,
    onClick: () -> Unit
) {
    Box(modifier = Modifier.padding(horizontal = space)) {
        IconButton(
            modifier = Modifier.size(size).padding(0.dp),
            shape = RoundedCornerShape(rounded),
            onClick = onClick
        ) {
            Icon(
                modifier = Modifier.size(icon),
                imageVector = image,
                contentDescription = description,
                tint = if(enable)
                    MaterialTheme.colorScheme.background.copy(alpha = 0.4f)
                else
                    MaterialTheme.colorScheme.background
            )
        }
    }
}
