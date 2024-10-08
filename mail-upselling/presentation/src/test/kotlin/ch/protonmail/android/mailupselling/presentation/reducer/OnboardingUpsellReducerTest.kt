package ch.protonmail.android.mailupselling.presentation.reducer

import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailcommon.domain.sample.UserSample
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailupselling.presentation.R
import ch.protonmail.android.mailupselling.presentation.mapper.OnboardingDynamicPlanInstanceUiMapper
import ch.protonmail.android.mailupselling.presentation.mapper.OnboardingUpsellButtonsUiModelMapper
import ch.protonmail.android.mailupselling.presentation.mapper.OnboardingUpsellPlanSwitcherUiModelMapper
import ch.protonmail.android.mailupselling.presentation.mapper.OnboardingUpsellPlanUiModelsMapper
import ch.protonmail.android.mailupselling.presentation.model.OnboardingDynamicPlanInstanceUiModel
import ch.protonmail.android.mailupselling.presentation.model.OnboardingUpsellState
import ch.protonmail.android.mailupselling.presentation.model.OnboardingUpsellState.OnboardingUpsellOperation.OnboardingUpsellEvent
import ch.protonmail.android.mailupselling.presentation.ui.onboarding.OnboardingUpsellPreviewData
import ch.protonmail.android.testdata.upselling.UpsellingTestData
import ch.protonmail.android.testdata.upselling.UpsellingTestData.PlusPlan
import ch.protonmail.android.testdata.upselling.UpsellingTestData.UnlimitedPlan
import io.mockk.every
import io.mockk.mockk
import me.proton.core.plan.domain.entity.DynamicPlan
import me.proton.core.plan.domain.entity.DynamicPlanInstance
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
internal class OnboardingUpsellReducerTest(private val testInput: TestInput) {

    private val onboardingUpsellPlanSwitchUiModelMapper = mockk<OnboardingUpsellPlanSwitcherUiModelMapper> {
        every { toUiModel(UpsellingTestData.DynamicPlans) } returns OnboardingUpsellPreviewData.PlanSwitcherUiModel
    }
    private val onboardingUpsellPlanUiModelsMapper = mockk<OnboardingUpsellPlanUiModelsMapper> {
        every {
            toUiModel(
                UpsellingTestData.DynamicPlans,
                UserSample.Primary.userId
            )
        } returns OnboardingUpsellPreviewData.PlanUiModels
    }
    private val onboardingUpsellButtonsUiModelMapper = mockk<OnboardingUpsellButtonsUiModelMapper> {
        every { toUiModel(UpsellingTestData.DynamicPlans) } returns OnboardingUpsellPreviewData.ButtonsUiModel
    }
    private val onboardingDynamicPlanInstanceUiMapper = mockk<OnboardingDynamicPlanInstanceUiMapper> { }

    private val onboardingUpsellReducer: OnboardingUpsellReducer = OnboardingUpsellReducer(
        onboardingUpsellPlanSwitchUiModelMapper,
        onboardingUpsellPlanUiModelsMapper,
        onboardingUpsellButtonsUiModelMapper
    )

    private fun expectDynamicPlanInstanceUiMapper(
        dynamicPlanInstance: DynamicPlanInstance,
        dynamicPlan: DynamicPlan,
        uiModelToReturn: OnboardingDynamicPlanInstanceUiModel
    ) {
        every {
            onboardingDynamicPlanInstanceUiMapper.toUiModel(
                any(),
                dynamicPlanInstance,
                dynamicPlan
            )
        } returns uiModelToReturn
    }

    @BeforeTest
    fun mockDynamicPlanInstances() {
        expectDynamicPlanInstanceUiMapper(
            UnlimitedPlan.instances[1]!!,
            UnlimitedPlan,
            OnboardingUpsellPreviewData.OnboardingDynamicPlanInstanceUiModel
        )
        expectDynamicPlanInstanceUiMapper(
            UnlimitedPlan.instances[12]!!,
            UnlimitedPlan,
            OnboardingUpsellPreviewData.OnboardingDynamicPlanInstanceUiModel
        )
        expectDynamicPlanInstanceUiMapper(
            PlusPlan.instances[1]!!,
            PlusPlan,
            OnboardingUpsellPreviewData.OnboardingDynamicPlanInstanceUiModel
        )
        expectDynamicPlanInstanceUiMapper(
            PlusPlan.instances[12]!!,
            PlusPlan,
            OnboardingUpsellPreviewData.OnboardingDynamicPlanInstanceUiModel
        )
    }

    @Test
    fun `should reduce the correct state`() = with(testInput) {
        // When
        val actualState = onboardingUpsellReducer.newStateFrom(currentState, operation)

        // Then
        assertEquals(expectedState, actualState)
    }

    companion object {

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = arrayOf(
            TestInput(
                currentState = OnboardingUpsellState.Loading,
                operation = OnboardingUpsellEvent.DataLoaded(
                    UserIdSample.Primary,
                    UpsellingTestData.DynamicPlans
                ),
                expectedState = OnboardingUpsellState.Data(
                    planSwitcherUiModel = OnboardingUpsellPreviewData.PlanSwitcherUiModel,
                    planUiModels = OnboardingUpsellPreviewData.PlanUiModels,
                    buttonsUiModel = OnboardingUpsellPreviewData.ButtonsUiModel,
                    selectedPayButtonPlanUiModel = null
                )
            ),
            TestInput(
                currentState = OnboardingUpsellState.Loading,
                operation = OnboardingUpsellEvent.LoadingError.NoUserId,
                expectedState = OnboardingUpsellState.Error(
                    Effect.of(TextUiModel.TextRes(R.string.upselling_snackbar_error_no_user_id))
                )
            ),
            TestInput(
                currentState = OnboardingUpsellState.Loading,
                operation = OnboardingUpsellEvent.LoadingError.NoSubscriptions,
                expectedState = OnboardingUpsellState.Error(
                    Effect.of(TextUiModel.TextRes(R.string.upselling_snackbar_error_no_subscriptions))
                )
            ),
            TestInput(
                currentState = OnboardingUpsellState.Data(
                    planSwitcherUiModel = OnboardingUpsellPreviewData.PlanSwitcherUiModel,
                    planUiModels = OnboardingUpsellPreviewData.PlanUiModels,
                    buttonsUiModel = OnboardingUpsellPreviewData.ButtonsUiModel,
                    selectedPayButtonPlanUiModel = null
                ),
                operation = OnboardingUpsellEvent.PlanSelected(
                    OnboardingUpsellPreviewData.OnboardingDynamicPlanInstanceUiModel
                ),
                expectedState = OnboardingUpsellState.Data(
                    planSwitcherUiModel = OnboardingUpsellPreviewData.PlanSwitcherUiModel,
                    planUiModels = OnboardingUpsellPreviewData.PlanUiModels,
                    buttonsUiModel = OnboardingUpsellPreviewData.ButtonsUiModel,
                    selectedPayButtonPlanUiModel = OnboardingUpsellPreviewData.OnboardingDynamicPlanInstanceUiModel
                )
            )
        )
    }

    data class TestInput(
        val currentState: OnboardingUpsellState,
        val operation: OnboardingUpsellState.OnboardingUpsellOperation,
        val expectedState: OnboardingUpsellState
    )
}
