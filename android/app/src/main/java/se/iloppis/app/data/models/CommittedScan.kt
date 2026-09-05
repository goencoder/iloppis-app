package se.iloppis.app.data.models

import kotlinx.serialization.Serializable

/** A confirmed scan retained for history and offline duplicate detection. */
@Serializable
data class CommittedScan(
    val scanId: String,
    val ticketId: String,
    val eventId: String,
    val scannedAt: String,
    val committedAt: String,
    val wasOffline: Boolean,
    val ticketType: String? = null,
    val email: String? = null,
    val status: String = "SUCCESS",
    val errorMessage: String? = null
)
