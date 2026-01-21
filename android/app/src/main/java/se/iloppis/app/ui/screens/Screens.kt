package se.iloppis.app.ui.screens

import androidx.compose.runtime.Composable
import se.iloppis.app.ui.screens.events.EventScreenProvider

/**
 * Application Screens Provider
 *
 * This provides all necessary screen providers
 * for the local context
 */
@Composable
fun Screens(content: @Composable () -> Unit) {
    ScreenModelProvider {
        EventScreenProvider {
            content()
        }
    }
}
