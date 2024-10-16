package ch.protonmail.android.mailupselling.presentation.mapper

import ch.protonmail.android.mailcommon.domain.sample.UserSample
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailupselling.domain.model.UpsellingEntryPoint
import ch.protonmail.android.mailupselling.presentation.mapper.DynamicPlanEntitlementsUiMapper.Companion.OnboardingFreeOverriddenEntitlements
import ch.protonmail.android.mailupselling.presentation.model.DynamicEntitlementUiModel
import ch.protonmail.android.mailupselling.presentation.model.OnboardingUpsellPlanUiModel
import ch.protonmail.android.mailupselling.presentation.model.OnboardingUpsellPlanUiModels
import ch.protonmail.android.mailupselling.presentation.ui.onboarding.OnboardingUpsellPreviewData.OnboardingDynamicPlanInstanceUiModel
import ch.protonmail.android.mailupselling.presentation.ui.onboarding.OnboardingUpsellPreviewData.PremiumValuePlusDrawables
import ch.protonmail.android.mailupselling.presentation.ui.onboarding.OnboardingUpsellPreviewData.PremiumValueUnlimitedDrawables
import ch.protonmail.android.testdata.upselling.UpsellingTestData
import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals

class OnboardingUpsellPlanUiModelsMapperTest {

    private val dynamicPlanEntitlementsUiMapper = mockk<DynamicPlanEntitlementsUiMapper> {
        every {
            toUiModel(any(), UpsellingEntryPoint.PostOnboarding)
        } returns listOf(DynamicEntitlementUiModel.Overridden(TextUiModel.Text("entitlement"), 0))
    }
    private val onboardingDynamicPlanInstanceUiMapper = mockk<OnboardingDynamicPlanInstanceUiMapper> {
        every {
            toUiModel(any(), any(), any())
        } returns OnboardingDynamicPlanInstanceUiModel
    }

    private val onboardingUpsellPlanUiModelsMapper = OnboardingUpsellPlanUiModelsMapper(
        dynamicPlanEntitlementsUiMapper,
        onboardingDynamicPlanInstanceUiMapper
    )

    @Test
    fun `should map dynamic plans to ui models correctly`() {
        // Given
        val expected = OnboardingUpsellPlanUiModels(
            monthlyPlans = listOf(
                OnboardingUpsellPlanUiModel(
                    title = "Proton Unlimited",
                    currency = "EUR",
                    monthlyPrice = null,
                    monthlyPriceWithDiscount = TextUiModel.Text("0.1"),
                    entitlements = listOf(DynamicEntitlementUiModel.Overridden(TextUiModel.Text("entitlement"), 0)),
                    payButtonPlanUiModel = OnboardingDynamicPlanInstanceUiModel,
                    premiumValueDrawables = PremiumValueUnlimitedDrawables
                ),
                OnboardingUpsellPlanUiModel(
                    title = "Mail Plus",
                    currency = "EUR",
                    monthlyPrice = null,
                    monthlyPriceWithDiscount = TextUiModel.Text("0.1"),
                    entitlements = listOf(DynamicEntitlementUiModel.Overridden(TextUiModel.Text("entitlement"), 0)),
                    payButtonPlanUiModel = OnboardingDynamicPlanInstanceUiModel,
                    premiumValueDrawables = PremiumValuePlusDrawables
                ),
                OnboardingUpsellPlanUiModel(
                    title = "Proton Free",
                    currency = null,
                    monthlyPrice = null,
                    monthlyPriceWithDiscount = null,
                    entitlements = OnboardingFreeOverriddenEntitlements,
                    payButtonPlanUiModel = null,
                    premiumValueDrawables = emptyList()
                )
            ),
            annualPlans = listOf(
                OnboardingUpsellPlanUiModel(
                    title = "Proton Unlimited",
                    currency = "EUR",
                    monthlyPrice = TextUiModel.Text("0.1"),
                    monthlyPriceWithDiscount = TextUiModel.Text("0.09"),
                    entitlements = listOf(DynamicEntitlementUiModel.Overridden(TextUiModel.Text("entitlement"), 0)),
                    payButtonPlanUiModel = OnboardingDynamicPlanInstanceUiModel,
                    premiumValueDrawables = PremiumValueUnlimitedDrawables
                ),
                OnboardingUpsellPlanUiModel(
                    title = "Mail Plus",
                    currency = "EUR",
                    monthlyPrice = TextUiModel.Text("0.1"),
                    monthlyPriceWithDiscount = TextUiModel.Text("0.09"),
                    entitlements = listOf(DynamicEntitlementUiModel.Overridden(TextUiModel.Text("entitlement"), 0)),
                    payButtonPlanUiModel = OnboardingDynamicPlanInstanceUiModel,
                    premiumValueDrawables = PremiumValuePlusDrawables
                ),
                OnboardingUpsellPlanUiModel(
                    title = "Proton Free",
                    currency = null,
                    monthlyPrice = null,
                    monthlyPriceWithDiscount = null,
                    entitlements = OnboardingFreeOverriddenEntitlements,
                    payButtonPlanUiModel = null,
                    premiumValueDrawables = emptyList()
                )
            )
        )

        // When
        val actual = onboardingUpsellPlanUiModelsMapper.toUiModel(
            UpsellingTestData.DynamicPlans,
            UserSample.Primary.userId
        )

        // Then
        assertEquals(expected, actual)
    }
}
