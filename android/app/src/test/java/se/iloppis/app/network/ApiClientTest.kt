package se.iloppis.app.data.network

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import se.iloppis.app.network.ApiClient
import se.iloppis.app.network.EventApi

class ApiClientTest {
    @Test
    fun `Test API client Market and Events getter from ID list`() = runTest {

        // ==================================
        // This may use an outdated market
        // ==================================

        val client = ApiClient.create<EventApi>()

        val market = "ed71222f-36ad-40a6-a102-2fb821bed1c0"
        val events = "66af78e4-be70-4145-be55-0c3cdfe6637c,66af78e4-be70-4145-be55-0c3cdfe6637d"

        val resMarket = client.getMarketsByIds(market)
        val resEvents = client.getEventsByIds(events)

        println("\nMarkets: $resMarket")
        println("Events: $resEvents\n")

        assertEquals(
            "\n========= Market =========\n" +
                "\t resMarket.total != 1\n" +
                "\n========= Market =========\n\n",
            1,
            resMarket.total
        )
        assertEquals(
            "\n========= Events =========\n" +
                    "\t resEvents.total != 1\n" +
                    "\n========= Events =========\n\n",
            1,
            resMarket.total
        )
    }
}
