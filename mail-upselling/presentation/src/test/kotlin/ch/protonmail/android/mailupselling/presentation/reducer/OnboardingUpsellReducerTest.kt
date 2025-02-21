package ch.protonmail.android.mailupselling.presentation.reducer

import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailcommon.domain.sample.UserSample
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailupselling.presentation.R
import ch.protonmail.android.mailupselling.presentation.mapper.DynamicPlanInstanceUiMapper
import ch.protonmail.android.mailupselling.presentation.mapper.OnboardingUpsellButtonsUiModelMapper
import ch.protonmail.android.mailupselling.presentation.mapper.OnboardingUpsellPlanSwitcherUiModelMapper
import ch.protonmail.android.mailupselling.presentation.mapper.OnboardingUpsellPlanUiModelsMapper
import ch.protonmail.android.mailupselling.presentation.model.dynamicplans.DynamicPlanCycle
import ch.protonmail.android.mailupselling.presentation.model.dynamicplans.DynamicPlanInstanceUiModel
import ch.protonmail.android.mailupselling.presentation.model.onboarding.OnboardingUpsellState
import ch.protonmail.android.mailupselling.presentation.model.onboarding.OnboardingUpsellState.OnboardingUpsellOperation.OnboardingUpsellEvent
import ch.protonmail.android.mailupselling.presentation.ui.onboarding.OnboardingUpsellPreviewData
import ch.protonmail.android.testdata.upselling.UpsellingTestData
import ch.protonmail.android.testdata.upselling.UpsellingTestData.PlusPlan
import ch.protonmail.android.testdata.upselling.UpsellingTestData.UnlimitedPlan
import io.mockk.every
import io.mockk.mockk
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
    private val onboardingDynamicPlanInstanceUiMapper = mockk<DynamicPlanInstanceUiMapper> { }

    private val onboardingUpsellReducer: OnboardingUpsellReducer = OnboardingUpsellReducer(
        onboardingUpsellPlanSwitchUiModelMapper,
        onboardingUpsellPlanUiModelsMapper,
        onboardingUpsellButtonsUiModelMapper
    )

    private fun expectDynamicPlanInstanceUiMapper(
        dynamicPlanInstance: DynamicPlanInstance,
        dynamicPlan: DynamicPlanCycle,
        uiModelToReturn: DynamicPlanInstanceUiModel
    ) {
        every {
            onboardingDynamicPlanInstanceUiMapper.createPlanUiModel(
                any(),
                any(),
                dynamicPlanInstance,
                dynamicPlan,
                any()
            )
        } returns uiModelToReturn
    }

    @BeforeTest
    fun mockDynamicPlanInstances() {
        expectDynamicPlanInstanceUiMapper(
            UnlimitedPlan.instances[1]!!,
            DynamicPlanCycle.Monthly,
            OnboardingUpsellPreviewData.OnboardingDynamicPlanInstanceUiModel
        )
        expectDynamicPlanInstanceUiMapper(
            UnlimitedPlan.instances[12]!!,
            DynamicPlanCycle.Yearly,
            OnboardingUpsellPreviewData.OnboardingDynamicPlanInstanceUiModel
        )
        expectDynamicPlanInstanceUiMapper(
            PlusPlan.instances[1]!!,
            DynamicPlanCycle.Monthly,
            OnboardingUpsellPreviewData.OnboardingDynamicPlanInstanceUiModel
        )
        expectDynamicPlanInstanceUiMapper(
            PlusPlan.instances[12]!!,
            DynamicPlanCycle.Yearly,
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
            ),
            TestInput(
                currentState = OnboardingUpsellState.Loading,
                operation = OnboardingUpsellEvent.UnsupportedFlow.PaidUser,
                expectedState = OnboardingUpsellState.UnsupportedFlow
            ),
            TestInput(
                currentState = OnboardingUpsellState.Loading,
                operation = OnboardingUpsellEvent.UnsupportedFlow.NoSubscriptions,
                expectedState = OnboardingUpsellState.UnsupportedFlow
            ),
            TestInput(
                currentState = OnboardingUpsellState.Loading,
                operation = OnboardingUpsellEvent.UnsupportedFlow.PlansMismatch,
                expectedState = OnboardingUpsellState.UnsupportedFlow
            )
        )
    }

    data class TestInput(
        val currentState: OnboardingUpsellState,
        val operation: OnboardingUpsellState.OnboardingUpsellOperation,
        val expectedState: OnboardingUpsellState
    )
}
