package se.iloppis.app.data

import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * Tests for OfflineToastGatekeeper.
 */
class OfflineToastGatekeeperTest {

    private lateinit var gatekeeper: OfflineToastGatekeeper

    @Before
    fun setup() {
        gatekeeper = OfflineToastGatekeeper()
    }

    @After
    fun tearDown() = runTest {
        gatekeeper.reset()
    }

    @Test
    fun testFirstMissedUpload_noToast() = runTest {
        // Act
        val shouldShow = gatekeeper.recordMissedUpload("purchase-1")

        // Assert - First failure should not show toast
        assertFalse(shouldShow)
        assertEquals(1, gatekeeper.getFailureCount("purchase-1"))
    }

    @Test
    fun testSecondMissedUpload_showToast() = runTest {
        // Arrange - First failure
        val firstShow = gatekeeper.recordMissedUpload("purchase-1")
        assertFalse(firstShow)

        // Act - Second failure for same purchase
        val secondShow = gatekeeper.recordMissedUpload("purchase-1")

        // Assert - Should show toast on second failure
        assertTrue(secondShow)
        assertEquals(2, gatekeeper.getFailureCount("purchase-1"))
        assertTrue(gatekeeper.isToastSuppressed())
    }

    @Test
    fun testThirdMissedUpload_toastSuppressed() = runTest {
        // Arrange - Two failures, toast shown
        gatekeeper.recordMissedUpload("purchase-1")
        gatekeeper.recordMissedUpload("purchase-1")
        assertTrue(gatekeeper.isToastSuppressed())

        // Act - Third failure for same purchase
        val thirdShow = gatekeeper.recordMissedUpload("purchase-1")

        // Assert - Should not show toast again (suppressed)
        assertFalse(thirdShow)
    }

    @Test
    fun testDifferentPurchase_beforeSuppress_separateCount() = runTest {
        // Arrange - One failure for purchase-1
        gatekeeper.recordMissedUpload("purchase-1")

        // Act - One failure for purchase-2
        val showForPurchase2 = gatekeeper.recordMissedUpload("purchase-2")

        // Assert - Should not show toast (first failure for purchase-2)
        assertFalse(showForPurchase2)
        assertEquals(1, gatekeeper.getFailureCount("purchase-1"))
        assertEquals(1, gatekeeper.getFailureCount("purchase-2"))
    }

    @Test
    fun testDifferentPurchase_afterSuppress_noToast() = runTest {
        // Arrange - Toast shown and suppressed for purchase-1
        gatekeeper.recordMissedUpload("purchase-1")
        gatekeeper.recordMissedUpload("purchase-1")
        assertTrue(gatekeeper.isToastSuppressed())

        // Act - Failures for a different purchase while suppressed
        val showForPurchase2_1 = gatekeeper.recordMissedUpload("purchase-2")
        val showForPurchase2_2 = gatekeeper.recordMissedUpload("purchase-2")

        // Assert - Should not show toast (suppressed from previous purchase)
        assertFalse(showForPurchase2_1)
        assertFalse(showForPurchase2_2)
        assertTrue(gatekeeper.isToastSuppressed())
    }

    @Test
    fun testSuccessfulUpload_resetsState() = runTest {
        // Arrange - Toast shown and suppressed
        gatekeeper.recordMissedUpload("purchase-1")
        gatekeeper.recordMissedUpload("purchase-1")
        assertTrue(gatekeeper.isToastSuppressed())

        // Act - Successful upload (confirmed online)
        gatekeeper.recordSuccessfulUpload()

        // Assert - State should be reset
        assertFalse(gatekeeper.isToastSuppressed())
        assertEquals(0, gatekeeper.getFailureCount("purchase-1"))
    }

    @Test
    fun testSuccessfulUpload_allowsNewToast() = runTest {
        // Arrange - Toast shown, then success
        gatekeeper.recordMissedUpload("purchase-1")
        gatekeeper.recordMissedUpload("purchase-1")
        gatekeeper.recordSuccessfulUpload()

        // Act - New purchase fails twice
        val show1 = gatekeeper.recordMissedUpload("purchase-2")
        val show2 = gatekeeper.recordMissedUpload("purchase-2")

        // Assert - Toast should show again after reset
        assertFalse(show1) // First failure
        assertTrue(show2)  // Second failure shows toast
    }

    @Test
    fun testMultiplePurchases_twoFailuresEachBeforeSuppress() = runTest {
        // Arrange & Act
        val show1_1 = gatekeeper.recordMissedUpload("purchase-1")
        val show2_1 = gatekeeper.recordMissedUpload("purchase-2")
        val show3_1 = gatekeeper.recordMissedUpload("purchase-3")
        
        assertFalse(show1_1)
        assertFalse(show2_1)
        assertFalse(show3_1)

        // Second failure for purchase-2
        val show2_2 = gatekeeper.recordMissedUpload("purchase-2")
        
        // Assert - Toast should show on second failure for purchase-2
        assertTrue(show2_2)
        assertTrue(gatekeeper.isToastSuppressed())
        
        // Now even if purchase-1 or purchase-3 fail again, no toast
        val show1_2 = gatekeeper.recordMissedUpload("purchase-1")
        val show3_2 = gatekeeper.recordMissedUpload("purchase-3")
        assertFalse(show1_2)
        assertFalse(show3_2)
    }

    @Test
    fun testCompleteFlow_realistic() = runTest {
        // Scenario: Purchase A fails once, Purchase B fails twice (shows toast),
        // then success, then Purchase C fails twice (shows toast again)
        
        // Purchase A - first failure
        assertFalse(gatekeeper.recordMissedUpload("purchase-a"))
        
        // Purchase B - first failure
        assertFalse(gatekeeper.recordMissedUpload("purchase-b"))
        
        // Purchase B - second failure (toast shows)
        assertTrue(gatekeeper.recordMissedUpload("purchase-b"))
        assertTrue(gatekeeper.isToastSuppressed())
        
        // Purchase C - failures while suppressed (no toast)
        assertFalse(gatekeeper.recordMissedUpload("purchase-c"))
        assertFalse(gatekeeper.recordMissedUpload("purchase-c"))
        
        // Successful upload (confirmed online)
        gatekeeper.recordSuccessfulUpload()
        assertFalse(gatekeeper.isToastSuppressed())
        
        // Purchase D - first failure
        assertFalse(gatekeeper.recordMissedUpload("purchase-d"))
        
        // Purchase D - second failure (toast shows again)
        assertTrue(gatekeeper.recordMissedUpload("purchase-d"))
        assertTrue(gatekeeper.isToastSuppressed())
    }
}
