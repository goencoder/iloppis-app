package se.iloppis.app.utils.provider

import android.content.Context
import androidx.compose.runtime.Composable
import kotlinx.serialization.json.Json.Default.decodeFromString
import se.iloppis.app.BuildConfig
import se.iloppis.app.R
import se.iloppis.app.network.config.ClientConfig
import se.iloppis.app.network.config.ClientConfigProvider

/**
 * Provides the current context with iLoppis application providers
 *
 * @see ClientConfigProvider
 */
@Composable
fun Provider(
    context: Context,
    networkConfigFile: Int = R.raw.client,
    content: @Composable () -> Unit
) {
    val networkConfig = try {
        context.resources.openRawResource(networkConfigFile).use {
            decodeFromString<ClientConfig>(it.readBytes().decodeToString())
        }
    } catch (e: Exception) {
        throw IllegalStateException(
            "Missing or invalid client configuration for the compiled ${BuildConfig.APP_ENVIRONMENT} environment",
            e
        )
    }

    check(normalizeBaseUrl(networkConfig.url) == normalizeBaseUrl(BuildConfig.API_BASE_URL)) {
        "Client configuration URL does not match the compiled ${BuildConfig.APP_ENVIRONMENT} environment"
    }

    val compiledConfig = networkConfig.copy(debug = BuildConfig.ENABLE_NETWORK_DEBUG_LOGGING)
    ClientConfigProvider(compiledConfig) {
        content()
    }
}

private fun normalizeBaseUrl(url: String): String = url.trim().removeSuffix("/") + "/"
