package ch.protonmail.android.mailupselling.presentation.reducer

import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailupselling.domain.model.DynamicPlansOneClickIds
import ch.protonmail.android.mailupselling.presentation.R
import ch.protonmail.android.mailupselling.presentation.mapper.OnboardingUpsellButtonsUiModelMapper
import ch.protonmail.android.mailupselling.presentation.mapper.OnboardingUpsellPlanSwitcherUiModelMapper
import ch.protonmail.android.mailupselling.presentation.mapper.OnboardingUpsellPlanUiModelsMapper
import ch.protonmail.android.mailupselling.presentation.model.OnboardingUpsellState
import ch.protonmail.android.mailupselling.presentation.model.OnboardingUpsellState.OnboardingUpsellOperation.OnboardingUpsellEvent
import ch.protonmail.android.mailupselling.presentation.ui.onboarding.OnboardingUpsellPreviewData
import ch.protonmail.android.testdata.upselling.UpsellingTestData
import io.mockk.every
import io.mockk.mockk
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
internal class OnboardingUpsellReducerTest(private val testInput: TestInput) {

    private val onboardingUpsellPlanSwitchUiModelMapper = mockk<OnboardingUpsellPlanSwitcherUiModelMapper> {
        every { toUiModel(UpsellingTestData.DynamicPlans) } returns OnboardingUpsellPreviewData.PlanSwitcherUiModel
    }
    private val onboardingUpsellPlanUiModelsMapper = mockk<OnboardingUpsellPlanUiModelsMapper> {
        every { toUiModel(UpsellingTestData.DynamicPlans) } returns OnboardingUpsellPreviewData.PlanUiModels
    }
    private val onboardingUpsellButtonsUiModelMapper = mockk<OnboardingUpsellButtonsUiModelMapper> {
        every {
            toUiModel(UpsellingTestData.DynamicPlans, DynamicPlansOneClickIds.UnlimitedPlanId)
        } returns OnboardingUpsellPreviewData.ButtonsUiModel
    }

    private val onboardingUpsellReducer = OnboardingUpsellReducer(
        onboardingUpsellPlanSwitchUiModelMapper,
        onboardingUpsellPlanUiModelsMapper,
        onboardingUpsellButtonsUiModelMapper
    )

    @Test
    fun `should reduce the correct state`() = with(testInput) {
        // When
        val actualState = onboardingUpsellReducer.newStateFrom(operation)

        // Then
        assertEquals(expectedState, actualState)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = arrayOf(
            TestInput(
                operation = OnboardingUpsellEvent.DataLoaded(UpsellingTestData.DynamicPlans),
                expectedState = OnboardingUpsellState.Data(
                    planSwitcherUiModel = OnboardingUpsellPreviewData.PlanSwitcherUiModel,
                    planUiModels = OnboardingUpsellPreviewData.PlanUiModels,
                    buttonsUiModel = OnboardingUpsellPreviewData.ButtonsUiModel
                )
            ),
            TestInput(
                operation = OnboardingUpsellEvent.LoadingError.NoUserId,
                expectedState = OnboardingUpsellState.Error(
                    Effect.of(TextUiModel.TextRes(R.string.upselling_snackbar_error_no_user_id))
                )
            ),
            TestInput(
                operation = OnboardingUpsellEvent.LoadingError.NoSubscriptions,
                expectedState = OnboardingUpsellState.Error(
                    Effect.of(TextUiModel.TextRes(R.string.upselling_snackbar_error_no_subscriptions))
                )
            )
        )
    }

    data class TestInput(
        val operation: OnboardingUpsellState.OnboardingUpsellOperation,
        val expectedState: OnboardingUpsellState
    )
}
