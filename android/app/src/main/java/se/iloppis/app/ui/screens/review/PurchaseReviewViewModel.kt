package se.iloppis.app.ui.screens.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import se.iloppis.app.data.RejectedPurchaseStore
import se.iloppis.app.data.models.RejectedPurchase

/** Loads rejected purchases and observes additions to [RejectedPurchaseStore]. */
class PurchaseReviewViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(PurchaseReviewUiState())
    val uiState: StateFlow<PurchaseReviewUiState> = _uiState.asStateFlow()

    init {
        loadPurchases()
        viewModelScope.launch {
            RejectedPurchaseStore.rejectedPurchaseAdded.collect {
                loadPurchases()
            }
        }
    }

    /** Reloads all rejected purchases from local storage. */
    fun refresh() {
        loadPurchases()
    }

    private fun loadPurchases() {
        _uiState.value = _uiState.value.copy(isLoading = true)
        viewModelScope.launch(Dispatchers.IO) {
            val purchases = RejectedPurchaseStore.getAllRejectedPurchases()
            _uiState.value = PurchaseReviewUiState(
                purchases = purchases,
                isLoading = false
            )
        }
    }
}

data class PurchaseReviewUiState(
    val purchases: List<RejectedPurchase> = emptyList(),
    val isLoading: Boolean = true
)
