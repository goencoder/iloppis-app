package se.iloppis.app.ui.screens.cashier

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CashierPriceValidationTest {
    @Test
    fun `zero and positive whole-number prices are valid`() {
        assertEquals(0, parseCashierPrice("0"))
        assertEquals(150, parseCashierPrice("150"))
    }

    @Test
    fun `negative fractional and non-numeric prices are invalid`() {
        assertNull(parseCashierPrice("-1"))
        assertNull(parseCashierPrice("1.5"))
        assertNull(parseCashierPrice("abc"))
    }
}
