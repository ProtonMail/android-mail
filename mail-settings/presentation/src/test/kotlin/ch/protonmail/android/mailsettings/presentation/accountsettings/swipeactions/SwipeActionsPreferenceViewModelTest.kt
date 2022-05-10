/*
 * Copyright (c) 2021 Proton Technologies AG
 * This file is part of Proton Technologies AG and ProtonMail.
 *
 * ProtonMail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ProtonMail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ProtonMail.  If not, see <https://www.gnu.org/licenses/>.
 */

package ch.protonmail.android.mailsettings.presentation.accountsettings.swipeactions

import app.cash.turbine.test
import ch.protonmail.android.mailsettings.domain.usecase.ObserveSwipeActionsPreference
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

internal class SwipeActionsPreferenceViewModelTest {

    private val observeSwipeActionsPreference: ObserveSwipeActionsPreference = mockk {
        every { this@mockk() } returns flowOf(mockk())
    }
    private val swipeActionPreferenceUiModelMapper: SwipeActionPreferenceUiModelMapper = mockk {
        every { toUiModel(any()) } returns uiModel
    }
    private lateinit var viewModel: SwipeActionsPreferenceViewModel

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())

        viewModel = SwipeActionsPreferenceViewModel(
            observeSwipeActionsPreference = observeSwipeActionsPreference,
            swipeActionPreferenceUiModelMapper = swipeActionPreferenceUiModelMapper
        )
    }

    @Test
    fun `on start emits Loading`() = runTest {
        // when
        viewModel.state.test {
            // then
            assertEquals(SwipeActionsPreferenceState.Loading, awaitItem())
        }
    }

    @Test
    fun `emits correct data`() = runTest {
        // when
        viewModel.state.test {
            assertEquals(SwipeActionsPreferenceState.Loading, awaitItem())

            // then
            advanceUntilIdle()
            assertEquals(SwipeActionsPreferenceState.Data(uiModel), awaitItem())
        }
    }

    private companion object TestData {

        val uiModel = SwipeActionsPreferenceUiModel(
            left = SwipeActionPreferenceUiModel(0, 1, 2),
            right = SwipeActionPreferenceUiModel(3, 4, 5)
        )
    }
}
