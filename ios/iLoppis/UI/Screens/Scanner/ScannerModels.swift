import Foundation

// Uses domain model `VisitorTicket` from `Domain/Model/VisitorTicket.swift`.

enum ManualEntryError: Equatable {
    case emptyInput
    case wrongEvent
    case invalidFormat
}

enum ScanStatus: Equatable {
    case success
    case duplicate
    case invalid
    case offline
    case error
}

struct ScanResult: Identifiable, Equatable {
    let id: String
    let ticket: VisitorTicket?
    let status: ScanStatus
    let timestamp: Date
    let message: String?
    let offline: Bool

    init(
        id: String = UUID().uuidString,
        ticket: VisitorTicket?,
        status: ScanStatus,
        timestamp: Date = Date(),
        message: String? = nil,
        offline: Bool = false
    ) {
        self.id = id
        self.ticket = ticket
        self.status = status
        self.timestamp = timestamp
        self.message = message
        self.offline = offline
    }
}

struct PendingScan: Identifiable, Equatable {
    let id: String
    let ticketId: String
    let createdAt: Date

    init(ticketId: String, createdAt: Date = Date()) {
        self.id = UUID().uuidString
        self.ticketId = ticketId
        self.createdAt = createdAt
    }
}

struct ScannerState: Equatable {
    let eventName: String

    var isProcessing: Bool = false
    var manualEntryVisible: Bool = false
    var manualEntryError: ManualEntryError? = nil

    var activeResult: ScanResult? = nil
    var history: [ScanResult] = []
    var pendingScans: [PendingScan] = []

    var pendingCount: Int { pendingScans.count }
}

enum ScannerAction {
    case requestManualEntry
    case dismissManualEntry
    case submitCode(String)
    case clearManualError
    case dismissResult
}
