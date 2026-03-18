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
    private var searchTask: Task<Void, Never>?
    private var ticketTypesTask: Task<Void, Never>?
    private var shouldReopenSearchAfterDetailDismiss = false

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

        case .requestTicketSearch:
            openTicketSearch()

        case .dismissTicketSearch:
            searchTask?.cancel()
            ticketTypesTask?.cancel()
            state.ticketSearchVisible = false
            state.isSearching = false
            state.searchResults = []
            state.searchError = nil

        case .submitTicketSearch(let query, let ticketTypeId):
            handleTicketSearch(query: query, ticketTypeId: ticketTypeId)

        case .selectSearchResult(let ticket):
            shouldReopenSearchAfterDetailDismiss = true
            state.searchDetailTicket = ticket
            state.ticketSearchVisible = false

        case .dismissSearchDetail:
            state.searchDetailTicket = nil
            state.ticketSearchVisible = shouldReopenSearchAfterDetailDismiss
            shouldReopenSearchAfterDetailDismiss = false

        case .scanFromDetail(let ticketId):
            performScanFromDetail(ticketId: ticketId)
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
        case .http(let status, let message, _):
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

    // MARK: - Ticket Search

    private func openTicketSearch() {
        ticketTypesTask?.cancel()
        state.ticketSearchVisible = true
        state.searchResults = []
        state.searchError = nil
        state.ticketTypes = []
        // Load ticket types
        ticketTypesTask = Task {
            do {
                let response = try await apiClient.listTicketTypes(
                    eventId: eventId,
                    apiKey: apiKey
                )
                guard !Task.isCancelled, state.ticketSearchVisible else { return }
                state.ticketTypes = response.types.map {
                    TicketTypeOption(id: $0.id, name: $0.type)
                }
            } catch {
                // Non-critical: picker just won't show
            }
        }
    }

    private func handleTicketSearch(query: String, ticketTypeId: String?) {
        guard !query.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { return }
        searchTask?.cancel()
        state.isSearching = true
        state.searchError = nil

        searchTask = Task {
            defer { if !Task.isCancelled { state.isSearching = false } }
            do {
                // Resolve ticket type ID to name for the API filter
                let ticketTypeName: String? = ticketTypeId.flatMap { id in
                    state.ticketTypes.first(where: { $0.id == id })?.name
                }
                let filter = VisitorTicketFilterDto(
                    email: nil,
                    ticketType: ticketTypeName,
                    status: nil,
                    freeText: query.trimmingCharacters(in: .whitespacesAndNewlines)
                )
                let response = try await apiClient.filterVisitorTickets(
                    eventId: eventId,
                    apiKey: apiKey,
                    filter: filter
                )
                guard !Task.isCancelled else { return }
                let tickets = (response.tickets ?? []).map { VisitorTicketMapper.toDomain($0) }
                state.searchResults = tickets
            } catch {
                guard !Task.isCancelled else { return }
                state.searchError = error.localizedDescription
            }
        }
    }

    private func performScanFromDetail(ticketId: String) {
        guard !state.isProcessing else { return }
        state.isProcessing = true
        shouldReopenSearchAfterDetailDismiss = false

        Task {
            defer { state.isProcessing = false }
            do {
                let response = try await apiClient.scanVisitorTicket(
                    eventId: eventId,
                    apiKey: apiKey,
                    ticketId: ticketId
                )
                let ticket = response.ticket.map { VisitorTicketMapper.toDomain($0) }
                state.searchDetailTicket = nil
                state.ticketSearchVisible = false
                rememberTicket(ticketId)
                registerResult(ScanResult(ticket: ticket, status: .success))
            } catch let apiError as ApiError {
                state.searchDetailTicket = nil
                state.ticketSearchVisible = false
                await handleApiError(ticketId: ticketId, error: apiError)
            } catch {
                state.searchDetailTicket = nil
                state.ticketSearchVisible = false
                registerResult(ScanResult(ticket: nil, status: .error, message: error.localizedDescription))
            }
        }
    }
}
