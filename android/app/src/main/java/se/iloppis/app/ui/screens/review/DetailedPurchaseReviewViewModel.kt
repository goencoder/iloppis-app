package se.iloppis.app.ui.screens.review

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import se.iloppis.app.R
import se.iloppis.app.data.RejectedPurchaseStore
import se.iloppis.app.data.BackgroundSyncManager
import se.iloppis.app.data.PendingItemsStore
import se.iloppis.app.data.VendorRepository
import se.iloppis.app.data.models.RejectedItemWithDetails
import se.iloppis.app.data.models.RejectedPurchase
import se.iloppis.app.data.models.SerializableSoldItemErrorCode
import se.iloppis.app.data.models.StoredSoldItem
import se.iloppis.app.network.cashier.CashierAPI
import se.iloppis.app.network.cashier.SoldItemObject
import se.iloppis.app.network.cashier.SoldItemsRequest
import se.iloppis.app.network.config.clientConfig
import se.iloppis.app.network.ILoppisClient
import se.iloppis.app.ui.util.UiText

private const val TAG = "DetailedPurchaseReviewVM"

/**
 * ViewModel for detailed purchase review screen.
 *
 * Handles:
 * - Loading a specific rejected purchase by ID
 * - Editing seller numbers on individual items
 * - Removing items from the purchase
 * - Deleting the entire purchase
 * - Retrying upload with modified data
 */
class DetailedPurchaseReviewViewModel(
    private val purchaseId: String,
    private val eventId: String,
    private val apiKey: String
) : ViewModel() {

    companion object {
        fun factory(purchaseId: String, eventId: String, apiKey: String) =
            object : androidx.lifecycle.ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    DetailedPurchaseReviewViewModel(purchaseId, eventId, apiKey) as T
            }
    }

    private val _uiState = MutableStateFlow(DetailedPurchaseUiState())
    val uiState: StateFlow<DetailedPurchaseUiState> = _uiState.asStateFlow()

    private val api: CashierAPI = ILoppisClient(clientConfig()).create()
    // Use global singleton VendorRepository (initialized by CashierViewModel)

    init {
        loadPurchase()
        loadValidSellers()
    }

    private fun loadPurchase() {
        viewModelScope.launch(Dispatchers.IO) {
            val purchase = RejectedPurchaseStore.getAllRejectedPurchases()
                .find { it.purchaseId == purchaseId }

            if (purchase == null) {
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(
                        error = UiText.StringResource(R.string.review_error_purchase_not_found),
                        purchaseNotFound = true
                    )
                }
                return@launch
            }

            withContext(Dispatchers.Main) {
                _uiState.value = _uiState.value.copy(
                    purchase = purchase,
                    items = purchase.items.toMutableList(),
                    isLoading = false
                )
            }
        }
    }

    private fun loadValidSellers() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val sellers = VendorRepository.getOrFetch()
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(validSellers = sellers)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load valid sellers", e)
            }
        }
    }

    fun onAction(action: DetailedPurchaseAction) {
        when (action) {
            is DetailedPurchaseAction.EditSeller -> editSellerNumber(action.itemIndex, action.newSeller)
            is DetailedPurchaseAction.RemoveItem -> removeItem(action.itemIndex)
            is DetailedPurchaseAction.DeletePurchase -> deletePurchase()
            is DetailedPurchaseAction.RetryUpload -> retryUpload()
            is DetailedPurchaseAction.DismissError -> dismissError()
            is DetailedPurchaseAction.OpenSellerEditor -> openSellerEditor(action.itemIndex)
            is DetailedPurchaseAction.CloseSellerEditor -> closeSellerEditor()
        }
    }

    private fun openSellerEditor(itemIndex: Int) {
        val item = _uiState.value.items.getOrNull(itemIndex) ?: return
        _uiState.value = _uiState.value.copy(
            showSellerEditor = true,
            editingItemIndex = itemIndex,
            editingSellerNumber = item.item.seller.toString()
        )
    }

    private fun closeSellerEditor() {
        _uiState.value = _uiState.value.copy(
            showSellerEditor = false,
            editingItemIndex = null,
            editingSellerNumber = ""
        )
    }

    private fun editSellerNumber(itemIndex: Int, newSellerStr: String) {
        val newSeller = newSellerStr.toIntOrNull()
        if (newSeller == null || newSeller <= 0) {
            _uiState.value = _uiState.value.copy(error = UiText.StringResource(R.string.review_error_invalid_seller))
            return
        }

        // Validate against valid sellers if available
        if (_uiState.value.validSellers.isNotEmpty() && newSeller !in _uiState.value.validSellers) {
            _uiState.value = _uiState.value.copy(error = UiText.StringResource(R.string.review_error_seller_not_approved, listOf(newSeller)))
            return
        }

        val updatedItems = _uiState.value.items.toMutableList()
        val oldItem = updatedItems.getOrNull(itemIndex) ?: return

        // Update the item with new seller number
        val updatedStoredItem = oldItem.item.copy(seller = newSeller)
        val updatedRejectedItem = oldItem.copy(
            item = updatedStoredItem,
            reason = "", // Clear error after manual edit
            errorCode = SerializableSoldItemErrorCode.UNSPECIFIED
        )

        updatedItems[itemIndex] = updatedRejectedItem
        _uiState.value = _uiState.value.copy(
            items = updatedItems,
            hasUnsavedChanges = true,
            showSellerEditor = false,
            editingItemIndex = null,
            editingSellerNumber = ""
        )
    }

    private fun removeItem(itemIndex: Int) {
        if (_uiState.value.items.size <= 1) {
            _uiState.value = _uiState.value.copy(error = UiText.StringResource(R.string.review_error_cannot_remove_last))
            return
        }

        val updatedItems = _uiState.value.items.toMutableList()
        updatedItems.removeAt(itemIndex)

        _uiState.value = _uiState.value.copy(
            items = updatedItems,
            hasUnsavedChanges = true
        )
    }

    private fun deletePurchase() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Remove from rejected purchases store
                RejectedPurchaseStore.removeRejectedPurchase(purchaseId)

                // Remove from PendingItemsStore (row deletion = uploaded)
                PendingItemsStore.deleteByPurchaseId(purchaseId)
                BackgroundSyncManager.refreshPendingCount()

                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(
                        purchaseDeleted = true,
                        successMessage = UiText.StringResource(R.string.review_success_deleted)
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete purchase", e)
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(error = UiText.StringResource(R.string.review_error_delete_failed, listOf(e.message ?: "")))
                }
            }
        }
    }

    private fun retryUpload() {
        if (_uiState.value.isUploading) return

        _uiState.value = _uiState.value.copy(isUploading = true, error = null)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Convert RejectedItemWithDetails back to SoldItemRequest
                val itemRequests = _uiState.value.items.map { rejectedItem ->
                    SoldItemObject(
                        itemId = rejectedItem.item.itemId,
                        purchaseId = "", // Let backend generate new ID
                        seller = rejectedItem.item.seller,
                        price = rejectedItem.item.price,
                        paymentMethod = rejectedItem.item.paymentMethod
                    )
                }

                val request = SoldItemsRequest(
                    items = itemRequests
                )

                val response = api.createSoldItems(
                    authorization = "Bearer $apiKey",
                    eventId = eventId,
                    request = request
                )

                if (response.acceptedItems?.isNotEmpty() == true) {
                    // Success! Remove from rejected store
                    RejectedPurchaseStore.removeRejectedPurchase(purchaseId)

                    // Remove from PendingItemsStore (row deletion = uploaded)
                    PendingItemsStore.deleteByPurchaseId(purchaseId)
                    BackgroundSyncManager.refreshPendingCount()

                    withContext(Dispatchers.Main) {
                        _uiState.value = _uiState.value.copy(
                            isUploading = false,
                            uploadSuccess = true,
                            successMessage = UiText.StringResource(R.string.review_success_uploaded)
                        )
                    }
                } else if (response.rejectedItems?.isNotEmpty() == true) {
                    // Still rejected - update with new error details
                    val newRejectedItems = response.rejectedItems.map { rejectedItem ->
                        RejectedItemWithDetails(
                            item = StoredSoldItem(
                                itemId = rejectedItem.item.itemId ?: "",
                                eventId = rejectedItem.item.eventId ?: eventId,
                                purchaseId = rejectedItem.item.purchaseId,
                                seller = rejectedItem.item.seller,
                                price = rejectedItem.item.price,
                                paymentMethod = rejectedItem.item.paymentMethod,
                                soldTime = System.currentTimeMillis(),
                                uploaded = false
                            ),
                            reason = rejectedItem.reason,
                            errorCode = SerializableSoldItemErrorCode.fromString(rejectedItem.errorCode)
                        )
                    }

                    // Update the rejected purchase with new error info
                    val firstError = response.rejectedItems.firstOrNull()
                    if (firstError != null) {
                        val updatedPurchase = RejectedPurchase(
                            purchaseId = firstError.item.purchaseId,
                            items = newRejectedItems,
                            errorCode = SerializableSoldItemErrorCode.fromString(firstError.errorCode),
                            errorMessage = firstError.reason,
                            timestamp = java.time.Instant.now().toString(),
                            retryAttempts = (_uiState.value.purchase?.retryAttempts ?: 0) + 1,
                            autoRecoveryAttempted = true,
                            needsManualReview = true
                        )

                        RejectedPurchaseStore.updateRejectedPurchase(updatedPurchase)

                        withContext(Dispatchers.Main) {
                            _uiState.value = _uiState.value.copy(
                                isUploading = false,
                                items = newRejectedItems.toMutableList(),
                                purchase = updatedPurchase,
                                error = UiText.StringResource(R.string.review_error_upload_failed, listOf(firstError.reason)),
                                hasUnsavedChanges = false
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to retry upload", e)
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(
                        isUploading = false,
                        error = UiText.StringResource(R.string.review_error_network, listOf(e.message ?: ""))
                    )
                }
            }
        }
    }

    private fun dismissError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class DetailedPurchaseUiState(
    val purchase: RejectedPurchase? = null,
    val items: MutableList<RejectedItemWithDetails> = mutableListOf(),
    val validSellers: Set<Int> = emptySet(),
    val isLoading: Boolean = true,
    val isUploading: Boolean = false,
    val hasUnsavedChanges: Boolean = false,
    val error: UiText? = null,
    val successMessage: UiText? = null,
    val purchaseNotFound: Boolean = false,
    val purchaseDeleted: Boolean = false,
    val uploadSuccess: Boolean = false,

    // Seller editor dialog state
    val showSellerEditor: Boolean = false,
    val editingItemIndex: Int? = null,
    val editingSellerNumber: String = ""
)

sealed class DetailedPurchaseAction {
    data class EditSeller(val itemIndex: Int, val newSeller: String) : DetailedPurchaseAction()
    data class RemoveItem(val itemIndex: Int) : DetailedPurchaseAction()
    data object DeletePurchase : DetailedPurchaseAction()
    data object RetryUpload : DetailedPurchaseAction()
    data object DismissError : DetailedPurchaseAction()
    data class OpenSellerEditor(val itemIndex: Int) : DetailedPurchaseAction()
    data object CloseSellerEditor : DetailedPurchaseAction()
}
