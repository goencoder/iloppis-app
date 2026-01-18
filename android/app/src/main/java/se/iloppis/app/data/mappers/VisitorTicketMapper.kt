package se.iloppis.app.data.mappers

import se.iloppis.app.domain.model.VisitorTicket
import se.iloppis.app.domain.model.VisitorTicketStatus
import se.iloppis.app.network.VisitorTicketDto
import java.time.Instant

/**
 * Maps visitor ticket DTOs to domain models.
 */
object VisitorTicketMapper {

    fun VisitorTicketDto.toDomain(): VisitorTicket = VisitorTicket(
        id = id,
        eventId = eventId,
        ticketType = ticketType,
        email = email,
        status = mapTicketStatus(status),
        issuedAt = issuedAt.toInstantOrNull(),
        validFrom = validFrom.toInstantOrNull(),
        validUntil = validUntil.toInstantOrNull(),
        scannedAt = scannedAt.toInstantOrNull()
    )

    private fun mapTicketStatus(value: String?): VisitorTicketStatus = when (value) {
        "TICKET_STATUS_SCANNED" -> VisitorTicketStatus.SCANNED
        "TICKET_STATUS_NOT_SCANNED" -> VisitorTicketStatus.NOT_SCANNED
        else -> VisitorTicketStatus.UNSPECIFIED
    }

    private fun String?.toInstantOrNull(): Instant? = this?.let { value ->
        runCatching { Instant.parse(value) }.getOrNull()
    }
}
