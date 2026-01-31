package se.iloppis.app.ui.screens.scanner

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import retrofit2.HttpException
import se.iloppis.app.data.CommittedScansStore
import se.iloppis.app.data.PendingScansStore
import se.iloppis.app.data.mappers.VisitorTicketMapper.toDomain
import se.iloppis.app.domain.model.VisitorTicket
import se.iloppis.app.domain.model.VisitorTicketStatus
import se.iloppis.app.network.config.clientConfig
import se.iloppis.app.network.iLoppisClient
import se.iloppis.app.network.visitor.VisitorAPI
import java.io.IOException
import java.time.Instant
import java.util.ArrayDeque
import java.util.UUID

private const val TAG = "ScannerViewModel"
private const val MAX_HISTORY = 20
private const val HISTORY_PAGE_SIZE = 30
private const val RECENT_SCAN_BUFFER = 10
private const val SCAN_TIMEOUT_MS = 5000L

/**
 * Sealed actions that the scanner screen can trigger.
 */
sealed class ScannerAction {
    data object RequestManualEntry : ScannerAction()
    data object DismissManualEntry : ScannerAction()
    data class SubmitCode(val code: String) : ScannerAction()
    data object ClearManualError : ScannerAction()
    data object DismissResult : ScannerAction()
    data class ShowTicketDetails(val result: ScanResult) : ScannerAction()
    data object DismissTicketDetails : ScannerAction()
    data object CommitCurrentGroup : ScannerAction()
    data object ToggleGroupExpanded : ScannerAction()
    data class RemoveFromGroup(val ticketId: String) : ScannerAction()
    data object LoadMoreHistory : ScannerAction()
    data object ToggleErrorScans : ScannerAction()
}

/**
 * Local error reasons for manual code entry.
 */
enum class ManualEntryError {
    EMPTY_INPUT,
    WRONG_EVENT,
    INVALID_FORMAT
}

/**
 * Scanner status buckets that drive the result UI.
 */
enum class ScanStatus {
    SUCCESS,
    DUPLICATE,
    INVALID,
    OFFLINE,
    OFFLINE_SUCCESS,
    ERROR
}

/**
 * Individual scan outcome record shown in the sheet/history.
 */
data class ScanResult(
    val ticket: VisitorTicket?,
    val status: ScanStatus,
    val timestamp: Instant = Instant.now(),
    val message: String? = null,
    val offline: Boolean = false
)

/**
 * Pending offline scan awaiting sync.
 */
data class PendingScan(
    val ticketId: String,
    val createdAt: Instant = Instant.now()
)

/**
 * Complete UI state for the scanner screen.
 */
data class ScannerUiState(
    val eventName: String,
    val isProcessing: Boolean = false,
    val manualEntryVisible: Boolean = false,
    val manualEntryError: ManualEntryError? = null,
    val activeResult: ScanResult? = null,
    val history: List<ScanResult> = emptyList(),
    val groupedHistory: List<HistoryItem> = emptyList(),
    val hasMoreHistory: Boolean = false,
    val isLoadingHistory: Boolean = false,
    val pendingScans: List<PendingScan> = emptyList(),
    val ticketDetailsResult: ScanResult? = null,
    val totalScansCount: Int = 0,
    val currentGroup: List<ScanResult> = emptyList(),
    val currentGroupEmail: String? = null,
    val currentGroupTicketType: String? = null,
    val isGroupExpanded: Boolean = false,
    val showErrorScans: Boolean = false
) {
    val pendingCount: Int get() = pendingScans.size
    val currentGroupCount: Int get() = currentGroup.size
}

/**
 * Refactored ViewModel using composition and OOP principles.
 * Reduced from ~650 to ~380 lines by extracting responsibilities.
 */
class ScannerViewModel(
    private val eventId: String,
    eventName: String,
    private val apiKey: String
) : ViewModel() {

    private val visitorTicketApi: VisitorAPI = iLoppisClient(clientConfig()).create()
    private val groupManager = GroupScanManager()
    private val recentScanIds = ArrayDeque<String>(RECENT_SCAN_BUFFER)

    var uiState by mutableStateOf(
        ScannerUiState(
            eventName = eventName,
            currentGroupEmail = groupManager.groupEmail,
            currentGroupTicketType = groupManager.groupTicketType,
            currentGroup = groupManager.scans
        )
    )
        private set

    init {
        // Initialize file stores for this event
        val context = se.iloppis.app.ILoppisAppHolder.appContext
        PendingScansStore.initialize(context, eventId)
        CommittedScansStore.initialize(context, eventId)

        loadInitialData()
        // Ticket type name resolution
        viewModelScope.launch(Dispatchers.IO) {
            se.iloppis.app.data.TicketTypeRepository.refresh(eventId, apiKey)
        }
    }

    fun onAction(action: ScannerAction) {
        when (action) {
            is ScannerAction.RequestManualEntry -> uiState = uiState.copy(manualEntryVisible = true)
            is ScannerAction.DismissManualEntry -> uiState = uiState.copy(
                manualEntryVisible = false,
                manualEntryError = null
            )
            is ScannerAction.SubmitCode -> handleCodeSubmission(action.code)
            is ScannerAction.ClearManualError -> uiState = uiState.copy(manualEntryError = null)
            is ScannerAction.DismissResult -> uiState = uiState.copy(activeResult = null)
            is ScannerAction.ShowTicketDetails -> uiState = uiState.copy(ticketDetailsResult = action.result)
            is ScannerAction.DismissTicketDetails -> uiState = uiState.copy(ticketDetailsResult = null)
            is ScannerAction.CommitCurrentGroup -> viewModelScope.launch { commitCurrentGroup() }
            is ScannerAction.ToggleGroupExpanded -> uiState = uiState.copy(
                isGroupExpanded = !uiState.isGroupExpanded
            )
            is ScannerAction.RemoveFromGroup -> removeFromGroup(action.ticketId)
            is ScannerAction.LoadMoreHistory -> viewModelScope.launch { loadMoreHistory() }
            is ScannerAction.ToggleErrorScans -> {
                val newShowErrors = !uiState.showErrorScans
                uiState = uiState.copy(showErrorScans = newShowErrors)
                // Re-filter history
                updateGroupedHistory()
            }
        }
    }

    private fun handleCodeSubmission(rawCode: String) {
        val trimmed = rawCode.trim()

        when {
            trimmed.isEmpty() -> {
                uiState = uiState.copy(manualEntryError = ManualEntryError.EMPTY_INPUT)
                return
            }
        }

        val payload = decodePayload(trimmed) ?: run {
            uiState = uiState.copy(manualEntryError = ManualEntryError.INVALID_FORMAT)
            return
        }

        if (!payload.eventId.isNullOrBlank() && payload.eventId != eventId) {
            uiState = uiState.copy(manualEntryError = ManualEntryError.WRONG_EVENT)
            return
        }

        performScan(payload)
    }

    private fun performScan(payload: TicketPayload) {
        if (uiState.isProcessing) {
            Log.w(TAG, "Scan already in progress, ignoring")
            return
        }

        viewModelScope.launch {
            uiState = uiState.copy(isProcessing = true, manualEntryError = null)
            val scanId = UUID.randomUUID().toString()
            val now = Instant.now().toString()

            try {
                val ticket = performOnlineScan(payload)
                handleSuccessfulScan(scanId, payload.ticketId, now, ticket, wasOffline = false)
            } catch (e: TimeoutCancellationException) {
                Log.w(TAG, "Scan timeout, switching to offline mode")
                handleOfflineScan(scanId, payload.ticketId, now)
            } catch (e: HttpException) {
                handleHttpError(e, payload)
            } catch (e: IOException) {
                Log.w(TAG, "Network error, switching to offline: ${e.message}")
                handleOfflineScan(scanId, payload.ticketId, now)
            } catch (e: Throwable) {
                Log.e(TAG, "Unexpected scan failure", e)
                showResult(ScanResultHandler.Error(e.message ?: "Unknown error"))
            }
        }
    }

    private suspend fun performOnlineScan(payload: TicketPayload): VisitorTicket? {
        val response = withTimeout(SCAN_TIMEOUT_MS) {
            visitorTicketApi.scanTicket(
                authorization = "Bearer $apiKey",
                eventId = eventId,
                ticketId = payload.ticketId
            )
        }
        return response.ticket?.toDomain()?.let { resolveTicketTypeName(it) }
    }

    private suspend fun handleSuccessfulScan(
        scanId: String,
        ticketId: String,
        scannedAt: String,
        ticket: VisitorTicket?,
        wasOffline: Boolean
    ) = withContext(Dispatchers.IO) {
        // Check for duplicate in current group
        if (groupManager.isDuplicate(ticketId)) {
            Log.d(TAG, "Ticket $ticketId already in current group, ignoring")
            withContext(Dispatchers.Main) {
                uiState = uiState.copy(isProcessing = false)
            }
            return@withContext
        }

        val ticketEmail = ticket?.email
        val ticketType = ticket?.ticketType

        // Auto-commit if group keys changed
        if (groupManager.shouldCommit(ticketEmail, ticketType)) {
            Log.d(TAG, "Group keys changed, committing current group")
            commitCurrentGroup()
        }

        // Persist scan
        CommittedScansStore.appendScan(
            se.iloppis.app.data.models.CommittedScan(
                scanId = scanId,
                ticketId = ticketId,
                eventId = eventId,
                scannedAt = scannedAt,
                committedAt = scannedAt,
                wasOffline = wasOffline,
                ticketType = ticketType,
                email = ticketEmail,
                status = if (wasOffline) "OFFLINE_SUCCESS" else "SUCCESS",
                errorMessage = null
            )
        )
        rememberTicket(ticketId)

        // Add to group and show result
        val handler = if (wasOffline) {
            ScanResultHandler.OfflineSuccess(ticket)
        } else {
            ScanResultHandler.Success(ticket!!)
        }

        val result = ScanResultHandler.toScanResult(handler)
        groupManager.addScan(result, ticketEmail, ticketType)

        withContext(Dispatchers.Main) {
            updateGroupState()
            showResult(handler)
        }
        updateTotalCount()
    }

    private suspend fun handleOfflineScan(scanId: String, ticketId: String, scannedAt: String) = withContext(Dispatchers.IO) {
        // Check for duplicate in current group
        if (groupManager.isDuplicate(ticketId)) {
            Log.d(TAG, "Duplicate offline scan in group, ignoring")
            withContext(Dispatchers.Main) {
                uiState = uiState.copy(isProcessing = false)
            }
            return@withContext
        }

        // Check for duplicate in committed scans (offline dedup)
        if (CommittedScansStore.hasTicket(ticketId)) {
            Log.d(TAG, "Ticket $ticketId already scanned (offline dedup), showing ALREADY_SCANNED")
            withContext(Dispatchers.Main) {
                showResult(ScanResultHandler.Duplicate(null, "Biljetten har redan skannats"))
            }
            return@withContext
        }

        PendingScansStore.appendScan(
            se.iloppis.app.data.models.PendingScan(
                scanId = scanId,
                ticketId = ticketId,
                eventId = eventId,
                scannedAt = scannedAt
            )
        )
        CommittedScansStore.appendScan(
            se.iloppis.app.data.models.CommittedScan(
                scanId = scanId,
                ticketId = ticketId,
                eventId = eventId,
                scannedAt = scannedAt,
                committedAt = scannedAt,
                wasOffline = true,
                ticketType = null,
                email = null,
                status = "OFFLINE_SUCCESS",
                errorMessage = null
            )
        )
        rememberTicket(ticketId)

        val handler = ScanResultHandler.OfflineSuccess(null)
        val result = ScanResultHandler.toScanResult(handler)
        groupManager.addScan(result, null, null)

        withContext(Dispatchers.Main) {
            updateGroupState()
            showResult(handler)
        }
        loadPendingScans()
        updateTotalCount()
    }

    private suspend fun handleHttpError(error: HttpException, payload: TicketPayload) = withContext(Dispatchers.IO) {
        val message = extractErrorMessage(error)
        val ticket = fetchTicketIfExists(payload.ticketId)
        val handler = ScanResultHandler.fromHttpCode(error.code(), message, ticket)

        // Save error scan to history
        val scanId = UUID.randomUUID().toString()
        val now = Instant.now().toString()
        val result = ScanResultHandler.toScanResult(handler)

        CommittedScansStore.appendScan(
            se.iloppis.app.data.models.CommittedScan(
                scanId = scanId,
                ticketId = payload.ticketId,
                eventId = eventId,
                scannedAt = now,
                committedAt = now,
                wasOffline = false,
                ticketType = ticket?.ticketType,
                email = ticket?.email,
                status = result.status.name,
                errorMessage = message
            )
        )

        withContext(Dispatchers.Main) {
            showResult(handler)
            // Reload history to show the error
            loadScanHistory()
        }
    }

    private suspend fun fetchTicketIfExists(ticketId: String): VisitorTicket? {
        return try {
            val response = visitorTicketApi.getTicket(
                authorization = "Bearer $apiKey",
                eventId = eventId,
                ticketId = ticketId
            )
            response.ticket?.toDomain()?.let { resolveTicketTypeName(it) }
        } catch (e: Exception) {
            null
        }
    }

    private fun showResult(handler: ScanResultHandler, closeManual: Boolean = true) {
        val result = ScanResultHandler.toScanResult(handler)
        val updatedHistory = (listOf(result) + uiState.history).take(MAX_HISTORY)

        uiState = uiState.copy(
            isProcessing = false,
            manualEntryVisible = if (closeManual) false else uiState.manualEntryVisible,
            manualEntryError = null,
            activeResult = result,
            history = updatedHistory
        )
    }

    private suspend fun commitCurrentGroup() {
        if (groupManager.groupCount == 0) return

        groupManager.commit()
        updateGroupState()
    }

    private fun removeFromGroup(ticketId: String) {
        groupManager.removeScan(ticketId)
        updateGroupState()
    }

    private fun updateGroupState() {
        uiState = uiState.copy(
            currentGroupEmail = groupManager.groupEmail,
            currentGroupTicketType = groupManager.groupTicketType,
            currentGroup = groupManager.scans
        )
    }

    private fun updateGroupedHistory() {
        val filteredHistory = if (uiState.showErrorScans) {
            uiState.history
        } else {
            uiState.history.filter {
                it.status == ScanStatus.SUCCESS || it.status == ScanStatus.OFFLINE_SUCCESS
            }
        }
        val grouped = HistoryGrouper.groupHistory(filteredHistory)
        uiState = uiState.copy(groupedHistory = grouped)
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            loadScanHistory()
            loadPendingScans()
            updateTotalCount()
        }
    }

    private suspend fun loadScanHistory() = withContext(Dispatchers.IO) {
        val scans = CommittedScansStore.getRecentScansForEvent(eventId, HISTORY_PAGE_SIZE)
        val history = scans.map { scan ->
            val ticket = if (scan.ticketType != null || scan.email != null) {
                VisitorTicket(
                    id = scan.ticketId,
                    eventId = scan.eventId,
                    ticketType = scan.ticketType,
                    email = scan.email,
                    status = VisitorTicketStatus.SCANNED,
                    issuedAt = null,
                    validFrom = null,
                    validUntil = null,
                    scannedAt = Instant.parse(scan.scannedAt)
                )
            } else null

            val status = try {
                ScanStatus.valueOf(scan.status)
            } catch (e: Exception) {
                if (scan.wasOffline) ScanStatus.OFFLINE_SUCCESS else ScanStatus.SUCCESS
            }

            ScanResult(
                ticket = ticket,
                status = status,
                timestamp = Instant.parse(scan.scannedAt),
                message = scan.errorMessage
            )
        }

        val totalCount = CommittedScansStore.countScansForEvent(eventId)
        val hasMore = history.size < totalCount

        withContext(Dispatchers.Main) {
            uiState = uiState.copy(
                history = history,
                hasMoreHistory = hasMore
            )
            updateGroupedHistory()
        }
    }

    private suspend fun loadMoreHistory() {
        if (uiState.isLoadingHistory || !uiState.hasMoreHistory) return

        withContext(Dispatchers.Main) {
            uiState = uiState.copy(isLoadingHistory = true)
        }

        withContext(Dispatchers.IO) {
            val currentSize = uiState.history.size
            val scans = CommittedScansStore.getRecentScansForEvent(eventId, currentSize + HISTORY_PAGE_SIZE)
            val history = scans.map { scan ->
                val ticket = if (scan.ticketType != null || scan.email != null) {
                    VisitorTicket(
                        id = scan.ticketId,
                        eventId = scan.eventId,
                        ticketType = scan.ticketType,
                        email = scan.email,
                        status = VisitorTicketStatus.SCANNED,
                        issuedAt = null,
                        validFrom = null,
                        validUntil = null,
                        scannedAt = Instant.parse(scan.scannedAt)
                    )
                } else null

                val status = try {
                    ScanStatus.valueOf(scan.status)
                } catch (e: Exception) {
                    if (scan.wasOffline) ScanStatus.OFFLINE_SUCCESS else ScanStatus.SUCCESS
                }

                ScanResult(
                    ticket = ticket,
                    status = status,
                    timestamp = Instant.parse(scan.scannedAt),
                    message = scan.errorMessage
                )
            }

            val totalCount = CommittedScansStore.countScansForEvent(eventId)
            val hasMore = history.size < totalCount

            withContext(Dispatchers.Main) {
                uiState = uiState.copy(
                    history = history,
                    hasMoreHistory = hasMore,
                    isLoadingHistory = false
                )
                updateGroupedHistory()
            }
        }
    }

    private suspend fun loadPendingScans() = withContext(Dispatchers.IO) {
        val pending = PendingScansStore.getAllScans()
        val pendingList = pending.map { scan ->
            PendingScan(ticketId = scan.ticketId, createdAt = Instant.parse(scan.scannedAt))
        }
        withContext(Dispatchers.Main) {
            uiState = uiState.copy(pendingScans = pendingList)
        }
    }

    private suspend fun updateTotalCount() = withContext(Dispatchers.IO) {
        val count = CommittedScansStore.countScansForEvent(eventId)
        withContext(Dispatchers.Main) {
            uiState = uiState.copy(totalScansCount = count)
        }
    }

    private fun rememberTicket(ticketId: String) {
        if (ticketId.isBlank() || recentScanIds.contains(ticketId)) return
        if (recentScanIds.size >= RECENT_SCAN_BUFFER) {
            recentScanIds.removeFirst()
        }
        recentScanIds.addLast(ticketId)
    }

    private suspend fun resolveTicketTypeName(ticket: VisitorTicket): VisitorTicket {
        val typeName = ticket.ticketType?.let {
            se.iloppis.app.data.TicketTypeRepository.resolveTypeName(it)
        }
        return ticket.copy(ticketType = typeName)
    }

    private fun extractErrorMessage(error: HttpException): String {
        return try {
            val errorBody = error.response()?.errorBody()?.string()
            errorBody?.let { JSONObject(it).optString("error", it) } ?: error.message()
        } catch (e: Exception) {
            error.message()
        }
    }

    private fun decodePayload(raw: String): TicketPayload? {
        return if (raw.startsWith("{") && raw.endsWith("}")) {
            runCatching {
                val json = JSONObject(raw)
                val ticketId = json.optString("ticket_id").ifBlank { json.optString("ticketId") }
                val eventId = json.optString("event_id").ifBlank { json.optString("eventId") }
                TicketPayload(ticketId, eventId.ifBlank { null })
            }.getOrNull()
        } else {
            TicketPayload(raw, null)
        }
    }

    private data class TicketPayload(val ticketId: String, val eventId: String?)
}
