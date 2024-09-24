package ch.protonmail.android.mailupselling.presentation.mapper

import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailupselling.domain.model.UpsellingEntryPoint
import ch.protonmail.android.mailupselling.presentation.mapper.DynamicPlanEntitlementsUiMapper.Companion.OnboardingFreeOverriddenEntitlements
import ch.protonmail.android.mailupselling.presentation.model.DynamicEntitlementUiModel
import ch.protonmail.android.mailupselling.presentation.model.OnboardingUpsellPlanUiModel
import ch.protonmail.android.mailupselling.presentation.model.OnboardingUpsellPlanUiModels
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

    private val onboardingUpsellPlanUiModelsMapper = OnboardingUpsellPlanUiModelsMapper(dynamicPlanEntitlementsUiMapper)

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
                    entitlements = listOf(DynamicEntitlementUiModel.Overridden(TextUiModel.Text("entitlement"), 0))
                ),
                OnboardingUpsellPlanUiModel(
                    title = "Mail Plus",
                    currency = "EUR",
                    monthlyPrice = null,
                    monthlyPriceWithDiscount = TextUiModel.Text("0.1"),
                    entitlements = listOf(DynamicEntitlementUiModel.Overridden(TextUiModel.Text("entitlement"), 0))
                ),
                OnboardingUpsellPlanUiModel(
                    title = "Proton Free",
                    currency = null,
                    monthlyPrice = null,
                    monthlyPriceWithDiscount = null,
                    entitlements = OnboardingFreeOverriddenEntitlements
                )
            ),
            annualPlans = listOf(
                OnboardingUpsellPlanUiModel(
                    title = "Proton Unlimited",
                    currency = "EUR",
                    monthlyPrice = TextUiModel.Text("0.1"),
                    monthlyPriceWithDiscount = TextUiModel.Text("0.09"),
                    entitlements = listOf(DynamicEntitlementUiModel.Overridden(TextUiModel.Text("entitlement"), 0))
                ),
                OnboardingUpsellPlanUiModel(
                    title = "Mail Plus",
                    currency = "EUR",
                    monthlyPrice = TextUiModel.Text("0.1"),
                    monthlyPriceWithDiscount = TextUiModel.Text("0.09"),
                    entitlements = listOf(DynamicEntitlementUiModel.Overridden(TextUiModel.Text("entitlement"), 0))
                ),
                OnboardingUpsellPlanUiModel(
                    title = "Proton Free",
                    currency = null,
                    monthlyPrice = null,
                    monthlyPriceWithDiscount = null,
                    entitlements = OnboardingFreeOverriddenEntitlements
                )
            )
        )

        // When
        val actual = onboardingUpsellPlanUiModelsMapper.toUiModel(UpsellingTestData.DynamicPlans)

        // Then
        assertEquals(expected, actual)
    }
}
