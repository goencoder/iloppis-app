package se.iloppis.app.ui.screens.events

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import retrofit2.HttpException
import se.iloppis.app.data.mappers.EventMapper.toDomain
import se.iloppis.app.domain.model.Event
import se.iloppis.app.domain.model.EventDisplayStatus
import se.iloppis.app.domain.model.displayStatus
import se.iloppis.app.network.config.clientConfig
import se.iloppis.app.network.events.EventAPI
import se.iloppis.app.network.events.EventFilter
import se.iloppis.app.network.events.EventFilterRequest
import se.iloppis.app.network.events.EventLifecycle
import se.iloppis.app.network.ILoppisClient
import java.time.Instant

/**
 * ViewModel for the unified event list screen.
 *
 * Handles search with debounce, filter chips, and saved events.
 * All filter chips map to real API calls per the UX spec.
 */
class EventListViewModel : ViewModel() {

    var uiState by mutableStateOf(EventListUiState())
        private set

    private var searchJob: Job? = null
    private var loadJob: Job? = null

    init {
        loadEvents()
    }

    fun onAction(action: EventListAction) {
        when (action) {
            is EventListAction.LoadEvents -> loadEvents()
            is EventListAction.UpdateSearch -> updateSearch(action.query)
            is EventListAction.SelectFilter -> selectFilter(action.filter)
        }
    }

    /**
     * Update search query with 300ms debounce.
     */
    private fun updateSearch(query: String) {
        uiState = uiState.copy(searchQuery = query)
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            if (query.isNotEmpty()) delay(300)
            loadEvents()
        }
    }

    /**
     * Change active filter chip and reload events.
     */
    private fun selectFilter(filter: EventFilterChip) {
        if (filter == uiState.activeFilter) return
        uiState = uiState.copy(activeFilter = filter)
        loadEvents()
    }

    /**
     * Load events based on current search + filter state.
     */
    private fun loadEvents() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, errorMessage = null)
            try {
                val events = loadFilteredEvents()
                uiState = uiState.copy(
                    events = events,
                    isLoading = false
                )
            } catch (e: Exception) {
                val errorMsg = when (e) {
                    is HttpException -> "HTTP ${e.code()}: ${e.message()}"
                    else -> "Network Error: ${e.message}"
                }
                Log.e(TAG, "Error loading events", e)
                uiState = uiState.copy(
                    isLoading = false,
                    errorMessage = errorMsg
                )
            }
        }
    }

    /**
     * Load events from API with filter + search applied.
     */
    private suspend fun loadFilteredEvents(): List<Event> {
        val api = ILoppisClient(clientConfig()).create<EventAPI>()
        val now = "${Instant.now()}"

        val filter = when (uiState.activeFilter) {
            EventFilterChip.ALL -> EventFilter(
                searchText = uiState.searchQuery.takeIf { it.length >= 2 }
            )
            EventFilterChip.UPCOMING -> EventFilter(
                lifecycleStates = listOf(EventLifecycle.OPEN),
                dateFrom = now,
                searchText = uiState.searchQuery.takeIf { it.length >= 2 }
            )
            EventFilterChip.ONGOING -> EventFilter(
                lifecycleStates = listOf(EventLifecycle.OPEN),
                searchText = uiState.searchQuery.takeIf { it.length >= 2 }
            )
            EventFilterChip.PAST -> EventFilter(
                lifecycleStates = listOf(EventLifecycle.CLOSED, EventLifecycle.FINALIZED),
                searchText = uiState.searchQuery.takeIf { it.length >= 2 }
            )
        }

        val request = EventFilterRequest(
            filter = filter,
            pagination = mapOf("pageSize" to 50)
        )
        Log.d(TAG, "Loading events: filter=${uiState.activeFilter}, search='${uiState.searchQuery}'")
        val response = api.get(request)
        var events = response.events.map { it.toDomain() }

        // Client-side filter for computed display statuses
        events = when (uiState.activeFilter) {
            EventFilterChip.ONGOING -> events.filter { it.displayStatus() == EventDisplayStatus.ONGOING }
            EventFilterChip.UPCOMING -> events.filter { it.displayStatus() == EventDisplayStatus.UPCOMING }
            else -> events
        }

        return events
    }

    companion object {
        val TAG: String = EventListViewModel::class.java.simpleName
    }
}



/**
 * Event view model context
 */
private val localEventScreenViewModel = compositionLocalOf<EventListViewModel> {
    error("No events view model provider is present in this context")
}



/**
 * Event screen state provider
 */
@Composable
fun EventScreenProvider(
    screen: EventListViewModel = viewModel(),
    content: @Composable () -> Unit
) {
    val state = remember { screen }
    CompositionLocalProvider(localEventScreenViewModel provides state) {
        content()
    }
}



/**
 * Event screen state context
 */
@Composable
fun eventContext(): EventListViewModel {
    return localEventScreenViewModel.current
}
