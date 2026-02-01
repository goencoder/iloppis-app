package se.iloppis.app.utils.provider

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import kotlinx.serialization.json.Json.Default.decodeFromString
import se.iloppis.app.R
import se.iloppis.app.network.config.ClientConfig
import se.iloppis.app.network.config.ClientConfigProvider
import se.iloppis.app.utils.context.ContextProvider
import se.iloppis.app.utils.storage.LocalStorageProvider

/**
 * Provides the current context with iLoppis application providers
 *
 * @see ContextProvider
 * @see LocalStorageProvider
 * @see ClientConfigProvider
 */
@Composable
fun Provider(
    context: Context,
    networkConfigFile: Int = R.raw.client,
    content: @Composable () -> Unit
) {
    var networkConfig: ClientConfig? = null
    try {
        context.resources.openRawResource(networkConfigFile).use {
            networkConfig = decodeFromString<ClientConfig>(it.readBytes().decodeToString())
        }
    }catch (e: Exception) {
        Log.e("ProviderError", e.message ?: "Error parsing json for network client config")
    }

    ContextProvider(context) {
        LocalStorageProvider(context) {
            if(networkConfig != null) {
                ClientConfigProvider(networkConfig) {
                    content()
                }
            }else {
                content()
            }
        }
    }
}
