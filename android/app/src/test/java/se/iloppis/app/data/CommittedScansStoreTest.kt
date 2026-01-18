package se.iloppis.app.data

import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import se.iloppis.app.data.models.CommittedScan
import java.io.File

/**
 * Unit tests for CommittedScansStore.
 * Tests append, hasTicket, and getRecentScans operations.
 */
class CommittedScansStoreTest {

    private lateinit var testDir: File

    @Before
    fun setup() {
        // Create a temporary directory for testing
        testDir = File(System.getProperty("java.io.tmpdir"), "test_committed_scans_${System.currentTimeMillis()}")
        testDir.mkdirs()

        // Initialize with test directory
        CommittedScansStore.initializeForTesting(testDir)
    }

    @After
    fun tearDown() {
        // Clean up test directory
        testDir.deleteRecursively()
    }

    @Test
    fun `appendScan adds single scan to file`() = runTest {
        // Arrange
        val scan = CommittedScan(
            scanId = "scan-1",
            ticketId = "ticket-123",
            eventId = "event-1",
            scannedAt = "2024-01-01T10:00:00Z",
            committedAt = "2024-01-01T10:00:01Z",
            wasOffline = false
        )

        // Act
        CommittedScansStore.appendScan(scan)
        val result = CommittedScansStore.getRecentScans()

        // Assert
        assertEquals(1, result.size)
        assertEquals("scan-1", result[0].scanId)
        assertEquals("ticket-123", result[0].ticketId)
        assertFalse(result[0].wasOffline)
    }

    @Test
    fun `hasTicket returns true when ticket exists`() = runTest {
        // Arrange
        CommittedScansStore.appendScan(
            CommittedScan("scan-1", "ticket-123", "event-1", "2024-01-01T10:00:00Z", "2024-01-01T10:00:01Z", false)
        )

        // Act
        val exists = CommittedScansStore.hasTicket("ticket-123")

        // Assert
        assertTrue(exists)
    }

    @Test
    fun `hasTicket returns false when ticket does not exist`() = runTest {
        // Arrange
        CommittedScansStore.appendScan(
            CommittedScan("scan-1", "ticket-123", "event-1", "2024-01-01T10:00:00Z", "2024-01-01T10:00:01Z", false)
        )

        // Act
        val exists = CommittedScansStore.hasTicket("ticket-999")

        // Assert
        assertFalse(exists)
    }

    @Test
    fun `hasTicket returns false when file is empty`() = runTest {
        // Act
        val exists = CommittedScansStore.hasTicket("ticket-123")

        // Assert
        assertFalse(exists)
    }

    @Test
    fun `getRecentScans returns empty list when file does not exist`() = runTest {
        // Act
        val result = CommittedScansStore.getRecentScans()

        // Assert
        assertEquals(0, result.size)
    }

    @Test
    fun `getRecentScans returns scans in reverse order (most recent first)`() = runTest {
        // Arrange
        CommittedScansStore.appendScan(
            CommittedScan("scan-1", "ticket-1", "event-1", "2024-01-01T10:00:00Z", "2024-01-01T10:00:01Z", false)
        )
        CommittedScansStore.appendScan(
            CommittedScan("scan-2", "ticket-2", "event-1", "2024-01-01T10:01:00Z", "2024-01-01T10:01:01Z", false)
        )
        CommittedScansStore.appendScan(
            CommittedScan("scan-3", "ticket-3", "event-1", "2024-01-01T10:02:00Z", "2024-01-01T10:02:01Z", false)
        )

        // Act
        val result = CommittedScansStore.getRecentScans()

        // Assert - Most recent first
        assertEquals(3, result.size)
        assertEquals("scan-3", result[0].scanId)
        assertEquals("scan-2", result[1].scanId)
        assertEquals("scan-1", result[2].scanId)
    }

    @Test
    fun `getRecentScans respects limit parameter`() = runTest {
        // Arrange - Add 5 scans
        for (i in 1..5) {
            CommittedScansStore.appendScan(
                CommittedScan("scan-$i", "ticket-$i", "event-1", "2024-01-01T10:0$i:00Z", "2024-01-01T10:0$i:01Z", false)
            )
        }

        // Act
        val result = CommittedScansStore.getRecentScans(limit = 3)

        // Assert - Only 3 most recent
        assertEquals(3, result.size)
        assertEquals("scan-5", result[0].scanId)
        assertEquals("scan-4", result[1].scanId)
        assertEquals("scan-3", result[2].scanId)
    }

    @Test
    fun `appendScan handles offline scans`() = runTest {
        // Arrange
        val scan = CommittedScan(
            scanId = "scan-1",
            ticketId = "ticket-123",
            eventId = "event-1",
            scannedAt = "2024-01-01T10:00:00Z",
            committedAt = "2024-01-01T10:00:01Z",
            wasOffline = true
        )

        // Act
        CommittedScansStore.appendScan(scan)
        val result = CommittedScansStore.getRecentScans()

        // Assert
        assertEquals(1, result.size)
        assertTrue(result[0].wasOffline)
    }

    @Test
    fun `hasTicket can detect duplicate offline scans`() = runTest {
        // Arrange - Offline scan
        CommittedScansStore.appendScan(
            CommittedScan("scan-1", "ticket-123", "event-1", "2024-01-01T10:00:00Z", "2024-01-01T10:00:01Z", true)
        )

        // Act - Check for duplicate before second scan
        val hasDuplicate = CommittedScansStore.hasTicket("ticket-123")

        // Assert
        assertTrue(hasDuplicate)
    }

    @Test
    fun `multiple scans of same ticket are all stored`() = runTest {
        // Arrange - This tests history, not duplicate prevention
        CommittedScansStore.appendScan(
            CommittedScan("scan-1", "ticket-123", "event-1", "2024-01-01T10:00:00Z", "2024-01-01T10:00:01Z", false)
        )
        CommittedScansStore.appendScan(
            CommittedScan("scan-2", "ticket-123", "event-1", "2024-01-01T10:01:00Z", "2024-01-01T10:01:01Z", false)
        )

        // Act
        val result = CommittedScansStore.getRecentScans()

        // Assert - Both stored (duplicate prevention happens in ViewModel)
        assertEquals(2, result.size)
        assertEquals("ticket-123", result[0].ticketId)
        assertEquals("ticket-123", result[1].ticketId)
    }
}
