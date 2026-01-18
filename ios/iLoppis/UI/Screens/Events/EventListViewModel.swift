import Foundation
import Combine

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
            state.codeEntryState = CodeEntryState(mode: mode, event: event)

        case .dismissCodeEntry:
            state.codeEntryState = nil

        case let .updateCode(code):
            guard var entry = state.codeEntryState else { return }
            entry.code = code
            entry.errorMessage = nil
            state.codeEntryState = entry

        case let .submitCode(code):
            guard var entry = state.codeEntryState else { return }
            entry.isValidating = true
            entry.errorMessage = nil
            state.codeEntryState = entry
            Task { await validateCode(code: code, entry: entry) }

        case let .codeValidated(apiKey, event, mode):
            state.codeEntryState = nil
            state.selectedEvent = nil
            switch mode {
            case .cashier:
                state.currentScreen = .cashier(event: event, apiKey: apiKey)
            case .scanner:
                state.currentScreen = .scanner(event: event, apiKey: apiKey)
            }

        case let .validationFailed(message):
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
                onAction(.validationFailed("Invalid code format"))
                return
            }

            let response = try await apiClient.getApiKeyByAlias(eventId: entry.event.id, alias: normalized)
            DebugLogStore.shared.append("[CodeExchange] alias=\(normalized) type=\(response.type ?? "(nil)") isActive=\(response.isActive)")
            guard response.isActive else {
                onAction(.validationFailed("Code is inactive"))
                return
            }

            if let type = response.type?.uppercased(), !type.isEmpty {
                let normalizedType = type.replacingOccurrences(of: "-", with: "_")
                let cashierTypes: Set<String> = [
                    "CASHIER",
                    "API_KEY_TYPE_CASHIER",
                    "API_KEY_TYPE_WEB_CASHIER"
                ]
                let scannerTypes: Set<String> = [
                    "SCANNER",
                    "API_KEY_TYPE_SCANNER",
                    "API_KEY_TYPE_WEB_SCANNER"
                ]

                switch entry.mode {
                case .cashier:
                    if !cashierTypes.contains(normalizedType) {
                        onAction(.validationFailed("Wrong code type"))
                        return
                    }
                case .scanner:
                    if !scannerTypes.contains(normalizedType) {
                        onAction(.validationFailed("Wrong code type"))
                        return
                    }
                }
            }

            onAction(.codeValidated(apiKey: response.apiKey, event: entry.event, mode: entry.mode))
        } catch {
            let message = error.localizedDescription
            onAction(.validationFailed(message))
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
