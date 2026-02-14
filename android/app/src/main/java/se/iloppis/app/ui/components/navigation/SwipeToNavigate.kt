package se.iloppis.app.ui.components.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation3.scene.Scene
import androidx.navigationevent.NavigationEvent
import kotlinx.coroutines.launch

/**
 * Animates slide out effect on navigation
 *
 * This will slide content into the view with
 * [AnimatedContentTransitionScope.SlideDirection.Right]
 * and out of view with
 * [AnimatedContentTransitionScope.SlideDirection.Right]
 */
fun <T : Any> animateSlideOut() : AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform = {
    ContentTransform(
        targetContentEnter = slideIntoContainer(
            AnimatedContentTransitionScope.SlideDirection.Right
        ),
        initialContentExit = slideOutOfContainer(
        AnimatedContentTransitionScope.SlideDirection.Right
        )
    )
}

/**
 * Animates predictive slide out effect on navigation
 *
 * This will slide content into the view with
 * [AnimatedContentTransitionScope.SlideDirection.Right]
 * and out of view with
 * [AnimatedContentTransitionScope.SlideDirection.Right]
 */
fun <T : Any> animatePredictiveSlideOut() : AnimatedContentTransitionScope<Scene<T>>.(@NavigationEvent.SwipeEdge Int) -> ContentTransform = {
    ContentTransform(
        targetContentEnter = slideIntoContainer(
            AnimatedContentTransitionScope.SlideDirection.Right
        ),
        initialContentExit = slideOutOfContainer(
            AnimatedContentTransitionScope.SlideDirection.Right
        )
    )
}
