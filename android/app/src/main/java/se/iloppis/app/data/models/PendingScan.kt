package se.iloppis.app.data.models

import kotlinx.serialization.Serializable

/** A ticket scan retained locally until its backend upload succeeds. */
@Serializable
data class PendingScan(
    val scanId: String,
    val ticketId: String,
    val eventId: String,
    val scannedAt: String,
    val errorText: String = ""
)
