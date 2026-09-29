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
import ch.protonmail.android.mailpinlock.domain.AutoLockCheckPendingState
import ch.protonmail.android.mailpinlock.domain.usecase.ShouldPresentPinInsertionScreen
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

internal class LauncherRouterViewModelTest {

    private val autoLockCheckPendingState = AutoLockCheckPendingState()
    private val shouldPresentPinInsertionScreen: ShouldPresentPinInsertionScreen = mockk()

    @Test
    fun `should proxy the value from the UC (true)`() = runTest {
        // Given
        val channel = Channel<Boolean>(Channel.UNLIMITED)
        every { shouldPresentPinInsertionScreen() } returns channel.receiveAsFlow()
        val viewModel = LauncherRouterViewModel(autoLockCheckPendingState, shouldPresentPinInsertionScreen)

        // When + Then
        viewModel.showLockScreenEvent.test {
            channel.send(true)
            awaitItem()
            ensureAllEventsConsumed()
        }
    }

    @Test
    fun `should not emit an event if the UC returns false`() = runTest {
        // Given
        val channel = Channel<Boolean>(Channel.UNLIMITED)
        every { shouldPresentPinInsertionScreen() } returns channel.receiveAsFlow()
        val viewModel = LauncherRouterViewModel(autoLockCheckPendingState, shouldPresentPinInsertionScreen)

        // When + Then
        viewModel.showLockScreenEvent.test {
            channel.send(false)
            expectNoEvents()
        }
    }

    @Test
    fun `should not miss any event emitted during ViewModel init`() = runTest {
        // Given
        val immediateFlow = flow {
            emit(true)
        }
        every { shouldPresentPinInsertionScreen() } returns immediateFlow

        // When
        val viewModel = LauncherRouterViewModel(autoLockCheckPendingState, shouldPresentPinInsertionScreen)

        // Then
        viewModel.showLockScreenEvent.test {
            awaitItem()
            ensureAllEventsConsumed()
        }
    }

    @Test
    fun `given the main screen is recreated, when the router is created, then the auto lock is checked again`() =
        runTest {
            // Given
            every { shouldPresentPinInsertionScreen() } returns emptyFlow()

            // When
            LauncherRouterViewModel(autoLockCheckPendingState, shouldPresentPinInsertionScreen)

            // Then
            autoLockCheckPendingState.autoLockCheckEvents.test {
                awaitItem()
                ensureAllEventsConsumed()
            }
        }
}
