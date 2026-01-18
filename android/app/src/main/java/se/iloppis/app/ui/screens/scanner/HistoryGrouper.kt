package se.iloppis.app.ui.screens.scanner

/**
 * Groups scan history by (email, ticketType).
 * ALL scans are grouped together, including errors/duplicates.
 * 
 * Example:
 * - goran@goencoder.se, heldag (5) - might be 3 success + 2 duplicates
 * - Offline scans (null, null) (3) - offline scans grouped together
 */
object HistoryGrouper {
    
    /**
     * Groups consecutive history items by (email, ticketType).
     * ALL scans with same email+ticketType are grouped, regardless of status.
     * 
     * @return List of HistoryItem.Group items
     */
    fun groupHistory(history: List<ScanResult>): List<HistoryItem> {
        if (history.isEmpty()) return emptyList()
        
        val groups = mutableListOf<HistoryItem>()
        var currentEmail = history.first().ticket?.email
        var currentTicketType = history.first().ticket?.ticketType
        var currentScans = mutableListOf<ScanResult>()
        
        history.forEach { scan ->
            val scanEmail = scan.ticket?.email
            val scanTicketType = scan.ticket?.ticketType
            
            // Check if we need to start a new group
            if (scanEmail != currentEmail || scanTicketType != currentTicketType) {
                // Save previous group
                if (currentScans.isNotEmpty()) {
                    groups.add(createGroup(currentEmail, currentTicketType, currentScans.toList()))
                }
                
                // Start new group
                currentEmail = scanEmail
                currentTicketType = scanTicketType
                currentScans = mutableListOf(scan)
            } else {
                // Add to current group
                currentScans.add(scan)
            }
        }
        
        // Don't forget last group
        if (currentScans.isNotEmpty()) {
            groups.add(createGroup(currentEmail, currentTicketType, currentScans.toList()))
        }
        
        return groups
    }
    
    private fun createGroup(
        email: String?,
        ticketType: String?,
        scans: List<ScanResult>
    ): HistoryItem.Group {
        val successCount = scans.count { 
            it.status == ScanStatus.SUCCESS || it.status == ScanStatus.OFFLINE_SUCCESS 
        }
        val errorCount = scans.size - successCount
        val hasErrors = errorCount > 0
        
        return HistoryItem.Group(
            email = email,
            ticketType = ticketType,
            scans = scans,
            count = scans.size,
            successCount = successCount,
            errorCount = errorCount,
            hasErrors = hasErrors,
            timestamp = scans.maxOf { it.timestamp } // Latest timestamp
        )
    }
}

/**
 * Represents a displayable group in history.
 * All items are groups (even single items are groups of size 1).
 */
sealed class HistoryItem {
    /**
     * Group of scans with same (email, ticketType).
     * Can contain mix of success and error scans.
     */
    data class Group(
        val email: String?,
        val ticketType: String?,
        val scans: List<ScanResult>,
        val count: Int,
        val successCount: Int,
        val errorCount: Int,
        val hasErrors: Boolean,
        val timestamp: java.time.Instant
    ) : HistoryItem()
}
