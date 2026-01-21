package se.iloppis.app.utils

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.core.content.edit
import kotlinx.serialization.json.Json.Default.decodeFromString
import kotlinx.serialization.json.Json.Default.encodeToString

/**
 * Local storage bucket name
 */
const val STORAGE_NAME: String = "iLoppis-LocalStorage"

/**
 * Local storage mode
 */
const val STORAGE_MODE: Int = Context.MODE_PRIVATE



/**
 * Application local storage
 */
data class LocalStorage(val context: Context, val name: String, val mode: Int) {
    /**
     * Storage instance
     */
    private val storage by mutableStateOf(context.getSharedPreferences(name, mode))



    /**
     * Puts data in the context local storage bucket
     */
    fun put(key: String, data: String) {
        storage.edit {
            putString(key, data)
        }
    }

    /**
     * Gets data stored in the contexts local storage bucket
     *
     * Use [default] to set a fallback value
     */
    fun get(key: String, default: String? = null): String? {
        return storage.getString(key, default)
    }



    /**
     * Puts Json data in local storage using [encodeToString]
     *
     * For full description of [putJson] read [put]
     *
     * @see put
     */
    inline fun <reified T> putJson(key: String, data: T) {
        val json = encodeToString(data)
        put(key, json)
    }

    /**
     * Gets stored data as JSON format using [decodeFromString]
     *
     * For full description of [getJson] read [get]
     *
     * @see get
     */
    inline fun <reified T> getJson(key: String, default: String = "{}") : T {
        return decodeFromString<T>(get(key, default) ?: default)
    }
}



/**
 * Local storage context
 */
private val localStorageContext = compositionLocalOf<LocalStorage> {
    error("No local storage provider present in the current context")
}



/**
 * Local storage provider
 *
 * This provides a local storage object
 * in the current context and allows
 * access to a storage bucket local
 * on the android phone.
 */
@Composable
fun LocalStorageProvider(context: Context, name: String = STORAGE_NAME, mode: Int = STORAGE_MODE, content: @Composable () -> Unit) {
    val storage = remember { LocalStorage(context, name, mode) }
    CompositionLocalProvider(localStorageContext provides storage) {
        content()
    }
}



/**
 * Local storage instance
 *
 * Gets the local storage context instance
 */
@Composable
fun localStorage(): LocalStorage {
    return localStorageContext.current
}
