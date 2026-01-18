package se.iloppis.app.ui.screens.scanner

import se.iloppis.app.domain.model.VisitorTicket
import se.iloppis.app.domain.model.VisitorTicketStatus
import se.iloppis.app.ui.theme.AppColors
import androidx.compose.ui.graphics.Color

/**
 * Handles scan result classification and formatting.
 * Encapsulates logic for determining scan status and UI properties.
 */
sealed class ScanResultHandler {
    abstract val status: ScanStatus
    abstract val borderColor: Color
    abstract val showGroupCounter: Boolean
    
    data class Success(val ticket: VisitorTicket) : ScanResultHandler() {
        override val status = ScanStatus.SUCCESS
        override val borderColor = AppColors.Success
        override val showGroupCounter = true
    }
    
    data class OfflineSuccess(val ticket: VisitorTicket?) : ScanResultHandler() {
        override val status = ScanStatus.OFFLINE_SUCCESS
        override val borderColor = AppColors.Success
        override val showGroupCounter = true
    }
    
    data class Duplicate(val ticket: VisitorTicket?, val message: String) : ScanResultHandler() {
        override val status = ScanStatus.DUPLICATE
        override val borderColor = AppColors.Warning
        override val showGroupCounter = false
    }
    
    data class Invalid(val ticket: VisitorTicket?, val message: String) : ScanResultHandler() {
        override val status = ScanStatus.INVALID
        override val borderColor = AppColors.Error
        override val showGroupCounter = false
    }
    
    data class Error(val message: String) : ScanResultHandler() {
        override val status = ScanStatus.ERROR
        override val borderColor = AppColors.Error
        override val showGroupCounter = false
    }
    
    companion object {
        fun fromHttpCode(
            code: Int,
            message: String,
            ticket: VisitorTicket?
        ): ScanResultHandler {
            return when (code) {
                200 -> Success(ticket ?: throw IllegalStateException("Success requires ticket"))
                400, 412 -> {
                    if (ticket?.status == VisitorTicketStatus.SCANNED) {
                        Duplicate(ticket, message)
                    } else {
                        Invalid(ticket, message)
                    }
                }
                401, 403 -> Error(message)
                else -> Error(message)
            }
        }
        
        fun toScanResult(handler: ScanResultHandler): ScanResult {
            return when (handler) {
                is Success -> ScanResult(handler.ticket, handler.status)
                is OfflineSuccess -> ScanResult(handler.ticket, handler.status, offline = true)
                is Duplicate -> ScanResult(handler.ticket, handler.status, message = handler.message)
                is Invalid -> ScanResult(handler.ticket, handler.status, message = handler.message)
                is Error -> ScanResult(null, handler.status, message = handler.message)
            }
        }
    }
}
