package se.iloppis.app.network.config

import androidx.compose.runtime.Composable

/**
 * Local client config stored as a static
 * object for static instance data getting.
 */
private object LocalClientConfig {
    /**
     * Config object
     */
    var config: ClientConfig? = null
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
    if(LocalClientConfig.config == null) LocalClientConfig.config = config
    content()
}

/**
 * Force sets client config
 *
 * This should be used with caution as it may have
 * unintended results.
 *
 * Consider using [ClientConfigProvider] to provide
 * a configuration object once. But if it is needed
 * to override the configuration after it has been
 * set once, then this can be used.
 *
 * @see ClientConfigProvider
 */
fun forceSetClientConfig(config: ClientConfig) {
    LocalClientConfig.config = config
}

/**
 * iLoppis client configuration
 *
 * This uses the [ClientConfigProvider] to
 * provide the local context with a
 * client configuration for the [se.iloppis.app.network.iLoppisClient]
 */
fun clientConfig() : ClientConfig {
    if(LocalClientConfig.config == null)
        throw IllegalAccessException("No network client config provided in this context")
    return LocalClientConfig.config!!
}
