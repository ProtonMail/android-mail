package ch.protonmail.android.mailupselling.presentation.mapper

import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailupselling.presentation.R
import ch.protonmail.android.mailupselling.presentation.model.OnboardingUpsellBillingMessageUiModel
import ch.protonmail.android.mailupselling.presentation.model.OnboardingUpsellButtonsUiModel
import ch.protonmail.android.testdata.upselling.UpsellingTestData
import kotlin.test.Test
import kotlin.test.assertEquals

class OnboardingUpsellButtonsUiModelMapperTest {

    private val onboardingUpsellButtonsUiModelMapper = OnboardingUpsellButtonsUiModelMapper()

    @Test
    fun `should map dynamic plans to buttons ui model correctly`() {
        // Given
        val expected = OnboardingUpsellButtonsUiModel(
            billingMessage = mapOf(
                "Proton Unlimited" to OnboardingUpsellBillingMessageUiModel(
                    monthlyBillingMessage = TextUiModel.TextResWithArgs(
                        R.string.upselling_onboarding_billing_monthly, listOf("EUR", "0.1")
                    ),
                    annualBillingMessage = TextUiModel.TextResWithArgs(
                        R.string.upselling_onboarding_billing_annual, listOf("EUR", "1.08")
                    )
                ),
                "Mail Plus" to OnboardingUpsellBillingMessageUiModel(
                    monthlyBillingMessage = TextUiModel.TextResWithArgs(
                        R.string.upselling_onboarding_billing_monthly, listOf("EUR", "0.1")
                    ),
                    annualBillingMessage = TextUiModel.TextResWithArgs(
                        R.string.upselling_onboarding_billing_annual, listOf("EUR", "1.08")
                    )
                )
            ),
            getButtonLabel = mapOf(
                "Proton Unlimited" to TextUiModel.TextResWithArgs(
                    R.string.upselling_onboarding_get_plan, listOf("Proton Unlimited")
                ),
                "Mail Plus" to TextUiModel.TextResWithArgs(
                    R.string.upselling_onboarding_get_plan, listOf("Mail Plus")
                )
            )
        )

        // When
        val actual = onboardingUpsellButtonsUiModelMapper.toUiModel(UpsellingTestData.DynamicPlans)

        // Then
        assertEquals(expected, actual)
    }
}
