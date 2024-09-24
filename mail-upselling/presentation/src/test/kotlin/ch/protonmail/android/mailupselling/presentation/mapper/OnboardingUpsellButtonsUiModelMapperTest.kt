package ch.protonmail.android.mailupselling.presentation.mapper

import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailupselling.domain.model.DynamicPlansOneClickIds
import ch.protonmail.android.mailupselling.presentation.R
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
            monthlyBillingMessage = TextUiModel.TextResWithArgs(
                R.string.upselling_onboarding_billing_monthly, listOf("EUR", TextUiModel.Text("0.1"))
            ),
            annualBillingMessage = TextUiModel.TextResWithArgs(
                R.string.upselling_onboarding_billing_annual, listOf("EUR", TextUiModel.Text("1.08"))
            )
        )

        // When
        val actual = onboardingUpsellButtonsUiModelMapper.toUiModel(
            UpsellingTestData.DynamicPlans,
            DynamicPlansOneClickIds.UnlimitedPlanId
        )

        // Then
        assertEquals(expected, actual)
    }
}
