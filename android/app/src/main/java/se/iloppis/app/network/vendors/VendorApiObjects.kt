package se.iloppis.app.network.vendors

/** Seller representation returned by the vendor API. */
data class ApiVendor(
    val id: String,
    val sellerNumber: Int,
    val firstName: String?,
    val lastName: String?,
    val email: String?,
    val phone: String?,
    val status: String?
)

/** Optional criteria sent to the vendor-filter endpoint. */
data class VendorFilter(
    val status: String? = null,
    val sellerNumber: Int? = null,
    val email: String? = null,
    val searchText: String? = null
)

/** Sort order for filtered vendor searches. */
data class VendorSortOrder(
    val field: String = "seller_number",
    val ascending: Boolean = true
)

/** Cursor pagination for vendor searches. */
data class VendorPagination(
    val pageSize: Int = 100,
    val nextPageToken: String? = null
)

/** Request envelope for filtered vendor searches. */
data class VendorFilterRequest(
    val filter: VendorFilter = VendorFilter(),
    val sort: VendorSortOrder? = null,
    val pagination: VendorPagination = VendorPagination()
)

/** Page returned by a vendor search. */
data class VendorApiResponse(
    val vendors: List<ApiVendor>,
    val nextPageToken: String?
)
