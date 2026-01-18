package se.iloppis.app.ui.screens.scanner

/**
 * Manages group scanning logic with composite key (email, ticketType).
 * Encapsulates grouping rules and state transitions.
 */
class GroupScanManager {
    private var currentEmail: String? = null
    private var currentTicketType: String? = null
    private val currentScans = mutableListOf<ScanResult>()
    
    val groupCount: Int get() = currentScans.size
    val groupEmail: String? get() = currentEmail
    val groupTicketType: String? get() = currentTicketType
    val scans: List<ScanResult> get() = currentScans.toList()
    
    /**
     * Checks if new scan requires committing current group.
     * Returns true if group should be committed before adding new scan.
     */
    fun shouldCommit(newEmail: String?, newTicketType: String?): Boolean {
        if (currentScans.isEmpty()) return false
        if (currentEmail == null) return false
        
        val emailChanged = newEmail != null && newEmail != currentEmail
        val typeChanged = newTicketType != null && newTicketType != currentTicketType
        
        return emailChanged || typeChanged
    }
    
    /**
     * Checks if ticket is duplicate within current group.
     */
    fun isDuplicate(ticketId: String): Boolean {
        return currentScans.any { it.ticket?.id == ticketId }
    }
    
    /**
     * Adds scan to current group, initializing group keys if needed.
     */
    fun addScan(result: ScanResult, email: String?, ticketType: String?) {
        if (currentScans.isEmpty()) {
            currentEmail = email
            currentTicketType = ticketType
        }
        currentScans.add(result)
    }
    
    /**
     * Removes scan from current group.
     */
    fun removeScan(ticketId: String) {
        currentScans.removeAll { it.ticket?.id == ticketId }
        if (currentScans.isEmpty()) {
            clear()
        }
    }
    
    /**
     * Commits current group and returns scans for persistence.
     * Clears internal state after commit.
     */
    fun commit(): List<ScanResult> {
        val committed = currentScans.toList()
        clear()
        return committed
    }
    
    /**
     * Clears all group state.
     */
    fun clear() {
        currentEmail = null
        currentTicketType = null
        currentScans.clear()
    }
}
