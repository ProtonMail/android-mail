package ch.protonmail.android.mailupselling.presentation.mapper

import ch.protonmail.android.mailcommon.domain.sample.UserSample
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailupselling.domain.model.UpsellingEntryPoint
import ch.protonmail.android.mailupselling.presentation.R
import ch.protonmail.android.mailupselling.presentation.mapper.DynamicPlanEntitlementsUiMapper.Companion.OnboardingFreeOverriddenEntitlements
import ch.protonmail.android.mailupselling.presentation.model.dynamicplans.PlanEntitlementListUiModel
import ch.protonmail.android.mailupselling.presentation.model.dynamicplans.PlanEntitlementsUiModel
import ch.protonmail.android.mailupselling.presentation.model.onboarding.OnboardingUpsellPlanUiModel
import ch.protonmail.android.mailupselling.presentation.model.onboarding.OnboardingUpsellPlanUiModels
import ch.protonmail.android.mailupselling.presentation.model.onboarding.OnboardingUpsellPriceUiModel
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
            toListUiModel(any(), UpsellingEntryPoint.PostOnboarding)
        } returns PlanEntitlementsUiModel.SimpleList(
            listOf(PlanEntitlementListUiModel.Overridden(TextUiModel.Text("entitlement"), 0))
        )
    }
    private val onboardingDynamicPlanInstanceUiMapper = mockk<DynamicPlanInstanceUiMapper> {
        every {
            createPlanUiModel(any(), any(), any(), any())
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
                    priceUiModel = OnboardingUpsellPriceUiModel.Paid(
                        currency = "EUR",
                        originalAmount = null,
                        amount = TextUiModel.Text("0.1"),
                        period = TextUiModel.TextRes(R.string.upselling_onboarding_month)
                    ),
                    entitlements = PlanEntitlementsUiModel.SimpleList(
                        listOf(PlanEntitlementListUiModel.Overridden(TextUiModel.Text("entitlement"), 0))
                    ),
                    payButtonPlanUiModel = OnboardingDynamicPlanInstanceUiModel,
                    premiumValueDrawables = PremiumValueUnlimitedDrawables
                ),
                OnboardingUpsellPlanUiModel(
                    title = "Mail Plus",
                    priceUiModel = OnboardingUpsellPriceUiModel.Paid(
                        currency = "EUR",
                        originalAmount = null,
                        amount = TextUiModel.Text("0.1"),
                        period = TextUiModel.TextRes(R.string.upselling_onboarding_month)
                    ),
                    entitlements = PlanEntitlementsUiModel.SimpleList(
                        listOf(PlanEntitlementListUiModel.Overridden(TextUiModel.Text("entitlement"), 0))
                    ),
                    payButtonPlanUiModel = OnboardingDynamicPlanInstanceUiModel,
                    premiumValueDrawables = PremiumValuePlusDrawables
                ),
                OnboardingUpsellPlanUiModel(
                    title = "Proton Free",
                    priceUiModel = OnboardingUpsellPriceUiModel.Free,
                    entitlements = PlanEntitlementsUiModel.SimpleList(OnboardingFreeOverriddenEntitlements),
                    payButtonPlanUiModel = null,
                    premiumValueDrawables = emptyList()
                )
            ),
            annualPlans = listOf(
                OnboardingUpsellPlanUiModel(
                    title = "Proton Unlimited",
                    priceUiModel = OnboardingUpsellPriceUiModel.Paid(
                        currency = "EUR",
                        originalAmount = TextUiModel.Text("1.2"),
                        amount = TextUiModel.Text("1.08"),
                        period = TextUiModel.TextRes(R.string.upselling_onboarding_year)
                    ),
                    entitlements = PlanEntitlementsUiModel.SimpleList(
                        listOf(PlanEntitlementListUiModel.Overridden(TextUiModel.Text("entitlement"), 0))
                    ),
                    payButtonPlanUiModel = OnboardingDynamicPlanInstanceUiModel,
                    premiumValueDrawables = PremiumValueUnlimitedDrawables
                ),
                OnboardingUpsellPlanUiModel(
                    title = "Mail Plus",
                    priceUiModel = OnboardingUpsellPriceUiModel.Paid(
                        currency = "EUR",
                        originalAmount = TextUiModel.Text("1.2"),
                        amount = TextUiModel.Text("1.08"),
                        period = TextUiModel.TextRes(R.string.upselling_onboarding_year)
                    ),
                    entitlements = PlanEntitlementsUiModel.SimpleList(
                        listOf(PlanEntitlementListUiModel.Overridden(TextUiModel.Text("entitlement"), 0))
                    ),
                    payButtonPlanUiModel = OnboardingDynamicPlanInstanceUiModel,
                    premiumValueDrawables = PremiumValuePlusDrawables
                ),
                OnboardingUpsellPlanUiModel(
                    title = "Proton Free",
                    priceUiModel = OnboardingUpsellPriceUiModel.Free,
                    entitlements = PlanEntitlementsUiModel.SimpleList(OnboardingFreeOverriddenEntitlements),
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
