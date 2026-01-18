package se.iloppis.app.data

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Manages the offline toast gating logic for the cashier flow.
 * 
 * Rules:
 * - Toast shows ONLY after 2 missed uploads for the SAME purchase (purchaseId)
 * - Once shown, suppress until confirmed online
 * - After confirmed online, reset and allow showing again after 2 missed uploads for a NEW purchase
 * 
 * Definitions:
 * - "Missed upload" = an upload attempt for that purchase fails due to network/DNS/timeout
 *   OR can't start due to no connectivity
 * - "Confirmed online" = at least one successful API call (e.g. successful upload in worker)
 */
class OfflineToastGatekeeper {
    private val mutex = Mutex()
    
    // Track consecutive failed uploads per purchaseId
    private val failedUploadsPerPurchase = mutableMapOf<String, Int>()
    
    // Track if toast has been shown and is suppressed until online confirmation
    private var toastSuppressed = false
    
    /**
     * Record a missed upload attempt for a purchase.
     * 
     * @param purchaseId The purchase that failed to upload
     * @return true if the toast should be shown now, false otherwise
     */
    suspend fun recordMissedUpload(purchaseId: String): Boolean {
        mutex.withLock {
            // If toast is already suppressed, don't show again
            if (toastSuppressed) {
                return false
            }
            
            // Increment failure count for this purchase
            val currentCount = failedUploadsPerPurchase.getOrDefault(purchaseId, 0)
            val newCount = currentCount + 1
            failedUploadsPerPurchase[purchaseId] = newCount
            
            // Show toast if this is the second failure for this purchase
            if (newCount == 2) {
                toastSuppressed = true
                return true
            }
            
            return false
        }
    }
    
    /**
     * Record a successful upload (confirmed online).
     * This resets the gating logic and allows toast to show again in the future.
     */
    suspend fun recordSuccessfulUpload() {
        mutex.withLock {
            // Clear all tracked failures
            failedUploadsPerPurchase.clear()
            // Re-enable toast
            toastSuppressed = false
        }
    }
    
    /**
     * Check if the toast is currently suppressed.
     * @return true if toast should not be shown even if uploads fail
     */
    suspend fun isToastSuppressed(): Boolean {
        mutex.withLock {
            return toastSuppressed
        }
    }
    
    /**
     * Get the current failure count for a specific purchase.
     * Useful for testing and debugging.
     */
    suspend fun getFailureCount(purchaseId: String): Int {
        mutex.withLock {
            return failedUploadsPerPurchase.getOrDefault(purchaseId, 0)
        }
    }
    
    /**
     * Reset all state (for testing purposes).
     */
    suspend fun reset() {
        mutex.withLock {
            failedUploadsPerPurchase.clear()
            toastSuppressed = false
        }
    }
}
