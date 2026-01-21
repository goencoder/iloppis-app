package se.iloppis.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import se.iloppis.app.ui.components.navigation.Navigator
import se.iloppis.app.ui.components.navigation.PageManager
import se.iloppis.app.ui.screens.ScreenModelProvider
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
            ScreenModelProvider {
                ILoppisTheme {
                    Scaffold(
                        modifier = Modifier,
                        bottomBar = { Navigator() }
                    ) {
                        Surface(
                            modifier = Modifier.padding(it).fillMaxSize(),
                            color = AppColors.Background
                        ) {
                            PageManager()
                        }
                    }
                }
            }
        }
    }
}
