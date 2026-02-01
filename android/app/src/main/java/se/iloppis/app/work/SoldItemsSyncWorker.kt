package se.iloppis.app.work

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import se.iloppis.app.data.PendingItemsStore
import se.iloppis.app.network.cashier.CashierAPI
import se.iloppis.app.network.cashier.PaymentMethod
import se.iloppis.app.network.cashier.SoldItemObject
import se.iloppis.app.network.cashier.SoldItemsRequest
import se.iloppis.app.network.config.clientConfig
import se.iloppis.app.network.ILoppisClient

/**
 * Simplified SyncWorker using pending_items.jsonl with mutex protection.
 *
 * Process:
 * 1. Read all pending items from JSONL
 * 2. Group by purchaseId (oldest first)
 * 3. Upload each purchase
 * 4. On success: delete accepted items, update errorText for rejected
 * 5. On network error: keep items with errorText="" for retry
 * 6. On server error: mark items with errorText="serverfel"
 *
 * ~100 lines vs 370 lines in old implementation.
 */
class SoldItemsSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val apiKey = inputData.getString(KEY_API_KEY) ?: return Result.success()
        val eventId = inputData.getString(KEY_EVENT_ID) ?: return Result.success()

        // Initialize store for this event
        PendingItemsStore.initialize(applicationContext, eventId)

        return try {
            // Read all pending items
            val allItems = PendingItemsStore.readAll()
            if (allItems.isEmpty()) {
                Log.d(TAG, "No pending items")
                return Result.success()
            }

            // Group by purchaseId, process oldest first
            val byPurchase = allItems
                .groupBy { it.purchaseId }
                .toList()
                .sortedBy { (_, items) -> items.minOf { it.timestamp } }

            Log.d(TAG, "Processing ${byPurchase.size} purchases with ${allItems.size} total items")

            val api = ILoppisClient(clientConfig()).create<CashierAPI>()

            for ((purchaseId, items) in byPurchase) {
                try {
                    // Upload purchase
                    val response = api.createSoldItems(
                        authorization = "Bearer $apiKey",
                        eventId = eventId,
                        request = SoldItemsRequest(
                            items.map {
                                SoldItemObject(
                                    itemId = it.itemId,
                                    purchaseId = it.purchaseId,
                                    seller = it.sellerId,
                                    price = it.price,
                                    paymentMethod = PaymentMethod.CASH
                                )
                            }
                        )
                    )

                    // Delete accepted items (row deletion = uploaded successfully)
                    val acceptedIds = response.acceptedItems?.mapNotNull { it.itemId }?.toSet() ?: emptySet()
                    if (acceptedIds.isNotEmpty()) {
                        Log.d(TAG, "Purchase $purchaseId: ${acceptedIds.size} items accepted")
                        PendingItemsStore.updateItems(purchaseId) { item ->
                            if (item.itemId in acceptedIds) null else item
                        }
                    }

                    // Update errorText for rejected items
                    response.rejectedItems?.forEach { rejected ->
                        val reason = rejected.reason.ifBlank { "Okänt fel" }
                        Log.w(TAG, "Item ${rejected.item.itemId}: $reason")

                        PendingItemsStore.updateItems(purchaseId) { item ->
                            if (item.itemId == rejected.item.itemId) {
                                item.copy(errorText = reason)
                            } else {
                                item
                            }
                        }
                    }

                } catch (e: retrofit2.HttpException) {
                    // Server error (5xx) → mark as "serverfel"
                    if (e.code() >= 500) {
                        Log.e(TAG, "Server error for purchase $purchaseId: ${e.code()}")
                        PendingItemsStore.updateItems(purchaseId) { item ->
                            item.copy(errorText = "serverfel")
                        }
                    } else {
                        // Other HTTP errors (4xx) → keep for retry, might be temporary
                        Log.w(TAG, "HTTP ${e.code()} for purchase $purchaseId: ${e.message()}")
                    }
                } catch (e: Exception) {
                    // Network error → keep items with errorText="" for retry
                    Log.w(TAG, "Network error for purchase $purchaseId: ${e.message}")
                }
            }

            Result.success()

        } catch (e: Exception) {
            Log.e(TAG, "Sync failed", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "SoldItemsSyncWorker"
        const val KEY_API_KEY = "apiKey"
        const val KEY_EVENT_ID = "eventId"
    }
}
