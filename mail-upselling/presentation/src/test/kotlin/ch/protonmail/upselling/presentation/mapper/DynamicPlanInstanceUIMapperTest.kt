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

package ch.protonmail.upselling.presentation.mapper

import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailupselling.presentation.mapper.DynamicPlanInstanceUiMapper
import ch.protonmail.android.mailupselling.presentation.model.UserIdUiModel
import ch.protonmail.android.mailupselling.presentation.model.dynamicplans.DynamicPlanCycle
import ch.protonmail.android.mailupselling.presentation.model.dynamicplans.DynamicPlanInstanceUiModel
import ch.protonmail.android.mailupselling.presentation.usecase.GetDiscountRate
import ch.protonmail.android.testdata.upselling.UpsellingTestData
import me.proton.core.domain.entity.UserId
import kotlin.test.Test
import kotlin.test.assertEquals

internal class DynamicPlanInstanceUIMapperTest {

    private val getDiscountRate = GetDiscountRate()
    private val mapper = DynamicPlanInstanceUiMapper(getDiscountRate)

    @Test
    fun `should map standard instances correctly`() {
        // Given
        val plan = UpsellingTestData.PlusPlan
        val monthlyPlan = UpsellingTestData.MonthlyDynamicPlanInstance
        val yearlyPlan = UpsellingTestData.YearlyDynamicPlanInstance

        val monthlyExpected = DynamicPlanInstanceUiModel.Standard(
            userId = UserIdUiModel(UserId),
            name = UpsellingTestData.PlusPlan.title,
            pricePerCycle = TextUiModel.Text("0.1"),
            totalPrice = TextUiModel.Text("0.1"),
            discountRate = null,
            currency = "EUR",
            cycle = DynamicPlanCycle.Monthly,
            viewId = "$UserId$monthlyPlan".hashCode(),
            dynamicPlan = plan
        )

        val yearlyExpected = DynamicPlanInstanceUiModel.Standard(
            userId = UserIdUiModel(UserId),
            name = UpsellingTestData.PlusPlan.title,
            pricePerCycle = TextUiModel.Text("0.09"),
            totalPrice = TextUiModel.Text("1.08"),
            discountRate = 10,
            currency = "EUR",
            cycle = DynamicPlanCycle.Yearly,
            viewId = "$UserId$yearlyPlan".hashCode(),
            dynamicPlan = plan
        )

        // When
        val actual = mapper.toUiModel(
            plan = plan,
            userId = UserId,
            monthlyPlanInstance = monthlyPlan,
            yearlyPlanInstance = yearlyPlan
        )
        // Then
        assertEquals(actual, Pair(monthlyExpected, yearlyExpected))
    }

    @Test
    fun `should map promo instances correctly (monthly promo)`() {
        // Given
        val plan = UpsellingTestData.PlusMonthlyPromoPlan
        val monthlyPlan = UpsellingTestData.MonthlyDynamicPromoPlanInstance
        val yearlyPlan = UpsellingTestData.YearlyDynamicPlanInstance

        val monthlyExpected = DynamicPlanInstanceUiModel.Promotional(
            userId = UserIdUiModel(UserId),
            name = UpsellingTestData.PlusPlan.title,
            pricePerCycle = TextUiModel.Text("0.1"),
            promotionalPrice = TextUiModel.Text("0.1"),
            renewalPrice = TextUiModel.Text("0.4"),
            discountRate = 75,
            currency = "EUR",
            cycle = DynamicPlanCycle.Monthly,
            viewId = "$UserId$monthlyPlan".hashCode(),
            dynamicPlan = plan
        )

        val yearlyExpected = DynamicPlanInstanceUiModel.Standard(
            userId = UserIdUiModel(UserId),
            name = UpsellingTestData.PlusPlan.title,
            pricePerCycle = TextUiModel.Text("0.09"),
            totalPrice = TextUiModel.Text("1.08"),
            discountRate = 10,
            currency = "EUR",
            cycle = DynamicPlanCycle.Yearly,
            viewId = "$UserId$yearlyPlan".hashCode(),
            dynamicPlan = plan
        )

        // When
        val actual = mapper.toUiModel(
            plan = plan,
            userId = UserId,
            monthlyPlanInstance = monthlyPlan,
            yearlyPlanInstance = yearlyPlan
        )

        // Then
        assertEquals(actual, Pair(monthlyExpected, yearlyExpected))
    }

    @Test
    fun `should map promo instances correctly (yearly promo)`() {
        // Given
        val plan = UpsellingTestData.PlusYearlyPromoPlan
        val monthlyPlan = UpsellingTestData.MonthlyDynamicPlanInstance
        val yearlyPlan = UpsellingTestData.YearlyDynamicPromoPlanInstance

        val monthlyExpected = DynamicPlanInstanceUiModel.Standard(
            userId = UserIdUiModel(UserId),
            name = UpsellingTestData.PlusPlan.title,
            pricePerCycle = TextUiModel.Text("0.1"),
            totalPrice = TextUiModel.Text("0.1"),
            discountRate = null,
            currency = "EUR",
            cycle = DynamicPlanCycle.Monthly,
            viewId = "$UserId$monthlyPlan".hashCode(),
            dynamicPlan = plan
        )

        val yearlyExpected = DynamicPlanInstanceUiModel.Promotional(
            userId = UserIdUiModel(UserId),
            name = UpsellingTestData.PlusPlan.title,
            pricePerCycle = TextUiModel.Text("0.09"),
            promotionalPrice = TextUiModel.Text("1.08"),
            renewalPrice = TextUiModel.Text("1.2"),
            discountRate = 10,
            currency = "EUR",
            cycle = DynamicPlanCycle.Yearly,
            viewId = "$UserId$yearlyPlan".hashCode(),
            dynamicPlan = plan
        )

        // When
        val actual = mapper.toUiModel(
            plan = plan,
            userId = UserId,
            monthlyPlanInstance = monthlyPlan,
            yearlyPlanInstance = yearlyPlan
        )

        // Then
        assertEquals(actual, Pair(monthlyExpected, yearlyExpected))
    }

    @Test
    fun `should return the expected standard instance ui model with no discount`() {
        // Given
        val plan = UpsellingTestData.PlusPlan
        val planInstance = plan.instances[1]!!
        val expected = DynamicPlanInstanceUiModel.Standard(
            userId = UserIdUiModel(UserId),
            name = UpsellingTestData.PlusPlan.title,
            pricePerCycle = TextUiModel.Text("0.1"),
            totalPrice = TextUiModel.Text("0.1"),
            discountRate = null,
            currency = "EUR",
            cycle = DynamicPlanCycle.Monthly,
            viewId = "$UserId$planInstance".hashCode(),
            dynamicPlan = plan
        )

        // When
        val actual = mapper.createPlanUiModel(
            userId = UserId,
            planInstance = planInstance,
            cycle = DynamicPlanCycle.Monthly,
            dynamicPlan = plan
        )

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `should return the expected standard instance ui model with discount`() {
        // Given
        val plan = UpsellingTestData.PlusPlan
        val planInstance = plan.instances[12]!!
        val comparisonPlanInstance = plan.instances[1]!!
        val expected = DynamicPlanInstanceUiModel.Standard(
            userId = UserIdUiModel(UserId),
            name = UpsellingTestData.PlusPlan.title,
            pricePerCycle = TextUiModel.Text("0.09"),
            totalPrice = TextUiModel.Text("1.08"),
            discountRate = 10,
            currency = "EUR",
            cycle = DynamicPlanCycle.Yearly,
            viewId = "$UserId$planInstance".hashCode(),
            dynamicPlan = plan
        )

        // When
        val actual = mapper.createPlanUiModel(
            userId = UserId,
            planInstance = planInstance,
            cycle = DynamicPlanCycle.Yearly,
            dynamicPlan = plan,
            comparisonPriceInstance = comparisonPlanInstance
        )

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `should return the expected monthly promotional instance ui model with discount`() {
        // Given
        val plan = UpsellingTestData.PlusMonthlyPromoPlan
        val planInstance = plan.instances[1]!!
        val expected = DynamicPlanInstanceUiModel.Promotional(
            userId = UserIdUiModel(UserId),
            name = UpsellingTestData.PlusPlan.title,
            pricePerCycle = TextUiModel.Text("0.1"),
            promotionalPrice = TextUiModel.Text("0.1"),
            renewalPrice = TextUiModel.Text("0.4"),
            discountRate = 75,
            currency = "EUR",
            cycle = DynamicPlanCycle.Monthly,
            viewId = "$UserId$planInstance".hashCode(),
            dynamicPlan = plan
        )

        // When
        val actual = mapper.createPlanUiModel(
            userId = UserId,
            planInstance = planInstance,
            cycle = DynamicPlanCycle.Monthly,
            dynamicPlan = plan
        )

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `should return the expected yearly promotional instance ui model with discount`() {
        // Given
        val plan = UpsellingTestData.PlusYearlyPromoPlan
        val planInstance = plan.instances[12]!!
        val monthlyInstance = plan.instances[1]!!
        val expected = DynamicPlanInstanceUiModel.Promotional(
            userId = UserIdUiModel(UserId),
            name = UpsellingTestData.PlusPlan.title,
            pricePerCycle = TextUiModel.Text("0.09"),
            promotionalPrice = TextUiModel.Text("1.08"),
            renewalPrice = TextUiModel.Text("1.2"),
            discountRate = 10,
            currency = "EUR",
            cycle = DynamicPlanCycle.Yearly,
            viewId = "$UserId$planInstance".hashCode(),
            dynamicPlan = plan
        )

        // When
        val actual = mapper.createPlanUiModel(
            userId = UserId,
            planInstance = planInstance,
            cycle = DynamicPlanCycle.Yearly,
            dynamicPlan = plan,
            comparisonPriceInstance = monthlyInstance
        )

        // Then
        assertEquals(expected, actual)
    }

    private companion object {

        val UserId = UserId("id")
    }
}
