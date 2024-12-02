package ch.protonmail.android.feature.postsubscription

import androidx.appcompat.app.AppCompatActivity
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.testdata.user.UserIdTestData
import ch.protonmail.android.testdata.user.UserTestData
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import me.proton.core.accountmanager.domain.SessionManager
import me.proton.core.featureflag.domain.entity.FeatureFlag
import me.proton.core.featureflag.domain.entity.FeatureId
import me.proton.core.featureflag.domain.entity.Scope
import me.proton.core.network.domain.session.SessionId
import me.proton.core.payment.domain.PurchaseManager
import me.proton.core.payment.domain.entity.Purchase
import me.proton.core.payment.domain.entity.PurchaseState
import me.proton.core.user.domain.UserManager
import kotlin.test.Test

class ObservePostSubscriptionTest {

    private val basePurchase = Purchase(
        sessionId = SessionId(UserIdTestData.userId.id),
        planName = "mail2022",
        planCycle = 1,
        purchaseState = PurchaseState.Acknowledged,
        purchaseFailure = null,
        paymentProvider = mockk(),
        paymentOrderId = null,
        paymentToken = null,
        paymentCurrency = mockk(),
        paymentAmount = 1L
    )

    private val testDispatcher = StandardTestDispatcher()
    private val coroutineScope = TestScope(testDispatcher)
    private val observePostSubscriptionFlowEnabled = mockk<ObservePostSubscriptionFlowEnabled> {
        every { this@mockk.invoke(UserIdTestData.userId) } returns flowOf(
            FeatureFlag(
                userId = UserIdTestData.userId,
                featureId = FeatureId(""),
                scope = Scope.Unleash,
                defaultValue = false,
                value = true
            )
        )
    }
    private val observePrimaryUserId = mockk<ObservePrimaryUserId> {
        every { this@mockk.invoke() } returns flowOf(UserIdTestData.userId)
    }
    private val purchaseManager = mockk<PurchaseManager>()
    private val sessionManager = mockk<SessionManager> {
        coEvery { getSessionId(UserIdTestData.userId) } returns SessionId(UserIdTestData.userId.id)
    }
    private val userManager = mockk<UserManager> {
        coEvery { getUser(UserIdTestData.userId) } returns UserTestData.freeUser
    }

    private val observePostSubscription = ObservePostSubscription(
        observePostSubscriptionFlowEnabled = observePostSubscriptionFlowEnabled,
        observePrimaryUserId = observePrimaryUserId,
        purchaseManager = purchaseManager,
        sessionManager = sessionManager,
        userManager = userManager
    )

    @Test
    fun `when purchase was acknowledged then start the post subscription activity`() {
        // Given
        val activity = mockk<AppCompatActivity>(relaxUnitFun = true)
        every {
            purchaseManager.observePurchases()
        } returns flowOf(listOf(basePurchase.copy(purchaseState = PurchaseState.Acknowledged)))

        // When
        observePostSubscription.start(activity, coroutineScope)
        coroutineScope.advanceUntilIdle()

        // Then
        verify { activity.startActivity(any()) }
    }

    @Test
    fun `when purchase was not acknowledged then don't start the post subscription activity`() {
        // Given
        val activity = mockk<AppCompatActivity>(relaxUnitFun = true)
        every {
            purchaseManager.observePurchases()
        } returns flowOf(listOf(basePurchase.copy(purchaseState = PurchaseState.Pending)))

        // When
        observePostSubscription.start(activity, coroutineScope)
        coroutineScope.advanceUntilIdle()

        // Then
        verify(exactly = 0) { activity.startActivity(any()) }
    }
}
