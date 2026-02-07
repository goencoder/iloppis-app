package se.iloppis.app.ui.components.navigation

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch

/**
 * Swipe to navigate component
 */
@Composable
fun SwipeToNavigate(
    modifier: Modifier = Modifier,
    onNavigate: () -> Unit,
    content: @Composable (RowScope.() -> Unit)
) {
    val state = rememberSwipeToDismissBoxState()
    val scope = rememberCoroutineScope()
    SwipeToDismissBox(
        state = state,
        modifier = modifier,
        enableDismissFromEndToStart = false,
        backgroundContent = {},
        onDismiss = {
            if(it == SwipeToDismissBoxValue.StartToEnd) onNavigate()
            else scope.launch { state.reset() }
        },
        content = content
    )
}
