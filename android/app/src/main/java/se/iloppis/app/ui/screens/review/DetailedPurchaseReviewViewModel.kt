package se.iloppis.app.ui.screens.review

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import se.iloppis.app.data.PurchaseRecoveryManager
import se.iloppis.app.data.RejectedPurchaseStore
import se.iloppis.app.data.SoldItemFileStore
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

    var uiState by mutableStateOf(DetailedPurchaseUiState())
        private set

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
                    uiState = uiState.copy(
                        error = "Köpet hittades inte",
                        purchaseNotFound = true
                    )
                }
                return@launch
            }

            withContext(Dispatchers.Main) {
                uiState = uiState.copy(
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
                    uiState = uiState.copy(validSellers = sellers)
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
        val item = uiState.items.getOrNull(itemIndex) ?: return
        uiState = uiState.copy(
            showSellerEditor = true,
            editingItemIndex = itemIndex,
            editingSellerNumber = item.item.seller.toString()
        )
    }

    private fun closeSellerEditor() {
        uiState = uiState.copy(
            showSellerEditor = false,
            editingItemIndex = null,
            editingSellerNumber = ""
        )
    }

    private fun editSellerNumber(itemIndex: Int, newSellerStr: String) {
        val newSeller = newSellerStr.toIntOrNull()
        if (newSeller == null || newSeller <= 0) {
            uiState = uiState.copy(error = "Ogiltigt säljarnummer")
            return
        }

        // Validate against valid sellers if available
        if (uiState.validSellers.isNotEmpty() && newSeller !in uiState.validSellers) {
            uiState = uiState.copy(error = "Säljare $newSeller är inte godkänd för detta event")
            return
        }

        val updatedItems = uiState.items.toMutableList()
        val oldItem = updatedItems.getOrNull(itemIndex) ?: return

        // Update the item with new seller number
        val updatedStoredItem = oldItem.item.copy(seller = newSeller)
        val updatedRejectedItem = oldItem.copy(
            item = updatedStoredItem,
            reason = "", // Clear error after manual edit
            errorCode = SerializableSoldItemErrorCode.UNSPECIFIED
        )

        updatedItems[itemIndex] = updatedRejectedItem
        uiState = uiState.copy(
            items = updatedItems,
            hasUnsavedChanges = true,
            showSellerEditor = false,
            editingItemIndex = null,
            editingSellerNumber = ""
        )
    }

    private fun removeItem(itemIndex: Int) {
        if (uiState.items.size <= 1) {
            uiState = uiState.copy(error = "Kan inte ta bort sista artikeln. Radera hela köpet istället.")
            return
        }

        val updatedItems = uiState.items.toMutableList()
        updatedItems.removeAt(itemIndex)

        uiState = uiState.copy(
            items = updatedItems,
            hasUnsavedChanges = true
        )
    }

    private fun deletePurchase() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Remove from rejected purchases store
                RejectedPurchaseStore.removeRejectedPurchase(purchaseId)

                // Also mark as handled in SoldItemFileStore
                val allItems = SoldItemFileStore.getAllSoldItems()
                val itemsInThisPurchase = allItems.filter { it.purchaseId == purchaseId }
                val updatedItems = allItems.map { item ->
                    if (item.purchaseId == purchaseId) {
                        item.copy(uploaded = true)
                    } else {
                        item
                    }
                }
                SoldItemFileStore.saveSoldItems(updatedItems)

                withContext(Dispatchers.Main) {
                    uiState = uiState.copy(
                        purchaseDeleted = true,
                        successMessage = "Köpet har raderats"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete purchase", e)
                withContext(Dispatchers.Main) {
                    uiState = uiState.copy(error = "Kunde inte radera köpet: ${e.message}")
                }
            }
        }
    }

    private fun retryUpload() {
        if (uiState.isUploading) return

        uiState = uiState.copy(isUploading = true, error = null)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Convert RejectedItemWithDetails back to SoldItemRequest
                val itemRequests = uiState.items.map { rejectedItem ->
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

                    // Mark original items as uploaded in SoldItemFileStore
                    val allItems = SoldItemFileStore.getAllSoldItems()
                    val updatedItems = allItems.map { item ->
                        if (item.purchaseId == purchaseId) {
                            item.copy(uploaded = true)
                        } else {
                            item
                        }
                    }
                    SoldItemFileStore.saveSoldItems(updatedItems)

                    withContext(Dispatchers.Main) {
                        uiState = uiState.copy(
                            isUploading = false,
                            uploadSuccess = true,
                            successMessage = "Köpet har laddats upp!"
                        )
                    }
                } else if (response.rejectedItems?.isNotEmpty() == true) {
                    // Still rejected - update with new error details
                    val recoveryManager = PurchaseRecoveryManager(eventId, apiKey)
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
                            retryAttempts = (uiState.purchase?.retryAttempts ?: 0) + 1,
                            autoRecoveryAttempted = true,
                            needsManualReview = true
                        )

                        RejectedPurchaseStore.updateRejectedPurchase(updatedPurchase)

                        withContext(Dispatchers.Main) {
                            uiState = uiState.copy(
                                isUploading = false,
                                items = newRejectedItems.toMutableList(),
                                purchase = updatedPurchase,
                                error = "Uppladdning misslyckades: ${firstError.reason}",
                                hasUnsavedChanges = false
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to retry upload", e)
                withContext(Dispatchers.Main) {
                    uiState = uiState.copy(
                        isUploading = false,
                        error = "Nätverksfel: ${e.message}"
                    )
                }
            }
        }
    }

    private fun dismissError() {
        uiState = uiState.copy(error = null)
    }
}

data class DetailedPurchaseUiState(
    val purchase: RejectedPurchase? = null,
    val items: MutableList<RejectedItemWithDetails> = mutableListOf(),
    val validSellers: Set<Int> = emptySet(),
    val isLoading: Boolean = true,
    val isUploading: Boolean = false,
    val hasUnsavedChanges: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
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
