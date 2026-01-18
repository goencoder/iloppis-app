import Foundation

@MainActor
final class ScannerViewModel: ObservableObject {
    @Published private(set) var state: ScannerState

    private let eventId: String
    private let apiKey: String
    private let apiClient: ApiClient

    private let maxHistory = 20
    private let recentScanBuffer = 50
    private var recentScanIds: [String] = []

    init(eventId: String, eventName: String, apiKey: String, apiClient: ApiClient = ApiClient()) {
        self.eventId = eventId
        self.apiKey = apiKey
        self.apiClient = apiClient
        self.state = ScannerState(eventName: eventName)
    }

    func onAction(_ action: ScannerAction) {
        switch action {
        case .requestManualEntry:
            state.manualEntryVisible = true
            state.manualEntryError = nil

        case .dismissManualEntry:
            state.manualEntryVisible = false
            state.manualEntryError = nil

        case .clearManualError:
            state.manualEntryError = nil

        case .dismissResult:
            state.activeResult = nil

        case .submitCode(let code):
            handleManualSubmission(rawCode: code)
        }
    }

    private func handleManualSubmission(rawCode: String) {
        let trimmed = rawCode.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else {
            state.manualEntryError = .emptyInput
            return
        }

        guard let payload = decodePayload(trimmed) else {
            state.manualEntryError = .invalidFormat
            return
        }

        if let payloadEvent = payload.eventId, !payloadEvent.isEmpty, payloadEvent != eventId {
            state.manualEntryError = .wrongEvent
            return
        }

        performScan(ticketId: payload.ticketId)
    }

    private func performScan(ticketId: String) {
        state.isProcessing = true
        state.manualEntryError = nil

        Task {
            defer { state.isProcessing = false }
            do {
                let response = try await apiClient.scanVisitorTicket(
                    eventId: eventId,
                    apiKey: apiKey,
                    ticketId: ticketId
                )
                let ticket = response.ticket.map { VisitorTicketMapper.toDomain($0) }
                rememberTicket(ticketId)
                registerResult(ScanResult(ticket: ticket, status: .success))
            } catch let apiError as ApiError {
                await handleApiError(ticketId: ticketId, error: apiError)
            } catch {
                // Treat non-HTTP failures as offline-ish.
                registerOffline(ticketId: ticketId, message: error.localizedDescription)
            }
        }
    }

    private func handleApiError(ticketId: String, error: ApiError) async {
        switch error {
        case .http(let status, let message):
            switch status {
            case 404:
                registerResult(ScanResult(ticket: nil, status: .invalid, message: message), closeManual: false)

            case 400, 412:
                let ticket = await fetchTicketIfExists(ticketId: ticketId)
                if ticket?.status == .scanned {
                    registerResult(ScanResult(ticket: ticket, status: .duplicate, message: message))
                } else {
                    registerResult(ScanResult(ticket: ticket, status: .invalid, message: message), closeManual: false)
                }

            case 401, 403:
                registerResult(ScanResult(ticket: nil, status: .error, message: message))

            default:
                registerResult(ScanResult(ticket: nil, status: .error, message: message), closeManual: false)
            }

        default:
            registerResult(ScanResult(ticket: nil, status: .error, message: error.localizedDescription), closeManual: false)
        }
    }

    private func fetchTicketIfExists(ticketId: String) async -> VisitorTicket? {
        do {
            let response = try await apiClient.getVisitorTicket(
                eventId: eventId,
                apiKey: apiKey,
                ticketId: ticketId
            )
            return response.ticket.map { VisitorTicketMapper.toDomain($0) }
        } catch {
            return nil
        }
    }

    private func registerResult(_ result: ScanResult, closeManual: Bool = true) {
        state.history = ([result] + state.history).prefix(maxHistory).map { $0 }
        state.activeResult = result
        if closeManual {
            state.manualEntryVisible = false
        }
        state.manualEntryError = nil
    }

    private func registerOffline(ticketId: String, message: String?) {
        let pending = PendingScan(ticketId: ticketId)
        state.pendingScans = ([pending] + state.pendingScans).prefix(maxHistory).map { $0 }
        registerResult(
            ScanResult(ticket: nil, status: .offline, message: message, offline: true)
        )
    }

    private func rememberTicket(_ ticketId: String) {
        guard !ticketId.isEmpty else { return }
        if recentScanIds.contains(ticketId) { return }
        if recentScanIds.count >= recentScanBuffer {
            recentScanIds.removeFirst()
        }
        recentScanIds.append(ticketId)
    }

    private func decodePayload(_ raw: String) -> TicketPayload? {
        if raw.hasPrefix("{") && raw.hasSuffix("}") {
            guard let data = raw.data(using: .utf8),
                  let json = (try? JSONSerialization.jsonObject(with: data)) as? [String: Any] else {
                return nil
            }
            let ticketId = (json["ticket_id"] as? String) ?? (json["ticketId"] as? String)
            let eventId = (json["event_id"] as? String) ?? (json["eventId"] as? String)
            guard let ticketId, !ticketId.isEmpty else { return nil }
            return TicketPayload(ticketId: ticketId, eventId: eventId)
        }

        return TicketPayload(ticketId: raw, eventId: nil)
    }

    private struct TicketPayload {
        let ticketId: String
        let eventId: String?
    }
}
