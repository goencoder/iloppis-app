package se.iloppis.app.ui.screens.pending

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import se.iloppis.app.data.PendingItemsStore
import se.iloppis.app.data.models.PendingItem

data class PendingPurchasesUiState(
    val purchases: List<PendingPurchaseUi> = emptyList(),
    val expandedPurchaseId: String? = null,
    val processingPurchaseId: String? = null,
    val isLoading: Boolean = true
)

data class PendingPurchaseUi(
    val purchaseId: String,
    val items: List<PendingItemUi>,
    val severity: PurchaseSeverity,
    val timestamp: String
)

data class PendingItemUi(
    val itemId: String,
    val sellerId: Int,
    val price: Int,
    val errorText: String
)

enum class PurchaseSeverity {
    INFO,      // All errorText=""
    WARNING,   // Some errorText but no "serverfel"
    CRITICAL   // Contains "serverfel"
}

class PendingPurchasesViewModel(private val eventId: String) : ViewModel() {
    private val _uiState = MutableStateFlow(PendingPurchasesUiState())
    val uiState: StateFlow<PendingPurchasesUiState> = _uiState.asStateFlow()
    
    private var processingJob: Job? = null

    init {
        // Initialize file store for this event
        val context = se.iloppis.app.ILoppisAppHolder.appContext
        PendingItemsStore.initialize(context, eventId)
        
        loadPurchases()
        
        // Listen for updates from PendingItemsStore
        viewModelScope.launch {
            PendingItemsStore.itemsUpdated.collect {
                loadPurchases()
            }
        }
    }

    private fun loadPurchases() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            val items = PendingItemsStore.readAll()
            val purchases = items
                .groupBy { it.purchaseId }
                .map { (purchaseId, purchaseItems) ->
                    val severity = when {
                        purchaseItems.any { it.errorText.contains("serverfel", ignoreCase = true) } ->
                            PurchaseSeverity.CRITICAL
                        purchaseItems.any { it.errorText.isNotBlank() } ->
                            PurchaseSeverity.WARNING
                        else ->
                            PurchaseSeverity.INFO
                    }
                    
                    PendingPurchaseUi(
                        purchaseId = purchaseId,
                        items = purchaseItems.map { item ->
                            PendingItemUi(
                                itemId = item.itemId,
                                sellerId = item.sellerId,
                                price = item.price,
                                errorText = item.errorText
                            )
                        },
                        severity = severity,
                        timestamp = purchaseItems.first().timestamp
                    )
                }
                .sortedBy { 
                    try {
                        java.time.Instant.parse(it.timestamp).toEpochMilli()
                    } catch (e: Exception) {
                        0L
                    }
                }
            
            _uiState.value = _uiState.value.copy(
                purchases = purchases,
                isLoading = false
            )
        }
    }

    fun toggleExpanded(purchaseId: String) {
        _uiState.value = _uiState.value.copy(
            expandedPurchaseId = if (_uiState.value.expandedPurchaseId == purchaseId) {
                null
            } else {
                purchaseId
            }
        )
    }

    fun changeSeller(purchaseId: String, itemId: String, newSeller: Int) {
        startProcessing(purchaseId) {
            PendingItemsStore.updateItems(purchaseId) { item ->
                if (item.itemId == itemId) {
                    item.copy(sellerId = newSeller, errorText = "")
                } else {
                    item
                }
            }
        }
    }

    fun deleteItem(purchaseId: String, itemId: String) {
        startProcessing(purchaseId) {
            PendingItemsStore.updateItems(purchaseId) { item ->
                if (item.itemId == itemId) null else item
            }
        }
    }

    fun deletePurchase(purchaseId: String) {
        startProcessing(purchaseId) {
            PendingItemsStore.deleteByPurchaseId(purchaseId)
        }
    }

    fun retryPurchase(purchaseId: String, apiKey: String, eventId: String, context: android.content.Context) {
        startProcessing(purchaseId) {
            // Trigger immediate sync via WorkManager
            se.iloppis.app.work.SyncScheduler.enqueueImmediate(context, apiKey, eventId)
            
            // Wait for sync to complete (max 5 seconds)
            var attempts = 0
            while (attempts < 50) {
                delay(100)
                
                // Check if purchase still exists
                val remainingItems = PendingItemsStore.readAll().filter { it.purchaseId == purchaseId }
                if (remainingItems.isEmpty()) {
                    // Success! All items uploaded
                    Log.d("PendingPurchasesVM", "Purchase $purchaseId successfully uploaded")
                    break
                }
                
                // Check if error messages have been updated (indicates sync ran)
                val hasUpdatedErrors = remainingItems.any { it.errorText.isNotBlank() }
                if (hasUpdatedErrors && attempts > 10) {
                    // Sync ran and updated errors, done waiting
                    Log.d("PendingPurchasesVM", "Purchase $purchaseId has updated error messages")
                    break
                }
                
                attempts++
            }
            
            // Reload purchases to show updated state
            loadPurchases()
        }
    }

    private fun startProcessing(purchaseId: String, action: suspend () -> Unit) {
        processingJob?.cancel()
        processingJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(processingPurchaseId = purchaseId)
            
            try {
                action()
                delay(500) // Brief delay for visual feedback
            } finally {
                _uiState.value = _uiState.value.copy(processingPurchaseId = null)
            }
        }
    }
}
