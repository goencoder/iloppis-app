package se.iloppis.app.ui.screens.live_stats

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import retrofit2.HttpException
import se.iloppis.app.network.ILoppisClient
import se.iloppis.app.network.config.clientConfig
import se.iloppis.app.network.stats.LiveStatsApiResponse
import se.iloppis.app.network.stats.StatsAPI
import kotlin.math.min

private const val TAG = "LiveStatsViewModel"
internal const val POLL_INTERVAL_MS = 10_000L
private const val MAX_BACKOFF_MS = 60_000L

data class LiveStatsUiState(
    val snapshot: LiveStatsApiResponse? = null,
    val isLoading: Boolean = true,
    val errorKey: String? = null
)

class LiveStatsViewModel(
    private val eventId: String,
    private val apiKey: String
) : ViewModel() {
    companion object {
        fun factory(eventId: String, apiKey: String) =
            object : androidx.lifecycle.ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    LiveStatsViewModel(eventId, apiKey) as T
            }
    }

    private val statsApi: StatsAPI = ILoppisClient(clientConfig()).create()
    private val _uiState = MutableStateFlow(LiveStatsUiState())
    val uiState: StateFlow<LiveStatsUiState> = _uiState.asStateFlow()

    private var pollingJob: Job? = null
    private var currentPollDelayMs: Long = POLL_INTERVAL_MS
    private var consecutiveFailures: Int = 0

    init {
        startPolling()
    }

    fun onScreenStarted() {
        if (pollingJob?.isActive != true) {
            startPolling()
        }
    }

    fun onScreenStopped() {
        pollingJob?.cancel()
        pollingJob = null
    }

    fun retry() {
        viewModelScope.launch { fetchSnapshot(forceLoading = _uiState.value.snapshot == null) }
    }

    override fun onCleared() {
        onScreenStopped()
        super.onCleared()
    }

    private fun startPolling() {
        pollingJob?.cancel()
        currentPollDelayMs = POLL_INTERVAL_MS
        consecutiveFailures = 0
        pollingJob = viewModelScope.launch {
            while (isActive) {
                fetchSnapshot(forceLoading = _uiState.value.snapshot == null)
                delay(currentPollDelayMs)
            }
        }
    }

    private suspend fun fetchSnapshot(forceLoading: Boolean) {
        if (forceLoading) {
            _uiState.value = _uiState.value.copy(isLoading = true, errorKey = null)
        }

        try {
            val response = statsApi.getEventLiveStats(
                eventId = eventId,
                authorization = "Bearer $apiKey"
            )
            _uiState.value = LiveStatsUiState(
                snapshot = response,
                isLoading = false,
                errorKey = null
            )
            consecutiveFailures = 0
            currentPollDelayMs = POLL_INTERVAL_MS
        } catch (error: HttpException) {
            consecutiveFailures += 1
            val retryAfterMs = parseRetryAfterMs(error)
            currentPollDelayMs = retryAfterMs ?: nextBackoffMs(consecutiveFailures)
            Log.w(
                TAG,
                "Live stats request failed with HTTP ${error.code()}, next poll in ${currentPollDelayMs}ms",
                error
            )
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorKey = when {
                    error.code() == 429 -> "rate_limited"
                    error.code() in 500..599 -> "server"
                    else -> "network"
                }
            )
        } catch (error: Exception) {
            consecutiveFailures += 1
            currentPollDelayMs = nextBackoffMs(consecutiveFailures)
            Log.w(TAG, "Live stats request failed, next poll in ${currentPollDelayMs}ms", error)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorKey = "network"
            )
        }
    }

    private fun nextBackoffMs(failureCount: Int): Long {
        val step = min(failureCount, 3)
        val multiplier = 1L shl step
        return min(MAX_BACKOFF_MS, POLL_INTERVAL_MS * multiplier)
    }

    private fun parseRetryAfterMs(error: HttpException): Long? {
        val retryAfterHeader = error.response()?.headers()?.get("Retry-After") ?: return null
        val seconds = retryAfterHeader.toLongOrNull() ?: return null
        return if (seconds > 0) seconds * 1000L else null
    }
}
