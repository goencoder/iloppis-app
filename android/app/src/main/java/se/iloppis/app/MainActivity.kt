package se.iloppis.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import se.iloppis.app.ui.screens.events.EventListScreen
import se.iloppis.app.ui.theme.AppColors
import se.iloppis.app.ui.theme.ILoppisTheme

/**
 * Main entry point for the iLoppis app.
 * Responsible only for setting up the theme and navigation root.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ILoppisTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = AppColors.Background
                ) {
                    EventListScreen()
                }
            }
        }
    }
}
