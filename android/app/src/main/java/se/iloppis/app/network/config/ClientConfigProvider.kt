package se.iloppis.app.network.config

import androidx.compose.runtime.Composable

private object LocalClientConfig {
    var config: ClientConfig? = null
}

/**
 * Initializes the process-wide client [config] if none has been set, then renders [content].
 */
@Composable
fun ClientConfigProvider(config: ClientConfig, content: @Composable () -> Unit) {
    if(LocalClientConfig.config == null) LocalClientConfig.config = config
    content()
}

/** Replaces the process-wide client configuration immediately. */
fun forceSetClientConfig(config: ClientConfig) {
    LocalClientConfig.config = config
}

/**
 * Returns the process-wide client configuration.
 *
 * @throws IllegalAccessException when no configuration has been provided.
 */
fun clientConfig() : ClientConfig {
    if(LocalClientConfig.config == null)
        throw IllegalAccessException("No network client config provided in this context")
    return LocalClientConfig.config!!
}
