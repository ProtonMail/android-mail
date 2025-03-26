package ch.protonmail.android.feature.postsubscription

import androidx.appcompat.app.AppCompatActivity
import ch.protonmail.android.mailcommon.domain.sample.UserSample
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUser
import ch.protonmail.android.mailupselling.domain.model.UserUpgradeState
import ch.protonmail.android.testdata.user.UserIdTestData
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import me.proton.core.featureflag.domain.entity.FeatureFlag
import me.proton.core.featureflag.domain.entity.FeatureId
import me.proton.core.featureflag.domain.entity.Scope
import me.proton.core.user.domain.entity.User
import org.junit.After
import org.junit.Before
import kotlin.test.Test

class ObservePostSubscriptionTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val observePostSubscriptionFlowEnabled = mockk<ObservePostSubscriptionFlowEnabled> {
        every { this@mockk.invoke(FreeUser.userId) } returns flowOf(
            FeatureFlag(
                userId = UserIdTestData.userId,
                featureId = FeatureId(""),
                scope = Scope.Unleash,
                defaultValue = false,
                value = true
            )
        )
    }
    private val observePrimaryUser = mockk<ObservePrimaryUser>()
    private val userUpgradeState = mockk<UserUpgradeState>()

    private val observePostSubscription = ObservePostSubscription(
        observePostSubscriptionFlowEnabled = observePostSubscriptionFlowEnabled,
        observePrimaryUser = observePrimaryUser,
        userUpgradeState = userUpgradeState
    )

    private val mockActivity = mockk<AppCompatActivity>(relaxUnitFun = true)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `upon a paid user, cancel further calls`() = runTest {
        // Given
        val activity = mockActivity
        expectPaidUser()

        // When
        observePostSubscription.start(activity)

        // Then
        coVerify(exactly = 0) { observePostSubscriptionFlowEnabled(any()) }

    }

    @Test
    fun `upon a free user without valid purchases, do not show post-sub activity`() = runTest {
        // Given
        val activity = mockActivity
        expectFreeUser()
        expectUpgradeCheckStates(flowOf(UserUpgradeState.UserUpgradeCheckState.Completed))

        // When
        observePostSubscription.start(activity)

        // Then
        coVerify(exactly = 1) { observePostSubscriptionFlowEnabled(any()) }
        verify(exactly = 0) { activity.startActivity(any()) }
    }

    @Test
    fun `upon a free user with valid purchases for Mail Plus, show post-sub activity`() = runTest {
        // Given
        val activity = mockActivity
        expectFreeUser()
        expectUpgradeCheckStates(
            flowOf(
                UserUpgradeState.UserUpgradeCheckState.Pending,
                UserUpgradeState.UserUpgradeCheckState.CompletedWithUpgrade(listOf(MailPlusPlanName))
            )
        )

        // When
        observePostSubscription.start(activity)

        // Then
        coVerify(exactly = 1) { observePostSubscriptionFlowEnabled(any()) }
        verify(exactly = 1) { activity.startActivity(any()) }
    }

    @Test
    fun `upon a free user with valid purchases for a different plan, don't show post-sub activity`() = runTest {
        // Given
        val activity = mockActivity
        expectFreeUser()
        expectUpgradeCheckStates(
            flowOf(
                UserUpgradeState.UserUpgradeCheckState.Pending,
                UserUpgradeState.UserUpgradeCheckState.CompletedWithUpgrade(listOf(OtherPlanName))
            )
        )

        // When
        observePostSubscription.start(activity)

        // Then
        coVerify(exactly = 1) { observePostSubscriptionFlowEnabled(any()) }
        verify(exactly = 0) { activity.startActivity(any()) }
    }

    @Test
    fun `upon a free user with pending purchases but no ack, don't show post-sub activity`() = runTest {
        // Given
        val activity = mockActivity
        expectFreeUser()
        expectUpgradeCheckStates(
            flowOf(
                UserUpgradeState.UserUpgradeCheckState.Pending,
                UserUpgradeState.UserUpgradeCheckState.Completed
            )
        )

        // When
        observePostSubscription.start(activity)

        // Then
        coVerify(exactly = 1) { observePostSubscriptionFlowEnabled(any()) }
        verify(exactly = 0) { activity.startActivity(any()) }
    }

    @Test
    fun `upon a free user with pending purchases, show post-sub activity upon a new paid user emission`() = runTest {
        // Given
        val activity = mockActivity
        expectUsersFlow(
            flow {
                emit(FreeUser)
                delay(2000)
                emit(PaidUser)
            }
        )
        expectUpgradeCheckStates(
            flow {
                emit(UserUpgradeState.UserUpgradeCheckState.Pending)
                emit(UserUpgradeState.UserUpgradeCheckState.CompletedWithUpgrade(listOf(MailPlusPlanName)))
            }
        )

        // When
        observePostSubscription.start(activity)

        // Then
        coVerify(exactly = 1) { observePostSubscriptionFlowEnabled(any()) }
        verify(exactly = 1) { activity.startActivity(any()) }
    }

    private fun expectFreeUser() {
        every { observePrimaryUser() } returns flowOf(FreeUser)
    }

    private fun expectPaidUser() {
        every { observePrimaryUser() } returns flowOf(PaidUser)
    }

    private fun expectUsersFlow(flow: Flow<User>) {
        every { observePrimaryUser() } returns flow
    }

    private fun expectUpgradeCheckStates(flow: Flow<UserUpgradeState.UserUpgradeCheckState>) {
        coEvery { userUpgradeState.userUpgradeCheckState } coAnswers { flow }
    }

    companion object {

        private const val OtherPlanName = "plan-123"
        private const val MailPlusPlanName = "mail2022"

        private val PaidUser = UserSample.Primary.copy(subscribed = 1)
        private val FreeUser = UserSample.Primary.copy(subscribed = 0)
    }
}
