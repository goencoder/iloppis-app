package se.iloppis.app.data

import se.iloppis.app.utils.AppLog as Log
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import se.iloppis.app.network.config.clientConfig
import se.iloppis.app.network.ILoppisClient
import se.iloppis.app.network.vendors.VendorAPI
import se.iloppis.app.network.vendors.VendorFilter
import se.iloppis.app.network.vendors.VendorFilterRequest
import se.iloppis.app.network.vendors.VendorPagination

private const val TAG = "VendorRepository"

/** Thread-safe cache of approved seller numbers for the initialized event. */
object VendorRepository {
    private lateinit var eventId: String
    private lateinit var apiKey: String
    private val vendorApi: VendorAPI by lazy { ILoppisClient(clientConfig()).create<VendorAPI>() }
    private val mutex = Mutex()

    @Volatile
    private var cachedSellers: Set<Int>? = null

    /** Sets the event credentials used by subsequent requests. Repeated calls replace them. */
    fun initialize(eventId: String, apiKey: String) {
        this.eventId = eventId
        this.apiKey = apiKey
        Log.d(TAG, "Initialized for event $eventId")
    }

    /** Returns whether event credentials have been supplied. */
    fun isInitialized(): Boolean = ::eventId.isInitialized && ::apiKey.isInitialized

    /** Returns cached sellers without network access, or `null` before a successful fetch. */
    fun getCached(): Set<Int>? = cachedSellers

    /**
     * Fetch approved sellers from API and update cache.
     *
     * All pages are loaded while concurrent callers wait on the same mutex.
     *
     * @throws IllegalStateException when [initialize] has not been called.
     * @throws Exception when an API request fails.
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

    /** Returns cached sellers, fetching them when the cache is empty. */
    suspend fun getOrFetch(): Set<Int> {
        return getCached() ?: refresh()
    }

    /** Clears cached sellers so the next [getOrFetch] performs a request. */
    fun clearCache() {
        cachedSellers = null
        Log.d(TAG, "Cache cleared")
    }

    /** Returns whether [sellerNumber] is in the cache; an empty cache returns `false`. */
    fun isApproved(sellerNumber: Int): Boolean {
        return cachedSellers?.contains(sellerNumber) ?: false
    }
}
