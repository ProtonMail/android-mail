/*
 * Copyright (c) 2025 Proton Technologies AG
 * This file is part of Proton Technologies AG and Proton Mail.
 *
 * Proton Mail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Mail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Mail. If not, see <https://www.gnu.org/licenses/>.
 */

package ch.protonmail.android.mailupselling.presentation.model

import java.math.BigDecimal
import ch.protonmail.android.mailupselling.presentation.model.planupgrades.PlanUpgradePriceUiModel
import kotlin.test.Test
import kotlin.test.assertEquals

class PlanUpgradePriceUiModelTest {

    @Test
    fun `getShorthandFormat with currency shows symbol for whole numbers`() {
        val model = PlanUpgradePriceUiModel(BigDecimal(54.0), "USD")
        assertEquals("$54", model.getShorthandFormat())
    }

    @Test
    fun `getShorthandFormat with currency shows symbol for decimal numbers`() {
        val model = PlanUpgradePriceUiModel(BigDecimal(54.99), "USD")
        assertEquals("$54.99", model.getShorthandFormat())
    }

    @Test
    fun `getShorthandFormat with currency pads a single decimal to two`() {
        val model = PlanUpgradePriceUiModel(BigDecimal(54.1), "USD")
        assertEquals("$54.10", model.getShorthandFormat())
    }

    @Test
    fun `getShorthandFormat with unknown currency code falls back to code format`() {
        val model = PlanUpgradePriceUiModel(BigDecimal(42.0), "XYZ")
        assertEquals("XYZ 42", model.getShorthandFormat())
    }

    @Test
    fun `getShorthandFormat with unknown currency code shows decimals when needed`() {
        val model = PlanUpgradePriceUiModel(BigDecimal(42.75), "XYZ")
        assertEquals("XYZ 42.75", model.getShorthandFormat())
    }

    @Test
    fun `getShorthandFormat with currency that has same symbol as code uses code format`() {
        val model = PlanUpgradePriceUiModel(BigDecimal(1000.0), "JPY")
        assertEquals("¥1,000", model.getShorthandFormat())
    }

    @Test
    fun `getFullFormat with currency shows code format for whole numbers`() {
        val model = PlanUpgradePriceUiModel(BigDecimal(54.0), "USD")
        assertEquals("USD 54", model.getFullFormat())
    }

    @Test
    fun `getFullFormat with currency shows code format for decimal numbers`() {
        val model = PlanUpgradePriceUiModel(BigDecimal(54.99), "USD")
        assertEquals("USD 54.99", model.getFullFormat())
    }

    @Test
    fun `getFullFormat with unknown currency code works correctly`() {
        val model = PlanUpgradePriceUiModel(BigDecimal(42.75), "XYZ")
        assertEquals("XYZ 42.75", model.getFullFormat())
    }

    @Test
    fun `getFullFormatWithCents keeps two decimals for whole numbers`() {
        val model = PlanUpgradePriceUiModel(BigDecimal("5.00"), "USD")
        assertEquals("USD 5.00", model.getFullFormatWithCents())
    }

    @Test
    fun `getFullFormatWithCents keeps two decimals for decimal numbers`() {
        val model = PlanUpgradePriceUiModel(BigDecimal("6.49"), "USD")
        assertEquals("USD 6.49", model.getFullFormatWithCents())
    }

    @Test
    fun `formatting works correctly for zero amount`() {
        val model = PlanUpgradePriceUiModel(BigDecimal(0.0), "USD")
        assertEquals("$0", model.getShorthandFormat())
        assertEquals("USD 0", model.getFullFormat())
    }

    @Test
    fun `formatting works correctly for large whole numbers`() {
        val model = PlanUpgradePriceUiModel(BigDecimal(1000.0), "USD")
        assertEquals("$1,000", model.getShorthandFormat())
        assertEquals("USD 1,000", model.getFullFormat())
    }

    @Test
    fun `formatting works correctly for small decimal numbers`() {
        val model = PlanUpgradePriceUiModel(BigDecimal(0.99), "USD")
        assertEquals("$0.99", model.getShorthandFormat())
        assertEquals("USD 0.99", model.getFullFormat())
    }
}
