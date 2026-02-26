package se.iloppis.app.data

import android.content.Context
import android.util.Log

/**
 * Centralised one-call initialisation for all event-scoped file stores.
 *
 * Call [initializeForEvent] **once** when the user enters an event context
 * (e.g. in CashierViewModel.init).  The method is idempotent – subsequent
 * calls with the same (context, eventId) pair are no-ops.
 */
object EventStoreManager {

    private const val TAG = "EventStoreManager"

    @Volatile
    private var currentEventId: String? = null

    /**
     * Initialise event-scoped file stores for the given event.
     *
     * Safe to call multiple times; re-initialises only when the
     * eventId changes.
     */
    fun initializeForEvent(context: Context, eventId: String) {
        if (eventId == currentEventId) return          // already done
        synchronized(this) {
            if (eventId == currentEventId) return       // double-check
            Log.d(TAG, "Initialising stores for event $eventId")
            PendingItemsStore.initialize(context, eventId)
            SoldItemFileStore.initialize(context, eventId)
            RejectedPurchaseStore.initialize(context, eventId)
            PendingScansStore.initialize(context, eventId)
            CommittedScansStore.initialize(context, eventId)
            currentEventId = eventId
        }
    }
}
