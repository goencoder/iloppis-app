package se.iloppis.app.network

import org.junit.Assert.assertEquals
import org.junit.Test
import se.iloppis.app.network.events.EventLifecycle
import se.iloppis.app.network.events.ApiEventListResponse
import com.google.gson.Gson

class iLoppisClientTest {
    @Test
    fun `parses event list response with total`() {
        val json = """
            {
              "events": [
                {
                  "id": "evt-123",
                  "marketId": "market-1",
                  "name": "Testevent",
                  "lifecycleState": "OPEN"
                }
              ],
              "total": 1
            }
        """.trimIndent()

        val response = Gson().fromJson(json, ApiEventListResponse::class.java)

        assertEquals(1, response.total)
        assertEquals("evt-123", response.events.first().id)
    }


    @Test
    fun `parses event lifecycle enum`() {
        val json = """
            {
              "events": [
                {
                  "id": "evt-456",
                  "marketId": "market-2",
                  "name": "Enumtest",
                  "lifecycleState": "OPEN"
                }
              ],
              "total": 1
            }
        """.trimIndent()

        val response = Gson().fromJson(json, ApiEventListResponse::class.java)

        assertEquals(EventLifecycle.OPEN, response.events.first().lifecycleState)
    }
}
