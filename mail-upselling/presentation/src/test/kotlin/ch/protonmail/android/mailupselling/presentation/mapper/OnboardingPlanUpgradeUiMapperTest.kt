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

package ch.protonmail.android.mailupselling.presentation.mapper

import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailupselling.domain.model.PlanUpgradeCycle
import ch.protonmail.android.mailupselling.domain.model.PlanUpgradePlanType
import ch.protonmail.android.mailupselling.presentation.R
import ch.protonmail.android.mailupselling.presentation.model.onboarding.OnboardingPlanUpgradeUiModel
import ch.protonmail.android.mailupselling.presentation.model.onboarding.OnboardingPlanUpgradesListUiModel
import ch.protonmail.android.mailupselling.presentation.model.planupgrades.PlanUpgradeEntitlementListUiModel
import ch.protonmail.android.mailupselling.presentation.model.planupgrades.PlanUpgradeInstanceUiModel
import ch.protonmail.android.mailupselling.presentation.model.planupgrades.PlanUpgradeVariant
import ch.protonmail.android.testdata.upselling.UpsellingTestData
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

internal class OnboardingPlanUpgradeUiMapperTest {

    private val instanceMapper = mockk<PlanUpgradeInstanceUiModelMapper>()
    private val entitlementsMapper = mockk<PlanUpgradeEntitlementsUiMapper>()
    private val planUpgradeMapper = mockk<PlanUpgradeMapper>()

    private lateinit var uiMapper: OnboardingPlanUpgradeUiMapper

    @BeforeTest
    fun setup() {
        uiMapper = OnboardingPlanUpgradeUiMapper(
            planUpgradeMapper,
            instanceMapper,
            entitlementsMapper
        )
    }

    @AfterTest
    fun teardown() {
        clearAllMocks()
    }

    @Test
    fun `should map to error if no plans are served`() = runTest {
        // When
        val actual = uiMapper.toUiModel(emptyList())

        // Then
        assertEquals(PlanMappingError.EmptyList.left(), actual)
    }

    @Test
    fun `should map to error if invalid instances are served`() = runTest {
        // Given
        val productDetails = listOf(
            UpsellingTestData.MailPlusProducts.MonthlyProductOfferDetail,
            UpsellingTestData.UnlimitedMailProduct.YearlyProductDetail
        )

        // When
        val actual = uiMapper.toUiModel(productDetails)

        // Then
        assertEquals(PlanMappingError.InvalidList.left(), actual)
    }

    @Test
    fun `should map to monthly, yearly and free plans ui model`() = runTest {
        // Given
        coEvery {
            planUpgradeMapper.resolveVariant(any(), any(), any())
        } returns PlanUpgradeVariant.Normal.MailPlus

        val monthlyMailPlus = UpsellingTestData.MailPlusProducts.MonthlyProductOfferDetail
        val yearlyMailPlus = UpsellingTestData.MailPlusProducts.YearlyProductOfferDetail
        val monthlyUnlimited = UpsellingTestData.UnlimitedMailProduct.MonthlyProductOfferDetail
        val yearlyUnlimited = UpsellingTestData.UnlimitedMailProduct.YearlyProductDetail

        val mockedMailPlusEntitlements = listOf(mockk<PlanUpgradeEntitlementListUiModel>())
        val mockedUnlimitedEntitlements = listOf(mockk<PlanUpgradeEntitlementListUiModel>())

        val mockedMonthlyMailPlusModel = mockk<PlanUpgradeInstanceUiModel>()
        val mockedYearlyMailPlusModel = mockk<PlanUpgradeInstanceUiModel>()

        val mockedMonthlyUnlimitedModel = mockk<PlanUpgradeInstanceUiModel>()
        val mockedYearlyUnlimitedModel = mockk<PlanUpgradeInstanceUiModel>()

        val expectedMonthlyMailUiModel = OnboardingPlanUpgradeUiModel.Paid(
            planType = PlanUpgradePlanType.MailPlus,
            entitlements = mockedMailPlusEntitlements,
            variant = PlanUpgradeVariant.Normal.MailPlus,
            cycle = PlanUpgradeCycle.Monthly,
            planInstance = mockedMonthlyMailPlusModel
        )

        val expectedYearlyMailUiModel = OnboardingPlanUpgradeUiModel.Paid(
            planType = PlanUpgradePlanType.MailPlus,
            entitlements = mockedMailPlusEntitlements,
            variant = PlanUpgradeVariant.Normal.MailPlus,
            cycle = PlanUpgradeCycle.Yearly,
            planInstance = mockedYearlyMailPlusModel
        )

        val expectedMonthlyBundleModel = OnboardingPlanUpgradeUiModel.Paid(
            planType = PlanUpgradePlanType.Unlimited,
            entitlements = mockedUnlimitedEntitlements,
            variant = PlanUpgradeVariant.Normal.MailPlus,
            cycle = PlanUpgradeCycle.Monthly,
            planInstance = mockedMonthlyUnlimitedModel
        )

        val expectedYearlyBundleModel = OnboardingPlanUpgradeUiModel.Paid(
            planType = PlanUpgradePlanType.Unlimited,
            entitlements = mockedUnlimitedEntitlements,
            variant = PlanUpgradeVariant.Normal.MailPlus,
            cycle = PlanUpgradeCycle.Yearly,
            planInstance = mockedYearlyUnlimitedModel
        )

        val expectedPlans = OnboardingPlanUpgradesListUiModel(
            listOf(expectedMonthlyBundleModel, expectedMonthlyMailUiModel, FreePlan),
            listOf(expectedYearlyBundleModel, expectedYearlyMailUiModel, FreePlan)
        )

        val productDetails = listOf(
            monthlyMailPlus,
            yearlyMailPlus,
            monthlyUnlimited,
            yearlyUnlimited
        )

        every {
            instanceMapper.toUiModel(monthlyMailPlus, yearlyMailPlus)
        } returns Pair(mockedMonthlyMailPlusModel, mockedYearlyMailPlusModel)

        every {
            instanceMapper.toUiModel(monthlyUnlimited, yearlyUnlimited)
        } returns Pair(mockedMonthlyUnlimitedModel, mockedYearlyUnlimitedModel)

        every { entitlementsMapper.toOnboardingUiModel(monthlyMailPlus) } returns mockedMailPlusEntitlements
        every { entitlementsMapper.toOnboardingUiModel(yearlyMailPlus) } returns mockedMailPlusEntitlements
        every { entitlementsMapper.toOnboardingUiModel(monthlyUnlimited) } returns mockedUnlimitedEntitlements
        every { entitlementsMapper.toOnboardingUiModel(yearlyUnlimited) } returns mockedUnlimitedEntitlements

        // When
        val actual = uiMapper.toUiModel(productDetails)

        // Then
        assertEquals(expectedPlans.right(), actual)
    }

    private companion object {

        val FreePlan = OnboardingPlanUpgradeUiModel.Free(
            planName = TextUiModel("Proton Free"),
            entitlements = listOf(
                PlanUpgradeEntitlementListUiModel.Local(
                    TextUiModel.TextRes(R.string.upselling_onboarding_free_entitlement_storage),
                    R.drawable.ic_storage
                ),
                PlanUpgradeEntitlementListUiModel.Local(
                    TextUiModel.TextRes(R.string.upselling_onboarding_free_entitlement_mail),
                    R.drawable.ic_proton_envelope
                )
            ),
            currency = "EUR"
        )
    }
}
