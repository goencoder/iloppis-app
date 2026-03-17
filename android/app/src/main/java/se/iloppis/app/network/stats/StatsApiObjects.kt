package se.iloppis.app.network.stats

import com.google.gson.annotations.SerializedName

data class LiveStatsApiResponse(
    @SerializedName(value = "event_id", alternate = ["eventId"])
    val eventId: String,
    @SerializedName("event_name")
    val eventName: String?,
    val cashiers: LiveCashierStats?,
    val tickets: LiveTicketStats?,
    @SerializedName(value = "cashier_statuses", alternate = ["cashierStatuses"])
    val cashierStatuses: List<LiveCashierStatus>? = emptyList(),
    @SerializedName(value = "generated_at", alternate = ["generatedAt"])
    val generatedAt: String?,
    @SerializedName(value = "event_image_url", alternate = ["eventImageUrl"])
    val eventImageUrl: String?,
    @SerializedName(value = "event_city", alternate = ["eventCity"])
    val eventCity: String?,
    @SerializedName(value = "event_start_time", alternate = ["eventStartTime"])
    val eventStartTime: String?,
    @SerializedName(value = "event_end_time", alternate = ["eventEndTime"])
    val eventEndTime: String?,
    val sales: LiveSalesStats?
)

data class LiveCashierStats(
    @SerializedName(value = "open_count", alternate = ["openCount"])
    val openCount: Int = 0,
    @SerializedName(value = "processing_count", alternate = ["processingCount"])
    val processingCount: Int = 0,
    @SerializedName(value = "stalled_count", alternate = ["stalledCount"])
    val stalledCount: Int = 0
)

data class LiveTicketStats(
    @SerializedName(value = "booked_count", alternate = ["bookedCount"])
    val bookedCount: Int = 0,
    @SerializedName(value = "scanned_count", alternate = ["scannedCount"])
    val scannedCount: Int = 0,
    @SerializedName(value = "not_scanned_count", alternate = ["notScannedCount"])
    val notScannedCount: Int = 0
)

data class LiveSalesStats(
    @SerializedName(value = "purchases_total", alternate = ["purchasesTotal"])
    val purchasesTotal: Int = 0,
    @SerializedName(value = "items_total", alternate = ["itemsTotal"])
    val itemsTotal: Int = 0,
    @SerializedName(value = "revenue_total_sek", alternate = ["revenueTotalSek"])
    val revenueTotalSek: Long = 0
)

data class LiveCashierStatus(
    @SerializedName(value = "display_name", alternate = ["displayName"])
    val displayName: String?,
    val state: String?,
    @SerializedName(value = "last_heartbeat_at", alternate = ["lastHeartbeatAt"])
    val lastHeartbeatAt: String?,
    @SerializedName(value = "last_purchase_at", alternate = ["lastPurchaseAt"])
    val lastPurchaseAt: String?,
    @SerializedName(value = "pending_purchases_count", alternate = ["pendingPurchasesCount"])
    val pendingPurchasesCount: Int = 0,
    @SerializedName(value = "client_type", alternate = ["clientType"])
    val clientType: String?
)
