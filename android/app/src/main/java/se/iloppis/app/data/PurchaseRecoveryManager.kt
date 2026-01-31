package se.iloppis.app.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import se.iloppis.app.data.models.RejectedPurchase
import se.iloppis.app.data.models.RecoveryResult
import se.iloppis.app.data.models.SerializableSoldItemErrorCode
import retrofit2.HttpException
import se.iloppis.app.network.cashier.CashierAPI
import se.iloppis.app.network.cashier.RejectedItem
import se.iloppis.app.network.cashier.SoldItemObject
import se.iloppis.app.network.config.clientConfig
import se.iloppis.app.network.iLoppisClient
import java.io.IOException
import java.net.SocketTimeoutException

private const val TAG = "PurchaseRecoveryManager"

/**
 * Handles auto-recovery of rejected purchases.
 *
 * Implements the recovery strategy defined in Issue #001b:
 * - INVALID_SELLER: Re-fetch approved sellers → Retry upload
 * - DUPLICATE_RECEIPT: Mark as uploaded locally (silent success)
 * - Server errors (5xx): Save to pending_uploads with exponential backoff
 * - Network errors: Save to pending_uploads for retry
 *
 * Max 1 auto-recovery attempt per purchase.
 */
class PurchaseRecoveryManager(
    private val eventId: String,
    private val apiKey: String
) {
    private val soldItemsApi = iLoppisClient(clientConfig()).create<CashierAPI>()
    // Use global singleton VendorRepository (initialized by CashierViewModel)

    /**
     * Classify an error from backend/network into a recovery strategy.
     *
     * Uses manual error code parsing from backend API response.
     * Backend sends proto enum values as strings/numbers which we parse to our enum.
     *
     * Strategy:
     * 1. If we have structured rejectedItem with error_code → classify per-item
     * 2. If we have HTTP error without structured response → mark all items with that error
     */
    fun classifyError(error: Throwable, rejectedItem: RejectedItem? = null): ErrorType {
        // Priority 1: Check structured error code from backend
        if (rejectedItem != null && rejectedItem.errorCode != null) {
            // Backend sends error code as string, parse it
            val errorCodeEnum = SerializableSoldItemErrorCode.fromString(rejectedItem.errorCode)

            return when (errorCodeEnum) {
                SerializableSoldItemErrorCode.INVALID_SELLER -> ErrorType.InvalidSeller
                SerializableSoldItemErrorCode.DUPLICATE_RECEIPT -> ErrorType.Duplicate
                SerializableSoldItemErrorCode.UNSPECIFIED -> ErrorType.ValidationError(rejectedItem.reason)
            }
        }

        // Priority 2: Check HTTP status codes
        return when (error) {
            is HttpException -> when {
                error.code() >= 500 -> ErrorType.ServerError
                error.code() == 400 -> ErrorType.ValidationError(error.message())
                else -> ErrorType.Unknown(error.message ?: "HTTP ${error.code()}")
            }
            is IOException, is SocketTimeoutException -> ErrorType.NetworkError
            else -> ErrorType.Unknown(error.message ?: "Unknown error")
        }
    }

    /**
     * Attempt auto-recovery for INVALID_SELLER error.
     *
     * Strategy:
     * 1. Re-fetch approved sellers from API via VendorRepository
     * 2. Repository updates its cache automatically
     * 3. Validate purchase items against fresh seller list
     * 4. If all sellers now valid → Retry upload
     * 5. If still invalid → Return NeedsManualReview
     */
    suspend fun handleInvalidSellerError(purchase: RejectedPurchase): RecoveryResult {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Auto-recovery: Re-fetching approved sellers for event $eventId")

                // Step 1: Fetch fresh seller list from backend (updates global repository cache)
                val freshSellers = VendorRepository.refresh()

                // Step 2: Validate all sellers in the purchase
                val invalidSellers = purchase.items
                    .map { it.item.seller }
                    .filter { it !in freshSellers }
                    .distinct()

                if (invalidSellers.isEmpty()) {
                    // Step 3: All sellers now valid → Retry upload
                    Log.d(TAG, "Auto-recovery: All sellers now valid, retrying upload")
                    return@withContext retryUpload(purchase)
                } else {
                    // Step 4: Still invalid → Manual review needed
                    Log.d(TAG, "Auto-recovery: Still invalid sellers: $invalidSellers")
                    return@withContext RecoveryResult.NeedsManualReview(invalidSellers)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Auto-recovery failed: ${e.message}", e)
                return@withContext RecoveryResult.Failed("Could not refresh sellers: ${e.message}")
            }
        }
    }

    /**
     * Handle DUPLICATE_RECEIPT error by marking items as uploaded.
     * This is a silent success - no user notification needed.
     */
    fun handleDuplicateError(purchase: RejectedPurchase) {
        Log.d(TAG, "Handling duplicate receipt: ${purchase.purchaseId}")

        // Mark all items in this purchase as uploaded
        val allItems = SoldItemFileStore.getAllSoldItems()
        val updated = allItems.map { item ->
            if (item.purchaseId == purchase.purchaseId) {
                item.copy(uploaded = true)
            } else {
                item
            }
        }

        SoldItemFileStore.saveSoldItems(updated)
        Log.d(TAG, "Marked purchase ${purchase.purchaseId} as uploaded (duplicate)")
    }

    /**
     * Retry uploading a purchase to backend.
     */
    private suspend fun retryUpload(purchase: RejectedPurchase): RecoveryResult {
        return try {
            val requestItems = purchase.items.map {
                SoldItemObject(
                    itemId = it.item.itemId,
                    purchaseId = it.item.purchaseId,
                    seller = it.item.seller,
                    price = it.item.price,
                    paymentMethod = it.item.paymentMethod
                )
            }

            val response = soldItemsApi.createSoldItems(
                authorization = "Bearer $apiKey",
                eventId = eventId,
                request = CreateSoldItemsRequest(requestItems)
            )

            // Check if upload succeeded
            val acceptedIds = response.acceptedItems?.mapNotNull { it.itemId }?.toSet() ?: emptySet()
            val allItemIds = purchase.items.map { it.item.itemId }.toSet()

            if (acceptedIds.containsAll(allItemIds)) {
                // All items accepted → Mark as uploaded
                val allItems = SoldItemFileStore.getAllSoldItems()
                val updated = allItems.map { item ->
                    if (item.purchaseId == purchase.purchaseId) {
                        item.copy(uploaded = true)
                    } else {
                        item
                    }
                }
                SoldItemFileStore.saveSoldItems(updated)

                Log.d(TAG, "Retry upload succeeded for purchase ${purchase.purchaseId}")
                RecoveryResult.Success
            } else {
                RecoveryResult.Failed("Some items still rejected")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Retry upload failed: ${e.message}", e)
            RecoveryResult.Failed("Upload failed: ${e.message}")
        }
    }
}

/**
 * Error types for classification.
 */
sealed class ErrorType {
    /** Server error (5xx) - needs support, show warning */
    data object ServerError : ErrorType()

    /** Invalid seller - auto-recovery possible */
    data object InvalidSeller : ErrorType()

    /** Duplicate receipt - auto-fix by marking uploaded */
    data object Duplicate : ErrorType()

    /** Network error - auto-retry */
    data object NetworkError : ErrorType()

    /** Other validation error */
    data class ValidationError(val message: String) : ErrorType()

    /** Unknown error */
    data class Unknown(val message: String) : ErrorType()
}
