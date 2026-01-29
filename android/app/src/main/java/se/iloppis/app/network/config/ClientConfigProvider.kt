package se.iloppis.app.network.config

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf

/**
 * Local client config composition
 */
private val localClientConfig = compositionLocalOf<ClientConfig> {
    error("No client config provider is present in this context")
}



/**
 * iLoppis client configuration provider
 *
 * This will store the [config] provided and
 * make it available for all [Composable]
 * components in this local context.
 */
@Composable
fun ClientConfigProvider(config: ClientConfig, content: @Composable () -> Unit) {
    CompositionLocalProvider(localClientConfig provides config) {
        content()
    }
}



/**
 * iLoppis client configuration
 *
 * This uses the [ClientConfigProvider] to
 * provide the local context with a
 * client configuration for the [se.iloppis.app.network.iLoppisClient]
 */
@Composable
fun clientConfig() : ClientConfig = localClientConfig.current
