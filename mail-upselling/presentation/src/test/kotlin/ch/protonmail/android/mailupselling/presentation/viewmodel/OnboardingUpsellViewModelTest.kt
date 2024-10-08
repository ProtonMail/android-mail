package ch.protonmail.android.mailupselling.presentation.viewmodel

import java.time.Instant
import app.cash.turbine.test
import ch.protonmail.android.mailcommon.domain.sample.UserSample
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUser
import ch.protonmail.android.mailupselling.presentation.mapper.OnboardingUpsellButtonsUiModelMapper
import ch.protonmail.android.mailupselling.presentation.mapper.OnboardingUpsellPlanSwitcherUiModelMapper
import ch.protonmail.android.mailupselling.presentation.mapper.OnboardingUpsellPlanUiModelsMapper
import ch.protonmail.android.mailupselling.presentation.model.OnboardingUpsellOperation
import ch.protonmail.android.mailupselling.presentation.model.OnboardingUpsellState
import ch.protonmail.android.mailupselling.presentation.reducer.OnboardingUpsellReducer
import ch.protonmail.android.mailupselling.presentation.ui.onboarding.OnboardingUpsellPreviewData
import ch.protonmail.android.mailupselling.presentation.ui.onboarding.PlansType
import ch.protonmail.android.testdata.upselling.UpsellingTestData
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import me.proton.core.domain.entity.UserId
import me.proton.core.plan.domain.entity.DynamicPlans
import me.proton.core.plan.domain.usecase.GetDynamicPlansAdjustedPrices
import me.proton.core.user.domain.entity.User
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class OnboardingUpsellViewModelTest {

    private val user = UserSample.Primary
    private val dynamicPlans = UpsellingTestData.DynamicPlans

    private val observePrimaryUser = mockk<ObservePrimaryUser>()
    private val getDynamicPlansAdjustedPrices = mockk<GetDynamicPlansAdjustedPrices>()
    private val onboardingUpsellPlanSwitchUiModelMapper = mockk<OnboardingUpsellPlanSwitcherUiModelMapper> {
        every { toUiModel(UpsellingTestData.DynamicPlans) } returns OnboardingUpsellPreviewData.PlanSwitcherUiModel
    }
    private val onboardingUpsellPlanUiModelsMapper = mockk<OnboardingUpsellPlanUiModelsMapper> {
        every {
            toUiModel(UpsellingTestData.DynamicPlans, user.userId)
        } returns OnboardingUpsellPreviewData.PlanUiModels
    }
    private val onboardingUpsellButtonsUiModelMapper = mockk<OnboardingUpsellButtonsUiModelMapper> {
        every { toUiModel(UpsellingTestData.DynamicPlans) } returns OnboardingUpsellPreviewData.ButtonsUiModel
    }

    private val onboardingUpsellReducer = OnboardingUpsellReducer(
        onboardingUpsellPlanSwitchUiModelMapper,
        onboardingUpsellPlanUiModelsMapper,
        onboardingUpsellButtonsUiModelMapper
    )

    private val viewModel: OnboardingUpsellViewModel by lazy {
        OnboardingUpsellViewModel(
            observePrimaryUser,
            getDynamicPlansAdjustedPrices,
            onboardingUpsellReducer
        )
    }

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        mockkStatic(Instant::class)
    }

    @AfterTest
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `should emit loading state at start`() = runTest {
        every { observePrimaryUser() } returns flowOf()

        // When + Then
        viewModel.state.test {
            assertEquals(OnboardingUpsellState.Loading, awaitItem())
        }
    }

    @Test
    fun `should emit error when user is null`() = runTest {
        // Given
        expectPrimaryUser(null)

        // When + Then
        viewModel.state.test {
            assertIs<OnboardingUpsellState.Error>(awaitItem())
        }
    }

    @Test
    fun `should emit error when dynamic plans fail to be fetched`() = runTest {
        // Given
        expectPrimaryUser(user)
        expectDynamicPlansError(user.userId)

        // When + Then
        viewModel.state.test {
            assertIs<OnboardingUpsellState.Error>(awaitItem())
        }
    }

    @Test
    fun `should emit data when dynamic plans are fetched`() = runTest {
        // Given
        expectPrimaryUser(user)
        expectDynamicPlans(user.userId, dynamicPlans)

        // When + Then
        viewModel.state.test {
            assertIs<OnboardingUpsellState.Data>(awaitItem())
        }
    }

    @Test
    fun `should emit correct state when PlanSelected action is submitted`() = runTest {
        // Given
        expectPrimaryUser(user)
        expectDynamicPlans(user.userId, dynamicPlans)
        val expectedSelectedPlanInstanceUiModel = OnboardingUpsellPreviewData.OnboardingDynamicPlanInstanceUiModel

        // When + Then
        viewModel.state.test {
            assertNull((viewModel.state.value as OnboardingUpsellState.Data).selectedPayButtonPlanUiModel)

            viewModel.submit(OnboardingUpsellOperation.Action.PlanSelected(PlansType.Monthly, "Mail Plus"))

            awaitItem()
            val actual = awaitItem()

            assertIs<OnboardingUpsellState.Data>(actual)
            assertEquals(expectedSelectedPlanInstanceUiModel, actual.selectedPayButtonPlanUiModel)
        }
    }

    private fun expectPrimaryUser(user: User?) {
        every { observePrimaryUser() } returns flowOf(user)
    }

    private fun expectDynamicPlans(userId: UserId, dynamicPlans: DynamicPlans) {
        coEvery { getDynamicPlansAdjustedPrices(userId) } returns dynamicPlans
    }

    private fun expectDynamicPlansError(userId: UserId) {
        coEvery { getDynamicPlansAdjustedPrices(userId) } throws Exception()
    }
}
