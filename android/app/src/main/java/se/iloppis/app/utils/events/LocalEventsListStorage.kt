package se.iloppis.app.utils.events

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import se.iloppis.app.utils.storage.LocalStorage
import se.iloppis.app.utils.storage.localStorage

/**
 * Local events list storage
 *
 * This uses [LocalStorage] to store
 * data on the device in a
 * storage bucket.
 */
class LocalEventsListStorage(val storage: LocalStorage) {
    /**
     * List data ( Local access only )
     */
    private val data = mutableStateSetOf<String>().apply { addAll(getData()) }



    /**
     * Checks if [id] exists in local storage
     */
    fun contains(id: String) = data.contains(id)

    /**
     * Checks if the storage is empty or not
     */
    fun empty() = data.isEmpty()

    /**
     * Adds event [id] to locally stored events list
     *
     * If [sync] is **false** the changes will not
     * be synced with local storage.
     *
     * @see se.iloppis.app.utils.events.LocalEventsListStorage.save
     */
    fun add(id: String, sync: Boolean = true) {
        data.add(id)
        if(sync) save()
    }

    /**
     * Sets the data in the storage
     *
     * This will clear the old data and
     * add the new data.
     *
     * If [sync] is **false** the changes will not
     * be synced with local storage.
     *
     * @see se.iloppis.app.utils.events.LocalEventsListStorage.save
     */
    fun set(ids: Set<String>, sync: Boolean = true) {
        data.clear()
        data.addAll(ids)
        if(sync) save()
    }

    /**
     * Removes event [id] from local events list
     *
     * If [sync] is **false** the changes will not
     * be synced with local storage.
     *
     * @see se.iloppis.app.utils.events.LocalEventsListStorage.save
     */
    fun remove(id: String, sync: Boolean = true) {
        data.remove(id)
        if(sync) save()
    }

    /**
     * Removes all events with [ids] from local events list
     *
     * If [sync] is **false** the changes will not
     * be synced with local storage.
     *
     * @see se.iloppis.app.utils.events.LocalEventsListStorage.save
     */
    fun removeAll(ids: Set<String>, sync: Boolean = true) {
        data.removeAll(ids)
        if(sync) save()
    }



    /**
     * Gets data from local storage
     *
     * @see LocalStorage.getJson
     */
    fun getData() : Set<String> = storage.getJson(BUCKET, DEFAULT)

    /**
     * Saves data to local storage
     *
     * @see LocalStorage.putJson
     */
    fun save() = storage.putJson(BUCKET, data.toSet())



    companion object {
        /**
         * Storage bucket
         *
         * This is the bucket where all data
         * is stored.
         */
        const val BUCKET = "stored-events"

        /**
         * Default JSON object
         *
         * This is used when the list in the
         * bucket does not exist.
         */
        const val DEFAULT = "[]"
    }
}



/**
 * Local events list storage composition
 */
private val localEventsListStorage = compositionLocalOf<LocalEventsListStorage> {
    error("No events list storage provider in the local context")
}



/**
 * Local events list storage provider
 *
 * Provides access to events list in [LocalStorage]
 */
@Composable
fun LocalEventsListStorageProvider(storage: LocalStorage = localStorage(), content: @Composable () -> Unit) {
    val state = remember { LocalEventsListStorage(storage) }
    CompositionLocalProvider(localEventsListStorage provides state) {
        content()
    }
}



/**
 * Local events storage instance
 */
@Composable
fun localEventsStorage() = localEventsListStorage.current
