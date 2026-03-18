import Foundation

struct VisitorTicket: Identifiable, Equatable {
    let id: String
    let eventId: String
    let ticketType: String?
    let email: String?
    let status: VisitorTicketStatus
    let issuedAt: String?
    let validFrom: String?
    let validUntil: String?
    let scannedAt: String?
}

enum VisitorTicketStatus: Equatable {
    case issued
    case scanned
    case unknown(String)

    init(raw: String?) {
        switch (raw ?? "").uppercased() {
        case "SCANNED", "TICKET_STATUS_SCANNED":
            self = .scanned
        case "ISSUED", "TICKET_STATUS_NOT_SCANNED":
            self = .issued
        default:
            self = .unknown(raw ?? "")
        }
    }
}
