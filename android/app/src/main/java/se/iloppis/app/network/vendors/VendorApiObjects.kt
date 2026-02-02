package se.iloppis.app.network.vendors

/**
 * iLoppis API vendor object
 */
data class ApiVendor(
    /**
     * Vendor ID
     */
    val id: String,

    /**
     * Seller number of the vendor
     */
    val sellerNumber: Int,

    /**
     * Vendor first name
     */
    val firstName: String?,
    /**
     * Vendor last name
     */
    val lastName: String?,

    /**
     * Vendor email
     */
    val email: String?,
    /**
     * Vendor number
     */
    val phone: String?,

    /**
     * Vendor status
     *
     * This indicates the vendors status
     * in an event.
     */
    val status: String?
)

/**
 * Vendor filter object
 */
data class VendorFilter(
    /**
     * Filter after status
     */
    val status: String? = null,

    /**
     * Filter after a specific seller number
     */
    val sellerNumber: Int? = null,

    /**
     * Filter after a specific email
     */
    val email: String? = null,

    /**
     * Filter a search text
     */
    val searchText: String? = null
)

/**
 * Vendor sort order object
 */
data class VendorSortOrder(
    /**
     * The field to sort after
     */
    val field: String = "seller_number",

    /**
     * If the sort order will be ascending or descending
     */
    val ascending: Boolean = true
)

/**
 * Vendor pagination object
 */
data class VendorPagination(
    /**
     * The size of a page
     *
     * This is the amount of vendors
     * to take in per page.
     *
     * **Default 100**
     */
    val pageSize: Int = 100,

    /**
     * Next page token
     */
    val nextPageToken: String? = null
)

/**
 * Vendor filter request object
 */
data class VendorFilterRequest(
    /**
     * Request filter
     */
    val filter: VendorFilter = VendorFilter(),

    /**
     * Sort order
     */
    val sort: VendorSortOrder? = null,

    /**
     * Request pagination
     */
    val pagination: VendorPagination = VendorPagination()
)

/**
 * Vendor API response object
 */
data class VendorApiResponse(
    /**
     * List of vendors
     */
    val vendors: List<ApiVendor>,

    /**
     * Next page token
     */
    val nextPageToken: String?
)
