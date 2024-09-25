/*
 * Copyright (c) 2022 Proton Technologies AG
 * This file is part of Proton Technologies AG and Proton Mail.
 *
 * Proton Mail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Mail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Mail. If not, see <https://www.gnu.org/licenses/>.
 */

package ch.protonmail.upselling.domain.usecase

import ch.protonmail.android.mailcommon.domain.sample.UserSample
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailupselling.domain.model.UserUpgradeState
import ch.protonmail.android.mailupselling.domain.usecase.ObserveUserSubscriptionUpgrade
import ch.protonmail.upselling.domain.UpsellingTestData
import ch.protonmail.upselling.domain.UpsellingTestData.BasePurchase
import io.mockk.called
import io.mockk.coEvery
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import io.mockk.verifyOrder
import io.mockk.verifySequence
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import me.proton.core.accountmanager.domain.SessionManager
import me.proton.core.network.domain.session.SessionId
import me.proton.core.payment.domain.PurchaseManager
import me.proton.core.payment.domain.entity.PurchaseState
import me.proton.core.user.domain.UserManager
import org.junit.After
import org.junit.Before
import kotlin.test.Test

@ExperimentalCoroutinesApi
class ObserveUserSubscriptionUpgradeTest {

    private val userManager = mockk<UserManager>()
    private val observePrimaryUserId = mockk<ObservePrimaryUserId>()
    private val sessionManager = mockk<SessionManager>()
    private val purchaseManager = mockk<PurchaseManager>()
    private val userUpgradeState = mockk<UserUpgradeState>()

    private val testDispatcher: TestDispatcher by lazy {
        StandardTestDispatcher()
    }

    private val coroutineScope = CoroutineScope(SupervisorJob() + testDispatcher)

    private val observeUserSubscriptionUpgrade by lazy {
        ObserveUserSubscriptionUpgrade(
            userManager,
            observePrimaryUserId,
            sessionManager,
            purchaseManager,
            userUpgradeState,
            coroutineScope
        )
    }

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `should do nothing when the user already has a mail subscription`() = runTest {
        // Given
        val user = UpsellingTestData.userId

        every { observePrimaryUserId.invoke() } returns flowOf(user)
        coEvery { userManager.getUser(user) } returns UserSample.Primary.copy(subscribed = 1)


        // When
        observeUserSubscriptionUpgrade.start()

        // Then
        verify(exactly = 1) {
            sessionManager wasNot called
            purchaseManager wasNot called
            userUpgradeState wasNot called
        }
    }

    @Test
    fun `should set the state as Not Pending when the user has no purchases`() = runTest {
        // Given
        val user = UpsellingTestData.userId

        every { observePrimaryUserId.invoke() } returns flowOf(user)
        coEvery { userManager.getUser(user) } returns UserSample.Primary.copy(subscribed = 0)
        coEvery { sessionManager.getSessionId(user) } returns SessionId(user.id)
        every { purchaseManager.observePurchases() } returns flowOf(emptyList())
        every { userUpgradeState.updateState(UserUpgradeState.UserUpgradeCheckState.Completed) } just runs

        // When
        observeUserSubscriptionUpgrade.start()

        // Then
        verify(exactly = 1) { userUpgradeState.updateState(UserUpgradeState.UserUpgradeCheckState.Completed) }
        confirmVerified(userUpgradeState)
    }

    @Test
    fun `should set the state as Not Pending when the user has no eligible purchases to trigger the check`() = runTest {
        // Given
        val user = UpsellingTestData.userId
        val purchases = listOf(
            BasePurchase.copy(purchaseState = PurchaseState.Deleted, purchaseFailure = "dummy message")
        )

        every { observePrimaryUserId.invoke() } returns flowOf(user)
        coEvery { userManager.getUser(user) } returns UserSample.Primary.copy(subscribed = 0)
        coEvery { sessionManager.getSessionId(user) } returns SessionId(user.id)
        every { purchaseManager.observePurchases() } returns flowOf(purchases)
        every { userUpgradeState.updateState(UserUpgradeState.UserUpgradeCheckState.Completed) } just runs

        // When
        observeUserSubscriptionUpgrade.start()

        // Then
        verify(exactly = 1) { userUpgradeState.updateState(UserUpgradeState.UserUpgradeCheckState.Completed) }
        confirmVerified(userUpgradeState)
    }

    @Test
    fun `should update state to Pending with pending purchases and user has no mail subscription`() = runTest {
        // Given
        val user = UpsellingTestData.userId
        val purchases = listOf(
            BasePurchase.copy(purchaseState = PurchaseState.Pending),
            BasePurchase.copy(purchaseState = PurchaseState.Subscribed)
        )

        every { observePrimaryUserId.invoke() } returns flowOf(user)
        coEvery { sessionManager.getSessionId(user) } returns SessionId(user.id)
        every { purchaseManager.observePurchases() } returns flowOf(purchases)
        coEvery { userManager.getUser(user) } returns UserSample.Primary.copy(subscribed = 0)
        every { userUpgradeState.updateState(UserUpgradeState.UserUpgradeCheckState.Pending) } just runs

        // When
        observeUserSubscriptionUpgrade.start()

        // Then
        verify(exactly = 1) { userUpgradeState.updateState(UserUpgradeState.UserUpgradeCheckState.Pending) }
        confirmVerified(userUpgradeState)
    }

    @Test
    fun `should stop and start observation when there are acknowledged or deleted purchases`() = runTest {
        // Given
        val user = UpsellingTestData.userId
        val purchases = listOf(
            BasePurchase.copy(purchaseState = PurchaseState.Acknowledged),
            BasePurchase.copy(purchaseState = PurchaseState.Deleted, purchaseFailure = null)
        )

        every { observePrimaryUserId.invoke() } returns flowOf(user)
        coEvery { sessionManager.getSessionId(user) } returns SessionId(user.id)
        every { purchaseManager.observePurchases() } returns flowOf(purchases)
        coEvery { userManager.getUser(user) } returns UserSample.Primary.copy(subscribed = 0)
        coEvery { userManager.observeUser(user) } returns flowOf(UserSample.Primary.copy(subscribed = 0))
        every { userUpgradeState.updateState(UserUpgradeState.UserUpgradeCheckState.Initial) } just runs
        every { userUpgradeState.updateState(UserUpgradeState.UserUpgradeCheckState.Pending) } just runs

        // When
        observeUserSubscriptionUpgrade.start()

        // Then
        verifyOrder {
            userUpgradeState.updateState(UserUpgradeState.UserUpgradeCheckState.Initial)
            userUpgradeState.updateState(UserUpgradeState.UserUpgradeCheckState.Pending)
        }

        confirmVerified(userUpgradeState)
    }

    @Test
    fun `should stop observing as soon as user has mail subscription`() = runTest {
        // Given
        val user = UpsellingTestData.userId
        val purchases = listOf(
            BasePurchase.copy(purchaseState = PurchaseState.Acknowledged),
            BasePurchase.copy(purchaseState = PurchaseState.Deleted, purchaseFailure = null)
        )

        every { observePrimaryUserId.invoke() } returns flowOf(user)
        coEvery { sessionManager.getSessionId(user) } returns SessionId(user.id)
        every { purchaseManager.observePurchases() } returns flowOf(purchases)
        coEvery { userManager.getUser(user) } returns UserSample.Primary.copy(subscribed = 0)
        coEvery { userManager.observeUser(user) } coAnswers {
            flowOf(UserSample.Primary.copy(subscribed = 1))
        }

        every { userUpgradeState.updateState(UserUpgradeState.UserUpgradeCheckState.Initial) } just runs
        every { userUpgradeState.updateState(UserUpgradeState.UserUpgradeCheckState.Pending) } just runs
        every { userUpgradeState.updateState(UserUpgradeState.UserUpgradeCheckState.Completed) } just runs

        // When
        observeUserSubscriptionUpgrade.start()
        advanceUntilIdle()

        // Then
        verifySequence {
            userUpgradeState.updateState(UserUpgradeState.UserUpgradeCheckState.Initial)
            userUpgradeState.updateState(UserUpgradeState.UserUpgradeCheckState.Pending)
            userUpgradeState.updateState(UserUpgradeState.UserUpgradeCheckState.Completed)
        }

        confirmVerified(userUpgradeState)
    }
}

