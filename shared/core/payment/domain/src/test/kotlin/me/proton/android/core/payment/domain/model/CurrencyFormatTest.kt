package me.proton.android.core.payment.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class CurrencyFormatTest {

    @Test
    fun `replaceBareDollarSign keeps the dollar sign for USD`() {
        assertEquals("$12.99", replaceBareDollarSign("$12.99", "USD"))
        assertEquals("$12.99", replaceBareDollarSign("$12.99", "usd"))
    }

    @Test
    fun `replaceBareDollarSign shows the code for another currency`() {
        assertEquals("NZD 12.99", replaceBareDollarSign("$12.99", "NZD"))
    }

    @Test
    fun `replaceBareDollarSign keeps the store number formatting`() {
        assertEquals("COP 45.900", replaceBareDollarSign("$ 45.900", "COP"))
        assertEquals("12,99\u00A0CAD", replaceBareDollarSign("12,99\u00A0$", "CAD"))
    }

    @Test
    fun `replaceBareDollarSign keeps a dollar sign that has a prefix`() {
        assertEquals("NZ$12.99", replaceBareDollarSign("NZ$12.99", "NZD"))
        assertEquals("R$ 12,99", replaceBareDollarSign("R$ 12,99", "BRL"))
    }

    @Test
    fun `replaceBareDollarSign keeps other symbols`() {
        assertEquals("€12.99", replaceBareDollarSign("€12.99", "EUR"))
    }
}
