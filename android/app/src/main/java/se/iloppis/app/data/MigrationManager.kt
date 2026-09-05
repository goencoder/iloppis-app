package se.iloppis.app.data

import android.content.Context
import se.iloppis.app.utils.AppLog as Log
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import se.iloppis.app.data.models.PendingItem
import se.iloppis.app.data.models.StoredSoldItem
import java.io.File

/** Migrates legacy sold and rejected purchase files to the pending-items JSONL store. */
object MigrationManager {
    private const val TAG = "MigrationManager"
    private const val MIGRATION_FLAG = "migration_v1_completed"

    /** Runs the migration once; failures remain eligible for retry on the next launch. */
    suspend fun runMigrationIfNeeded(context: Context) = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences("migration", Context.MODE_PRIVATE)
        if (prefs.getBoolean(MIGRATION_FLAG, false)) {
            Log.d(TAG, "Migration already completed, skipping")
            return@withContext
        }

        Log.i(TAG, "Starting migration from old system to pending_items.jsonl")

        try {
            val pendingItems = mutableListOf<PendingItem>()

            val soldItems = migrateSoldItems(context)
            pendingItems.addAll(soldItems)
            Log.d(TAG, "Migrated ${soldItems.size} pending sold items")

            val rejectedItems = migrateRejectedPurchases(context)
            pendingItems.addAll(rejectedItems)
            Log.d(TAG, "Migrated ${rejectedItems.size} rejected items")

            if (pendingItems.isNotEmpty()) {
                PendingItemsStore.appendItems(pendingItems)
                Log.i(TAG, "Successfully migrated ${pendingItems.size} total items to pending_items.jsonl")
            }

            renameOldFiles(context)

            prefs.edit { putBoolean(MIGRATION_FLAG, true) }
            Log.i(TAG, "Migration completed successfully")

        } catch (e: Exception) {
            Log.e(TAG, "Migration failed", e)
            // Leave the flag unset so the next launch retries.
        }
    }

    private val json = Json { ignoreUnknownKeys = true }

    private fun migrateSoldItems(context: Context): List<PendingItem> {
        val items = mutableListOf<PendingItem>()

        try {
            val file = File(context.filesDir, "sold_items.json")
            if (!file.exists()) return items
            val content = file.readText()
            if (content.isBlank()) return items
            val allSoldItems = json.decodeFromString<List<StoredSoldItem>>(content)
            val pending = allSoldItems.filter { !it.uploaded }

            pending.forEach { sold ->
                items.add(
                    PendingItem(
                        itemId = sold.itemId,
                        purchaseId = sold.purchaseId,
                        sellerId = sold.seller,
                        price = sold.price,
                        errorText = "", // Waiting for upload
                        timestamp = java.time.Instant.ofEpochMilli(sold.soldTime).toString()
                    )
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to migrate sold_items.json: ${e.message}")
        }

        return items
    }

    private fun migrateRejectedPurchases(context: Context): List<PendingItem> {
        val items = mutableListOf<PendingItem>()

        try {
            val rejectedPurchases = RejectedPurchaseStore.getAllRejectedPurchases()

            rejectedPurchases.forEach { rejected ->
                rejected.items.forEach { rejectedItem ->
                    items.add(
                        PendingItem(
                            itemId = rejectedItem.item.itemId,
                            purchaseId = rejected.purchaseId,
                            sellerId = rejectedItem.item.seller,
                            price = rejectedItem.item.price,
                            errorText = rejectedItem.reason.ifBlank { "Okänt fel" },
                            timestamp = rejected.timestamp // Already ISO-8601 string
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to migrate pending_review.json: ${e.message}")
        }

        return items
    }

    private fun renameOldFiles(context: Context) {
        try {
            val soldItemsFile = File(context.filesDir, "sold_items.json")
            if (soldItemsFile.exists()) {
                soldItemsFile.renameTo(File(context.filesDir, "sold_items.json.migrated"))
                Log.d(TAG, "Renamed sold_items.json to .migrated")
            }

            val rejectedFile = File(context.filesDir, "pending_review.json")
            if (rejectedFile.exists()) {
                rejectedFile.renameTo(File(context.filesDir, "pending_review.json.migrated"))
                Log.d(TAG, "Renamed pending_review.json to .migrated")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to rename old files: ${e.message}")
        }
    }
}
