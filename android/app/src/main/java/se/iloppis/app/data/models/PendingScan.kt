package se.iloppis.app.data.models

import kotlinx.serialization.Serializable

/**
 * Represents a single scan pending upload to the backend.
 * 
 * This scan is stored in pending_scans.jsonl (one JSON object per line).
 * Row existence = pending, deleted row = uploaded successfully.
 * 
 * @property scanId Unique identifier for this scan (UUID)
 * @property ticketId Ticket identifier from QR code
 * @property eventId Event context where scan occurred
 * @property scannedAt ISO-8601 timestamp when scan was performed
 * @property errorText Error message from backend/server. Empty = waiting/retry, text = has error
 */
@Serializable
data class PendingScan(
    val scanId: String,
    val ticketId: String,
    val eventId: String,
    val scannedAt: String,
    val errorText: String = ""
)
