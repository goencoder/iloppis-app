package se.iloppis.app.data

import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import se.iloppis.app.data.models.StoredSoldItem
import se.iloppis.app.network.cashier.PaymentMethod
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/**
 * Unit tests for SoldItemFileStore.
 * Tests append, read, overwrite, thread-safety, and error handling.
 */
class SoldItemFileStoreTest {

    private lateinit var testDir: File
    private lateinit var testFile: File

    @Before
    fun setup() {
        // Create a temporary directory for testing
        testDir = File(System.getProperty("java.io.tmpdir"), "test_sold_items_${System.currentTimeMillis()}")
        testDir.mkdirs()

        // Initialize with a test context that points to our temp directory
        SoldItemFileStore.initializeForTesting(testDir)
        testFile = SoldItemFileStore.getFile()

        // Ensure clean state
        if (testFile.exists()) {
            testFile.delete()
        }
    }

    @After
    fun tearDown() {
        // Clean up test file and directory
        if (testFile.exists()) {
            testFile.delete()
        }
        testDir.deleteRecursively()
    }

    @Test
    fun testAppendAndRead_twoItems() {
        // Arrange
        val items = listOf(
            createTestItem("item1", seller = 1, price = 100),
            createTestItem("item2", seller = 2, price = 200)
        )

        // Act
        SoldItemFileStore.appendSoldItems(items)
        val result = SoldItemFileStore.getAllSoldItems()

        // Assert
        assertEquals(2, result.size)
        assertEquals("item1", result[0].itemId)
        assertEquals(1, result[0].seller)
        assertEquals(100, result[0].price)
        assertEquals("item2", result[1].itemId)
        assertEquals(2, result[1].seller)
        assertEquals(200, result[1].price)
    }

    @Test
    fun testAppendAndRead_appendMore() {
        // Arrange - First append 2 items
        val firstBatch = listOf(
            createTestItem("item1", seller = 1, price = 100),
            createTestItem("item2", seller = 2, price = 200)
        )
        SoldItemFileStore.appendSoldItems(firstBatch)

        // Act - Append 3 more items
        val secondBatch = listOf(
            createTestItem("item3", seller = 3, price = 300),
            createTestItem("item4", seller = 4, price = 400),
            createTestItem("item5", seller = 5, price = 500)
        )
        SoldItemFileStore.appendSoldItems(secondBatch)

        val result = SoldItemFileStore.getAllSoldItems()

        // Assert - Should have 5 items total
        assertEquals(5, result.size)
        assertEquals("item1", result[0].itemId)
        assertEquals("item5", result[4].itemId)
    }

    @Test
    fun testEmptyFile_returnsEmptyList() {
        // Act - File doesn't exist yet
        val result = SoldItemFileStore.getAllSoldItems()

        // Assert
        assertTrue(result.isEmpty())
    }

    @Test
    fun testOverwrite_saveSoldItems() {
        // Arrange - Append 5 items first
        val fiveItems = List(5) { index ->
            createTestItem("item$index", seller = index, price = index * 100)
        }
        SoldItemFileStore.appendSoldItems(fiveItems)

        // Verify we have 5 items
        assertEquals(5, SoldItemFileStore.getAllSoldItems().size)

        // Act - Overwrite with just 1 item
        val oneItem = listOf(createTestItem("singleItem", seller = 99, price = 999))
        SoldItemFileStore.saveSoldItems(oneItem)

        val result = SoldItemFileStore.getAllSoldItems()

        // Assert - Should have only 1 item now
        assertEquals(1, result.size)
        assertEquals("singleItem", result[0].itemId)
        assertEquals(99, result[0].seller)
        assertEquals(999, result[0].price)
    }

    @Test
    fun testThreadSafety_concurrentWrites() {
        // Arrange
        val threadCount = 10
        val itemsPerThread = 5
        val latch = CountDownLatch(threadCount)
        val threads = mutableListOf<Thread>()

        // Act - Launch 10 threads, each appending 5 items
        repeat(threadCount) { threadIndex ->
            val thread = thread {
                try {
                    val items = List(itemsPerThread) { itemIndex ->
                        createTestItem(
                            "thread${threadIndex}_item${itemIndex}",
                            seller = threadIndex * 100 + itemIndex,
                            price = (threadIndex * 100 + itemIndex) * 10
                        )
                    }
                    SoldItemFileStore.appendSoldItems(items)
                } finally {
                    latch.countDown()
                }
            }
            threads.add(thread)
        }

        // Wait for all threads to complete
        val completed = latch.await(30, TimeUnit.SECONDS)
        assertTrue("Threads did not complete in time", completed)

        threads.forEach { it.join(1000) }

        // Assert - Should have exactly 50 items (10 threads * 5 items)
        val result = SoldItemFileStore.getAllSoldItems()
        assertEquals(threadCount * itemsPerThread, result.size)

        // Verify no data corruption - all items should be unique
        val uniqueItemIds = result.map { it.itemId }.toSet()
        assertEquals(threadCount * itemsPerThread, uniqueItemIds.size)
    }

    @Test
    fun testJsonParsingError_returnsEmptyList() {
        // Arrange - Write invalid JSON to the file
        testFile.writeText("{ this is not valid json }")

        // Act
        val result = SoldItemFileStore.getAllSoldItems()

        // Assert - Should return empty list and not throw exception
        assertTrue(result.isEmpty())
    }

    @Test
    fun testJsonParsingError_emptyFile() {
        // Arrange - Create empty file
        testFile.writeText("")

        // Act
        val result = SoldItemFileStore.getAllSoldItems()

        // Assert
        assertTrue(result.isEmpty())
    }

    @Test
    fun testUploadedFlag_defaultValue() {
        // Arrange
        val item = createTestItem("item1", seller = 1, price = 100)

        // Act
        SoldItemFileStore.appendSoldItems(listOf(item))
        val result = SoldItemFileStore.getAllSoldItems()

        // Assert - uploaded should default to false
        assertEquals(1, result.size)
        assertFalse(result[0].uploaded)
    }

    @Test
    fun testUploadedFlag_setToTrue() {
        // Arrange
        val item = createTestItem("item1", seller = 1, price = 100, uploaded = true)

        // Act
        SoldItemFileStore.appendSoldItems(listOf(item))
        val result = SoldItemFileStore.getAllSoldItems()

        // Assert
        assertEquals(1, result.size)
        assertTrue(result[0].uploaded)
    }

    @Test
    fun testPaymentMethod_kontant() {
        // Arrange
        val item = createTestItem("item1", seller = 1, price = 100, paymentMethod = PaymentMethod.KONTANT)

        // Act
        SoldItemFileStore.appendSoldItems(listOf(item))
        val result = SoldItemFileStore.getAllSoldItems()

        // Assert
        assertEquals(PaymentMethod.KONTANT, result[0].paymentMethod)
    }

    @Test
    fun testPaymentMethod_swish() {
        // Arrange
        val item = createTestItem("item1", seller = 1, price = 100, paymentMethod = PaymentMethod.SWISH)

        // Act
        SoldItemFileStore.appendSoldItems(listOf(item))
        val result = SoldItemFileStore.getAllSoldItems()

        // Assert
        assertEquals(PaymentMethod.SWISH, result[0].paymentMethod)
    }

    @Test
    fun testAppendSoldItems_emptyList() {
        // Arrange - First add some items
        val item = createTestItem("item1", seller = 1, price = 100)
        SoldItemFileStore.appendSoldItems(listOf(item))
        assertEquals(1, SoldItemFileStore.getAllSoldItems().size)

        // Act - Append empty list (should be no-op)
        SoldItemFileStore.appendSoldItems(emptyList())

        // Assert - Should still have 1 item
        val result = SoldItemFileStore.getAllSoldItems()
        assertEquals(1, result.size)
        assertEquals("item1", result[0].itemId)
    }

    @Test
    fun testSaveSoldItems_emptyList() {
        // Arrange - First add some items
        val items = listOf(
            createTestItem("item1", seller = 1, price = 100),
            createTestItem("item2", seller = 2, price = 200)
        )
        SoldItemFileStore.appendSoldItems(items)
        assertEquals(2, SoldItemFileStore.getAllSoldItems().size)

        // Act - Save empty list (clear all items)
        SoldItemFileStore.saveSoldItems(emptyList())

        // Assert - Should have no items
        val result = SoldItemFileStore.getAllSoldItems()
        assertTrue(result.isEmpty())
    }

    @Test
    fun testDeduplication_sameItemId() {
        // Arrange - Create items with same itemId
        val item1 = createTestItem("duplicate-id", seller = 1, price = 100)
        val item2 = createTestItem("duplicate-id", seller = 2, price = 200)

        // Act - Append first item
        SoldItemFileStore.appendSoldItems(listOf(item1))
        assertEquals(1, SoldItemFileStore.getAllSoldItems().size)

        // Append second item with same itemId (should be deduplicated)
        SoldItemFileStore.appendSoldItems(listOf(item2))

        // Assert - Should still have only 1 item
        val result = SoldItemFileStore.getAllSoldItems()
        assertEquals(1, result.size)
        assertEquals("duplicate-id", result[0].itemId)
        // Should keep the original item
        assertEquals(1, result[0].seller)
        assertEquals(100, result[0].price)
    }

    @Test
    fun testDeduplication_multipleDuplicates() {
        // Arrange
        val originalItems = listOf(
            createTestItem("item1", seller = 1, price = 100),
            createTestItem("item2", seller = 2, price = 200),
            createTestItem("item3", seller = 3, price = 300)
        )
        SoldItemFileStore.appendSoldItems(originalItems)

        // Act - Try to append a mix of new and duplicate items
        val mixedItems = listOf(
            createTestItem("item2", seller = 22, price = 222), // duplicate
            createTestItem("item4", seller = 4, price = 400),  // new
            createTestItem("item1", seller = 11, price = 111), // duplicate
            createTestItem("item5", seller = 5, price = 500)   // new
        )
        SoldItemFileStore.appendSoldItems(mixedItems)

        // Assert - Should have 5 unique items (3 original + 2 new)
        val result = SoldItemFileStore.getAllSoldItems()
        assertEquals(5, result.size)

        // Verify originals are unchanged
        val item1 = result.find { it.itemId == "item1" }!!
        assertEquals(1, item1.seller) // Original value

        val item2 = result.find { it.itemId == "item2" }!!
        assertEquals(2, item2.seller) // Original value

        // Verify new items were added
        assertTrue(result.any { it.itemId == "item4" })
        assertTrue(result.any { it.itemId == "item5" })
    }

    // Helper function to create test items
    private fun createTestItem(
        itemId: String,
        eventId: String = "test-event-123",
        purchaseId: String = "purchase-ulid-123",
        seller: Int,
        price: Int,
        paymentMethod: PaymentMethod = PaymentMethod.KONTANT,
        soldTime: Long = System.currentTimeMillis(),
        uploaded: Boolean = false
    ): StoredSoldItem {
        return StoredSoldItem(
            itemId = itemId,
            eventId = eventId,
            purchaseId = purchaseId,
            seller = seller,
            price = price,
            paymentMethod = paymentMethod,
            soldTime = soldTime,
            uploaded = uploaded
        )
    }
}
