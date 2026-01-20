package se.iloppis.app.ui.components.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import se.iloppis.app.ui.screens.screenContext

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
fun Navigator(height: Dp = 120.dp, paddingValues: Dp = 25.dp, alpha: Float = 0.4f) {
    val screen = screenContext()

    NavigationBar(
        modifier = Modifier.height(height),
        containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha)
    ) {
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(
                start = paddingValues,
                top = paddingValues,
                bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
                end = paddingValues
            )
        ) {
        }
    }
}
