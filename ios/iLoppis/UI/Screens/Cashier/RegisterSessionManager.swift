import Foundation

@MainActor
final class RegisterSessionManager {
    enum State: String {
        case open = "OPEN"
        case closeRequested = "CLOSE_REQUESTED"
        case closed = "CLOSED"
    }

    struct Session: Equatable {
        let sessionId: String
        let eventId: String
        let registerId: String
        let state: State
        let pendingLifecycleEvent: RegisterLifecycleEventType?
    }

    private let defaults: UserDefaults
    private let storageKey = "cashier_register_session"
    private var current: Session?

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
        self.current = loadFromDefaults()
    }

    func openSession(eventId: String, registerId: String) -> Session {
        let session = Session(
            sessionId: UUID().uuidString,
            eventId: eventId,
            registerId: registerId,
            state: .open,
            pendingLifecycleEvent: .open
        )
        current = session
        persist(session)
        return session
    }

    func requestClose() -> Bool {
        guard let session = current, session.state == .open else {
            return false
        }
        let updated = Session(
            sessionId: session.sessionId,
            eventId: session.eventId,
            registerId: session.registerId,
            state: .closeRequested,
            pendingLifecycleEvent: .closeRequested
        )
        current = updated
        persist(updated)
        return true
    }

    func confirmClose() -> Bool {
        guard let session = current, session.state == .closeRequested else {
            return false
        }
        let updated = Session(
            sessionId: session.sessionId,
            eventId: session.eventId,
            registerId: session.registerId,
            state: session.state,
            pendingLifecycleEvent: .closeConfirmed
        )
        current = updated
        persist(updated)
        return true
    }

    func clearPendingLifecycleEvent(expectedLifecycleEvent: RegisterLifecycleEventType?, expectedSessionId: String?) {
        guard
            let expectedLifecycleEvent,
            let expectedSessionId,
            !expectedSessionId.isEmpty,
            let session = current,
            session.sessionId == expectedSessionId,
            session.pendingLifecycleEvent == expectedLifecycleEvent
        else {
            return
        }

        let nextState: State
        if expectedLifecycleEvent == .closeConfirmed && session.state == .closeRequested {
            nextState = .closed
        } else {
            nextState = session.state
        }

        let updated = Session(
            sessionId: session.sessionId,
            eventId: session.eventId,
            registerId: session.registerId,
            state: nextState,
            pendingLifecycleEvent: nil
        )
        current = updated
        persist(updated)
    }

    func recordSync() {
        guard let session = current, session.state != .closed, session.pendingLifecycleEvent == nil else {
            return
        }
        let updated = Session(
            sessionId: session.sessionId,
            eventId: session.eventId,
            registerId: session.registerId,
            state: session.state,
            pendingLifecycleEvent: .sync
        )
        current = updated
        persist(updated)
    }

    func getCurrent() -> Session? {
        current
    }

    func isSessionActive() -> Bool {
        current?.state == .open || current?.state == .closeRequested
    }

    private func persist(_ session: Session) {
        let payload: [String: String] = [
            "session_id": session.sessionId,
            "event_id": session.eventId,
            "register_id": session.registerId,
            "state": session.state.rawValue,
            "pending_lifecycle": session.pendingLifecycleEvent?.rawValue ?? ""
        ]
        defaults.set(payload, forKey: storageKey)
    }

    private func loadFromDefaults() -> Session? {
        guard
            let payload = defaults.dictionary(forKey: storageKey) as? [String: String],
            let sessionId = payload["session_id"], !sessionId.isEmpty,
            let eventId = payload["event_id"], !eventId.isEmpty,
            let registerId = payload["register_id"], !registerId.isEmpty,
            let stateRaw = payload["state"],
            let state = State(rawValue: stateRaw)
        else {
            return nil
        }

        let pendingRaw = payload["pending_lifecycle"] ?? ""
        let pendingLifecycle = pendingRaw.isEmpty ? nil : RegisterLifecycleEventType(rawValue: pendingRaw)

        return Session(
            sessionId: sessionId,
            eventId: eventId,
            registerId: registerId,
            state: state,
            pendingLifecycleEvent: pendingLifecycle
        )
    }
}
