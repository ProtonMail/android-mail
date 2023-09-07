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
import ch.protonmail.android.mailcomposer.domain.model.MessageSendingStatus
import ch.protonmail.android.mailcomposer.domain.usecase.ObserveSendingMessagesStatus
import ch.protonmail.android.mailcomposer.domain.usecase.ResetSendingMessagesStatus
import ch.protonmail.android.navigation.model.HomeState
import io.mockk.coVerify
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

    private val observeSendingMessagesStatus = mockk<ObserveSendingMessagesStatus> {
        every { this@mockk.invoke(any()) } returns flowOf(MessageSendingStatus.None)
    }

    private val resetSendingMessageStatus = mockk<ResetSendingMessagesStatus>(relaxUnitFun = true)

    private val homeViewModel by lazy {
        HomeViewModel(
            networkManager,
            observeSendingMessagesStatus,
            resetSendingMessageStatus,
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
    fun `when observe sending message status emits Send and then None then emit only send effect`() = runTest {
        // Given
        every { networkManager.observe() } returns flowOf(NetworkStatus.Metered)
        val sendingMessageStatusFlow = MutableStateFlow<MessageSendingStatus>(MessageSendingStatus.MessageSent)
        every { observeSendingMessagesStatus(user.userId) } returns sendingMessageStatusFlow

        // When
        homeViewModel.state.test {
            val actualItem = awaitItem()
            val expectedItem = HomeState(
                networkStatusEffect = Effect.of(NetworkStatus.Metered),
                messageSendingStatusEffect = Effect.of(MessageSendingStatus.MessageSent)
            )
            sendingMessageStatusFlow.emit(MessageSendingStatus.None)

            // Then
            assertEquals(expectedItem, actualItem)
        }
    }

    @Test
    fun `when observe sending message status emits error then emit effect and reset sending messages status`() =
        runTest {
            // Given
            every { networkManager.observe() } returns flowOf(NetworkStatus.Metered)
            every { observeSendingMessagesStatus(user.userId) } returns flowOf(
                MessageSendingStatus.SendMessageError
            )

            // When
            homeViewModel.state.test {
                val actualItem = awaitItem()
                val expectedItem = HomeState(
                    networkStatusEffect = Effect.of(NetworkStatus.Metered),
                    messageSendingStatusEffect = Effect.of(MessageSendingStatus.SendMessageError)
                )

                // Then
                assertEquals(expectedItem, actualItem)
                coVerify { resetSendingMessageStatus(user.userId) }
            }
        }


}
