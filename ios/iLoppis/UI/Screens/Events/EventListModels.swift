import Foundation

struct CodeEntryState: Equatable {
    var mode: CodeEntryMode
    var event: Event
    var code: String = ""
    var isValidating: Bool = false
    var errorMessage: String?
}

enum AppScreen: Equatable {
    case eventList
    case cashier(event: Event, apiKey: String)
    case scanner(event: Event, apiKey: String)
}

enum EventFilter: Equatable {
    case all
    case open
    case upcoming
    case past
}

struct EventListState: Equatable {
    var events: [Event] = []
    var filteredEvents: [Event] { applyFilterAndSearch() }
    var selectedEvent: Event?
    var codeEntryState: CodeEntryState?
    var currentScreen: AppScreen = .eventList
    var isLoading: Bool = false
    var errorMessage: String?
    var searchQuery: String = ""
    var filter: EventFilter = .all

    private func applyFilterAndSearch() -> [Event] {
        let filtered: [Event]
        switch filter {
        case .all: filtered = events
        case .open: filtered = events.filter { ($0.lifecycleState ?? "").uppercased() == "OPEN" }
        case .upcoming: filtered = events.filter { ($0.lifecycleState ?? "").uppercased() == "UPCOMING" }
        case .past: filtered = events.filter { ($0.lifecycleState ?? "").uppercased() == "CLOSED" }
        }

        guard !searchQuery.isEmpty else { return filtered }
        let lower = searchQuery.lowercased()
        return filtered.filter {
            if $0.name.lowercased().contains(lower) { return true }
            if ($0.addressCity ?? "").lowercased().contains(lower) { return true }
            if ($0.addressStreet ?? "").lowercased().contains(lower) { return true }
            return false
        }
    }
}

enum EventListAction {
    case loadEvents
    case eventsLoaded([Event])
    case loadingFailed(String)
    case selectEvent(Event)
    case dismissEventDetail
    case startCodeEntry(mode: CodeEntryMode, event: Event)
    case dismissCodeEntry
    case updateCode(String)
    case submitCode(String)
    case codeValidated(apiKey: String, event: Event, mode: CodeEntryMode)
    case validationFailed(String)
    case navigateBack
    case updateSearch(String)
    case updateFilter(EventFilter)
}
