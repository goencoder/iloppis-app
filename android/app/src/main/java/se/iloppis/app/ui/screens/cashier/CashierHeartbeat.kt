package se.iloppis.app.ui.screens.cashier

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import se.iloppis.app.data.RegisterSessionManager
import se.iloppis.app.network.cashier.CashierClientState
import se.iloppis.app.network.cashier.CashierPresenceHeartbeatRequest
import se.iloppis.app.network.cashier.CashierPresenceHeartbeatResponse

internal data class CashierPresenceSnapshot(
    val clientState: CashierClientState,
    val pendingPurchasesCount: Int,
    val displayName: String?
)

internal fun CashierUiState.toCashierPresenceSnapshot(rawPendingPurchasesCount: Int): CashierPresenceSnapshot {
    val activeTransactionCount = if (transactions.isNotEmpty()) 1 else 0
    val pendingPurchasesCount = rawPendingPurchasesCount + activeTransactionCount
    val clientState = when {
        isProcessingPayment -> CashierClientState.CASHIER_CLIENT_STATE_SUBMITTING
        pendingPurchasesCount > 0 -> CashierClientState.CASHIER_CLIENT_STATE_ACTIVE_TRANSACTION
        else -> CashierClientState.CASHIER_CLIENT_STATE_IDLE
    }
    return CashierPresenceSnapshot(
        clientState = clientState,
        pendingPurchasesCount = pendingPurchasesCount,
        displayName = heartbeatDisplayName
    )
}

/** Builds a heartbeat request without serializing the session manager itself. */
internal fun CashierPresenceSnapshot.toHeartbeatRequest(
    clientType: se.iloppis.app.network.cashier.CashierClientType,
    sessionManager: RegisterSessionManager
): CashierPresenceHeartbeatRequest {
    val session = sessionManager.getCurrent()
    return CashierPresenceHeartbeatRequest(
        clientState = clientState,
        pendingPurchasesCount = pendingPurchasesCount,
        clientType = clientType,
        displayName = displayName,
        lifecycleEventType = session?.pendingLifecycleEvent,
        registerId = session?.registerId,
        sessionId = session?.sessionId
    )
}

internal class CashierHeartbeatCoordinator(
    private val scope: CoroutineScope,
    private val intervalMs: Long = 15_000L,
    private val shouldRun: () -> Boolean,
    private val requestFactory: () -> CashierPresenceHeartbeatRequest,
    private val sendHeartbeat: suspend (CashierPresenceHeartbeatRequest) -> CashierPresenceHeartbeatResponse,
    private val onHeartbeatResponse: (CashierPresenceHeartbeatResponse) -> Unit,
    private val onHeartbeatFailure: (Throwable) -> Unit,
    /** Clears acknowledged lifecycle transitions after successful heartbeats. */
    private val sessionManager: RegisterSessionManager? = null
) {
    private var job: Job? = null

    /** Starts one heartbeat loop when [shouldRun] permits it; repeated calls are idempotent. */
    fun start() {
        if (job?.isActive == true || !shouldRun()) {
            return
        }

        job = scope.launch {
            while (isActive) {
                if (!shouldRun()) {
                    break
                }

                try {
                    val request = requestFactory()
                    val response = sendHeartbeat(request)
                    sessionManager?.clearPendingLifecycleEvent(
                        expectedLifecycleEvent = request.lifecycleEventType,
                        expectedSessionId = request.sessionId
                    )
                    if (request.lifecycleEventType == null) {
                        sessionManager?.recordSync()
                    }
                    onHeartbeatResponse(response)
                } catch (cancellationException: CancellationException) {
                    throw cancellationException
                } catch (t: Throwable) {
                    onHeartbeatFailure(t)
                }

                delay(intervalMs)
            }
        }
    }

    /** Cancels the active heartbeat loop; repeated calls are safe. */
    fun stop() {
        job?.cancel()
        job = null
    }
}
