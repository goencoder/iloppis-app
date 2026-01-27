package se.iloppis.app.network

import android.content.Context
import android.util.Log
import kotlinx.serialization.json.Json.Default.decodeFromString
import se.iloppis.app.R

/**
 * iLoppis API Client object
 */
class iLoppisApiClient(val context: Context, val configFile: Int = R.raw.client) {
    /**
     * iLoppis API Client configuration
     */
    var config: ApiClientConfig
        private set



    init {
        val stream = context.resources.openRawResource(configFile)
        config = decodeFromString<ApiClientConfig>(stream.readBytes().decodeToString())
    }
}
