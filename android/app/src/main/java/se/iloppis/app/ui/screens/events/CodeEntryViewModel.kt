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
import se.iloppis.app.data.SavedCode
import se.iloppis.app.data.SavedCodesStore
import se.iloppis.app.data.mappers.EventMapper.toDomain
import se.iloppis.app.domain.model.CodeEntryMode
import se.iloppis.app.domain.model.Event
import se.iloppis.app.network.ILoppisClient
import se.iloppis.app.network.config.clientConfig
import se.iloppis.app.network.events.EventAPI
import se.iloppis.app.network.keys.KeyAPI

private const val TAG = "CodeEntryViewModel"

private fun normalizeApiKeyType(type: String?): String =
    type?.uppercase()?.replace("-", "_").orEmpty()

private fun resolveToolMode(type: String?): CodeEntryMode? {
    val normalized = normalizeApiKeyType(type)
    return when {
        normalized.contains("LIVE_STATS") -> CodeEntryMode.LIVE_STATS
        normalized.contains("SCANNER") -> CodeEntryMode.SCANNER
        normalized.contains("CASHIER") -> CodeEntryMode.CASHIER
        else -> null
    }
}

private fun isToolModeAllowed(entryMode: CodeEntryMode, resolvedMode: CodeEntryMode?): Boolean {
    if (resolvedMode == null) return false
    return when (entryMode) {
        CodeEntryMode.TOOL -> true
        else -> entryMode == resolvedMode
    }
}

private fun wrongTypeErrorKey(entryMode: CodeEntryMode): String = when (entryMode) {
    CodeEntryMode.CASHIER -> "wrong_type_cashier"
    CodeEntryMode.SCANNER -> "wrong_type_scanner"
    CodeEntryMode.LIVE_STATS -> "wrong_type_live_stats"
    CodeEntryMode.TOOL -> "wrong_type_tool"
}

// ──────────────────────────────────────────────
// State
// ──────────────────────────────────────────────

/**
 * A saved code entry that has been validated against the API.
 */
data class ValidatedSavedCode(
    val savedCode: SavedCode,
    /** True if the API confirmed this code is still active + correct type. */
    val isValid: Boolean,
    /** True while the async validation is running. */
    val isValidating: Boolean = false
)

data class CodeEntryUiState(
    val mode: CodeEntryMode = CodeEntryMode.CASHIER,
    val rawCode: String = "",
    val isLoading: Boolean = false,
    val errorKey: String? = null,
    /** Set on successful verification — screen reads this to navigate. */
    val verifiedResult: CodeVerifiedResult? = null,
    /** Saved codes (filtered by eventId if applicable, and by mode). */
    val savedCodes: List<ValidatedSavedCode> = emptyList(),
    /** Whether saved codes are still being loaded/validated. */
    val isSavedCodesLoading: Boolean = true
) {
    /** Formatted display code: XXX-XXX */
    val displayCode: String
        get() = rawCode

    val isCodeComplete: Boolean get() = rawCode.length == 6
}

data class CodeVerifiedResult(
    val event: Event,
    val apiKey: String,
    val alias: String,
    val entryMode: CodeEntryMode,
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
    /** User tapped a previously saved code to re-use it. */
    data class SelectSavedCode(val savedCode: SavedCode) : CodeEntryAction()
    /** User swiped to remove a saved code. */
    data class RemoveSavedCode(val alias: String) : CodeEntryAction()
}

// ──────────────────────────────────────────────
// ViewModel
// ──────────────────────────────────────────────

class CodeEntryViewModel(
    private val mode: CodeEntryMode,
    private val eventId: String?
) : ViewModel() {

    companion object {
        fun factory(mode: CodeEntryMode, eventId: String?) =
            object : androidx.lifecycle.ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    CodeEntryViewModel(mode, eventId) as T
            }
    }

    private val _uiState = MutableStateFlow(CodeEntryUiState(mode = mode))
    val uiState: StateFlow<CodeEntryUiState> = _uiState.asStateFlow()

    init {
        loadSavedCodes()
    }

    fun onAction(action: CodeEntryAction) {
        when (action) {
            is CodeEntryAction.UpdateCode -> onCodeChange(action.input)
            is CodeEntryAction.VerifyCode -> verifyCode()
            is CodeEntryAction.NavigationConsumed -> {
                _uiState.value = _uiState.value.copy(
                    verifiedResult = null,
                    rawCode = "",
                    errorKey = null
                )
                // Reload saved codes so the just-used code appears in the list
                loadSavedCodes()
            }
            is CodeEntryAction.SelectSavedCode -> useSavedCode(action.savedCode)
            is CodeEntryAction.RemoveSavedCode -> removeSavedCode(action.alias)
        }
    }

    // ── Saved codes ─────────────────────────────

    private fun loadSavedCodes() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSavedCodesLoading = true)
            try {
                val allCodes = SavedCodesStore.loadAll()

                // Filter by mode
                val filtered = allCodes
                    .filter {
                        mode == CodeEntryMode.TOOL || it.codeType == mode.name
                    }
                    .let { codes ->
                        // Filter by eventId if navigating from event page
                        if (eventId != null) codes.filter { it.eventId == eventId }
                        else codes
                    }

                // Show immediately. Do not auto-validate all aliases against backend,
                // because that can trigger burst traffic and rate limiting.
                val initial = filtered.map {
                    ValidatedSavedCode(savedCode = it, isValid = true, isValidating = false)
                }
                _uiState.value = _uiState.value.copy(
                    savedCodes = initial,
                    isSavedCodesLoading = false
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load saved codes", e)
                _uiState.value = _uiState.value.copy(
                    savedCodes = emptyList(),
                    isSavedCodesLoading = false
                )
            }
        }
    }

    private fun useSavedCode(code: SavedCode) {
        // Navigate directly via verify flow using the saved alias
        val raw = code.alias.replace("-", "")
        _uiState.value = _uiState.value.copy(rawCode = raw, errorKey = null)
        verifyCode()
    }

    private fun removeSavedCode(alias: String) {
        viewModelScope.launch {
            SavedCodesStore.remove(alias)
            val current = _uiState.value
            _uiState.value = current.copy(
                savedCodes = current.savedCodes.filter { it.savedCode.alias != alias }
            )
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
                val resolvedMode = resolveToolMode(response.type)
                if (!isToolModeAllowed(mode, resolvedMode)) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorKey = wrongTypeErrorKey(mode)
                    )
                    return@launch
                }

                // Fetch the event to show in confirmation
                val resolvedEventId = response.eventId.takeIf { it.isNotBlank() } ?: eventId
                if (resolvedEventId.isNullOrBlank()) {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorKey = "not_found")
                    return@launch
                }

                val eventApi = ILoppisClient(clientConfig()).create<EventAPI>()
                val eventResponse = eventApi.get(resolvedEventId)
                val event = eventResponse.events.firstOrNull()?.toDomain()

                if (event == null) {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorKey = "not_found")
                    return@launch
                }

                // Save the code for future quick access.
                // Do not persist the API key — it is re-fetched on next use.
                val actualMode = resolvedMode ?: run {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorKey = wrongTypeErrorKey(mode))
                    return@launch
                }

                SavedCodesStore.save(
                    SavedCode(
                        alias = formattedAlias,
                        eventId = event.id,
                        eventName = event.name,
                        codeType = actualMode.name
                    )
                )

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    verifiedResult = CodeVerifiedResult(
                        event = event,
                        apiKey = response.apiKey,
                        alias = formattedAlias,
                        entryMode = mode,
                        mode = actualMode
                    )
                )
            } catch (e: HttpException) {
                Log.e(TAG, "HTTP Error ${e.code()}", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorKey = when (e.code()) {
                        429 -> "rate_limited"
                        400, 404, 422 -> "not_found"
                        401, 403 -> "inactive"
                        in 500..599 -> "server"
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
