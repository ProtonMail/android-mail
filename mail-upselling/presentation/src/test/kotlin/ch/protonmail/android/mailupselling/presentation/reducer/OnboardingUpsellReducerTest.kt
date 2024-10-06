package ch.protonmail.android.mailupselling.presentation.reducer

import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailupselling.presentation.R
import ch.protonmail.android.mailupselling.presentation.mapper.DynamicPlanInstanceUiMapper
import ch.protonmail.android.mailupselling.presentation.mapper.OnboardingUpsellButtonsUiModelMapper
import ch.protonmail.android.mailupselling.presentation.mapper.OnboardingUpsellPlanSwitcherUiModelMapper
import ch.protonmail.android.mailupselling.presentation.mapper.OnboardingUpsellPlanUiModelsMapper
import ch.protonmail.android.mailupselling.presentation.model.DynamicPlanInstanceUiModel
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
        every { toUiModel(UpsellingTestData.DynamicPlans) } returns OnboardingUpsellPreviewData.PlanUiModels
    }
    private val onboardingUpsellButtonsUiModelMapper = mockk<OnboardingUpsellButtonsUiModelMapper> {
        every { toUiModel(UpsellingTestData.DynamicPlans) } returns OnboardingUpsellPreviewData.ButtonsUiModel
    }
    private val dynamicPlanInstanceUiMapper = mockk<DynamicPlanInstanceUiMapper> { }

    private val onboardingUpsellReducer: OnboardingUpsellReducer = OnboardingUpsellReducer(
        onboardingUpsellPlanSwitchUiModelMapper,
        onboardingUpsellPlanUiModelsMapper,
        onboardingUpsellButtonsUiModelMapper,
        dynamicPlanInstanceUiMapper
    )

    private fun expectDynamicPlanInstanceUiMapper(
        dynamicPlanInstance: DynamicPlanInstance,
        dynamicPlan: DynamicPlan,
        uiModelToReturn: DynamicPlanInstanceUiModel
    ) {
        every {
            dynamicPlanInstanceUiMapper.toUiModel(
                any(),
                dynamicPlanInstance,
                any(),
                any(),
                dynamicPlan
            )
        } returns uiModelToReturn
    }

    @BeforeTest
    fun mockDynamicPlanInstances() {
        expectDynamicPlanInstanceUiMapper(
            UnlimitedPlan.instances[1]!!,
            UnlimitedPlan,
            OnboardingUpsellPreviewData.DynamicPlanInstanceUiModel
        )
        expectDynamicPlanInstanceUiMapper(
            UnlimitedPlan.instances[12]!!,
            UnlimitedPlan,
            OnboardingUpsellPreviewData.DynamicPlanInstanceUiModel
        )
        expectDynamicPlanInstanceUiMapper(
            PlusPlan.instances[1]!!,
            PlusPlan,
            OnboardingUpsellPreviewData.DynamicPlanInstanceUiModel
        )
        expectDynamicPlanInstanceUiMapper(
            PlusPlan.instances[12]!!,
            PlusPlan,
            OnboardingUpsellPreviewData.DynamicPlanInstanceUiModel
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
                    dynamicPlanInstanceUiModels = listOf(
                        OnboardingUpsellPreviewData.DynamicPlanInstanceUiModel,
                        OnboardingUpsellPreviewData.DynamicPlanInstanceUiModel,
                        OnboardingUpsellPreviewData.DynamicPlanInstanceUiModel,
                        OnboardingUpsellPreviewData.DynamicPlanInstanceUiModel
                    ),
                    selectedPlanInstanceUiModel = null
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
                    dynamicPlanInstanceUiModels = listOf(OnboardingUpsellPreviewData.DynamicPlanInstanceUiModel),
                    selectedPlanInstanceUiModel = null
                ),
                operation = OnboardingUpsellEvent.PlanSelected(OnboardingUpsellPreviewData.DynamicPlanInstanceUiModel),
                expectedState = OnboardingUpsellState.Data(
                    planSwitcherUiModel = OnboardingUpsellPreviewData.PlanSwitcherUiModel,
                    planUiModels = OnboardingUpsellPreviewData.PlanUiModels,
                    buttonsUiModel = OnboardingUpsellPreviewData.ButtonsUiModel,
                    dynamicPlanInstanceUiModels = listOf(OnboardingUpsellPreviewData.DynamicPlanInstanceUiModel),
                    selectedPlanInstanceUiModel = OnboardingUpsellPreviewData.DynamicPlanInstanceUiModel
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
