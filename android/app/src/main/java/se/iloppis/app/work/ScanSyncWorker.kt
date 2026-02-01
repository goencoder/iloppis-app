package se.iloppis.app.work

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import retrofit2.HttpException
import se.iloppis.app.data.CommittedScansStore
import se.iloppis.app.data.PendingScansStore
import se.iloppis.app.network.config.clientConfig
import se.iloppis.app.network.ILoppisClient
import se.iloppis.app.network.visitor.VisitorAPI
import java.io.IOException

/**
 * Background worker for syncing pending scans to the server.
 *
 * Process:
 * 1. Read all pending scans from pending_scans.jsonl
 * 2. Upload each scan to server with 5s timeout
 * 3. On success: Remove from pending file
 * 4. On 412 (already scanned): Remove from pending file
 * 5. On network error/timeout: Keep for retry with backoff
 * 6. On other errors: Mark with errorText
 */
class ScanSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val apiKey = inputData.getString(KEY_API_KEY) ?: return Result.success()
        val eventId = inputData.getString(KEY_EVENT_ID) ?: return Result.success()

        // Initialize stores for this event
        PendingScansStore.initialize(applicationContext, eventId)
        CommittedScansStore.initialize(applicationContext, eventId)

        return try {
            // Read all pending scans
            val allScans = PendingScansStore.getAllScans()
            if (allScans.isEmpty()) {
                Log.d(TAG, "No pending scans")
                return Result.success()
            }

            Log.d(TAG, "Processing ${allScans.size} pending scans")

            val api = ILoppisClient(clientConfig()).create<VisitorAPI>()
            var hasNetworkError = false

            for (scan in allScans) {
                try {
                    // Upload scan with 5s timeout
                    withTimeout(5000) {
                        api.scanTicket(
                            authorization = "Bearer $apiKey",
                            eventId = scan.eventId,
                            ticketId = scan.ticketId
                        )
                    }

                    // Success: Remove from pending
                    Log.d(TAG, "Scan ${scan.scanId} uploaded successfully")
                    PendingScansStore.removeScan(scan.scanId)

                } catch (timeout: TimeoutCancellationException) {
                    // Timeout: Keep for retry
                    Log.w(TAG, "Sync timeout for scan ${scan.scanId}")
                    hasNetworkError = true
                    break  // Stop processing on timeout

                } catch (http: HttpException) {
                    when (http.code()) {
                        412 -> {
                            // Already scanned on server: Remove from pending
                            Log.d(TAG, "Scan ${scan.scanId} already processed on server")
                            PendingScansStore.removeScan(scan.scanId)
                        }
                        else -> {
                            // Other HTTP errors: Mark with error
                            val errorMsg = http.message() ?: "HTTP ${http.code()}"
                            Log.w(TAG, "HTTP error for scan ${scan.scanId}: $errorMsg")
                            PendingScansStore.updateError(scan.scanId, errorMsg)
                        }
                    }

                } catch (io: IOException) {
                    // Network error: Keep for retry
                    Log.w(TAG, "Network error for scan ${scan.scanId}: ${io.message}")
                    hasNetworkError = true
                    break  // Stop processing on network error
                }
            }

            // If we had network errors, retry with backoff
            if (hasNetworkError) {
                Log.d(TAG, "Network error encountered, will retry with backoff")
                return Result.retry()
            }

            Result.success()

        } catch (e: Exception) {
            Log.e(TAG, "Sync failed", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "ScanSyncWorker"
        const val KEY_API_KEY = "apiKey"
        const val KEY_EVENT_ID = "eventId"
    }
}
