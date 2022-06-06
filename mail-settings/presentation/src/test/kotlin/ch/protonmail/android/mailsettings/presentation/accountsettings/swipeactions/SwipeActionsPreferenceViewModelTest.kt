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

package ch.protonmail.android.mailsettings.presentation.accountsettings.swipeactions

import androidx.compose.ui.graphics.Color
import app.cash.turbine.test
import ch.protonmail.android.mailsettings.domain.usecase.ObserveSwipeActionsPreference
import ch.protonmail.android.mailsettings.presentation.settings.swipeactions.SwipeActionPreferenceUiModel
import ch.protonmail.android.mailsettings.presentation.settings.swipeactions.SwipeActionPreferenceUiModelMapper
import ch.protonmail.android.mailsettings.presentation.settings.swipeactions.SwipeActionsPreferenceState
import ch.protonmail.android.mailsettings.presentation.settings.swipeactions.SwipeActionsPreferenceUiModel
import ch.protonmail.android.mailsettings.presentation.settings.swipeactions.SwipeActionsPreferenceViewModel
import ch.protonmail.android.testdata.user.UserIdTestData.userId
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import me.proton.core.accountmanager.domain.AccountManager
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

internal class SwipeActionsPreferenceViewModelTest {

    private val accountManager: AccountManager = mockk {
        every { getPrimaryUserId() } returns flowOf(userId)
    }
    private val observeSwipeActionsPreference: ObserveSwipeActionsPreference = mockk {
        every { this@mockk(any()) } returns flowOf(mockk())
    }
    private val swipeActionPreferenceUiModelMapper: SwipeActionPreferenceUiModelMapper = mockk {
        every { toUiModel(any()) } returns uiModel
    }
    private lateinit var viewModel: SwipeActionsPreferenceViewModel

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())

        viewModel = SwipeActionsPreferenceViewModel(
            accountManager = accountManager,
            observeSwipeActionsPreference = observeSwipeActionsPreference,
            swipeActionPreferenceUiModelMapper = swipeActionPreferenceUiModelMapper
        )
    }

    @Test
    fun `on start emits Loading`() = runTest {
        // given
        givenDataIsDelayed()

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

            // then
            assertEquals(SwipeActionsPreferenceState.Data(uiModel), awaitItem())
        }
    }

    private fun givenDataIsDelayed() {
        every { observeSwipeActionsPreference(any()) } returns flow {
            delay(1)
            mockk()
        }
        viewModel = SwipeActionsPreferenceViewModel(
            accountManager = accountManager,
            observeSwipeActionsPreference = observeSwipeActionsPreference,
            swipeActionPreferenceUiModelMapper = swipeActionPreferenceUiModelMapper
        )
    }

    private companion object TestData {

        val uiModel = SwipeActionsPreferenceUiModel(
            left = SwipeActionPreferenceUiModel(
                imageRes = 0,
                titleRes = 1,
                descriptionRes = 2,
                getColor = { Color.Black }
            ),
            right = SwipeActionPreferenceUiModel(
                imageRes = 3,
                titleRes = 4,
                descriptionRes = 5,
                getColor = { Color.Black }
            )
        )
    }
}
