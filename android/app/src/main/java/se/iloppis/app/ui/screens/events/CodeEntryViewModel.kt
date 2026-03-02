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
                _uiState.value = _uiState.value.copy(verifiedResult = null)
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
                val modeStr = mode.name
                val filtered = allCodes
                    .filter { it.codeType == modeStr }
                    .let { codes ->
                        // Filter by eventId if navigating from event page
                        if (eventId != null) codes.filter { it.eventId == eventId }
                        else codes
                    }

                // Show immediately, then validate async
                val initial = filtered.map {
                    ValidatedSavedCode(savedCode = it, isValid = true, isValidating = true)
                }
                _uiState.value = _uiState.value.copy(
                    savedCodes = initial,
                    isSavedCodesLoading = false
                )

                // Validate each code asynchronously
                for (code in filtered) {
                    launch { validateSavedCode(code) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load saved codes", e)
                _uiState.value = _uiState.value.copy(
                    savedCodes = emptyList(),
                    isSavedCodesLoading = false
                )
            }
        }
    }

    private suspend fun validateSavedCode(code: SavedCode) {
        try {
            val keyApi = ILoppisClient(clientConfig()).create<KeyAPI>()
            val response = keyApi.getApiKeyByAlias(code.alias)

            val responseType = response.type?.uppercase() ?: ""
            val isTypeValid = when (mode) {
                CodeEntryMode.CASHIER -> responseType.contains("CASHIER")
                CodeEntryMode.SCANNER -> responseType.contains("SCANNER")
            }
            val isValid = response.isActive && (responseType.isEmpty() || isTypeValid)

            updateSavedCodeValidation(code.alias, isValid)

            // If invalid, remove from persistent store
            if (!isValid) {
                SavedCodesStore.remove(code.alias)
            }
        } catch (e: HttpException) {
            if (e.code() == 404) {
                // Code no longer exists
                updateSavedCodeValidation(code.alias, isValid = false)
                SavedCodesStore.remove(code.alias)
            } else {
                // Network error — assume still valid
                updateSavedCodeValidation(code.alias, isValid = true)
            }
        } catch (e: Exception) {
            // Network error — assume still valid to not degrade offline experience
            Log.w(TAG, "Could not validate saved code ${code.alias}", e)
            updateSavedCodeValidation(code.alias, isValid = true)
        }
    }

    private fun updateSavedCodeValidation(alias: String, isValid: Boolean) {
        val current = _uiState.value
        val updated = current.savedCodes.map { entry ->
            if (entry.savedCode.alias == alias) {
                entry.copy(isValid = isValid, isValidating = false)
            } else entry
        }
        // Remove invalid entries from the displayed list
        _uiState.value = current.copy(savedCodes = updated.filter { it.isValid || it.isValidating })
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

                // Save the code for future quick access
                SavedCodesStore.save(
                    SavedCode(
                        alias = formattedAlias,
                        eventId = event.id,
                        eventName = event.name,
                        codeType = mode.name,
                        apiKey = response.apiKey
                    )
                )

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
