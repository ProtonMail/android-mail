/*
 * Copyright (c) 2022 Proton Technologies AG
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

package ch.protonmail.upselling.presentation.usecase

import ch.protonmail.android.mailupselling.presentation.usecase.GetDiscountRate
import ch.protonmail.android.testdata.upselling.UpsellingTestData
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class GetDiscountRateTest {

    private val getDiscountRate = GetDiscountRate()

    @Test
    fun `should return the expected discount rate (2 instances)`() {
        // Given
        val expected = 10

        // When
        val actual =
            getDiscountRate(UpsellingTestData.MonthlyDynamicPlanInstance, UpsellingTestData.YearlyDynamicPlanInstance)

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `should return null if the discount rate is 0 (2 instances)`() {
        // When
        val actual =
            getDiscountRate(UpsellingTestData.MonthlyDynamicPlanInstance, UpsellingTestData.MonthlyDynamicPlanInstance)

        // Then
        assertNull(actual)
    }

    @Test
    fun `should return null if the yearly price is higher than the monthly price (2 instances)`() {
        // When
        val actual =
            getDiscountRate(UpsellingTestData.YearlyDynamicPlanInstance, UpsellingTestData.MonthlyDynamicPlanInstance)

        // Then
        assertNull(actual)
    }

    @Test
    fun `should return the expected discount rate (promo prices)`() {
        // Given
        val expected = 90

        // When
        val actual = getDiscountRate(promotionalPrice = 10f, renewalPrice = 100f)

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `should return null if the discount rate is 0 (promo prices)`() {
        // When
        val actual = getDiscountRate(promotionalPrice = 100f, renewalPrice = 100f)

        // Then
        assertNull(actual)
    }

    @Test
    fun `should return null if promotional price is higher than renewal price (promo prices)`() {
        // When
        val actual = getDiscountRate(promotionalPrice = 500f, renewalPrice = 1f)

        // Then
        assertNull(actual)
    }

    @Test
    fun `should return null if the discount rate would exceed 100 percent`() {
        // When
        val actual = getDiscountRate(promotionalPrice = -1f, renewalPrice = 1f)

        // Then
        assertNull(actual)
    }
}
