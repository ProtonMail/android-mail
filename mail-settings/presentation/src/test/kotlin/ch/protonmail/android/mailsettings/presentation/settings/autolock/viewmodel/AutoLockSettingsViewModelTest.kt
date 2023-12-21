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

package ch.protonmail.android.mailsettings.presentation.settings.autolock.viewmodel

import app.cash.turbine.test
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailsettings.domain.model.autolock.AutoLockInterval
import ch.protonmail.android.mailsettings.domain.model.autolock.AutoLockPreference
import ch.protonmail.android.mailsettings.domain.repository.AutoLockPreferenceError
import ch.protonmail.android.mailsettings.domain.usecase.autolock.ObserveAutoLockEnabled
import ch.protonmail.android.mailsettings.domain.usecase.autolock.ObserveAutoLockPinValue
import ch.protonmail.android.mailsettings.domain.usecase.autolock.ObserveSelectedAutoLockInterval
import ch.protonmail.android.mailsettings.domain.usecase.autolock.ToggleAutoLockEnabled
import ch.protonmail.android.mailsettings.domain.usecase.autolock.UpdateAutoLockInterval
import ch.protonmail.android.mailsettings.presentation.settings.autolock.mapper.AutoLockIntervalsUiModelMapper
import ch.protonmail.android.mailsettings.presentation.settings.autolock.model.AutoLockEnabledUiModel
import ch.protonmail.android.mailsettings.presentation.settings.autolock.model.AutoLockIntervalsUiModel
import ch.protonmail.android.mailsettings.presentation.settings.autolock.model.AutoLockSettingsState
import ch.protonmail.android.mailsettings.presentation.settings.autolock.model.AutoLockSettingsViewAction
import ch.protonmail.android.mailsettings.presentation.settings.autolock.reducer.AutoLockSettingsReducer
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

internal class AutoLockSettingsViewModelTest {

    private val observeAutoLockEnabled = mockk<ObserveAutoLockEnabled>()
    private val observeSelectedAutoLockInterval = mockk<ObserveSelectedAutoLockInterval>()
    private val observeAutoLockPinValue = mockk<ObserveAutoLockPinValue>()
    private val toggleAutoLockEnabled = mockk<ToggleAutoLockEnabled>()
    private val updateAutoLockInterval = mockk<UpdateAutoLockInterval>()
    private val reducer = spyk(AutoLockSettingsReducer(mapper))

    private val viewModel by lazy {
        AutoLockSettingsViewModel(
            observeAutoLockEnabled,
            observeSelectedAutoLockInterval,
            observeAutoLockPinValue,
            toggleAutoLockEnabled,
            updateAutoLockInterval,
            reducer
        )
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `should return loading state when first launched`() = runTest {
        // Given
        every { observeAutoLockEnabled() } returns flowOf()
        every { observeSelectedAutoLockInterval() } returns flowOf()

        // When + Then
        viewModel.state.test {
            val loadingState = awaitItem()
            assertEquals(AutoLockSettingsState.Loading, loadingState)
        }
    }

    @Test
    fun `should return default values when settings are not present`() = runTest {
        // Given
        every { observeAutoLockEnabled() } returns flowOf(AutoLockPreferenceError.DataStoreError.left())
        every { observeSelectedAutoLockInterval() } returns flowOf(AutoLockPreferenceError.DataStoreError.left())

        // When + Then
        viewModel.state.test {
            val loadingState = awaitItem()
            assertEquals(defaultBaseState, loadingState)
        }
    }

    @Test
    fun `should return actual values when settings are present`() = runTest {
        // Given
        expectAutoLockPreference()
        expectAutoLockInterval(interval = AutoLockInterval.OneDay)

        val expectedState = defaultBaseState.copy(
            AutoLockSettingsState.DataLoaded.AutoLockEnabledState(AutoLockEnabledUiModel(true)),
            AutoLockSettingsState.DataLoaded.AutoLockIntervalState(
                dropdownExpanded = false,
                autoLockIntervalsUiModel = AutoLockIntervalsUiModel(
                    toSelectedUiModel(AutoLockInterval.OneDay),
                    baseModels
                )
            )
        )

        // When + Then
        viewModel.state.test {
            val actualState = awaitItem()
            assertEquals(expectedState, actualState)
        }
    }

    @Test
    fun `should update the state when the auto lock preference is toggled`() = runTest {
        // Given
        expectAutoLockPreference()
        expectAutoLockInterval()
        coEvery { toggleAutoLockEnabled(false) } returns Unit.right()

        val expectedState = defaultBaseState.copy(
            AutoLockSettingsState.DataLoaded.AutoLockEnabledState(AutoLockEnabledUiModel(false))
        )

        // When + Then
        viewModel.state.test {
            skipItems(1) // Base state

            viewModel.submit(AutoLockSettingsViewAction.ToggleAutoLockPreference(false))
            assertEquals(expectedState, awaitItem())
        }
    }

    @Test
    fun `should update the state when the auto lock interval is manually set`() = runTest {
        // Given
        expectAutoLockPreference(isEnabled = false)
        expectAutoLockInterval()
        coEvery { updateAutoLockInterval(AutoLockInterval.OneHour) } returns Unit.right()

        val expectedState = defaultBaseState.copy(
            autoLockIntervalsState = AutoLockSettingsState.DataLoaded.AutoLockIntervalState(
                dropdownExpanded = false,
                autoLockIntervalsUiModel = AutoLockIntervalsUiModel(
                    toSelectedUiModel(AutoLockInterval.OneHour),
                    baseModels
                )
            )
        )

        // When + Then
        viewModel.state.test {
            skipItems(1) // Base state

            viewModel.submit(AutoLockSettingsViewAction.UpdateAutoLockInterval(AutoLockInterval.OneHour))
            assertEquals(expectedState, awaitItem())
        }
    }

    @Test
    fun `should signal an error when auto lock preference cannot be updated`() = runTest {
        // Given
        expectAutoLockPreference(false)
        expectAutoLockInterval()
        coEvery { toggleAutoLockEnabled(true) } returns AutoLockPreferenceError.DataStoreError.left()

        val expectedState = defaultBaseState.copy(updateError = Effect.of(Unit))

        // When + Then
        viewModel.state.test {
            skipItems(1) // Base state

            viewModel.submit(AutoLockSettingsViewAction.ToggleAutoLockPreference(true))
            assertEquals(expectedState, awaitItem())
        }
    }

    @Test
    fun `should signal an error when auto lock interval cannot be updated`() = runTest {
        // Given
        expectAutoLockPreference(isEnabled = false)
        expectAutoLockInterval()
        coEvery {
            updateAutoLockInterval(AutoLockInterval.OneHour)
        } returns AutoLockPreferenceError.DataStoreError.left()

        val expectedState = defaultBaseState.copy(updateError = Effect.of(Unit))

        // When + Then
        viewModel.state.test {
            skipItems(1) // Base state

            viewModel.submit(AutoLockSettingsViewAction.UpdateAutoLockInterval(AutoLockInterval.OneHour))
            assertEquals(expectedState, awaitItem())
        }
    }

    @Test
    fun `should update the state when interacting with the auto lock interval item`() = runTest {
        expectAutoLockPreference(isEnabled = false)
        expectAutoLockInterval()

        val expectedState = defaultBaseState.copy(
            autoLockIntervalsState = defaultBaseState.autoLockIntervalsState.copy(dropdownExpanded = true)
        )

        // When + Then
        viewModel.state.test {
            skipItems(1) // Base state

            viewModel.submit(AutoLockSettingsViewAction.ToggleIntervalDropDownVisibility(true))
            assertEquals(expectedState, awaitItem())
        }
    }

    private fun expectAutoLockPreference(isEnabled: Boolean = true) {
        val expectedPreferenceValue = AutoLockPreference(isEnabled)
        every { observeAutoLockEnabled() } returns flowOf(expectedPreferenceValue.right())
    }

    private fun expectAutoLockInterval(interval: AutoLockInterval = AutoLockInterval.Immediately) {
        every { observeSelectedAutoLockInterval() } returns flowOf(interval.right())
    }

    private companion object {

        val mapper = AutoLockIntervalsUiModelMapper()
        val baseModels = mapper.toIntervalsListUiModel()

        val defaultBaseState = AutoLockSettingsState.DataLoaded(
            AutoLockSettingsState.DataLoaded.AutoLockEnabledState(AutoLockEnabledUiModel(false)),
            AutoLockSettingsState.DataLoaded.AutoLockIntervalState(
                dropdownExpanded = false,
                autoLockIntervalsUiModel = AutoLockIntervalsUiModel(
                    toSelectedUiModel(AutoLockInterval.Immediately),
                    baseModels
                )
            ),
            Effect.empty(),
            Effect.empty(),
            Effect.empty()
        )

        fun toSelectedUiModel(interval: AutoLockInterval) = mapper.toSelectedIntervalUiModel(interval)
    }
}
