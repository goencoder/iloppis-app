package se.iloppis.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import se.iloppis.app.ui.components.navigation.Navigator
import se.iloppis.app.ui.components.navigation.PageManager
import se.iloppis.app.ui.screens.Screens
import se.iloppis.app.ui.screens.screenContext
import se.iloppis.app.ui.states.ScreenAction
import se.iloppis.app.ui.theme.AppColors
import se.iloppis.app.ui.theme.ILoppisTheme
import se.iloppis.app.utils.provider.Provider

/**
 * Main entry point for the iLoppis app.
 * Responsible only for setting up the theme and navigation root.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Provider(applicationContext) {
                Screens {
                    ILoppisTheme {
                        Scaffold(
                            contentWindowInsets = WindowInsets(),
                            modifier = Modifier,
                            bottomBar = { Navigator() }
                        ) { padding ->
                            screenContext().onAction(ScreenAction.SetBorders(padding))
                            Surface(
                                modifier = Modifier.fillMaxSize(),
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
}
