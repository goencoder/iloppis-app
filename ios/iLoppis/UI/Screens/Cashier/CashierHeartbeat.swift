import Foundation

struct CashierPresenceSnapshot: Equatable {
    let clientState: CashierClientState
    let pendingPurchasesCount: Int
    let displayName: String?
}

extension CashierState {
    func heartbeatSnapshot() -> CashierPresenceSnapshot {
        let pendingPurchasesCount = transactions.isEmpty ? 0 : 1
        let clientState: CashierClientState

        if isProcessingPayment {
            clientState = .submitting
        } else if pendingPurchasesCount > 0 {
            clientState = .activeTransaction
        } else {
            clientState = .idle
        }

        return CashierPresenceSnapshot(
            clientState: clientState,
            pendingPurchasesCount: pendingPurchasesCount,
            displayName: heartbeatDisplayName
        )
    }
}

final class CashierHeartbeatCoordinator {
    typealias RequestFactory = @Sendable () async -> CashierPresenceHeartbeatRequest?
    typealias HeartbeatSender = @Sendable (CashierPresenceHeartbeatRequest) async throws -> CashierPresenceHeartbeatResponse
    typealias ResponseHandler = @Sendable (CashierPresenceHeartbeatResponse) async -> Void
    typealias ErrorHandler = @Sendable (Error) async -> Void

    private let intervalNanoseconds: UInt64
    private let shouldRun: () -> Bool
    private let requestFactory: RequestFactory
    private let sendHeartbeat: HeartbeatSender
    private let onHeartbeatResponse: ResponseHandler
    private let onHeartbeatFailure: ErrorHandler
    private let sessionManager: RegisterSessionManager?

    private var task: Task<Void, Never>?

    init(
        intervalNanoseconds: UInt64 = 15_000_000_000,
        shouldRun: @escaping () -> Bool,
        requestFactory: @escaping RequestFactory,
        sendHeartbeat: @escaping HeartbeatSender,
        onHeartbeatResponse: @escaping ResponseHandler,
        onHeartbeatFailure: @escaping ErrorHandler,
        sessionManager: RegisterSessionManager? = nil
    ) {
        self.intervalNanoseconds = intervalNanoseconds
        self.shouldRun = shouldRun
        self.requestFactory = requestFactory
        self.sendHeartbeat = sendHeartbeat
        self.onHeartbeatResponse = onHeartbeatResponse
        self.onHeartbeatFailure = onHeartbeatFailure
        self.sessionManager = sessionManager
    }

    deinit {
        task?.cancel()
    }

    func start() {
        guard task == nil, shouldRun() else {
            return
        }

        let shouldRun = self.shouldRun
        let requestFactory = self.requestFactory
        let sendHeartbeat = self.sendHeartbeat
        let onHeartbeatResponse = self.onHeartbeatResponse
        let onHeartbeatFailure = self.onHeartbeatFailure
        let intervalNanoseconds = self.intervalNanoseconds
        let sessionManager = self.sessionManager

        task = Task { [weak self] in
            defer { self?.task = nil }

            while !Task.isCancelled {
                guard shouldRun(), let request = await requestFactory() else {
                    break
                }

                do {
                    let response = try await sendHeartbeat(request)
                    await MainActor.run {
                        sessionManager?.clearPendingLifecycleEvent(
                            expectedLifecycleEvent: request.lifecycleEventType,
                            expectedSessionId: request.sessionId
                        )
                        if request.lifecycleEventType == nil {
                            sessionManager?.recordSync()
                        }
                    }
                    await onHeartbeatResponse(response)
                } catch is CancellationError {
                    break
                } catch {
                    await onHeartbeatFailure(error)
                }

                do {
                    try await Task.sleep(nanoseconds: intervalNanoseconds)
                } catch {
                    break
                }
            }
        }
    }

    func stop() {
        task?.cancel()
        task = nil
    }
}
