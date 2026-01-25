package se.iloppis.app.data.network

import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Test
import se.iloppis.app.network.getTodayTime
import java.time.LocalDate

class TodayTimeTest {
    @Test
    fun `Compares old time format to new time format`() = runTest {
        val check = "${LocalDate.now()}T00:00:00Z"
        val res = getTodayTime() /* System clock */
        Assert.assertEquals(
            "\n======== Today ( System clock ) ========\n" +
                    "\t$check != $res\n" +
                    "======== Today ( System clock ) ========\n\n",
            check,
            res
        )
    }
}
