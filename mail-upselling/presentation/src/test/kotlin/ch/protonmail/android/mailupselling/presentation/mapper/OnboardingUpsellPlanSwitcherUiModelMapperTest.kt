package ch.protonmail.android.mailupselling.presentation.mapper

import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailupselling.presentation.R
import ch.protonmail.android.mailupselling.presentation.model.onboarding.OnboardingUpsellPlanSwitcherUiModel
import ch.protonmail.android.mailupselling.presentation.usecase.GetDiscountRate
import ch.protonmail.android.testdata.upselling.UpsellingTestData
import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals

class OnboardingUpsellPlanSwitcherUiModelMapperTest {

    private val getDiscountRate = mockk<GetDiscountRate> {
        every {
            this@mockk.invoke(UpsellingTestData.MonthlyDynamicPlanInstance, UpsellingTestData.YearlyDynamicPlanInstance)
        } returns 10
    }

    private val onboardingUpsellPlanSwitchUiModelMapper = OnboardingUpsellPlanSwitcherUiModelMapper(getDiscountRate)

    @Test
    fun `should map dynamic plans to plan switcher ui model`() {
        // Given
        val expected = OnboardingUpsellPlanSwitcherUiModel(
            discount = TextUiModel.TextResWithArgs(R.string.upselling_onboarding_save_label, listOf(10))
        )

        // When
        val actual = onboardingUpsellPlanSwitchUiModelMapper.toUiModel(UpsellingTestData.DynamicPlans)

        // Then
        assertEquals(expected, actual)
    }
}
