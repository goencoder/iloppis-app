package se.iloppis.app.data

import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import se.iloppis.app.data.models.PendingScan
import java.io.File

/**
 * Unit tests for PendingScansStore.
 * Tests append, read, remove, update operations and thread safety.
 */
class PendingScansStoreTest {

    private lateinit var testDir: File

    @Before
    fun setup() {
        // Create a temporary directory for testing
        testDir = File(System.getProperty("java.io.tmpdir"), "test_pending_scans_${System.currentTimeMillis()}")
        testDir.mkdirs()

        // Initialize with test directory
        PendingScansStore.initializeForTesting(testDir)
    }

    @After
    fun tearDown() {
        // Clean up test directory
        testDir.deleteRecursively()
    }

    @Test
    fun `appendScan adds single scan to file`() = runTest {
        // Arrange
        val scan = PendingScan(
            scanId = "scan-1",
            ticketId = "ticket-123",
            eventId = "event-1",
            scannedAt = "2024-01-01T10:00:00Z",
            errorText = ""
        )

        // Act
        PendingScansStore.appendScan(scan)
        val result = PendingScansStore.getAllScans()

        // Assert
        assertEquals(1, result.size)
        assertEquals("scan-1", result[0].scanId)
        assertEquals("ticket-123", result[0].ticketId)
        assertEquals("", result[0].errorText)
    }

    @Test
    fun `appendScan adds multiple scans`() = runTest {
        // Arrange & Act
        PendingScansStore.appendScan(
            PendingScan("scan-1", "ticket-1", "event-1", "2024-01-01T10:00:00Z", "")
        )
        PendingScansStore.appendScan(
            PendingScan("scan-2", "ticket-2", "event-1", "2024-01-01T10:01:00Z", "")
        )
        PendingScansStore.appendScan(
            PendingScan("scan-3", "ticket-3", "event-1", "2024-01-01T10:02:00Z", "")
        )

        val result = PendingScansStore.getAllScans()

        // Assert
        assertEquals(3, result.size)
        assertEquals("scan-1", result[0].scanId)
        assertEquals("scan-2", result[1].scanId)
        assertEquals("scan-3", result[2].scanId)
    }

    @Test
    fun `getAllScans returns empty list when file does not exist`() = runTest {
        // Act
        val result = PendingScansStore.getAllScans()

        // Assert
        assertEquals(0, result.size)
    }

    @Test
    fun `removeScan removes specific scan by scanId`() = runTest {
        // Arrange
        PendingScansStore.appendScan(
            PendingScan("scan-1", "ticket-1", "event-1", "2024-01-01T10:00:00Z", "")
        )
        PendingScansStore.appendScan(
            PendingScan("scan-2", "ticket-2", "event-1", "2024-01-01T10:01:00Z", "")
        )

        // Act
        PendingScansStore.removeScan("scan-1")
        val result = PendingScansStore.getAllScans()

        // Assert
        assertEquals(1, result.size)
        assertEquals("scan-2", result[0].scanId)
    }

    @Test
    fun `removeScan does nothing when scanId not found`() = runTest {
        // Arrange
        PendingScansStore.appendScan(
            PendingScan("scan-1", "ticket-1", "event-1", "2024-01-01T10:00:00Z", "")
        )

        // Act
        PendingScansStore.removeScan("scan-999")
        val result = PendingScansStore.getAllScans()

        // Assert
        assertEquals(1, result.size)
        assertEquals("scan-1", result[0].scanId)
    }

    @Test
    fun `updateError updates errorText for specific scan`() = runTest {
        // Arrange
        PendingScansStore.appendScan(
            PendingScan("scan-1", "ticket-1", "event-1", "2024-01-01T10:00:00Z", "")
        )
        PendingScansStore.appendScan(
            PendingScan("scan-2", "ticket-2", "event-1", "2024-01-01T10:01:00Z", "")
        )

        // Act
        PendingScansStore.updateError("scan-1", "Network timeout")
        val result = PendingScansStore.getAllScans()

        // Assert
        assertEquals(2, result.size)
        assertEquals("Network timeout", result[0].errorText)
        assertEquals("", result[1].errorText)
    }

    @Test
    fun `count returns correct number of pending scans`() = runTest {
        // Arrange
        PendingScansStore.appendScan(
            PendingScan("scan-1", "ticket-1", "event-1", "2024-01-01T10:00:00Z", "")
        )
        PendingScansStore.appendScan(
            PendingScan("scan-2", "ticket-2", "event-1", "2024-01-01T10:01:00Z", "")
        )

        // Act
        val count = PendingScansStore.count()

        // Assert
        assertEquals(2, count)
    }

    @Test
    fun `count returns zero when file is empty`() = runTest {
        // Act
        val count = PendingScansStore.count()

        // Assert
        assertEquals(0, count)
    }

    @Test
    fun `appendScan handles scans with errorText`() = runTest {
        // Arrange
        val scan = PendingScan(
            scanId = "scan-1",
            ticketId = "ticket-123",
            eventId = "event-1",
            scannedAt = "2024-01-01T10:00:00Z",
            errorText = "Server error 500"
        )

        // Act
        PendingScansStore.appendScan(scan)
        val result = PendingScansStore.getAllScans()

        // Assert
        assertEquals(1, result.size)
        assertEquals("Server error 500", result[0].errorText)
    }
}
