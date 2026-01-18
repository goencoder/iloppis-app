package se.iloppis.app.work

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object SyncScheduler {
    const val UNIQUE_IMMEDIATE = "iloppis-sold-items-sync"
    const val UNIQUE_PERIODIC = "iloppis-sold-items-sync-periodic"
    const val UNIQUE_SCAN_SYNC = "iloppis-scans-sync"

    fun enqueueImmediate(context: Context, apiKey: String, eventId: String) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val input = Data.Builder()
            .putString(SoldItemsSyncWorker.KEY_API_KEY, apiKey)
            .putString(SoldItemsSyncWorker.KEY_EVENT_ID, eventId)
            .build()

        val request = OneTimeWorkRequestBuilder<SoldItemsSyncWorker>()
            .setConstraints(constraints)
            .setInputData(input)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(UNIQUE_IMMEDIATE, ExistingWorkPolicy.REPLACE, request)
    }

    fun ensurePeriodic(context: Context, apiKey: String, eventId: String) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val input = Data.Builder()
            .putString(SoldItemsSyncWorker.KEY_API_KEY, apiKey)
            .putString(SoldItemsSyncWorker.KEY_EVENT_ID, eventId)
            .build()

        val request = PeriodicWorkRequestBuilder<SoldItemsSyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .setInputData(input)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(UNIQUE_PERIODIC, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    /**
     * Enqueue immediate scan sync with exponential backoff.
     * Used when offline scans are detected.
     */
    fun enqueueScanSync(context: Context, apiKey: String, eventId: String) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val input = Data.Builder()
            .putString(ScanSyncWorker.KEY_API_KEY, apiKey)
            .putString(ScanSyncWorker.KEY_EVENT_ID, eventId)
            .build()

        val request = OneTimeWorkRequestBuilder<ScanSyncWorker>()
            .setConstraints(constraints)
            .setInputData(input)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                5, // Initial delay: 5 seconds
                TimeUnit.SECONDS
            )
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(UNIQUE_SCAN_SYNC, ExistingWorkPolicy.REPLACE, request)
    }
}
