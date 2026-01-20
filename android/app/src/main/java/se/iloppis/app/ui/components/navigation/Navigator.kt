package se.iloppis.app.ui.components.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import se.iloppis.app.ui.screens.screenContext

/**
 * Navigation color alpha
 */
const val NAVIGATOR_ALPHA = 0.3f



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
fun Navigator(content: @Composable () -> Unit) {
    val screen = screenContext()

    Scaffold(
        modifier = Modifier,
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = NAVIGATOR_ALPHA)
            ) {
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxWidth()) {
            content()
        }
    }
}
