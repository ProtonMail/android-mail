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
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import me.proton.core.network.domain.NetworkManager
import me.proton.core.network.domain.NetworkStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.BeforeTest

class HomeViewModelTest {

    private val networkManager = mockk<NetworkManager>()

    private val homeViewModel by lazy {
        HomeViewModel(networkManager)
    }

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @Test
    fun `when initialized then emit initial value `() =
        runTest {
            // Given
            every { networkManager.observe() } returns emptyFlow()

            // When
            homeViewModel.state.test {
                val actualItem = awaitItem()
                val expectedItem = NetworkStatus.Unmetered

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
                val expectedItem = NetworkStatus.Disconnected

                // Then
                assertEquals(expectedItem, actualItem)
            }
        }

    @Test
    fun `when the status is disconnected and is metered after 5 seconds then emit metered status`() =
        runTest {
            // Given
            every { networkManager.observe() } returns flowOf(NetworkStatus.Disconnected)
            every { networkManager.networkStatus } returns NetworkStatus.Metered

            // When
            homeViewModel.state.test {
                awaitItem()
                advanceUntilIdle()
                val actualItem = awaitItem()
                val expectedItem = NetworkStatus.Metered

                // Then
                assertEquals(expectedItem, actualItem)
            }
        }

    @Test
    fun `when the status is metered then emit metered status`() =
        runTest {
            // Given
            every { networkManager.observe() } returns flowOf(NetworkStatus.Metered)

            // When
            homeViewModel.state.test {
                val actualItem = awaitItem()
                val expectedItem = NetworkStatus.Metered

                // Then
                assertEquals(expectedItem, actualItem)
            }
        }
}
