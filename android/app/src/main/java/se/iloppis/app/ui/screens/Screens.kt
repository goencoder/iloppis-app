package se.iloppis.app.ui.screens

import androidx.compose.runtime.Composable

/**
 * Application Screens Provider
 *
 * This provides all necessary screen providers
 * for the local context
 */
@Composable
fun Screens(content: @Composable () -> Unit) {
    ScreenModelProvider {
        content()
    }
}
