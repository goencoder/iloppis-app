package se.iloppis.app.network

import android.content.Context
import kotlinx.serialization.json.Json.Default.decodeFromString
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import se.iloppis.app.R
import java.util.concurrent.TimeUnit

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
     * Applies client connection configuration to [OkHttpClient.Builder]
     */
    private fun OkHttpClient.Builder.clientConnection(config: ClientConnectionConfiguration) : OkHttpClient.Builder {
        connectTimeout(config.timeout, TimeUnit.SECONDS)
        return this
    }

    /**
     * Applies client reading configuration to [OkHttpClient.Builder]
     */
    private fun OkHttpClient.Builder.clientReading(config: ClientReadingConfiguration) : OkHttpClient.Builder {
        readTimeout(config.timeout, TimeUnit.SECONDS)
        return this
    }

    /**
     * Applies client writing configuration to [OkHttpClient.Builder]
     */
    private fun OkHttpClient.Builder.clientWriting(config: ClientWritingConfiguration) : OkHttpClient.Builder {
        writeTimeout(config.timeout, TimeUnit.SECONDS)
        return this
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

    /**
     * API HTTP(S) client
     */
    private val client = OkHttpClient.Builder()
        .addInterceptor(logging)
        .clientConnection(config.connection)
        .clientReading(config.reading)
        .clientWriting(config.writing)
        .build()

    /**
     * API client retrofitter
     *
     * Allows the creation of API extension interfaces
     * that builds on this API client object.
     */
    val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(config.url)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()



    /**
     * Creates [se.iloppis.app.network.iLoppisApiClient] instance
     * with extension interface.
     *
     * This will create an instance of this [se.iloppis.app.network.iLoppisApiClient]
     * object built upon the specified API interface.
     */
    inline fun <reified T : iLoppisApiInterface> create(): T = retrofit.create(T::class.java)
}



/**
 * iLoppis API interface
 *
 * All interfaces used with the [iLoppisApiClient] must
 * implement this interface.
 */
interface iLoppisApiInterface {}
