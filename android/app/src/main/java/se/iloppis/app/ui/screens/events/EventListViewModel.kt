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
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json.Default.decodeFromString
import retrofit2.HttpException
import se.iloppis.app.R
import se.iloppis.app.data.mappers.EventMapper.toDomain
import se.iloppis.app.domain.model.Event
import se.iloppis.app.navigation.ScreenPage
import se.iloppis.app.network.ApiKeyApi
import se.iloppis.app.network.ClientConfig
import se.iloppis.app.network.events.EventAPI
import se.iloppis.app.network.events.EventFilter
import se.iloppis.app.network.events.EventFilterRequest
import se.iloppis.app.network.iLoppisClient
import se.iloppis.app.ui.screens.ScreenModel
import se.iloppis.app.ui.states.ScreenAction
import se.iloppis.app.utils.context.localContext
import java.time.LocalDate

private const val TAG = "EventListViewModel"

/**
 * ViewModel for the event list screen.
 * Handles all business logic and state management.
 */
class EventListViewModel : ViewModel() {

    var uiState by mutableStateOf(EventListUiState())
        private set

    lateinit var config: ClientConfig

    init {
        loadEvents()
    }

    fun onAction(action: EventListAction) {
        when (action) {
            is EventListAction.LoadEvents -> loadEvents()
            is EventListAction.SelectEvent -> selectEvent(action.event)
            is EventListAction.DismissEventDetail -> dismissEventDetail()
            is EventListAction.StartCodeEntry -> startCodeEntry(action.mode, action.event)
            is EventListAction.DismissCodeEntry -> dismissCodeEntry()
            is EventListAction.SubmitCode -> submitCode(action.state, action.code)
            is EventListAction.ValidateCode -> validateCode(action.state, action.code)
        }
    }

    private fun loadEvents() {
        Log.d(TAG, "Loading events from: https://iloppis.fly.dev/")
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, errorMessage = null)
            try {
                val api = iLoppisClient(config).create<EventAPI>()
                Log.d(TAG, "API client created, making filterEvents request")
                // Filtrera på dagens datum och endast OPEN evenemang
                val today = "${LocalDate.now()}T00:00:00Z"
                val filterRequest = EventFilterRequest(
                    filter = EventFilter(
                        dateFrom = today,
                        lifecycleStates = listOf("OPEN")
                    )
                )
                Log.d(TAG, "Filter request: dateFrom=$today, states=[OPEN]")
                val response = api.get(filterRequest)
                Log.d(TAG, "Response received, events count: ${response.events.size}")
                val events = response.events.map { it.toDomain() }
                uiState = uiState.copy(
                    events = events,
                    isLoading = false,
                    errorMessage = null
                )
            } catch (e: Exception) {
                val errorMsg = when (e) {
                    is HttpException -> "HTTP ${e.code()}: ${e.message()}"
                    else -> "Network Error: ${e.message}"
                }
                Log.e(TAG, "Error loading events", e)
                uiState = uiState.copy(
                    isLoading = false,
                    errorMessage = "API: https://iloppis.fly.dev/ - $errorMsg"
                )
            }
        }
    }

    private fun selectEvent(event: Event) {
        uiState = uiState.copy(selectedEvent = event)
    }

    private fun dismissEventDetail() {
        uiState = uiState.copy(selectedEvent = null)
    }

    private fun startCodeEntry(mode: CodeEntryMode, event: Event) {
        uiState = uiState.copy(
            selectedEvent = null,
            codeEntryState = CodeEntryState(mode = mode, event = event)
        )
    }

    private fun dismissCodeEntry() {
        uiState = uiState.copy(codeEntryState = null)
    }

    /**
     * Validate the code as the user types (implicit validation).
     * Shows error message when code is complete but invalid.
     */
    private fun validateCode(state: ScreenModel, code: String) {
        val codeEntry = uiState.codeEntryState ?: return

        // Format: XXX-YYY (3 chars, dash, 3 chars) = 7 chars total
        // But we receive it without dash as 6 chars
        if (code.length < 6) {
            // Code not complete yet - clear any error
            uiState = uiState.copy(
                codeEntryState = codeEntry.copy(errorMessage = null, isValidating = false)
            )
            return
        }

        // Code is complete - validate it against API
        viewModelScope.launch {
            uiState = uiState.copy(
                codeEntryState = codeEntry.copy(isValidating = true, errorMessage = null)
            )

            // Format code as XXX-YYY for API
            val formattedCode = "${code.substring(0, 3)}-${code.substring(3, 6)}".uppercase()
            val eventId = codeEntry.event.id

            Log.d(TAG, "Validating code: $formattedCode for event: $eventId")

            try {
                val api = iLoppisClient(config).create<ApiKeyApi>()
                val response = api.getApiKeyByAlias(eventId, formattedCode)

                Log.d(TAG, "API Response - alias: ${response.alias}, isActive: ${response.isActive}, type: ${response.type}")

                if (!response.isActive) {
                    Log.w(TAG, "API key is not active")
                    uiState = uiState.copy(
                        codeEntryState = codeEntry.copy(
                            isValidating = false,
                            errorMessage = "inactive" // Key for string resource
                        )
                    )
                    return@launch
                }

                // Check if type matches mode (if type is available)
                // API returns types like: API_KEY_TYPE_CASHIER, API_KEY_TYPE_WEB_CASHIER,
                // API_KEY_TYPE_SCANNER, API_KEY_TYPE_WEB_SCANNER
                val responseType = response.type?.uppercase() ?: ""
                val isValidType = when (codeEntry.mode) {
                    CodeEntryMode.CASHIER -> responseType.contains("CASHIER")
                    CodeEntryMode.SCANNER -> responseType.contains("SCANNER")
                }

                if (responseType.isNotEmpty() && !isValidType) {
                    Log.w(TAG, "API key type mismatch. Expected type containing: ${codeEntry.mode}, Got: $responseType")
                    uiState = uiState.copy(
                        codeEntryState = codeEntry.copy(
                            isValidating = false,
                            errorMessage = "wrong_type" // Key for string resource
                        )
                    )
                    return@launch
                }

                // Success - navigate to the appropriate screen
                Log.i(TAG, "Code validated successfully! Navigating to ${codeEntry.mode}")
                val screen = when (codeEntry.mode) {
                    CodeEntryMode.CASHIER -> ScreenPage.Cashier(codeEntry.event, response.apiKey)
                    CodeEntryMode.SCANNER -> ScreenPage.Scanner(codeEntry.event, response.apiKey)
                }
                state.onAction(ScreenAction.NavigateToPage(screen, false))
                uiState = uiState.copy(codeEntryState = null)

            } catch (e: HttpException) {
                val errorBody = e.response()?.errorBody()?.string()
                Log.e(TAG, "HTTP Error ${e.code()}: ${e.message()}")
                Log.e(TAG, "Error body: $errorBody")

                val errorKey = when (e.code()) {
                    404 -> "not_found"
                    401, 403 -> "unauthorized"
                    else -> "invalid"
                }

                uiState = uiState.copy(
                    codeEntryState = codeEntry.copy(
                        isValidating = false,
                        errorMessage = errorKey
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Validation error: ${e.message}", e)
                uiState = uiState.copy(
                    codeEntryState = codeEntry.copy(
                        isValidating = false,
                        errorMessage = "network_error"
                    )
                )
            }
        }
    }

    /**
     * Submit code (when user clicks verify button).
     * Same as validateCode but user-initiated.
     */
    private fun submitCode(state: ScreenModel, code: String) {
        validateCode(state, code)
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
fun EventScreenProvider(screen: EventListViewModel = viewModel(), content: @Composable () -> Unit) {
    val stream = localContext().resources.openRawResource(R.raw.client)
    val conf = decodeFromString<ClientConfig>(stream.readBytes().decodeToString())

    val state = remember { screen.apply { config = conf } }
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
