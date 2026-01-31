package se.iloppis.app.network

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import se.iloppis.app.network.config.ClientConfig
import se.iloppis.app.network.events.EventAPI
import se.iloppis.app.network.events.EventLifecycle

class iLoppisClientTest {
    val config: ClientConfig = ClientConfig("https://iloppis-staging.fly.dev/")



    @Test
    fun `Test API client Market and Events getter from ID list`() = runTest {

        // ==================================
        // This may use an outdated market
        // ==================================

        val client = iLoppisClient(config).create<EventAPI>()

        val market = "ed71222f-36ad-40a6-a102-2fb821bed1c0"
        val events = "66af78e4-be70-4145-be55-0c3cdfe6637c,66af78e4-be70-4145-be55-0c3cdfe6637d"

        val resMarket = client.getEventsFromMarkets(market)
        val resEvents = client.get(events)

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
            resEvents.total
        )
    }


    @Test
    fun `Tests API client with serialization of enum class`() = runTest {

        // ==================================
        // This may use an outdated market
        // ==================================

        val client = iLoppisClient(config).create<EventAPI>()
        val events = "66af78e4-be70-4145-be55-0c3cdfe6637c"
        val resEvents = client.get(events)

        println("Events: $resEvents\n")
        println(
            "Checks if states are parsed correctly\n" +
            "=====================================\n" +
            "Expected ${EventLifecycle.OPEN}\n" +
            "Result ${resEvents.events[0].lifecycleState}"
        )

        assertEquals(
            "\n========= Events =========\n" +
                    "\t resEvents.total != 1\n" +
                    "\n========= Events =========\n\n",
            EventLifecycle.OPEN,
            resEvents.events[0].lifecycleState
        )
    }
}
