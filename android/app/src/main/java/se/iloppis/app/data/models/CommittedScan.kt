package se.iloppis.app.data.models

import kotlinx.serialization.Serializable

/**
 * Represents a scan that has been committed (confirmed either online or offline).
 * 
 * This scan is stored in committed_scans.jsonl (one JSON object per line).
 * Used for offline duplicate detection and scan history.
 * 
 * @property scanId Unique identifier for this scan (same as PendingScan if it was pending)
 * @property ticketId Ticket identifier from QR code
 * @property eventId Event context where scan occurred
 * @property scannedAt ISO-8601 timestamp when scan was performed
 * @property committedAt ISO-8601 timestamp when scan was committed
 * @property wasOffline true = offline check, false = server check
 */
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
    val status: String = "SUCCESS", // SUCCESS, DUPLICATE, INVALID, ERROR, OFFLINE_SUCCESS
    val errorMessage: String? = null
)
