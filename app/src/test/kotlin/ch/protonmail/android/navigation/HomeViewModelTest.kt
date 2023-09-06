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

package ch.protonmail.android.navigation

import app.cash.turbine.test
import ch.protonmail.android.mailcommon.domain.sample.UserSample
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUser
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcomposer.domain.usecase.DeleteDraftState
import ch.protonmail.android.mailcomposer.domain.usecase.ObserveSendingDraftStates
import ch.protonmail.android.mailcomposer.domain.usecase.ResetDraftStateError
import ch.protonmail.android.mailcomposer.domain.sample.DraftStateSample
import ch.protonmail.android.mailcomposer.presentation.model.MessageSendingUiModel.MessageSent
import ch.protonmail.android.mailcomposer.presentation.model.MessageSendingUiModel.SendMessageError
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import ch.protonmail.android.navigation.model.HomeAction
import ch.protonmail.android.navigation.model.HomeState
import io.mockk.coJustRun
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import me.proton.core.network.domain.NetworkManager
import me.proton.core.network.domain.NetworkStatus
import me.proton.core.user.domain.entity.User
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class HomeViewModelTest {

    private val user = UserSample.Primary

    private val networkManager = mockk<NetworkManager>()

    private val observePrimaryUserMock = mockk<ObservePrimaryUser> {
        every { this@mockk() } returns MutableStateFlow<User?>(user)
    }

    private val observeSendingDraftStates = mockk<ObserveSendingDraftStates> {
        every { this@mockk.invoke(any()) } returns flowOf(emptyList())
    }

    private val resetDraftErrorState = mockk<ResetDraftStateError> {
        coJustRun { this@mockk.invoke(user.userId, any()) }
    }

    private val deleteDraftState = mockk<DeleteDraftState> {
        coJustRun { this@mockk.invoke(user.userId, any()) }
    }

    private val homeViewModel by lazy {
        HomeViewModel(
            networkManager,
            observeSendingDraftStates,
            resetDraftErrorState,
            deleteDraftState,
            observePrimaryUserMock
        )
    }

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @Test
    fun `when initialized then emit initial state`() = runTest {
        // Given
        every { networkManager.observe() } returns emptyFlow()

        // When
        homeViewModel.state.test {
            val actualItem = awaitItem()
            val expectedItem = HomeState.Initial

            // Then
            assertEquals(expectedItem, actualItem)
        }
    }

    @Test
    fun `when the status is disconnected and is still disconnected after 5 seconds then emit disconnected status`() =
        runTest {
            // Given
            every { networkManager.observe() } returns flowOf(NetworkStatus.Disconnected)
            every { networkManager.networkStatus } returns NetworkStatus.Disconnected

            // When
            homeViewModel.state.test {
                awaitItem()
                advanceUntilIdle()
                val actualItem = awaitItem()
                val expectedItem = HomeState(
                    networkStatusEffect = Effect.of(NetworkStatus.Disconnected),
                    messageSendingStatusEffect = Effect.empty()
                )

                // Then
                assertEquals(expectedItem, actualItem)
            }
        }

    @Test
    fun `when the status is disconnected and is metered after 5 seconds then emit metered status`() = runTest {
        // Given
        every { networkManager.observe() } returns flowOf(NetworkStatus.Disconnected)
        every { networkManager.networkStatus } returns NetworkStatus.Metered

        // When
        homeViewModel.state.test {
            awaitItem()
            advanceUntilIdle()
            val actualItem = awaitItem()
            val expectedItem = HomeState(
                networkStatusEffect = Effect.of(NetworkStatus.Metered),
                messageSendingStatusEffect = Effect.empty()
            )

            // Then
            assertEquals(expectedItem, actualItem)
        }
    }

    @Test
    fun `when the status is metered then emit metered status`() = runTest {
        // Given
        every { networkManager.observe() } returns flowOf(NetworkStatus.Metered)

        // When
        homeViewModel.state.test {
            val actualItem = awaitItem()
            val expectedItem = HomeState(
                networkStatusEffect = Effect.of(NetworkStatus.Metered),
                messageSendingStatusEffect = Effect.empty()
            )

            // Then
            assertEquals(expectedItem, actualItem)
        }
    }

    @Test
    fun `when there are draft that failed sending then emit error sending messages effect`() = runTest {
        // Given
        val errorSendingDraftState = DraftStateSample.RemoteDraftInErrorSendingState
        val expected = SendMessageError(errorSendingDraftState.userId, listOf(errorSendingDraftState.messageId))
        every { networkManager.observe() } returns flowOf(NetworkStatus.Metered)
        every { observeSendingDraftStates.invoke(user.userId) } returns flowOf(
            listOf(DraftStateSample.RemoteDraftInSendingState, errorSendingDraftState)
        )

        // When
        homeViewModel.state.test {
            val actualItem = awaitItem()
            val expectedItem = HomeState(
                networkStatusEffect = Effect.of(NetworkStatus.Metered),
                messageSendingStatusEffect = Effect.of(expected)
            )

            // Then
            assertEquals(expectedItem, actualItem)
        }
    }

    @Test
    fun `when there are draft that succeeded sending then emit messages sent effect`() = runTest {
        // Given
        val sentDraftState = DraftStateSample.RemoteDraftInSentState
        val expected = MessageSent(sentDraftState.userId, listOf(sentDraftState.messageId))
        every { networkManager.observe() } returns flowOf(NetworkStatus.Metered)
        every { observeSendingDraftStates.invoke(user.userId) } returns flowOf(
            listOf(DraftStateSample.RemoteDraftInSendingState, sentDraftState)
        )

        // When
        homeViewModel.state.test {
            val actualItem = awaitItem()
            val expectedItem = HomeState(
                networkStatusEffect = Effect.of(NetworkStatus.Metered),
                messageSendingStatusEffect = Effect.of(expected)
            )

            // Then
            assertEquals(expectedItem, actualItem)
        }
    }

    @Test
    fun `when there are both failed and succeeded draft states then emit error sending message effect`() = runTest {
        // Given
        val sentDraftState = DraftStateSample.RemoteDraftInSentState
        val errorSendingDraftState = DraftStateSample.RemoteDraftInErrorSendingState
        val expected = SendMessageError(sentDraftState.userId, listOf(errorSendingDraftState.messageId))
        every { networkManager.observe() } returns flowOf(NetworkStatus.Metered)
        every { observeSendingDraftStates.invoke(user.userId) } returns flowOf(
            listOf(DraftStateSample.RemoteDraftInSendingState, sentDraftState, errorSendingDraftState)
        )

        // When
        homeViewModel.state.test {
            val actualItem = awaitItem()
            val expectedItem = HomeState(
                networkStatusEffect = Effect.of(NetworkStatus.Metered),
                messageSendingStatusEffect = Effect.of(expected)
            )

            // Then
            assertEquals(expectedItem, actualItem)
        }
    }

    @Test
    fun `when there are no ErrorSending or Sent draft states then emit empty sending status effect`() = runTest {
        // Given
        val allDraftStates = listOf(
            DraftStateSample.RemoteDraftInSendingState,
            DraftStateSample.RemoteDraftState
        )
        every { networkManager.observe() } returns flowOf(NetworkStatus.Metered)
        every { observeSendingDraftStates(user.userId) } returns flowOf(allDraftStates)

        // When
        homeViewModel.state.test {
            val actualItem = awaitItem()
            val expectedItem = HomeState(
                networkStatusEffect = Effect.of(NetworkStatus.Metered),
                messageSendingStatusEffect = Effect.empty()
            )

            // Then
            assertEquals(expectedItem, actualItem)
        }
    }

    @Test
    fun `when submit message sending error shown action then draft states are updated`() = runTest {
        // Given
        val userId = user.userId
        val messageIds = listOf(MessageIdSample.NewDraftWithSubject, MessageIdSample.NewDraftWithSubjectAndBody)
        val action = HomeAction.MessageSendingErrorShown(SendMessageError(userId, messageIds))
        coJustRun { resetDraftErrorState(userId, messageIds[0]) }
        coJustRun { resetDraftErrorState(userId, messageIds[1]) }

        // When
        homeViewModel.submit(action)

        // Then
        coVerifyOrder {
            resetDraftErrorState(userId, messageIds[0])
            resetDraftErrorState(userId, messageIds[1])
        }
    }

    @Test
    fun `when submit message sent shown action then draft states are deleted`() = runTest {
        // Given
        val userId = user.userId
        val messageIds = listOf(MessageIdSample.NewDraftWithSubject, MessageIdSample.NewDraftWithSubjectAndBody)
        val action = HomeAction.MessageSentShown(MessageSent(userId, messageIds))
        coJustRun { deleteDraftState(userId, messageIds[0]) }
        coJustRun { deleteDraftState(userId, messageIds[1]) }

        // When
        homeViewModel.submit(action)

        // Then
        coVerifyOrder {
            deleteDraftState(userId, messageIds[0])
            deleteDraftState(userId, messageIds[1])
        }
    }
}
