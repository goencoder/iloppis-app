package se.iloppis.app.data

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import se.iloppis.app.data.models.PendingItem
import java.io.File

/**
 * One-time migration from old dual-file system to new single-file JSONL system.
 *
 * Migrates:
 * - sold_items.json (uploaded=false) → pending_items.jsonl with errorText=""
 * - pending_review.json (rejected purchases) → pending_items.jsonl with errorText from rejection reason
 *
 * After successful migration, renames old files to .migrated to prevent re-processing.
 */
object MigrationManager {
    private const val TAG = "MigrationManager"
    private const val MIGRATION_FLAG = "migration_v1_completed"

    /**
     * Run migration if not already completed.
     * Safe to call multiple times - migration only runs once.
     */
    suspend fun runMigrationIfNeeded(context: Context) = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences("migration", Context.MODE_PRIVATE)
        if (prefs.getBoolean(MIGRATION_FLAG, false)) {
            Log.d(TAG, "Migration already completed, skipping")
            return@withContext
        }

        Log.i(TAG, "Starting migration from old system to pending_items.jsonl")

        try {
            val pendingItems = mutableListOf<PendingItem>()

            // Migrate sold_items.json (uploaded=false items)
            val soldItems = migrateSoldItems(context)
            pendingItems.addAll(soldItems)
            Log.d(TAG, "Migrated ${soldItems.size} pending sold items")

            // Migrate pending_review.json (rejected purchases)
            val rejectedItems = migrateRejectedPurchases(context)
            pendingItems.addAll(rejectedItems)
            Log.d(TAG, "Migrated ${rejectedItems.size} rejected items")

            // Write all to pending_items.jsonl
            if (pendingItems.isNotEmpty()) {
                PendingItemsStore.appendItems(pendingItems)
                Log.i(TAG, "Successfully migrated ${pendingItems.size} total items to pending_items.jsonl")
            }

            // Rename old files to prevent re-processing
            renameOldFiles(context)

            // Mark migration complete
            prefs.edit { putBoolean(MIGRATION_FLAG, true) }
            Log.i(TAG, "Migration completed successfully")

        } catch (e: Exception) {
            Log.e(TAG, "Migration failed", e)
            // Don't mark as complete - will retry on next app start
        }
    }

    private fun migrateSoldItems(context: Context): List<PendingItem> {
        val items = mutableListOf<PendingItem>()

        try {
            val allSoldItems = SoldItemFileStore.getAllSoldItems()
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
