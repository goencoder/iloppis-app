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
    private var ticketTypeNamesById: [String: String] = [:]

    init(eventId: String, eventName: String, apiKey: String, apiClient: ApiClient = ApiClient()) {
        self.eventId = eventId
        self.apiKey = apiKey
        self.apiClient = apiClient
        self.state = ScannerState(eventName: eventName)
        ticketTypesTask = Task { [weak self] in
            await self?.loadTicketTypes(publishOptions: false)
        }
    }

    deinit {
        searchTask?.cancel()
        ticketTypesTask?.cancel()
    }

    func onAction(_ action: ScannerAction) {
        switch action {
        case .dismissResult:
            state.activeResult = nil

        case .requestTicketSearch:
            openTicketSearch()

        case .dismissTicketSearch:
            searchTask?.cancel()
            ticketTypesTask?.cancel()
            shouldReopenSearchAfterDetailDismiss = false
            state.ticketSearchVisible = false
            state.isSearching = false
            state.searchQuery = ""
            state.selectedTicketTypeId = nil
            state.hasSubmittedTicketSearch = false
            state.searchResults = []
            state.searchError = nil

        case .updateTicketSearchQuery(let query):
            state.searchQuery = query

        case .updateTicketSearchType(let ticketTypeId):
            state.selectedTicketTypeId = ticketTypeId

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

    private func performScan(ticketId: String) {
        state.isProcessing = true

        Task {
            defer { state.isProcessing = false }
            do {
                let response = try await apiClient.scanVisitorTicket(
                    eventId: eventId,
                    apiKey: apiKey,
                    ticketId: ticketId
                )
                let ticket = response.ticket
                    .map { VisitorTicketMapper.toDomain($0) }
                    .map(normalizeTicket)
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
                registerResult(ScanResult(ticket: nil, status: .invalid, message: message))

            case 400, 412:
                let ticket = await fetchTicketIfExists(ticketId: ticketId)
                if ticket?.status == .scanned {
                    registerResult(ScanResult(ticket: ticket, status: .duplicate, message: message))
                } else {
                    registerResult(ScanResult(ticket: ticket, status: .invalid, message: message))
                }

            case 401, 403:
                registerResult(ScanResult(ticket: nil, status: .error, message: message))

            default:
                registerResult(ScanResult(ticket: nil, status: .error, message: message))
            }

        default:
            registerResult(ScanResult(ticket: nil, status: .error, message: error.localizedDescription))
        }
    }

    private func fetchTicketIfExists(ticketId: String) async -> VisitorTicket? {
        do {
            let response = try await apiClient.getVisitorTicket(
                eventId: eventId,
                apiKey: apiKey,
                ticketId: ticketId
            )
            return response.ticket
                .map { VisitorTicketMapper.toDomain($0) }
                .map(normalizeTicket)
        } catch {
            return nil
        }
    }

    private func registerResult(_ result: ScanResult) {
        state.history = ([result] + state.history).prefix(maxHistory).map { $0 }
        state.activeResult = result
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

    // MARK: - Ticket Search

    private func openTicketSearch() {
        ticketTypesTask?.cancel()
        state.ticketSearchVisible = true
        state.ticketTypes = ticketTypeOptions()
        ticketTypesTask = Task { [weak self] in
            await self?.loadTicketTypes(publishOptions: true)
        }
    }

    private func handleTicketSearch(query: String, ticketTypeId: String?) {
        guard !query.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { return }
        searchTask?.cancel()
        state.isSearching = true
        state.hasSubmittedTicketSearch = true
        state.searchResults = []
        state.searchError = nil

        searchTask = Task {
            defer { if !Task.isCancelled { state.isSearching = false } }
            do {
                let filter = VisitorTicketFilterDto(
                    email: nil,
                    ticketType: ticketTypeId,
                    status: nil,
                    freeText: query.trimmingCharacters(in: .whitespacesAndNewlines)
                )
                let response = try await apiClient.filterVisitorTickets(
                    eventId: eventId,
                    apiKey: apiKey,
                    filter: filter
                )
                guard !Task.isCancelled else { return }
                let tickets = (response.tickets ?? [])
                    .map { VisitorTicketMapper.toDomain($0) }
                    .map(normalizeTicket)
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
                let ticket = response.ticket
                    .map { VisitorTicketMapper.toDomain($0) }
                    .map(normalizeTicket)
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

    private func loadTicketTypes(publishOptions: Bool) async {
        do {
            let response = try await apiClient.listTicketTypes(
                eventId: eventId,
                apiKey: apiKey
            )
            guard !Task.isCancelled else { return }

            ticketTypeNamesById = response.types.reduce(into: [:]) { result, type in
                result[type.id] = type.type
            }

            if publishOptions && state.ticketSearchVisible {
                state.ticketTypes = ticketTypeOptions()
            }

            normalizeVisibleTickets()
        } catch {
            // Non-critical: ticket type labels degrade gracefully.
        }
    }

    private func ticketTypeOptions() -> [TicketTypeOption] {
        ticketTypeNamesById
            .map { TicketTypeOption(id: $0.key, name: $0.value) }
            .sorted { $0.name.localizedCaseInsensitiveCompare($1.name) == .orderedAscending }
    }

    private func normalizeVisibleTickets() {
        state.activeResult = state.activeResult.map { result in
            ScanResult(
                id: result.id,
                ticket: result.ticket.map(normalizeTicket),
                status: result.status,
                timestamp: result.timestamp,
                message: result.message,
                offline: result.offline
            )
        }
        state.history = state.history.map { result in
            ScanResult(
                id: result.id,
                ticket: result.ticket.map(normalizeTicket),
                status: result.status,
                timestamp: result.timestamp,
                message: result.message,
                offline: result.offline
            )
        }
        state.searchResults = state.searchResults.map(normalizeTicket)
        state.searchDetailTicket = state.searchDetailTicket.map(normalizeTicket)
    }

    private func normalizeTicket(_ ticket: VisitorTicket) -> VisitorTicket {
        ticket.withTicketType(displayTicketType(ticket.ticketType))
    }

    private func displayTicketType(_ raw: String?) -> String? {
        guard let raw, !raw.isEmpty else { return nil }
        if let resolved = ticketTypeNamesById[raw], !resolved.isEmpty {
            return resolved
        }
        return UUID(uuidString: raw) == nil ? raw : nil
    }
}
