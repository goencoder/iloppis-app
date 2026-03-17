import Foundation
import Combine

private func normalizedApiKeyType(_ type: String?) -> String {
    type?.uppercased().replacingOccurrences(of: "-", with: "_") ?? ""
}

private func resolveToolMode(_ type: String?) -> CodeEntryMode? {
    let normalized = normalizedApiKeyType(type)
    if normalized.contains("LIVE_STATS") {
        return .liveStats
    }
    if normalized.contains("SCANNER") {
        return .scanner
    }
    if normalized.contains("CASHIER") {
        return .cashier
    }
    return nil
}

private func isToolModeAllowed(entryMode: CodeEntryMode, resolvedMode: CodeEntryMode?) -> Bool {
    guard let resolvedMode else { return false }
    switch entryMode {
    case .tool:
        return true
    default:
        return entryMode == resolvedMode
    }
}

private func wrongTypeErrorKey(entryMode: CodeEntryMode) -> String {
    switch entryMode {
    case .tool:
        return "code_entry_error_wrong_type_tool"
    case .cashier:
        return "code_entry_error_wrong_type_cashier"
    case .scanner:
        return "code_entry_error_wrong_type_scanner"
    case .liveStats:
        return "code_entry_error_wrong_type_live_stats"
    }
}

private func validationErrorKey(for error: Error) -> String {
    guard let apiError = error as? ApiError else {
        return "code_entry_error_network"
    }

    switch apiError {
    case .http(let statusCode, _):
        switch statusCode {
        case 429:
            return "code_entry_error_rate_limited"
        case 400, 404, 422:
            return "code_entry_error_not_found"
        case 401, 403:
            return "code_entry_error_inactive"
        case 500...599:
            return "code_entry_error_server"
        default:
            return "code_entry_error_network"
        }
    case .invalidUrl, .invalidResponse:
        return "code_entry_error_network"
    case .decoding:
        return "code_entry_error_server"
    }
}

@MainActor
final class EventListViewModel: ObservableObject {
    @Published private(set) var state = EventListState()

    private let apiClient: ApiClient

    init(apiClient: ApiClient = ApiClient()) {
        self.apiClient = apiClient
        onAction(.loadEvents)
    }

    func onAction(_ action: EventListAction) {
        switch action {
        case .loadEvents:
            state.isLoading = true
            Task { await loadEvents() }

        case let .eventsLoaded(events):
            state.isLoading = false
            state.errorMessage = nil
            state.events = events

        case let .loadingFailed(message):
            state.isLoading = false
            state.errorMessage = message

        case let .selectEvent(event):
            state.selectedEvent = event

        case .dismissEventDetail:
            state.selectedEvent = nil

        case let .startCodeEntry(mode, event):
            state.selectedEvent = nil
            state.codeConfirmationState = nil
            state.codeEntryState = CodeEntryState(mode: mode, event: event)

        case .dismissCodeEntry:
            state.codeEntryState = nil
            state.codeConfirmationState = nil

        case .dismissCodeConfirmation:
            state.codeConfirmationState = nil
            guard var entry = state.codeEntryState else { return }
            entry.isValidating = false
            state.codeEntryState = entry

        case let .updateCode(code):
            guard var entry = state.codeEntryState else { return }
            let normalized = code
                .uppercased()
                .filter { $0.isNumber || ("A"..."Z").contains(String($0)) }
            entry.code = String(normalized.prefix(6))
            entry.errorMessage = nil
            state.codeEntryState = entry

        case let .submitCode(code):
            guard var entry = state.codeEntryState else { return }
            entry.isValidating = true
            entry.errorMessage = nil
            state.codeEntryState = entry
            Task { await validateCode(code: code, entry: entry) }

        case let .codeValidated(apiKey, alias, event, entryMode, resolvedMode):
            state.selectedEvent = nil
            state.codeConfirmationState = CodeConfirmationState(
                entryMode: entryMode,
                resolvedMode: resolvedMode,
                event: event,
                apiKey: apiKey,
                alias: alias
            )
            if var entry = state.codeEntryState {
                entry.isValidating = false
                state.codeEntryState = entry
            }

        case .confirmCodeSelection:
            guard let confirmation = state.codeConfirmationState else { return }
            state.codeEntryState = nil
            state.codeConfirmationState = nil
            state.selectedEvent = nil
            switch confirmation.resolvedMode {
            case .tool:
                state.currentScreen = .eventList
            case .cashier:
                state.currentScreen = .cashier(event: confirmation.event, apiKey: confirmation.apiKey)
            case .scanner:
                state.currentScreen = .scanner(event: confirmation.event, apiKey: confirmation.apiKey)
            case .liveStats:
                state.currentScreen = .liveStats(event: confirmation.event, apiKey: confirmation.apiKey)
            }

        case let .validationFailed(message):
            state.codeConfirmationState = nil
            guard var entry = state.codeEntryState else { return }
            entry.isValidating = false
            entry.errorMessage = message
            state.codeEntryState = entry

        case .navigateBack:
            state.currentScreen = .eventList

        case let .updateSearch(query):
            state.searchQuery = query

        case let .updateFilter(filter):
            state.filter = filter
        }
    }

    // MARK: - Private

    private func loadEvents() async {
        do {
            // Filtrera på dagens datum och endast OPEN evenemang
            let today = ISO8601DateFormatter().string(from: Date()).prefix(10)
            let filter = EventFilter(
                city: nil,
                dateFrom: String(today),
                dateTo: nil,
                searchText: nil,
                lifecycleStates: ["OPEN"]
            )
            let dtos = try await apiClient.filterEvents(filter: filter)
            let events = dtos.map { dto in
                Event(
                    id: dto.id,
                    name: dto.name,
                    description: dto.description,
                    startTime: dto.startTime,
                    endTime: dto.endTime,
                    addressStreet: dto.addressStreet,
                    addressCity: dto.addressCity,
                    lifecycleState: dto.lifecycleState
                )
            }
            onAction(.eventsLoaded(events))
        } catch {
            onAction(.loadingFailed(error.localizedDescription))
        }
    }

    private func validateCode(code: String, entry: CodeEntryState) async {
        do {
            let normalized = Self.normalizeAlias(code)
            guard normalized.count == 7 else {
                onAction(.validationFailed("code_invalid"))
                return
            }

            let response: ApiKeyResponse
            if let eventId = entry.event?.id {
                response = try await apiClient.getApiKeyByAlias(eventId: eventId, alias: normalized)
            } else {
                response = try await apiClient.getApiKeyByAlias(alias: normalized)
            }
            DebugLogStore.shared.append("[CodeExchange] alias=\(normalized) type=\(response.type ?? "(nil)") isActive=\(response.isActive)")
            guard response.isActive else {
                onAction(.validationFailed("code_entry_error_inactive"))
                return
            }

            let resolvedMode = resolveToolMode(response.type)
            guard isToolModeAllowed(entryMode: entry.mode, resolvedMode: resolvedMode) else {
                onAction(.validationFailed(wrongTypeErrorKey(entryMode: entry.mode)))
                return
            }
            guard let actualMode = resolvedMode else {
                onAction(.validationFailed(wrongTypeErrorKey(entryMode: entry.mode)))
                return
            }

            // Resolve event: use the known event, or fetch it via the returned eventId.
            let resolvedEventId = response.eventId?.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty == false
                ? response.eventId
                : entry.event?.id

            let resolvedEvent: Event
            if let event = entry.event, event.id == resolvedEventId {
                resolvedEvent = event
            } else if let eventId = resolvedEventId {
                if let dto = try await apiClient.getEvent(eventId: eventId) {
                    resolvedEvent = Event(
                        id: dto.id,
                        name: dto.name,
                        description: dto.description,
                        startTime: dto.startTime,
                        endTime: dto.endTime,
                        addressStreet: dto.addressStreet,
                        addressCity: dto.addressCity,
                        lifecycleState: dto.lifecycleState
                    )
                } else {
                    onAction(.validationFailed("code_entry_error_event_not_found"))
                    return
                }
            } else {
                onAction(.validationFailed("code_entry_error_event_not_found"))
                return
            }

            onAction(
                .codeValidated(
                    apiKey: response.apiKey,
                    alias: normalized,
                    event: resolvedEvent,
                    entryMode: entry.mode,
                    resolvedMode: actualMode
                )
            )
        } catch {
            onAction(.validationFailed(validationErrorKey(for: error)))
        }
    }

    private static func normalizeAlias(_ raw: String) -> String {
        let alnum = raw
            .uppercased()
            .filter { $0.isNumber || ("A"..."Z").contains(String($0)) }
        guard alnum.count >= 6 else { return raw.trimmingCharacters(in: .whitespacesAndNewlines).uppercased() }
        let first = alnum.prefix(3)
        let restStart = alnum.index(alnum.startIndex, offsetBy: 3)
        let last = alnum[restStart..<alnum.index(restStart, offsetBy: min(3, alnum.count - 3))]
        return "\(first)-\(last)"
    }
}
