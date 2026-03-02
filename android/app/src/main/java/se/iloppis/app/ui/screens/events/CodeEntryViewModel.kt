package se.iloppis.app.ui.screens.events

import android.util.Log
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import se.iloppis.app.data.mappers.EventMapper.toDomain
import se.iloppis.app.domain.model.CodeEntryMode
import se.iloppis.app.domain.model.Event
import se.iloppis.app.network.ILoppisClient
import se.iloppis.app.network.config.clientConfig
import se.iloppis.app.network.events.EventAPI
import se.iloppis.app.network.keys.KeyAPI

private const val TAG = "CodeEntryViewModel"

// ──────────────────────────────────────────────
// State
// ──────────────────────────────────────────────

data class CodeEntryUiState(
    val mode: CodeEntryMode = CodeEntryMode.CASHIER,
    val rawCode: String = "",
    val isLoading: Boolean = false,
    val errorKey: String? = null,
    /** Set on successful verification — screen reads this to navigate. */
    val verifiedResult: CodeVerifiedResult? = null
) {
    /** Formatted display code: XXX-YYY */
    val displayCode: String
        get() = rawCode

    val isCodeComplete: Boolean get() = rawCode.length == 6
}

data class CodeVerifiedResult(
    val event: Event,
    val apiKey: String,
    val mode: CodeEntryMode
)

// ──────────────────────────────────────────────
// Code entry format transform
// ──────────────────────────────────────────────

class CodeEntryFormatTransform : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val format = AnnotatedString.Builder(
            if(text.length > 3) {
                "${text.substring(0, 3)}-${text.substring(3)}"
            } else {
                text.text
            }
        ).toAnnotatedString()

        val mapper = object : OffsetMapping  {
            override fun originalToTransformed(offset: Int): Int {
                return if(offset <= 3) offset
                else offset + 1
            }
            override fun transformedToOriginal(offset: Int): Int {
                return if(offset <= 4) offset
                else offset - 1
            }
        }

        return TransformedText(format, mapper)
    }
}

// ──────────────────────────────────────────────
// Actions
// ──────────────────────────────────────────────

sealed class CodeEntryAction {
    data class UpdateCode(val input: String) : CodeEntryAction()
    data object VerifyCode : CodeEntryAction()
    data object NavigationConsumed : CodeEntryAction()
}

// ──────────────────────────────────────────────
// ViewModel
// ──────────────────────────────────────────────

class CodeEntryViewModel(private val mode: CodeEntryMode) : ViewModel() {

    companion object {
        fun factory(mode: CodeEntryMode) = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                CodeEntryViewModel(mode) as T
        }
    }

    private val _uiState = MutableStateFlow(CodeEntryUiState(mode = mode))
    val uiState: StateFlow<CodeEntryUiState> = _uiState.asStateFlow()

    fun onAction(action: CodeEntryAction) {
        when (action) {
            is CodeEntryAction.UpdateCode -> onCodeChange(action.input)
            is CodeEntryAction.VerifyCode -> verifyCode()
            is CodeEntryAction.NavigationConsumed -> {
                _uiState.value = _uiState.value.copy(verifiedResult = null)
            }
        }
    }

    // ── Business logic ──────────────────────────

    private fun onCodeChange(input: String) {
        val cleaned = input.replace("-", "").replace(" ", "")
            .filter { it.isLetterOrDigit() }
            .take(6)
            .uppercase()
        _uiState.value = _uiState.value.copy(rawCode = cleaned, errorKey = null)
    }

    private fun verifyCode() {
        val currentState = _uiState.value
        if (!currentState.isCodeComplete || currentState.isLoading) return
        val raw = currentState.rawCode
        val formattedAlias = "${raw.substring(0, 3)}-${raw.substring(3, 6)}"

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorKey = null)
            try {
                val keyApi = ILoppisClient(clientConfig()).create<KeyAPI>()
                val response = keyApi.getApiKeyByAlias(formattedAlias)
                Log.d(TAG, "Alias resolved: eventId=${response.eventId}, type=${response.type}, active=${response.isActive}")

                // Validate isActive
                if (!response.isActive) {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorKey = "inactive")
                    return@launch
                }

                // Validate type matches mode
                val responseType = response.type?.uppercase() ?: ""
                val isValidType = when (mode) {
                    CodeEntryMode.CASHIER -> responseType.contains("CASHIER")
                    CodeEntryMode.SCANNER -> responseType.contains("SCANNER")
                }
                if (responseType.isNotEmpty() && !isValidType) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorKey = if (mode == CodeEntryMode.CASHIER) "wrong_type_cashier" else "wrong_type_scanner"
                    )
                    return@launch
                }

                // Fetch the event to show in confirmation
                val eventApi = ILoppisClient(clientConfig()).create<EventAPI>()
                val eventResponse = eventApi.get(response.eventId)
                val event = eventResponse.events.firstOrNull()?.toDomain()

                if (event == null) {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorKey = "not_found")
                    return@launch
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    verifiedResult = CodeVerifiedResult(
                        event = event,
                        apiKey = response.apiKey,
                        mode = mode
                    )
                )
            } catch (e: HttpException) {
                Log.e(TAG, "HTTP Error ${e.code()}", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorKey = when (e.code()) {
                        404 -> "not_found"
                        else -> "network"
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error verifying code", e)
                _uiState.value = _uiState.value.copy(isLoading = false, errorKey = "network")
            }
        }
    }
}
