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
import ch.protonmail.android.mailupselling.domain.model.UpsellingEntryPoint
import ch.protonmail.android.mailupselling.presentation.R
import ch.protonmail.android.mailupselling.presentation.mapper.DynamicPlanDescriptionUiMapper
import ch.protonmail.android.mailupselling.presentation.mapper.DynamicPlanEntitlementsUiMapper
import ch.protonmail.android.mailupselling.presentation.mapper.DynamicPlanIconUiMapper
import ch.protonmail.android.mailupselling.presentation.mapper.DynamicPlanInstanceUiMapper
import ch.protonmail.android.mailupselling.presentation.mapper.DynamicPlanTitleUiMapper
import ch.protonmail.android.mailupselling.presentation.mapper.DynamicPlanUiMapper
import ch.protonmail.android.mailupselling.presentation.model.UserIdUiModel
import ch.protonmail.android.mailupselling.presentation.model.dynamicplans.DynamicPlanCycle
import ch.protonmail.android.mailupselling.presentation.model.dynamicplans.DynamicPlanDescriptionUiModel
import ch.protonmail.android.mailupselling.presentation.model.dynamicplans.DynamicPlanIconUiModel
import ch.protonmail.android.mailupselling.presentation.model.dynamicplans.DynamicPlanInstanceListUiModel
import ch.protonmail.android.mailupselling.presentation.model.dynamicplans.DynamicPlanInstanceUiModel
import ch.protonmail.android.mailupselling.presentation.model.dynamicplans.DynamicPlanTitleUiModel
import ch.protonmail.android.mailupselling.presentation.model.dynamicplans.DynamicPlansUiModel
import ch.protonmail.android.mailupselling.presentation.model.dynamicplans.DynamicPlansVariant
import ch.protonmail.android.mailupselling.presentation.model.dynamicplans.PlanEntitlementListUiModel
import ch.protonmail.android.mailupselling.presentation.model.dynamicplans.PlanEntitlementsUiModel
import ch.protonmail.android.testdata.upselling.UpsellingTestData
import ch.protonmail.android.testdata.upselling.UpsellingTestData.DynamicPlanPlusWithIdenticalInstances
import ch.protonmail.android.testdata.upselling.UpsellingTestData.DynamicPlanPlusWithNoInstances
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import me.proton.core.domain.entity.UserId
import me.proton.core.plan.domain.entity.DynamicPlan
import me.proton.core.plan.domain.entity.DynamicPlanInstance
import javax.inject.Provider
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

internal class DynamicPlanUiMapperTest {

    private val iconUiMapper = mockk<DynamicPlanIconUiMapper>()
    private val titleUiMapper = mockk<DynamicPlanTitleUiMapper>()
    private val descriptionUiMapper = mockk<DynamicPlanDescriptionUiMapper>()
    private val planInstanceUiMapper = mockk<DynamicPlanInstanceUiMapper>()
    private val entitlementsUiMapper = mockk<DynamicPlanEntitlementsUiMapper>()
    private val providerHeaderUpsellVariantLayoutEnabled = mockk<Provider<Boolean>>()
    private val providerSocialProofLayoutEnabled = mockk<Provider<Boolean>>()

    private val mapper by lazy {
        DynamicPlanUiMapper(
            iconUiMapper,
            titleUiMapper,
            descriptionUiMapper,
            planInstanceUiMapper,
            entitlementsUiMapper,
            providerHeaderUpsellVariantLayoutEnabled.get(),
            providerSocialProofLayoutEnabled.get()
        )
    }

    @BeforeTest
    fun setUp() {
        MockKAnnotations.init(this)
        headerUpsellVariantLayoutEnabled(false)
        headerUpsellSocialProofEnabled(false)
    }

    @AfterTest
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `should return a plan with no options if the instances are not present`() {
        // Given
        expectIconUiModel()
        expectTitleUiModel()
        expectDescriptionUiModel()
        expectEntitlementsUiModel()

        val plan = DynamicPlanPlusWithNoInstances
        val expected = DynamicPlansUiModel(
            icon = ExpectedIconUiModel,
            title = ExpectedTitleUiModel,
            description = ExpectedDescriptionUiModel,
            entitlements = PlanEntitlementsUiModel.SimpleList(listOf(ExpectedEntitlementsUiModel)),
            list = DynamicPlanInstanceListUiModel.Empty,
            variant = DynamicPlansVariant.Normal
        )

        // When
        val actual = mapper.toUiModel(UserId, plan, UpsellingEntryPoint.Feature.Mailbox)

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `should return an invalid list if the instances are the same`() {
        // Given
        expectIconUiModel()
        expectTitleUiModel()
        expectDescriptionUiModel()
        expectEntitlementsUiModel()

        val plan = DynamicPlanPlusWithIdenticalInstances
        val instance = requireNotNull(DynamicPlanPlusWithIdenticalInstances.instances[1])

        val expectedInstance = DynamicPlanInstanceUiModel.Standard(
            userId = UserIdUiModel(UserId),
            name = UpsellingTestData.PlusPlan.title,
            pricePerCycle = TextUiModel.Text("0.1"),
            totalPrice = TextUiModel.Text("0.1"),
            currency = "EUR",
            cycle = DynamicPlanCycle.Monthly,
            viewId = "$UserId$instance".hashCode(),
            dynamicPlan = plan,
            discountRate = null
        )

        expectInstanceUiModel(
            userId = UserId,
            monthlyInstance = instance,
            yearlyInstance = instance,
            plan = plan,
            expectedInstance = Pair(expectedInstance, expectedInstance)
        )

        val expectedPlansUiModel = DynamicPlansUiModel(
            icon = ExpectedIconUiModel,
            title = ExpectedTitleUiModel,
            description = ExpectedDescriptionUiModel,
            entitlements = PlanEntitlementsUiModel.SimpleList(listOf(ExpectedEntitlementsUiModel)),
            list = DynamicPlanInstanceListUiModel.Invalid,
            variant = DynamicPlansVariant.Normal
        )

        // When
        val actual = mapper.toUiModel(UserId, plan, UpsellingEntryPoint.Feature.Mailbox)

        // Then
        assertEquals(expectedPlansUiModel, actual)
    }

    @Test
    fun `should return a plan with two options if the instances are different`() {
        // Given
        expectIconUiModel()
        expectTitleUiModel()
        expectDescriptionUiModel()
        expectEntitlementsUiModel()

        val plan = UpsellingTestData.PlusPlan
        val shorterInstance = requireNotNull(plan.instances[1])
        val longerInstance = requireNotNull(plan.instances[12])

        val expectedShorterInstance = DynamicPlanInstanceUiModel.Standard(
            userId = UserIdUiModel(UserId),
            name = plan.title,
            pricePerCycle = TextUiModel.Text("0.1"),
            totalPrice = TextUiModel.Text("0.1"),
            currency = "EUR",
            cycle = DynamicPlanCycle.Monthly,
            viewId = "$UserId$shorterInstance".hashCode(),
            dynamicPlan = plan,
            discountRate = null
        )
        val expectedLongerInstance = DynamicPlanInstanceUiModel.Standard(
            userId = UserIdUiModel(UserId),
            name = plan.title,
            pricePerCycle = TextUiModel.Text("0.09"),
            totalPrice = TextUiModel.Text("1.08"),
            currency = "EUR",
            cycle = DynamicPlanCycle.Yearly,
            viewId = "$UserId$longerInstance".hashCode(),
            dynamicPlan = plan,
            discountRate = 10
        )

        expectInstanceUiModel(
            userId = UserId,
            monthlyInstance = shorterInstance,
            yearlyInstance = longerInstance,
            plan = plan,
            expectedInstance = Pair(expectedShorterInstance, expectedLongerInstance)
        )

        val expectedPlansUiModel = DynamicPlansUiModel(
            icon = ExpectedIconUiModel,
            title = ExpectedTitleUiModel,
            description = ExpectedDescriptionUiModel,
            entitlements = PlanEntitlementsUiModel.SimpleList(listOf(ExpectedEntitlementsUiModel)),
            list = DynamicPlanInstanceListUiModel.Data.Standard(expectedShorterInstance, expectedLongerInstance),
            variant = DynamicPlansVariant.Normal
        )

        // When
        val actual = mapper.toUiModel(UserId, plan, UpsellingEntryPoint.Feature.Mailbox)

        // Then
        assertEquals(expectedPlansUiModel, actual)
    }

    @Test
    fun `should return a plan with promo if the variant FF is on`() {
        // Given
        expectIconUiModel()
        expectTitleUiModelPromo()
        expectDescriptionUiModelPromo()
        expectEntitlementsUiModel()
        headerUpsellVariantLayoutEnabled(true)

        val plan = UpsellingTestData.PlusMonthlyPromoPlan
        val shorterInstance = requireNotNull(plan.instances[1])
        val longerInstance = requireNotNull(plan.instances[12])

        val monthlyPlan = UpsellingTestData.MonthlyDynamicPromoPlanInstance
        val yearlyPlan = UpsellingTestData.YearlyDynamicPlanInstance

        val expectedShorterInstance = DynamicPlanInstanceUiModel.Promotional(
            userId = UserIdUiModel(UserId),
            name = UpsellingTestData.PlusPlan.title,
            pricePerCycle = TextUiModel.Text("0.1"),
            promotionalPrice = TextUiModel.Text("0.1"),
            renewalPrice = TextUiModel.Text("0.4"),
            discountRate = 75,
            currency = "EUR",
            cycle = DynamicPlanCycle.Monthly,
            viewId = "${UserId}$monthlyPlan".hashCode(),
            dynamicPlan = plan
        )
        val expectedLongerInstance = DynamicPlanInstanceUiModel.Standard(
            userId = UserIdUiModel(UserId),
            name = UpsellingTestData.PlusPlan.title,
            pricePerCycle = TextUiModel.Text("0.09"),
            totalPrice = TextUiModel.Text("1.08"),
            discountRate = 10,
            currency = "EUR",
            cycle = DynamicPlanCycle.Yearly,
            viewId = "${UserId}$yearlyPlan".hashCode(),
            dynamicPlan = plan
        )

        expectInstanceUiModel(
            userId = UserId,
            monthlyInstance = shorterInstance,
            yearlyInstance = longerInstance,
            plan = plan,
            expectedInstance = Pair(expectedShorterInstance, expectedLongerInstance)
        )

        val expectedPriceFormatted = TextUiModel.TextResWithArgs(
            R.string.upselling_get_button_promotional,
            listOf("EUR", "0.1")
        )
        val expectedPlansUiModel = DynamicPlansUiModel(
            icon = ExpectedIconUiModel,
            title = ExpectedTitleUiModelPromo,
            description = ExpectedDescriptionUiModelPromo,
            entitlements = PlanEntitlementsUiModel.SimpleList(listOf(ExpectedEntitlementsUiModel)),
            list = DynamicPlanInstanceListUiModel.Data.PromotionalVariantB(
                main = expectedShorterInstance,
                priceFormatted = expectedPriceFormatted
            ),
            variant = DynamicPlansVariant.PromoB
        )

        // When
        val actual = mapper.toUiModel(UserId, plan, UpsellingEntryPoint.Feature.MailboxPromo)

        // Then
        assertEquals(expectedPlansUiModel, actual)
    }

    @Test
    fun `should return a plan with default variant for non-header entry points`() {
        // Given
        expectIconUiModel()
        expectTitleUiModel()
        expectDescriptionUiModel()
        expectEntitlementsUiModel()
        headerUpsellVariantLayoutEnabled(false)
        headerUpsellSocialProofEnabled(true)

        val plan = UpsellingTestData.PlusPlan
        val shorterInstance = requireNotNull(plan.instances[1])
        val longerInstance = requireNotNull(plan.instances[12])

        val monthlyPlan = UpsellingTestData.MonthlyDynamicPlanInstance
        val yearlyPlan = UpsellingTestData.YearlyDynamicPlanInstance

        val expectedShorterInstance = DynamicPlanInstanceUiModel.Standard(
            userId = UserIdUiModel(UserId),
            name = UpsellingTestData.PlusPlan.title,
            pricePerCycle = TextUiModel.Text("0.1"),
            totalPrice = TextUiModel.Text("10.08"),
            discountRate = 75,
            currency = "EUR",
            cycle = DynamicPlanCycle.Monthly,
            viewId = "${UserId}$monthlyPlan".hashCode(),
            dynamicPlan = plan
        )
        val expectedLongerInstance = DynamicPlanInstanceUiModel.Standard(
            userId = UserIdUiModel(UserId),
            name = UpsellingTestData.PlusPlan.title,
            pricePerCycle = TextUiModel.Text("0.09"),
            totalPrice = TextUiModel.Text("1.08"),
            discountRate = 10,
            currency = "EUR",
            cycle = DynamicPlanCycle.Yearly,
            viewId = "${UserId}$yearlyPlan".hashCode(),
            dynamicPlan = plan
        )

        expectInstanceUiModel(
            userId = UserId,
            monthlyInstance = shorterInstance,
            yearlyInstance = longerInstance,
            plan = plan,
            expectedInstance = Pair(expectedShorterInstance, expectedLongerInstance)
        )

        val expectedPlansUiModel = DynamicPlansUiModel(
            icon = ExpectedIconUiModel,
            title = ExpectedTitleUiModel,
            description = ExpectedDescriptionUiModel,
            entitlements = PlanEntitlementsUiModel.SimpleList(listOf(ExpectedEntitlementsUiModel)),
            list = DynamicPlanInstanceListUiModel.Data.Standard(
                shorterCycle = expectedShorterInstance,
                longerCycle = expectedLongerInstance
            ),
            variant = DynamicPlansVariant.Normal
        )

        // When
        val actual = mapper.toUiModel(UserId, plan, UpsellingEntryPoint.Feature.Folders)

        // Then
        assertEquals(expectedPlansUiModel, actual)
    }

    @Test
    fun `should return a plan with social proof if the variant FF is on`() {
        // Given
        expectIconUiModelSocialProof()
        expectTitleUiModel()
        expectDescriptionUiModel()
        expectEntitlementsUiModelSocialProof()
        headerUpsellVariantLayoutEnabled(false)
        headerUpsellSocialProofEnabled(true)

        val plan = UpsellingTestData.PlusPlan
        val shorterInstance = requireNotNull(plan.instances[1])
        val longerInstance = requireNotNull(plan.instances[12])

        val monthlyPlan = UpsellingTestData.MonthlyDynamicPlanInstance
        val yearlyPlan = UpsellingTestData.YearlyDynamicPlanInstance

        val expectedShorterInstance = DynamicPlanInstanceUiModel.Standard(
            userId = UserIdUiModel(UserId),
            name = UpsellingTestData.PlusPlan.title,
            pricePerCycle = TextUiModel.Text("0.1"),
            totalPrice = TextUiModel.Text("10.08"),
            discountRate = 75,
            currency = "EUR",
            cycle = DynamicPlanCycle.Monthly,
            viewId = "${UserId}$monthlyPlan".hashCode(),
            dynamicPlan = plan
        )
        val expectedLongerInstance = DynamicPlanInstanceUiModel.Standard(
            userId = UserIdUiModel(UserId),
            name = UpsellingTestData.PlusPlan.title,
            pricePerCycle = TextUiModel.Text("0.09"),
            totalPrice = TextUiModel.Text("1.08"),
            discountRate = 10,
            currency = "EUR",
            cycle = DynamicPlanCycle.Yearly,
            viewId = "${UserId}$yearlyPlan".hashCode(),
            dynamicPlan = plan
        )

        expectInstanceUiModel(
            userId = UserId,
            monthlyInstance = shorterInstance,
            yearlyInstance = longerInstance,
            plan = plan,
            expectedInstance = Pair(expectedShorterInstance, expectedLongerInstance)
        )

        val expectedPlansUiModel = DynamicPlansUiModel(
            icon = ExpectedIconUiModelSocialProof,
            title = ExpectedTitleUiModel,
            description = ExpectedDescriptionUiModel,
            entitlements = ExpectedEntitlementsUiModelSocialProof,
            list = DynamicPlanInstanceListUiModel.Data.SocialProof(
                shorterCycle = expectedShorterInstance,
                longerCycle = expectedLongerInstance
            ),
            variant = DynamicPlansVariant.SocialProof
        )

        // When
        val actual = mapper.toUiModel(UserId, plan, UpsellingEntryPoint.Feature.Mailbox)

        // Then
        assertEquals(expectedPlansUiModel, actual)
    }

    private fun expectIconUiModel() {
        every { iconUiMapper.toUiModel(any(), any()) } returns ExpectedIconUiModel
    }

    private fun expectIconUiModelSocialProof() {
        every { iconUiMapper.toUiModel(any(), DynamicPlansVariant.SocialProof) } returns ExpectedIconUiModelSocialProof
    }

    private fun expectTitleUiModel() {
        every { titleUiMapper.toUiModel(any(), any()) } returns ExpectedTitleUiModel
    }

    private fun expectTitleUiModelPromo() {
        every { titleUiMapper.toUiModel(any(), UpsellingEntryPoint.Feature.MailboxPromo) } returns
            ExpectedTitleUiModelPromo
    }

    private fun expectDescriptionUiModel() {
        every { descriptionUiMapper.toUiModel(any(), any(), any()) } returns ExpectedDescriptionUiModel
    }

    private fun expectDescriptionUiModelPromo() {
        every { descriptionUiMapper.toUiModel(any(), any(), any()) } returns ExpectedDescriptionUiModelPromo
    }

    private fun expectEntitlementsUiModel() {
        every { entitlementsUiMapper.toUiModel(any(), any(), any()) } returns
            PlanEntitlementsUiModel.SimpleList(listOf(ExpectedEntitlementsUiModel))
    }

    private fun expectEntitlementsUiModelSocialProof() {
        every { entitlementsUiMapper.toUiModel(any(), any(), any()) } returns
            ExpectedEntitlementsUiModelSocialProof
    }

    @Suppress("LongParameterList")
    private fun expectInstanceUiModel(
        userId: UserId,
        monthlyInstance: DynamicPlanInstance,
        yearlyInstance: DynamicPlanInstance,
        plan: DynamicPlan,
        expectedInstance: Pair<DynamicPlanInstanceUiModel, DynamicPlanInstanceUiModel>
    ) {
        every { planInstanceUiMapper.toUiModel(userId, monthlyInstance, yearlyInstance, plan) } returns expectedInstance
    }

    private fun headerUpsellVariantLayoutEnabled(value: Boolean) {
        every {
            providerHeaderUpsellVariantLayoutEnabled.get()
        } returns value
    }

    private fun headerUpsellSocialProofEnabled(value: Boolean) {
        every {
            providerSocialProofLayoutEnabled.get()
        } returns value
    }

    private companion object {

        val ExpectedIconUiModel = DynamicPlanIconUiModel(R.drawable.illustration_upselling_mailbox)
        val ExpectedIconUiModelSocialProof = DynamicPlanIconUiModel(R.drawable.ic_mail_social_proof)
        val ExpectedDescriptionUiModel = DynamicPlanDescriptionUiModel.Simple(TextUiModel.Text("description"))
        val ExpectedDescriptionUiModelPromo = DynamicPlanDescriptionUiModel.Simple(
            TextUiModel.Text("description-promo")
        )
        val ExpectedEntitlementsUiModel = PlanEntitlementListUiModel.Default(TextUiModel.Text("item"), "")
        val ExpectedEntitlementsUiModelSocialProof = PlanEntitlementsUiModel.CheckedSimpleList(
            listOf(
                TextUiModel.Text("item1"),
                TextUiModel.Text("item2")
            )
        )
        val ExpectedTitleUiModel = DynamicPlanTitleUiModel(TextUiModel.Text("title"))
        val ExpectedTitleUiModelPromo = DynamicPlanTitleUiModel(TextUiModel.Text("title-promo"))

        val UserId = UserId("id")
    }
}
