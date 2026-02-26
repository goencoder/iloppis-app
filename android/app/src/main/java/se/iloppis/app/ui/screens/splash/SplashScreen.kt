package se.iloppis.app.ui.screens.splash

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import se.iloppis.app.R
import se.iloppis.app.navigation.ScreenPage
import se.iloppis.app.ui.screens.screenContext
import se.iloppis.app.ui.states.ScreenAction
import se.iloppis.app.ui.theme.AppColors

/**
 * Splash screen - brand moment shown on app launch.
 *
 * Shows iLoppis logo and tagline on green background,
 * then auto-dismisses after ~2 seconds to Main Screen.
 */
@Composable
fun SplashScreen() {
    val screen = screenContext()
    var visible by remember { mutableStateOf(true) }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 500),
        label = "splash_fade"
    )

    LaunchedEffect(Unit) {
        delay(3000)
        visible = false
        delay(500) // Wait for fade-out
        screen.onAction(ScreenAction.NavigateToPage(ScreenPage.EventList))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.SplashBackground)
            .alpha(alpha),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // OG banner image — displayed at natural landscape ratio
        Image(
            painter = painterResource(R.drawable.og_image),
            contentDescription = stringResource(R.string.app_name),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.FillWidth
        )

    }
}
