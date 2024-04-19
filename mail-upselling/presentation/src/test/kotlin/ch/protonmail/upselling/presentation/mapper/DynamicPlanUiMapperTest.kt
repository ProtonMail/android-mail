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
import ch.protonmail.android.mailupselling.domain.usecase.GetDiscountRate
import ch.protonmail.android.mailupselling.presentation.R
import ch.protonmail.android.mailupselling.presentation.mapper.DynamicPlanDescriptionUiMapper
import ch.protonmail.android.mailupselling.presentation.mapper.DynamicPlanEntitlementsUiMapper
import ch.protonmail.android.mailupselling.presentation.mapper.DynamicPlanInstanceUiMapper
import ch.protonmail.android.mailupselling.presentation.mapper.DynamicPlanTitleUiMapper
import ch.protonmail.android.mailupselling.presentation.mapper.DynamicPlanUiMapper
import ch.protonmail.android.mailupselling.presentation.model.DynamicEntitlementUiModel
import ch.protonmail.android.mailupselling.presentation.model.DynamicPlanDescriptionUiModel
import ch.protonmail.android.mailupselling.presentation.model.DynamicPlanInstanceUiModel
import ch.protonmail.android.mailupselling.presentation.model.DynamicPlanTitleUiModel
import ch.protonmail.android.mailupselling.presentation.model.DynamicPlansUiModel
import ch.protonmail.android.mailupselling.presentation.model.UserIdUiModel
import ch.protonmail.android.testdata.upselling.UpsellingTestData
import ch.protonmail.android.testdata.upselling.UpsellingTestData.DynamicPlanPlusWithIdenticalInstances
import ch.protonmail.android.testdata.upselling.UpsellingTestData.DynamicPlanPlusWithNoInstances
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import me.proton.core.domain.entity.UserId
import me.proton.core.plan.domain.entity.DynamicPlan
import me.proton.core.plan.domain.entity.DynamicPlanInstance
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

internal class DynamicPlanUiMapperTest {

    private val titleUiMapper = mockk<DynamicPlanTitleUiMapper>()
    private val descriptionUiMapper = mockk<DynamicPlanDescriptionUiMapper>()
    private val planInstanceUiMapper = mockk<DynamicPlanInstanceUiMapper>()
    private val entitlementsUiMapper = mockk<DynamicPlanEntitlementsUiMapper>()
    private val getDiscountRate = mockk<GetDiscountRate>()
    private val mapper = DynamicPlanUiMapper(
        titleUiMapper,
        descriptionUiMapper,
        planInstanceUiMapper,
        entitlementsUiMapper,
        getDiscountRate
    )

    @AfterTest
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `should return a plan with no options if the instances are not present`() {
        // Given
        expectTitleUiModel()
        expectDescriptionUiModel()
        expectEntitlementsUiModel()

        val plan = DynamicPlanPlusWithNoInstances
        val expected = DynamicPlansUiModel(
            title = ExpectedTitleUiModel,
            description = ExpectedDescriptionUiModel,
            entitlements = listOf(ExpectedEntitlementsUiModel),
            plans = emptyList()
        )

        // When
        val actual = mapper.toUiModel(UserId, plan)

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `should return a plan with only one option if the instances are the same`() {
        // Given
        expectTitleUiModel()
        expectDescriptionUiModel()
        expectEntitlementsUiModel()

        val plan = DynamicPlanPlusWithIdenticalInstances
        val instance = requireNotNull(DynamicPlanPlusWithIdenticalInstances.instances[1])

        val expectedInstance = DynamicPlanInstanceUiModel(
            userId = UserIdUiModel(UserId),
            name = UpsellingTestData.PlusPlan.title,
            price = TextUiModel.Text("0.1"),
            discount = null,
            currency = "EUR",
            cycle = 1,
            highlighted = false,
            viewId = "$UserId$instance".hashCode(),
            dynamicPlan = plan
        )

        expectInstanceUiModel(
            userId = UserId,
            instance = instance,
            highlighted = false,
            discount = null,
            plan = plan,
            expectedInstance = expectedInstance
        )

        val expectedPlansUiModel = DynamicPlansUiModel(
            title = ExpectedTitleUiModel,
            description = ExpectedDescriptionUiModel,
            entitlements = listOf(ExpectedEntitlementsUiModel),
            plans = listOf(expectedInstance)
        )

        // When
        val actual = mapper.toUiModel(UserId, plan)

        // Then
        assertEquals(expectedPlansUiModel, actual)
    }

    @Test
    fun `should return a plan with two options if the instances are different`() {
        // Given
        expectTitleUiModel()
        expectDescriptionUiModel()
        expectEntitlementsUiModel()

        val plan = UpsellingTestData.PlusPlan
        val shorterInstance = requireNotNull(plan.instances[1])
        val longerInstance = requireNotNull(plan.instances[12])
        expectDiscountRate(shorterInstance, longerInstance)

        val expectedShorterInstance = DynamicPlanInstanceUiModel(
            userId = UserIdUiModel(UserId),
            name = plan.title,
            price = TextUiModel.Text("0.1"),
            discount = null,
            currency = "EUR",
            cycle = 1,
            highlighted = false,
            viewId = "$UserId$shorterInstance".hashCode(),
            dynamicPlan = plan
        )
        val expectedLongerInstance = DynamicPlanInstanceUiModel(
            userId = UserIdUiModel(UserId),
            name = plan.title,
            price = TextUiModel.Text("0.09"),
            discount = TextUiModel.TextResWithArgs(R.string.upselling_discount_tag, listOf("10")),
            currency = "EUR",
            cycle = 12,
            highlighted = true,
            viewId = "$UserId$longerInstance".hashCode(),
            dynamicPlan = plan
        )

        expectInstanceUiModel(
            userId = UserId,
            instance = shorterInstance,
            highlighted = false,
            discount = null,
            plan = plan,
            expectedInstance = expectedShorterInstance
        )

        expectInstanceUiModel(
            userId = UserId,
            instance = longerInstance,
            highlighted = true,
            discount = 10,
            plan = plan,
            expectedInstance = expectedLongerInstance
        )

        val expectedPlansUiModel = DynamicPlansUiModel(
            title = ExpectedTitleUiModel,
            description = ExpectedDescriptionUiModel,
            entitlements = listOf(ExpectedEntitlementsUiModel),
            plans = listOf(expectedShorterInstance, expectedLongerInstance)
        )

        // When
        val actual = mapper.toUiModel(UserId, plan)

        // Then
        assertEquals(expectedPlansUiModel, actual)
    }

    private fun expectTitleUiModel() {
        every { titleUiMapper.toUiModel(any()) } returns ExpectedTitleUiModel
    }

    private fun expectDescriptionUiModel() {
        every { descriptionUiMapper.toUiModel(any()) } returns ExpectedDescriptionUiModel
    }

    private fun expectEntitlementsUiModel() {
        every { entitlementsUiMapper.toUiModel(any()) } returns listOf(ExpectedEntitlementsUiModel)
    }

    @Suppress("LongParameterList")
    private fun expectInstanceUiModel(
        userId: UserId,
        instance: DynamicPlanInstance,
        highlighted: Boolean,
        discount: Int?,
        plan: DynamicPlan,
        expectedInstance: DynamicPlanInstanceUiModel
    ) {
        every { planInstanceUiMapper.toUiModel(userId, instance, highlighted, discount, plan) } returns expectedInstance
    }

    private fun expectDiscountRate(shorterInstance: DynamicPlanInstance, longerInstance: DynamicPlanInstance) {
        every { getDiscountRate(shorterInstance, longerInstance) } returns 10
    }

    private companion object {

        val ExpectedDescriptionUiModel = DynamicPlanDescriptionUiModel(TextUiModel.Text("description"))
        val ExpectedEntitlementsUiModel = DynamicEntitlementUiModel.Default(TextUiModel.Text("item"), "")
        val ExpectedTitleUiModel = DynamicPlanTitleUiModel(TextUiModel.Text("title"))

        val UserId = UserId("id")
    }
}
