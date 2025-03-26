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
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUser
import ch.protonmail.android.mailupselling.domain.model.UserUpgradeState
import ch.protonmail.android.mailupselling.domain.model.UserUpgradeState.UserUpgradeCheckState.Completed
import ch.protonmail.android.mailupselling.domain.model.UserUpgradeState.UserUpgradeCheckState.CompletedWithUpgrade
import ch.protonmail.android.mailupselling.domain.model.UserUpgradeState.UserUpgradeCheckState.Pending
import ch.protonmail.android.mailupselling.domain.usecase.CurrentPurchasesState
import ch.protonmail.android.mailupselling.domain.usecase.ObserveCurrentPurchasesState
import ch.protonmail.android.mailupselling.domain.usecase.ObserveUserSubscriptionUpgrade
import io.mockk.called
import io.mockk.coEvery
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import me.proton.core.accountmanager.domain.SessionManager
import me.proton.core.network.domain.session.SessionId
import me.proton.core.user.domain.entity.User
import org.junit.After
import org.junit.Before
import kotlin.test.Test

@ExperimentalCoroutinesApi
class ObserveUserSubscriptionUpgradeTest {

    private val observePrimaryUser = mockk<ObservePrimaryUser>()
    private val sessionManager = mockk<SessionManager>()
    private val userUpgradeState = mockk<UserUpgradeState>()
    private val observeCurrentPurchasesState = mockk<ObserveCurrentPurchasesState>()

    private val testDispatcher: TestDispatcher by lazy {
        StandardTestDispatcher()
    }

    private val observeUserSubscriptionUpgrade by lazy {
        ObserveUserSubscriptionUpgrade(
            observePrimaryUser,
            sessionManager,
            userUpgradeState,
            observeCurrentPurchasesState
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
    fun `should only reset and mark complete when the user already has a mail subscription`() = runTest {
        // Given
        expectPaidUser()

        // When
        observeUserSubscriptionUpgrade.start()

        // Then
        verify(exactly = 1) {
            sessionManager wasNot called
            observeCurrentPurchasesState wasNot called
        }
        verify(exactly = 1) { userUpgradeState.updateState(CompletedWithUpgrade(emptyList())) }
    }

    @Test
    fun `should set the state as Completed when the user has no applicable purchases`() = runTest {
        // Given
        expectFreeUser()
        expectPurchaseStates(CurrentPurchasesState.NotApplicable)
        expectUpdateCheckRuns(Completed)

        // When
        observeUserSubscriptionUpgrade.start()

        // Then
        verify(exactly = 1) { userUpgradeState.updateState(Completed) }
        confirmVerified(userUpgradeState)
    }

    @Test
    fun `should update state to Pending with pending purchases and user has no mail subscription`() = runTest {
        // Given
        expectFreeUser()
        expectUpdateCheckRuns(Pending)
        expectPurchaseStates(CurrentPurchasesState.Pending)

        // When
        observeUserSubscriptionUpgrade.start()

        // Then
        verify(exactly = 1) { userUpgradeState.updateState(Pending) }
        confirmVerified(userUpgradeState)
    }

//
    @Test
    fun `should set to pending and then complete when there are deleted purchases`() = runTest {
        // Given
        expectFreeUser()
        expectPurchaseStates(CurrentPurchasesState.Deleted)
        expectUpdateCheckRuns(Pending)
        expectUpdateCheckRuns(Completed)

        // When
        observeUserSubscriptionUpgrade.start()

        // Then
        verifyOrder {
            userUpgradeState.updateState(Pending)
            userUpgradeState.updateState(Completed)
        }

        confirmVerified(userUpgradeState)
    }

    @Test
    fun `should set to complete with upgrade and then complete when there are acknowledged purchases`() = runTest {
        // Given
        expectFreeUser()
        expectPurchaseStates(CurrentPurchasesState.AcknowledgedOrSubscribed(listOf("plan-name")))
        expectUpdateCheckRuns(CompletedWithUpgrade(listOf("plan-name")))
        expectUpdateCheckRuns(Completed)

        // When
        observeUserSubscriptionUpgrade.start()

        // Then
        verifyOrder {
            userUpgradeState.updateState(CompletedWithUpgrade(listOf("plan-name")))
            userUpgradeState.updateState(Completed)
        }

        confirmVerified(userUpgradeState)
    }

    @Test
    fun `should set to complete with upgrade and finish observation upon a new paid user`() = runTest {
        // Given
        expectUsersFlow(
            flow {
                emit(FreeUser)
                delay(10_000)
                emit(PaidUser)
            }
        )

        expectPurchaseStates(CurrentPurchasesState.AcknowledgedOrSubscribed(listOf("plan-name")))
        expectUpdateCheckRuns(CompletedWithUpgrade(listOf("plan-name")))
        expectUpdateCheckRuns(CompletedWithUpgrade(emptyList()))

        // When
        observeUserSubscriptionUpgrade.start()

        // Then
        verifyOrder {
            userUpgradeState.updateState(CompletedWithUpgrade(listOf("plan-name")))
            userUpgradeState.updateState(CompletedWithUpgrade(emptyList()))
        }

        confirmVerified(userUpgradeState)
    }

    @Test
    fun `should set to complete and finish observation upon a new paid user`() = runTest {
        // Given
        expectUsersFlow(
            flow {
                emit(FreeUser)
                delay(10_000)
                emit(PaidUser)
            }
        )
        expectPurchaseStates(CurrentPurchasesState.Deleted)
        expectUpdateCheckRuns(Pending)
        expectUpdateCheckRuns(CompletedWithUpgrade(emptyList()))

        // When
        observeUserSubscriptionUpgrade.start()

        // Then
        verifyOrder {
            userUpgradeState.updateState(Pending)
            userUpgradeState.updateState(CompletedWithUpgrade(emptyList()))
        }

        confirmVerified(userUpgradeState)
    }

    @Test
    fun `should set to complete with upgrade and revert to complete if no new paid user`() = runTest {
        // Given
        expectUsersFlow(
            flow {
                emit(FreeUser)
            }
        )
        expectPurchaseStates(CurrentPurchasesState.AcknowledgedOrSubscribed(listOf("plan-name")))
        expectUpdateCheckRuns(CompletedWithUpgrade(listOf("plan-name")))
        expectUpdateCheckRuns(Completed)

        // When
        observeUserSubscriptionUpgrade.start()

        // Then
        verifyOrder {
            userUpgradeState.updateState(CompletedWithUpgrade(listOf("plan-name")))
            userUpgradeState.updateState(Completed)
        }

        confirmVerified(userUpgradeState)
    }

    @Test
    fun `should set to pending when there are deleted purchases and revert to complete if no new paid user`() =
        runTest {
            // Given
            expectUsersFlow(
                flow {
                    emit(FreeUser)
                }
            )
            expectPurchaseStates(CurrentPurchasesState.Deleted)
            expectUpdateCheckRuns(Pending)
            expectUpdateCheckRuns(Completed)

            // When
            observeUserSubscriptionUpgrade.start()

            // Then
            verifyOrder {
                userUpgradeState.updateState(Pending)
                userUpgradeState.updateState(Completed)
            }

            confirmVerified(userUpgradeState)
        }

    @Test
    fun `should update state corresponding to purchases states`() = runTest {
        // Given
        expectFreeUser()
        expectPurchaseStates(
            CurrentPurchasesState.NotApplicable,
            CurrentPurchasesState.Deleted,
            CurrentPurchasesState.NotApplicable,
            CurrentPurchasesState.AcknowledgedOrSubscribed(listOf("plan-name")),
            CurrentPurchasesState.NotApplicable
        )
        expectUpdateCheckRuns(Pending)
        expectUpdateCheckRuns(Completed)
        expectUpdateCheckRuns(CompletedWithUpgrade(listOf("plan-name")))

        // When
        observeUserSubscriptionUpgrade.start()

        // Then
        verifyOrder {
            userUpgradeState.updateState(Completed)
            userUpgradeState.updateState(Pending)
            userUpgradeState.updateState(Completed)
            userUpgradeState.updateState(CompletedWithUpgrade(listOf("plan-name")))
            userUpgradeState.updateState(Completed)
        }

        confirmVerified(userUpgradeState)
    }

    @Test
    fun `should set state to pending, then ignore further purcahses upon a new paid user`() = runTest {
        // Given
        expectUsersFlow(
            flow {
                emit(FreeUser)
                delay(10_000)
                emit(PaidUser)
            }
        )
        every { observeCurrentPurchasesState(SessionId(SessionId)) } returns flow {
            emit(CurrentPurchasesState.Pending)
            delay(15_000)
            emit(CurrentPurchasesState.NotApplicable)
        }
        expectUpdateCheckRuns(Pending)
        expectUpdateCheckRuns(CompletedWithUpgrade(emptyList()))

        // When
        observeUserSubscriptionUpgrade.start()

        // Then
        verifyOrder {
            userUpgradeState.updateState(Pending)
            userUpgradeState.updateState(CompletedWithUpgrade(emptyList()))
        }

        confirmVerified(userUpgradeState)
    }

    private fun expectFreeUser() {
        coEvery { observePrimaryUser() } returns flowOf(FreeUser)
        coEvery { sessionManager.getSessionId(FreeUser.userId) } returns SessionId(SessionId)
    }

    private fun expectPaidUser() {
        coEvery { observePrimaryUser() } returns flowOf(PaidUser)
        coEvery { userUpgradeState.updateState(any()) } just runs
    }

    private fun expectUsersFlow(user: Flow<User>) {
        coEvery { observePrimaryUser() } returns user
        coEvery { sessionManager.getSessionId(FreeUser.userId) } returns SessionId(SessionId)
        coEvery { sessionManager.getSessionId(PaidUser.userId) } returns SessionId(SessionId)
    }

    private fun expectUpdateCheckRuns(checkState: UserUpgradeState.UserUpgradeCheckState) {
        every { userUpgradeState.updateState(checkState) } just runs
    }

    private fun expectPurchaseStates(vararg states: CurrentPurchasesState) {
        every { observeCurrentPurchasesState(SessionId(SessionId)) } returns flowOf(*states)
    }

    companion object {
        private val PaidUser = UserSample.Primary.copy(subscribed = 1)
        private val FreeUser = UserSample.Primary.copy(subscribed = 0)
        private const val SessionId = "session-id"
    }
}

