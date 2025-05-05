package ch.protonmail.android.mailupselling.presentation.viewmodel

import java.time.Instant
import app.cash.turbine.test
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUser
import ch.protonmail.android.mailupselling.domain.model.UpsellingEntryPoint
import ch.protonmail.android.mailupselling.domain.model.telemetry.UpsellingTelemetryEventType
import ch.protonmail.android.mailupselling.domain.model.telemetry.UpsellingTelemetryTargetPlanPayload
import ch.protonmail.android.mailupselling.domain.repository.UpsellingTelemetryRepository
import ch.protonmail.android.mailupselling.presentation.mapper.OnboardingUpsellButtonsUiModelMapper
import ch.protonmail.android.mailupselling.presentation.mapper.OnboardingUpsellPlanSwitcherUiModelMapper
import ch.protonmail.android.mailupselling.presentation.mapper.OnboardingUpsellPlanUiModelsMapper
import ch.protonmail.android.mailupselling.presentation.model.onboarding.OnboardingUpsellOperation
import ch.protonmail.android.mailupselling.presentation.model.onboarding.OnboardingUpsellState
import ch.protonmail.android.mailupselling.presentation.reducer.OnboardingUpsellReducer
import ch.protonmail.android.mailupselling.presentation.ui.onboarding.OnboardingUpsellPreviewData
import ch.protonmail.android.mailupselling.presentation.ui.onboarding.PlansType
import ch.protonmail.android.mailupselling.presentation.usecase.GetOnboardingUpsellingPlans
import ch.protonmail.android.mailupselling.presentation.usecase.GetOnboardingUpsellingPlans.GetOnboardingPlansError
import ch.protonmail.android.testdata.upselling.UpsellingTestData
import ch.protonmail.android.testdata.user.UserTestData
import io.mockk.coEvery
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import me.proton.core.domain.entity.UserId
import me.proton.core.plan.domain.entity.DynamicPlans
import me.proton.core.user.domain.entity.User
import org.junit.Assert.assertNull
import javax.inject.Provider
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class OnboardingUpsellViewModelTest {

    private val freeUser = UserTestData.freeUser
    private val paidUser = UserTestData.paidUser
    private val dynamicPlans = UpsellingTestData.DynamicPlans

    private val observePrimaryUser = mockk<ObservePrimaryUser>()
    private val getOnboardingUpsellingPlans = mockk<GetOnboardingUpsellingPlans>()
    private val onboardingUpsellPlanSwitchUiModelMapper = mockk<OnboardingUpsellPlanSwitcherUiModelMapper> {
        every { toUiModel(UpsellingTestData.DynamicPlans) } returns OnboardingUpsellPreviewData.PlanSwitcherUiModel
    }
    private val onboardingUpsellPlanUiModelsMapper = mockk<OnboardingUpsellPlanUiModelsMapper> {
        every {
            toUiModel(UpsellingTestData.DynamicPlans, freeUser.userId)
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

    private val isFeatureEnabled = mockk<Provider<Boolean>> {
        every { this@mockk.get() } returns true
    }

    private val upsellingTelemetryRepository = mockk<UpsellingTelemetryRepository>(relaxUnitFun = true)

    private val viewModel: OnboardingUpsellViewModel by lazy {
        OnboardingUpsellViewModel(
            observePrimaryUser,
            getOnboardingUpsellingPlans,
            onboardingUpsellReducer,
            isFeatureEnabled.get(),
            upsellingTelemetryRepository
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
    fun `should emit unsupported flow - paid user - when user has a subscription`() = runTest {
        // Given
        expectPrimaryUser(paidUser)

        // When + Then
        viewModel.state.test {
            assertIs<OnboardingUpsellState.UnsupportedFlow>(awaitItem())
        }
    }

    @Test
    fun `should emit unsupported flow - mismatching plans - when dynamic plans are not as expected`() = runTest {
        // Given
        expectPrimaryUser(freeUser)
        expectDynamicPlansError(freeUser.userId, GetOnboardingPlansError.MismatchingPlans)

        // When + Then
        viewModel.state.test {
            assertIs<OnboardingUpsellState.UnsupportedFlow>(awaitItem())
        }
    }

    @Test
    fun `should emit unsupported flow - mismatching periods - when dynamic plans are not as expected`() = runTest {
        // Given
        expectPrimaryUser(freeUser)
        expectDynamicPlansError(freeUser.userId, GetOnboardingPlansError.MismatchingPlanCycles)

        // When + Then
        viewModel.state.test {
            assertIs<OnboardingUpsellState.UnsupportedFlow>(awaitItem())
        }
    }

    @Test
    fun `should emit unsupported flow - no plans - when dynamic plans are not as expected`() = runTest {
        // Given
        expectPrimaryUser(freeUser)
        expectDynamicPlansError(freeUser.userId, GetOnboardingPlansError.NoPlans)

        // When + Then
        viewModel.state.test {
            assertIs<OnboardingUpsellState.UnsupportedFlow>(awaitItem())
        }
    }

    @Test
    fun `should emit unsupported flow when FF is disabled`() = runTest {
        // Given
        every { isFeatureEnabled.get() } returns false
        expectPrimaryUser(freeUser)

        // When + Then
        viewModel.state.test {
            assertIs<OnboardingUpsellState.UnsupportedFlow>(awaitItem())
        }
    }

    @Test
    fun `should emit data when dynamic plans are fetched`() = runTest {
        // Given
        expectPrimaryUser(freeUser)
        expectDynamicPlans(freeUser.userId, dynamicPlans)

        // When + Then
        viewModel.state.test {
            assertIs<OnboardingUpsellState.Data>(awaitItem())
        }
    }

    @Test
    fun `should emit correct state when PlanSelected action is submitted`() = runTest {
        // Given
        expectPrimaryUser(freeUser)
        expectDynamicPlans(freeUser.userId, dynamicPlans)
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

    @Test
    fun `should call the telemetry repository when tracking an upgrade attempted event`() = runTest {
        expectPrimaryUser(freeUser)
        expectDynamicPlans(freeUser.userId, dynamicPlans)

        val payload = UpsellingTelemetryTargetPlanPayload(
            "plan", 1,
            isPromotional = false, isVariantB = false, isSocialProofVariant = false
        )
        val eventType = UpsellingTelemetryEventType.Upgrade.UpgradeAttempt(payload)

        // When
        viewModel.submit(OnboardingUpsellOperation.Action.TrackEvent.UpgradeAttempt(payload))

        // Then
        verify(exactly = 1) {
            upsellingTelemetryRepository.trackEvent(eventType = eventType, UpsellingEntryPoint.PostOnboarding)
        }
        confirmVerified(upsellingTelemetryRepository)
    }

    @Test
    fun `should call the telemetry repository when tracking an upgrade cancelled event`() = runTest {
        expectPrimaryUser(freeUser)
        expectDynamicPlans(freeUser.userId, dynamicPlans)

        val payload = UpsellingTelemetryTargetPlanPayload(
            "plan", 1,
            isPromotional = false, isVariantB = false, isSocialProofVariant = false
        )
        val eventType = UpsellingTelemetryEventType.Upgrade.UpgradeCancelled(payload)

        // When
        viewModel.submit(OnboardingUpsellOperation.Action.TrackEvent.UpgradeCancelled(payload))

        // Then
        verify(exactly = 1) {
            upsellingTelemetryRepository.trackEvent(eventType = eventType, UpsellingEntryPoint.PostOnboarding)
        }
        confirmVerified(upsellingTelemetryRepository)
    }

    @Test
    fun `should call the telemetry repository when tracking an upgrade errored event`() = runTest {
        expectPrimaryUser(freeUser)
        expectDynamicPlans(freeUser.userId, dynamicPlans)

        val payload = UpsellingTelemetryTargetPlanPayload(
            "plan", 1,
            isPromotional = false, isVariantB = false, isSocialProofVariant = false
        )
        val eventType = UpsellingTelemetryEventType.Upgrade.UpgradeErrored(payload)

        // When
        viewModel.submit(OnboardingUpsellOperation.Action.TrackEvent.UpgradeErrored(payload))

        // Then
        verify(exactly = 1) {
            upsellingTelemetryRepository.trackEvent(eventType = eventType, UpsellingEntryPoint.PostOnboarding)
        }
        confirmVerified(upsellingTelemetryRepository)
    }

    @Test
    fun `should call the telemetry repository when tracking an upgrade success event`() = runTest {
        expectPrimaryUser(freeUser)
        expectDynamicPlans(freeUser.userId, dynamicPlans)

        val payload = UpsellingTelemetryTargetPlanPayload(
            "plan", 1,
            isPromotional = false, isVariantB = false, isSocialProofVariant = false
        )
        val eventType = UpsellingTelemetryEventType.Upgrade.PurchaseCompleted(payload)

        // When
        viewModel.submit(OnboardingUpsellOperation.Action.TrackEvent.UpgradeSuccess(payload))

        // Then
        verify(exactly = 1) {
            upsellingTelemetryRepository.trackEvent(eventType = eventType, UpsellingEntryPoint.PostOnboarding)
        }
        confirmVerified(upsellingTelemetryRepository)
    }

    private fun expectPrimaryUser(user: User?) {
        every { observePrimaryUser() } returns flowOf(user)
    }

    private fun expectDynamicPlans(userId: UserId, dynamicPlans: DynamicPlans) {
        coEvery { getOnboardingUpsellingPlans(userId) } returns dynamicPlans.right()
    }

    private fun expectDynamicPlansError(userId: UserId, error: GetOnboardingPlansError) {
        coEvery { getOnboardingUpsellingPlans(userId) } returns error.left()
    }
}
