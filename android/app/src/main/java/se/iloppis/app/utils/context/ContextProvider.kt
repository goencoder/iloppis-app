package se.iloppis.app.utils.context

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf

/**
 * Context composition local
 */
private val localContext = compositionLocalOf<Context> {
    error("No context provider exists in this context")
}



/**
 * Context provider
 *
 * This will provide components of this
 * context with a getter to the context
 * instance.
 */
@Composable
fun ContextProvider(context: Context, content: @Composable () -> Unit) {
    CompositionLocalProvider(localContext provides context) {
        content()
    }
}



/**
 * Gets context instance
 *
 * This uses the [ContextProvider] to get
 * the local context instance.
 */
@Composable
fun localContext() : Context = localContext.current
