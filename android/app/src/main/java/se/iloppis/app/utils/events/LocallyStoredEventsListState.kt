package se.iloppis.app.utils.events

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import se.iloppis.app.utils.storage.LocalStorage
import se.iloppis.app.utils.storage.localStorage

/**
 * Events list state for locally stored events
 *
 * This uses [LocalStorage] to manage locally
 * stored events.
 *
 * @see LocalStorage
 */
class LocallyStoredEventsListState(val storage: LocalStorage) {
    /**
     * Data set ( local access only )
     */
    private val set: MutableSet<String> = mutableSetOf()

    init {
        set.apply {
            addAll(storage.getJson<Set<String>>(BUCKET, DEFAULT))
        }
    }



    /**
     * Checks if [id] exists in the locally stored set of events.
     *
     * If the **events id** exists in the set of
     * locally stored events this will return **true**
     */
    fun contains(id: String) : Boolean = set.contains(id)

    /**
     * Adds [id] to storage bucket
     *
     * This will store the [id] locally on the
     * device in a storage bucket with [LocalStorage]
     *
     * If [save] is false it will not sync with
     * the local storage bucket.
     *
     * @see LocalStorage
     * @see se.iloppis.app.utils.events.LocallyStoredEventsListState.save
     */
    fun add(id: String, save: Boolean = true) {
        set.add(id)
        if(save) save()
    }

    /**
     * Removes [id] from storage bucket
     *
     * This will remove the [id] from the local
     * device storage bucket.
     *
     * If [save] is false it will not sync with
     * the local storage bucket.
     *
     * @see LocalStorage
     * @see se.iloppis.app.utils.events.LocallyStoredEventsListState.save
     */
    fun remove(id: String, save: Boolean = true) {
        set.remove(id)
        if(save) save()
    }

    /**
     * Removes all [ids] from storage bucket
     *
     * This will remove the [ids] from the local
     * device storage bucket.
     *
     * If [save] is false it will not sync with
     * the local storage bucket.
     *
     * @see LocalStorage
     * @see se.iloppis.app.utils.events.LocallyStoredEventsListState.save
     */
    fun removeAll(ids: Set<String>, save: Boolean = true) {
        set.removeAll(ids)
        if(save) save()
    }

    /**
     * Saves the data in the local storage bucket
     *
     * @see LocalStorage
     */
    fun save() = storage.putJson(BUCKET, set.toSet())



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
 * Creates and remembers a [LocallyStoredEventsListState] with [remember]
 *
 * An events list state that holds locally stored
 * events from the [LocalStorage] bucket.
 *
 * @see remember
 */
@Composable
fun rememberLocallyStoredEventsListState(
    storage: LocalStorage = localStorage(),
    key: Any? = Unit
) : LocallyStoredEventsListState {
    return remember(key) {
        LocallyStoredEventsListState(storage)
    }
}
