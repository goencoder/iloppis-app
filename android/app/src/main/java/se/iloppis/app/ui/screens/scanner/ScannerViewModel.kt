package se.iloppis.app.ui.screens.scanner

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
import se.iloppis.app.network.ILoppisClient
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
    data object RequestTicketSearch : ScannerAction()
    data object DismissTicketSearch : ScannerAction()
    data class SubmitTicketSearch(val query: String, val ticketTypeId: String?) : ScannerAction()
    data class SelectSearchResult(val ticket: VisitorTicket) : ScannerAction()
    data object DismissSearchDetail : ScannerAction()
    data class ScanFromDetail(val ticketId: String) : ScannerAction()
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
 * A ticket type entry for the search dropdown.
 */
data class TicketTypeOption(
    val id: String,
    val name: String
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
    val showErrorScans: Boolean = false,
    val ticketSearchVisible: Boolean = false,
    val isSearching: Boolean = false,
    val searchResults: List<VisitorTicket> = emptyList(),
    val searchError: String? = null,
    val searchDetailTicket: VisitorTicket? = null,
    val ticketTypes: List<TicketTypeOption> = emptyList()
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

    companion object {
        fun factory(eventId: String, eventName: String, apiKey: String) =
            object : androidx.lifecycle.ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ScannerViewModel(eventId, eventName, apiKey) as T
            }
    }

    private val visitorTicketApi: VisitorAPI = ILoppisClient(clientConfig()).create()
    private val groupManager = GroupScanManager()
    private val recentScanIds = ArrayDeque<String>(RECENT_SCAN_BUFFER)
    private var searchJob: Job? = null
    private var ticketTypesLoadJob: Job? = null

    private val _uiState = MutableStateFlow(
        ScannerUiState(
            eventName = eventName,
            currentGroupEmail = groupManager.groupEmail,
            currentGroupTicketType = groupManager.groupTicketType,
            currentGroup = groupManager.scans
        )
    )
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    init {
        // Initialize file stores for this event
        val context = se.iloppis.app.ILoppisAppHolder.appContext
        se.iloppis.app.data.EventStoreManager.initializeForEvent(context, eventId)

        loadInitialData()
        // Ticket type name resolution
        viewModelScope.launch(Dispatchers.IO) {
            se.iloppis.app.data.TicketTypeRepository.refresh(eventId, apiKey)
        }
    }

    fun onAction(action: ScannerAction) {
        when (action) {
            is ScannerAction.RequestManualEntry -> _uiState.value = _uiState.value.copy(manualEntryVisible = true)
            is ScannerAction.DismissManualEntry -> _uiState.value = _uiState.value.copy(
                manualEntryVisible = false,
                manualEntryError = null
            )
            is ScannerAction.SubmitCode -> handleCodeSubmission(action.code)
            is ScannerAction.ClearManualError -> _uiState.value = _uiState.value.copy(manualEntryError = null)
            is ScannerAction.DismissResult -> _uiState.value = _uiState.value.copy(activeResult = null)
            is ScannerAction.ShowTicketDetails -> _uiState.value = _uiState.value.copy(ticketDetailsResult = action.result)
            is ScannerAction.DismissTicketDetails -> _uiState.value = _uiState.value.copy(ticketDetailsResult = null)
            is ScannerAction.CommitCurrentGroup -> viewModelScope.launch { commitCurrentGroup() }
            is ScannerAction.ToggleGroupExpanded -> _uiState.value = _uiState.value.copy(
                isGroupExpanded = !_uiState.value.isGroupExpanded
            )
            is ScannerAction.RemoveFromGroup -> removeFromGroup(action.ticketId)
            is ScannerAction.LoadMoreHistory -> viewModelScope.launch { loadMoreHistory() }
            is ScannerAction.ToggleErrorScans -> {
                val newShowErrors = !_uiState.value.showErrorScans
                _uiState.value = _uiState.value.copy(showErrorScans = newShowErrors)
                // Re-filter history
                updateGroupedHistory()
            }
            is ScannerAction.RequestTicketSearch -> openTicketSearch()
            is ScannerAction.DismissTicketSearch -> {
                searchJob?.cancel()
                ticketTypesLoadJob?.cancel()
                _uiState.value = _uiState.value.copy(
                    ticketSearchVisible = false,
                    isSearching = false,
                    searchResults = emptyList(),
                    searchError = null
                )
            }
            is ScannerAction.SubmitTicketSearch -> handleTicketSearch(action.query, action.ticketTypeId)
            is ScannerAction.SelectSearchResult -> _uiState.value = _uiState.value.copy(
                searchDetailTicket = action.ticket,
                ticketSearchVisible = false
            )
            is ScannerAction.DismissSearchDetail -> _uiState.value = _uiState.value.copy(
                searchDetailTicket = null,
                ticketSearchVisible = true
            )
            is ScannerAction.ScanFromDetail -> performScanFromDetail(action.ticketId)
        }
    }

    private fun openTicketSearch() {
        ticketTypesLoadJob?.cancel()
        _uiState.value = _uiState.value.copy(
            ticketSearchVisible = true,
            searchResults = emptyList(),
            searchError = null,
            ticketTypes = emptyList()
        )
        ticketTypesLoadJob = viewModelScope.launch(Dispatchers.IO) {
            val types = se.iloppis.app.data.TicketTypeRepository.getAllTypes()
                .map { TicketTypeOption(id = it.first, name = it.second) }
            withContext(Dispatchers.Main) {
                if (_uiState.value.ticketSearchVisible) {
                    _uiState.value = _uiState.value.copy(ticketTypes = types)
                }
            }
        }
    }

    private fun handleTicketSearch(query: String, ticketTypeId: String?) {
        if (query.isBlank()) return
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSearching = true, searchError = null)
            try {
                // Resolve ticket type ID to name for the API filter
                val ticketTypeName = ticketTypeId?.let {
                    se.iloppis.app.data.TicketTypeRepository.resolveTypeName(it)
                }
                val filter = se.iloppis.app.network.visitor.VisitorTicketFilter(
                    freeText = query.trim(),
                    ticketType = ticketTypeName,
                    status = null
                )
                val request = se.iloppis.app.network.visitor.FilterVisitorTicketsRequest(
                    filter = filter,
                    pagination = se.iloppis.app.network.visitor.PaginationRequest(pageSize = 50)
                )
                val response = withContext(Dispatchers.IO) {
                    visitorTicketApi.filterTickets(
                        authorization = "Bearer $apiKey",
                        eventId = eventId,
                        body = request
                    )
                }
                val tickets = response.tickets.map { it.toDomain() }.map { resolveTicketTypeName(it) }
                _uiState.value = _uiState.value.copy(
                    isSearching = false,
                    searchResults = tickets
                )
            } catch (e: CancellationException) {
                Log.d(TAG, "Ticket search cancelled")
            } catch (e: Exception) {
                Log.e(TAG, "Ticket search failed", e)
                _uiState.value = _uiState.value.copy(
                    isSearching = false,
                    searchError = e.message ?: e.localizedMessage ?: e.toString()
                )
            }
        }
    }

    private fun performScanFromDetail(ticketId: String) {
        if (_uiState.value.isProcessing) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isProcessing = true,
                searchDetailTicket = null,
                ticketSearchVisible = false,
                ticketDetailsResult = null
            )
            val scanId = UUID.randomUUID().toString()
            val now = Instant.now().toString()
            try {
                val ticket = performOnlineScan(TicketPayload(ticketId = ticketId, eventId = null))
                handleSuccessfulScan(scanId, ticketId, now, ticket, wasOffline = false)
            } catch (e: HttpException) {
                val message = extractErrorMessage(e)
                val fetchedTicket = fetchTicketIfExists(ticketId)
                val handler = ScanResultHandler.fromHttpCode(e.code(), message, fetchedTicket)
                showResult(handler)
            } catch (e: Exception) {
                Log.e(TAG, "Scan from detail failed", e)
                showResult(ScanResultHandler.Error(e.message ?: "Unknown error"))
            }
        }
    }

    private fun handleCodeSubmission(rawCode: String) {
        val trimmed = rawCode.trim()

        when {
            trimmed.isEmpty() -> {
                _uiState.value = _uiState.value.copy(manualEntryError = ManualEntryError.EMPTY_INPUT)
                return
            }
        }

        val payload = decodePayload(trimmed) ?: run {
            _uiState.value = _uiState.value.copy(manualEntryError = ManualEntryError.INVALID_FORMAT)
            return
        }

        if (!payload.eventId.isNullOrBlank() && payload.eventId != eventId) {
            _uiState.value = _uiState.value.copy(manualEntryError = ManualEntryError.WRONG_EVENT)
            return
        }

        performScan(payload)
    }

    private fun performScan(payload: TicketPayload) {
        if (_uiState.value.isProcessing) {
            Log.w(TAG, "Scan already in progress, ignoring")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessing = true, manualEntryError = null)
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
                _uiState.value = _uiState.value.copy(isProcessing = false)
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
                _uiState.value = _uiState.value.copy(isProcessing = false)
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
        val updatedHistory = (listOf(result) + _uiState.value.history).take(MAX_HISTORY)

        _uiState.value = _uiState.value.copy(
            isProcessing = false,
            manualEntryVisible = if (closeManual) false else _uiState.value.manualEntryVisible,
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
        _uiState.value = _uiState.value.copy(
            currentGroupEmail = groupManager.groupEmail,
            currentGroupTicketType = groupManager.groupTicketType,
            currentGroup = groupManager.scans
        )
    }

    private fun updateGroupedHistory() {
        val filteredHistory = if (_uiState.value.showErrorScans) {
            _uiState.value.history
        } else {
            _uiState.value.history.filter {
                it.status == ScanStatus.SUCCESS || it.status == ScanStatus.OFFLINE_SUCCESS
            }
        }
        val grouped = HistoryGrouper.groupHistory(filteredHistory)
        _uiState.value = _uiState.value.copy(groupedHistory = grouped)
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
            _uiState.value = _uiState.value.copy(
                history = history,
                hasMoreHistory = hasMore
            )
            updateGroupedHistory()
        }
    }

    private suspend fun loadMoreHistory() {
        if (_uiState.value.isLoadingHistory || !_uiState.value.hasMoreHistory) return

        withContext(Dispatchers.Main) {
            _uiState.value = _uiState.value.copy(isLoadingHistory = true)
        }

        withContext(Dispatchers.IO) {
            val currentSize = _uiState.value.history.size
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
                _uiState.value = _uiState.value.copy(
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
            _uiState.value = _uiState.value.copy(pendingScans = pendingList)
        }
    }

    private suspend fun updateTotalCount() = withContext(Dispatchers.IO) {
        val count = CommittedScansStore.countScansForEvent(eventId)
        withContext(Dispatchers.Main) {
            _uiState.value = _uiState.value.copy(totalScansCount = count)
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
