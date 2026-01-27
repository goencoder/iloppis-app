package se.iloppis.app.network

import android.content.Context
import android.util.Log
import kotlinx.serialization.json.Json.Default.decodeFromString
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import se.iloppis.app.R

/**
 * iLoppis API Client object
 */
class iLoppisApiClient(val context: Context, configFile: Int = R.raw.client) {
    /**
     * iLoppis API Client configuration
     */
    var config: ApiClientConfig
        private set



    init {
        val stream = context.resources.openRawResource(configFile)
        config = decodeFromString<ApiClientConfig>(stream.readBytes().decodeToString())
    }



    /**
     * Logging interceptor that:
     * - Uses BODY level only in debug builds (full request/response)
     * - Uses BASIC level in release builds (method + URL only)
     * - Redacts Authorization headers to prevent leaking API keys
     */
    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY // Force BODY logging for debugging
        redactHeader("Authorization")
        redactHeader("X-API-Key")
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(logging)
        .connectTimeout()
}
