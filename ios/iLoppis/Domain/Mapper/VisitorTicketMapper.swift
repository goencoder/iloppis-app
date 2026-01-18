import Foundation

enum VisitorTicketMapper {
    static func toDomain(_ dto: VisitorTicketDto) -> VisitorTicket {
        VisitorTicket(
            id: dto.id,
            eventId: dto.eventId,
            ticketType: dto.ticketType,
            email: dto.email,
            status: VisitorTicketStatus(raw: dto.status),
            issuedAt: dto.issuedAt,
            validFrom: dto.validFrom,
            validUntil: dto.validUntil,
            scannedAt: dto.scannedAt
        )
    }
}
