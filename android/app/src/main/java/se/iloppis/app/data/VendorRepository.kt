package se.iloppis.app.data

import android.util.Log
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import se.iloppis.app.network.config.clientConfig
import se.iloppis.app.network.ILoppisClient
import se.iloppis.app.network.vendors.VendorAPI
import se.iloppis.app.network.vendors.VendorFilter
import se.iloppis.app.network.vendors.VendorFilterRequest
import se.iloppis.app.network.vendors.VendorPagination

private const val TAG = "VendorRepository"

/**
 * Global singleton for approved sellers/vendors.
 *
 * Provides thread-safe cached access to approved seller numbers for an event.
 * Must be initialized once with event ID and API key before use.
 *
 * ## Usage
 *
 * ```kotlin
 * // Initialize once (e.g., in CashierViewModel init)
 * VendorRepository.initialize(eventId, apiKey)
 *
 * // Anywhere in app
 * val cached = VendorRepository.getCached()
 * val fresh = VendorRepository.refresh()
 * val sellers = VendorRepository.getOrFetch()
 * val isValid = VendorRepository.isApproved(sellerNumber)
 * ```
 *
 * ## Thread Safety
 *
 * Uses Mutex to ensure only one fetch operation at a time, preventing duplicate API calls
 * when multiple coroutines request seller data simultaneously.
 */
object VendorRepository {
    private lateinit var eventId: String
    private lateinit var apiKey: String
    private val vendorApi: VendorAPI by lazy { ILoppisClient(clientConfig()).create<VendorAPI>() }
    private val mutex = Mutex()

    @Volatile
    private var cachedSellers: Set<Int>? = null

    /**
     * Initialize the repository with event ID and API key.
     * Must be called once before any other methods.
     * Safe to call multiple times - will update credentials.
     */
    fun initialize(eventId: String, apiKey: String) {
        this.eventId = eventId
        this.apiKey = apiKey
        Log.d(TAG, "Initialized for event $eventId")
    }

    /**
     * Check if repository is initialized.
     */
    fun isInitialized(): Boolean = ::eventId.isInitialized && ::apiKey.isInitialized

    /**
     * Get cached approved sellers without making an API call.
     * Returns null if cache is empty (never fetched).
     */
    fun getCached(): Set<Int>? = cachedSellers

    /**
     * Fetch approved sellers from API and update cache.
     *
     * Paginates through all pages to ensure complete seller list.
     * Thread-safe: Multiple concurrent calls will wait for first fetch to complete.
     *
     * @return Set of approved seller numbers
     * @throws Exception if API call fails or not initialized
     */
    suspend fun refresh(): Set<Int> {
        check(isInitialized()) { "VendorRepository not initialized. Call initialize() first." }

        mutex.withLock {
            try {
                Log.d(TAG, "Fetching approved sellers for event $eventId")

                val vendors = mutableListOf<Int>()
                var nextPageToken: String? = null

                do {
                    val response = vendorApi.get(
                        authorization = "Bearer $apiKey",
                        eventId = eventId,
                        request = VendorFilterRequest(
                            filter = VendorFilter(status = "approved"),
                            pagination = VendorPagination(
                                pageSize = 100,
                                nextPageToken = nextPageToken
                            )
                        )
                    )

                    vendors.addAll(response.vendors.map { it.sellerNumber })
                    nextPageToken = response.nextPageToken
                } while (!nextPageToken.isNullOrEmpty())

                val sellerSet = vendors.toSet()
                cachedSellers = sellerSet

                Log.d(TAG, "Fetched ${sellerSet.size} approved sellers, cache updated")
                return sellerSet
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch approved sellers", e)
                throw e
            }
        }
    }

    /**
     * Get approved sellers from cache, or fetch from API if cache is empty.
     *
     * Convenience method that combines getCached() and refresh().
     *
     * @return Set of approved seller numbers
     * @throws Exception if API call fails and cache is empty
     */
    suspend fun getOrFetch(): Set<Int> {
        return getCached() ?: refresh()
    }

    /**
     * Clear the cache, forcing next access to fetch fresh data.
     */
    fun clearCache() {
        cachedSellers = null
        Log.d(TAG, "Cache cleared")
    }

    /**
     * Check if a seller number is approved.
     * Uses cached data if available.
     *
     * @param sellerNumber The seller number to check
     * @return true if seller is approved (and cache is populated), false otherwise
     */
    fun isApproved(sellerNumber: Int): Boolean {
        return cachedSellers?.contains(sellerNumber) ?: false
    }
}
