package se.iloppis.app.domain.model

import java.time.Instant

/**
 * Domain representation of a visitor ticket returned from the API.
 */
data class VisitorTicket(
    val id: String,
    val eventId: String,
    val ticketType: String?,
    val email: String?,
    val status: VisitorTicketStatus,
    val issuedAt: Instant?,
    val validFrom: Instant?,
    val validUntil: Instant?,
    val scannedAt: Instant?
)

/**
 * Simplified ticket status values that the UI can render.
 */
enum class VisitorTicketStatus {
    SCANNED,
    NOT_SCANNED,
    UNSPECIFIED
}
