package se.iloppis.app.utils.provider

import android.content.Context
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
    val stream = context.resources.openRawResource(networkConfigFile)
    val networkConfig = decodeFromString<ClientConfig>(stream.readBytes().decodeToString())

    ContextProvider(context) {
        LocalStorageProvider(context) {
            ClientConfigProvider(networkConfig) {
                content()
            }
        }
    }
}
