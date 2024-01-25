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
import ch.protonmail.android.mailsettings.presentation.R
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailsettings.domain.model.autolock.AutoLockInterval
import ch.protonmail.android.mailsettings.domain.model.autolock.AutoLockPin
import ch.protonmail.android.mailsettings.domain.model.autolock.AutoLockPreference
import ch.protonmail.android.mailsettings.domain.model.autolock.biometric.AutoLockBiometricsState
import ch.protonmail.android.mailsettings.domain.repository.AutoLockPreferenceError
import ch.protonmail.android.mailsettings.domain.usecase.autolock.ObserveAutoLockEnabled
import ch.protonmail.android.mailsettings.domain.usecase.autolock.ObserveAutoLockPinValue
import ch.protonmail.android.mailsettings.domain.usecase.autolock.ObserveSelectedAutoLockInterval
import ch.protonmail.android.mailsettings.domain.usecase.autolock.ToggleAutoLockBiometricsPreference
import ch.protonmail.android.mailsettings.domain.usecase.autolock.ToggleAutoLockEnabled
import ch.protonmail.android.mailsettings.domain.usecase.autolock.UpdateAutoLockInterval
import ch.protonmail.android.mailsettings.domain.usecase.autolock.biometric.ObserveAutoLockBiometricsState
import ch.protonmail.android.mailsettings.presentation.settings.autolock.helpers.AutoLockTestData
import ch.protonmail.android.mailsettings.presentation.settings.autolock.mapper.AutoLockBiometricsUiModelMapper
import ch.protonmail.android.mailsettings.presentation.settings.autolock.mapper.AutoLockIntervalsUiModelMapper
import ch.protonmail.android.mailsettings.presentation.settings.autolock.model.AutoLockBiometricsUiModel
import ch.protonmail.android.mailsettings.presentation.settings.autolock.model.AutoLockEnabledUiModel
import ch.protonmail.android.mailsettings.presentation.settings.autolock.model.AutoLockIntervalsUiModel
import ch.protonmail.android.mailsettings.presentation.settings.autolock.model.AutoLockSettingsState
import ch.protonmail.android.mailsettings.presentation.settings.autolock.model.AutoLockSettingsViewAction
import ch.protonmail.android.mailsettings.presentation.settings.autolock.reducer.AutoLockSettingsReducer
import io.mockk.called
import io.mockk.coEvery
import io.mockk.coVerify
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
    private val observeAutoLockBiometricsState = mockk<ObserveAutoLockBiometricsState>()
    private val toggleAutoLockEnabled = mockk<ToggleAutoLockEnabled>()
    private val toggleAutoLockBiometricsPreference = mockk<ToggleAutoLockBiometricsPreference>()

    private val updateAutoLockInterval = mockk<UpdateAutoLockInterval>()
    private val reducer = spyk(AutoLockSettingsReducer(intervalsUiModelMapper, biometricsUiModelMapper))

    private val viewModel by lazy {
        AutoLockSettingsViewModel(
            observeAutoLockEnabled,
            observeSelectedAutoLockInterval,
            observeAutoLockBiometricsState,
            observeAutoLockPinValue,
            toggleAutoLockEnabled,
            toggleAutoLockBiometricsPreference,
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
        every { observeAutoLockBiometricsState() } returns flowOf()

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
        expectAutoLockBiometricState()

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
        expectAutoLockBiometricState(false)

        val expectedState = defaultBaseState.copy(
            AutoLockSettingsState.DataLoaded.AutoLockEnabledState(AutoLockEnabledUiModel(true)),
            AutoLockSettingsState.DataLoaded.AutoLockIntervalState(
                dropdownExpanded = false,
                autoLockIntervalsUiModel = AutoLockIntervalsUiModel(
                    toSelectedUiModel(AutoLockInterval.OneDay),
                    baseModels
                )
            ),
            autoLockBiometricsState = defaultBaseBiometricsState.copy(enabled = false)
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
        expectAutoLockBiometricState()

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
    fun `should update the biometric state when the biometric preference is toggled`() = runTest {
        // Given
        expectAutoLockPreference()
        expectAutoLockInterval()
        expectAutoLockBiometricState()
        coEvery { toggleAutoLockBiometricsPreference(false) } returns Unit.right()
        val expectedState = defaultBaseState.copy(
            autoLockEnabledState = AutoLockSettingsState.DataLoaded.AutoLockEnabledState(AutoLockEnabledUiModel(true)),
            autoLockBiometricsState = defaultBaseBiometricsState.copy(enabled = false)
        )

        // When + Then
        viewModel.state.test {
            skipItems(1) // Base state

            viewModel.submit(AutoLockSettingsViewAction.ToggleAutoLockBiometricsPreference(defaultBaseBiometricsState))

            assertEquals(expectedState, awaitItem())
            coVerify { toggleAutoLockBiometricsPreference(false) }
        }
    }

    @Test
    fun `should not update the biometric state when the biometric preference is toggled given biometric hw error`() =
        runTest {
            // Given
            expectAutoLockPreference()
            expectAutoLockInterval()
            expectAutoLockBiometricHwError()
            val biometricErrorState = AutoLockBiometricsUiModel(
                enabled = false,
                biometricsEnrolled = false,
                biometricsHwAvailable = false
            )
            val expectedBaseState = defaultBaseState.copy(
                autoLockEnabledState = AutoLockSettingsState.DataLoaded.AutoLockEnabledState(
                    AutoLockEnabledUiModel(true)
                ),
                autoLockBiometricsState = biometricErrorState
            )
            val expectedToggleState = expectedBaseState.copy(
                autoLockBiometricsState = biometricErrorState.copy(
                    biometricsHwError = Effect.of(TextUiModel(R.string.biometric_error_hw_not_available))
                )
            )

            // When + Then
            viewModel.state.test {
                assertEquals(expectedBaseState, awaitItem())

                viewModel.submit(
                    AutoLockSettingsViewAction.ToggleAutoLockBiometricsPreference(
                        biometricErrorState
                    )
                )

                assertEquals(expectedToggleState, awaitItem())
                coVerify { toggleAutoLockBiometricsPreference wasNot called }
            }
        }

    @Test
    fun `should not update biometric state when the biometric preference is toggled given biometric not enrolled`() =
        runTest {
            // Given
            expectAutoLockPreference()
            expectAutoLockInterval()
            expectAutoLockBiometricNotEnrolledError()
            val biometricErrorState = AutoLockBiometricsUiModel(
                enabled = false,
                biometricsEnrolled = false,
                biometricsHwAvailable = true
            )
            val expectedBaseState = defaultBaseState.copy(
                autoLockEnabledState = AutoLockSettingsState.DataLoaded.AutoLockEnabledState(
                    AutoLockEnabledUiModel(true)
                ),
                autoLockBiometricsState = biometricErrorState
            )
            val expectedToggleState = expectedBaseState.copy(
                autoLockBiometricsState = biometricErrorState.copy(
                    biometricsEnrollmentError = Effect.of(TextUiModel(R.string.no_biometric_data_enrolled))
                )
            )

            // When + Then
            viewModel.state.test {
                assertEquals(expectedBaseState, awaitItem())

                viewModel.submit(
                    AutoLockSettingsViewAction.ToggleAutoLockBiometricsPreference(
                        biometricErrorState
                    )
                )

                assertEquals(expectedToggleState, awaitItem())
                coVerify { toggleAutoLockBiometricsPreference wasNot called }
            }
        }

    @Test
    fun `should update the state when the auto lock interval is manually set`() = runTest {
        // Given
        expectAutoLockPreference(isEnabled = false)
        expectAutoLockInterval()
        coEvery { updateAutoLockInterval(AutoLockInterval.OneHour) } returns Unit.right()
        expectAutoLockBiometricState()

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
    fun `should force pin creation when pin does not exist or is empty and feature is enabled`() = runTest {
        // Given
        expectAutoLockPreference(false)
        expectAutoLockInterval()
        coEvery { toggleAutoLockEnabled(true) } returns Unit.right()
        coEvery { observeAutoLockPinValue() } returns flowOf(AutoLockPin("").right())
        expectAutoLockBiometricState()

        val expectedState = defaultBaseState.copy(forceOpenPinCreation = Effect.of(Unit))

        // When + Then
        viewModel.state.test {
            skipItems(1) // Base state

            viewModel.submit(AutoLockSettingsViewAction.ToggleAutoLockPreference(true))
            assertEquals(expectedState, awaitItem())
        }
    }

    @Test
    fun `should signal an error when auto lock preference cannot be updated`() = runTest {
        // Given
        expectAutoLockPreference(false)
        expectAutoLockInterval()
        coEvery {
            toggleAutoLockEnabled(true)
        } returns AutoLockPreferenceError.DataStoreError.left()
        coEvery {
            observeAutoLockPinValue()
        } returns flowOf(AutoLockTestData.BaseAutoLockPin.right())
        expectAutoLockBiometricState()

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
        expectAutoLockBiometricState()

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
        expectAutoLockBiometricState()

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

    private fun expectAutoLockBiometricNotEnrolledError() {
        every {
            observeAutoLockBiometricsState()
        } returns flowOf(AutoLockBiometricsState.BiometricsAvailable.BiometricsNotEnrolled)
    }

    private fun expectAutoLockBiometricHwError() {
        every { observeAutoLockBiometricsState() } returns flowOf(AutoLockBiometricsState.BiometricsNotAvailable)
    }

    private fun expectAutoLockBiometricState(isEnabled: Boolean = true) {
        val expectedState = AutoLockBiometricsState.BiometricsAvailable.BiometricsEnrolled(enabled = isEnabled)
        every { observeAutoLockBiometricsState() } returns flowOf(expectedState)
    }

    private fun expectAutoLockPreference(isEnabled: Boolean = true) {
        val expectedPreferenceValue = AutoLockPreference(isEnabled)
        every { observeAutoLockEnabled() } returns flowOf(expectedPreferenceValue.right())
    }

    private fun expectAutoLockInterval(interval: AutoLockInterval = AutoLockInterval.Immediately) {
        every { observeSelectedAutoLockInterval() } returns flowOf(interval.right())
    }

    private companion object {

        val intervalsUiModelMapper = AutoLockIntervalsUiModelMapper()
        val biometricsUiModelMapper = AutoLockBiometricsUiModelMapper()
        val baseModels = intervalsUiModelMapper.toIntervalsListUiModel()
        val defaultBaseBiometricsState = AutoLockBiometricsUiModel(
            enabled = true,
            biometricsEnrolled = true,
            biometricsHwAvailable = true
        )

        val defaultBaseState = AutoLockSettingsState.DataLoaded(
            AutoLockSettingsState.DataLoaded.AutoLockEnabledState(AutoLockEnabledUiModel(false)),
            AutoLockSettingsState.DataLoaded.AutoLockIntervalState(
                dropdownExpanded = false,
                autoLockIntervalsUiModel = AutoLockIntervalsUiModel(
                    toSelectedUiModel(AutoLockInterval.Immediately),
                    baseModels
                )
            ),
            defaultBaseBiometricsState,
            Effect.empty(),
            Effect.empty(),
            Effect.empty()
        )

        fun toSelectedUiModel(interval: AutoLockInterval) = intervalsUiModelMapper.toSelectedIntervalUiModel(interval)
    }
}
