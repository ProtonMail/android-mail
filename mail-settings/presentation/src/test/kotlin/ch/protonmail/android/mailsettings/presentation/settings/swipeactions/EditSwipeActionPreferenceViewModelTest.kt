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

package ch.protonmail.android.mailsettings.presentation.settings.swipeactions

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailsettings.domain.model.SwipeActionDirection
import ch.protonmail.android.mailsettings.domain.model.SwipeActionsPreference
import ch.protonmail.android.mailsettings.domain.usecase.ObserveSwipeActionsPreference
import ch.protonmail.android.mailsettings.domain.usecase.UpdateSwipeActionPreference
import ch.protonmail.android.mailsettings.presentation.settings.swipeactions.EditSwipeActionPreferenceScreen.SWIPE_DIRECTION_KEY
import ch.protonmail.android.testdata.user.UserIdTestData.userId
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import me.proton.core.mailsettings.domain.entity.SwipeAction
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

internal class EditSwipeActionPreferenceViewModelTest {

    private val editSwipeActionPreferenceUiModelMapper = EditSwipeActionPreferenceUiModelMapper()
    private val observePrimaryUserId: ObservePrimaryUserId = mockk {
        every { this@mockk() } returns flowOf(userId)
    }
    private val observeSwipeActionsPreference: ObserveSwipeActionsPreference = mockk {
        every { this@mockk(userId) } returns flowOf(Preferences)
    }
    private val savedStateHandle: SavedStateHandle = mockk {
        every { get<String>(SWIPE_DIRECTION_KEY) } returns SwipeActionDirection.LEFT.name
    }
    private val updateSwipeActionPreference: UpdateSwipeActionPreference = mockk(relaxUnitFun = true)
    private val areAdditionalSwipeActionsEnabled = mockk<AreAdditionalSwipeActionsEnabled>()


    private val viewModel by lazy {
        EditSwipeActionPreferenceViewModel(
            editSwipeActionPreferenceUiModelMapper = editSwipeActionPreferenceUiModelMapper,
            observePrimaryUserId = observePrimaryUserId,
            observeSwipeActionsPreference = observeSwipeActionsPreference,
            savedStateHandle = savedStateHandle,
            updateSwipeActionPreference = updateSwipeActionPreference,
            areAdditionalSwipeActionsEnabled = areAdditionalSwipeActionsEnabled
        )
    }

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `state emits swipe actions with current preference selected for swipe right when ff is disabled`() = runTest {
        // given
        val direction = SwipeActionDirection.RIGHT
        every { savedStateHandle.get<String>(SWIPE_DIRECTION_KEY) } returns direction.name
        every { areAdditionalSwipeActionsEnabled(null) } returns false

        // when
        viewModel.state.test {
            awaitInitialState()

            // then
            val expected = run {
                val items = editSwipeActionPreferenceUiModelMapper.toUiModels(Preferences, direction, false)
                EditSwipeActionPreferenceState.Data(items)
            }
            assertEquals(expected, awaitItem())
        }
    }

    @Test
    fun `state emits swipe actions with current preference selected for swipe left when ff is disabled`() = runTest {
        // given
        val direction = SwipeActionDirection.LEFT
        every { savedStateHandle.get<String>(SWIPE_DIRECTION_KEY) } returns direction.name
        every { areAdditionalSwipeActionsEnabled(null) } returns false

        // when
        viewModel.state.test {
            awaitInitialState()

            // then
            val expected = run {
                val items = editSwipeActionPreferenceUiModelMapper.toUiModels(Preferences, direction, false)
                EditSwipeActionPreferenceState.Data(items)
            }
            assertEquals(expected, awaitItem())
        }
    }

    @Test
    fun `state emits swipe actions with current preference selected for swipe right when ff is enabled`() = runTest {
        // given
        val direction = SwipeActionDirection.RIGHT
        every { savedStateHandle.get<String>(SWIPE_DIRECTION_KEY) } returns direction.name
        every { areAdditionalSwipeActionsEnabled(null) } returns true

        // when
        viewModel.state.test {
            awaitInitialState()

            // then
            val expected = run {
                val items = editSwipeActionPreferenceUiModelMapper.toUiModels(Preferences, direction, true)
                EditSwipeActionPreferenceState.Data(items)
            }
            assertEquals(expected, awaitItem())
        }
    }

    @Test
    fun `state emits swipe actions with current preference selected for swipe left when ff is enabled`() = runTest {
        // given
        val direction = SwipeActionDirection.LEFT
        every { savedStateHandle.get<String>(SWIPE_DIRECTION_KEY) } returns direction.name
        every { areAdditionalSwipeActionsEnabled(null) } returns true

        // when
        viewModel.state.test {
            awaitInitialState()

            // then
            val expected = run {
                val items = editSwipeActionPreferenceUiModelMapper.toUiModels(Preferences, direction, true)
                EditSwipeActionPreferenceState.Data(items)
            }
            assertEquals(expected, awaitItem())
        }
    }

    @Test
    fun `state emits swipe actions with none preference selected for swipe right when ff is disabled`() = runTest {
        // given
        val direction = SwipeActionDirection.RIGHT
        every { savedStateHandle.get<String>(SWIPE_DIRECTION_KEY) } returns direction.name
        every { areAdditionalSwipeActionsEnabled(null) } returns false
        every { observeSwipeActionsPreference(userId) } returns flowOf(NonePreferences)

        // when
        viewModel.state.test {
            awaitInitialState()

            // then
            val expected = run {
                val items = editSwipeActionPreferenceUiModelMapper.toUiModels(NonePreferences, direction, false)
                EditSwipeActionPreferenceState.Data(items)
            }
            assertEquals(expected, awaitItem())
        }
    }

    @Test
    fun `state emits swipe actions with none preference selected for swipe left when ff is disabled`() = runTest {
        // given
        val direction = SwipeActionDirection.LEFT
        every { savedStateHandle.get<String>(SWIPE_DIRECTION_KEY) } returns direction.name
        every { areAdditionalSwipeActionsEnabled(null) } returns false
        every { observeSwipeActionsPreference(userId) } returns flowOf(NonePreferences)

        // when
        viewModel.state.test {
            awaitInitialState()

            // then
            val expected = run {
                val items = editSwipeActionPreferenceUiModelMapper.toUiModels(NonePreferences, direction, false)
                EditSwipeActionPreferenceState.Data(items)
            }
            assertEquals(expected, awaitItem())
        }
    }

    @Test
    fun `state emits swipe actions with none preference selected for swipe right when ff is enabled`() = runTest {
        // given
        val direction = SwipeActionDirection.RIGHT
        every { savedStateHandle.get<String>(SWIPE_DIRECTION_KEY) } returns direction.name
        every { areAdditionalSwipeActionsEnabled(null) } returns true
        every { observeSwipeActionsPreference(userId) } returns flowOf(NonePreferences)

        // when
        viewModel.state.test {
            awaitInitialState()

            // then
            val expected = run {
                val items = editSwipeActionPreferenceUiModelMapper.toUiModels(NonePreferences, direction, true)
                EditSwipeActionPreferenceState.Data(items)
            }
            assertEquals(expected, awaitItem())
        }
    }

    @Test
    fun `state emits swipe actions with none preference selected for swipe left when ff is enabled`() = runTest {
        // given
        val direction = SwipeActionDirection.LEFT
        every { savedStateHandle.get<String>(SWIPE_DIRECTION_KEY) } returns direction.name
        every { areAdditionalSwipeActionsEnabled(null) } returns true
        every { observeSwipeActionsPreference(userId) } returns flowOf(NonePreferences)

        // when
        viewModel.state.test {
            awaitInitialState()

            // then
            val expected = run {
                val items = editSwipeActionPreferenceUiModelMapper.toUiModels(NonePreferences, direction, true)
                EditSwipeActionPreferenceState.Data(items)
            }
            assertEquals(expected, awaitItem())
        }
    }

    @Test
    fun `emits not logged in state when there are no logged in users`() = runTest {
        // given
        every { observePrimaryUserId() } returns flowOf(null)

        // when
        viewModel.state.test {
            awaitInitialState()

            // then
            assertEquals(EditSwipeActionPreferenceState.NotLoggedIn, awaitItem())
        }
    }

    @Test
    fun `call use case with correct parameters when edit the swipe left action`() = runTest {
        // given
        val direction = SwipeActionDirection.LEFT
        val swipeAction = SwipeAction.MarkRead

        // when
        viewModel.submit(EditSwipeActionPreferenceViewModel.Action.UpdateSwipeAction(direction, swipeAction))

        // then
        advanceUntilIdle()
        coVerify { updateSwipeActionPreference(userId, direction, swipeAction) }
    }

    @Test
    fun `call use case with correct parameters when edit the swipe right action`() = runTest {
        // given
        val direction = SwipeActionDirection.RIGHT
        val swipeAction = SwipeAction.Star

        // when
        viewModel.submit(EditSwipeActionPreferenceViewModel.Action.UpdateSwipeAction(direction, swipeAction))

        // then
        advanceUntilIdle()
        coVerify { updateSwipeActionPreference(userId, direction, swipeAction) }
    }

    private suspend fun ReceiveTurbine<EditSwipeActionPreferenceState>.awaitInitialState() {
        assertEquals(viewModel.initial, awaitItem())
    }

    companion object TestData {

        val Preferences = SwipeActionsPreference(
            swipeLeft = SwipeAction.Archive,
            swipeRight = SwipeAction.MarkRead
        )

        val NonePreferences = SwipeActionsPreference(
            swipeLeft = SwipeAction.None,
            swipeRight = SwipeAction.None
        )
    }
}
