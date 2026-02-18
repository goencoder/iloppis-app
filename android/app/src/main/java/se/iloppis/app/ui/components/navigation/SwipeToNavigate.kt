package se.iloppis.app.ui.components.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.navigation3.scene.Scene
import androidx.navigationevent.NavigationEvent

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
