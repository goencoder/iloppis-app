package se.iloppis.app.network.config

import android.util.Log
import androidx.compose.runtime.Composable

/**
 * Local client config stored as a static
 * object for static instance data getting.
 */
private object LocalClientConfig {
    /**
     * Config object
     */
    var config: ClientConfig = ClientConfig("")
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
    LocalClientConfig.config = config

    Log.d("TAG", "updated config")

    content()
}



/**
 * iLoppis client configuration
 *
 * This uses the [ClientConfigProvider] to
 * provide the local context with a
 * client configuration for the [se.iloppis.app.network.iLoppisClient]
 */
fun clientConfig() : ClientConfig = LocalClientConfig.config
