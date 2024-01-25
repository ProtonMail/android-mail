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

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailsettings.domain.model.autolock.AutoLockInsertionMode
import ch.protonmail.android.mailsettings.domain.model.autolock.AutoLockPin
import ch.protonmail.android.mailsettings.domain.model.autolock.AutoLockRemainingAttempts
import ch.protonmail.android.mailsettings.domain.model.autolock.biometric.AutoLockBiometricsState
import ch.protonmail.android.mailsettings.domain.repository.AutoLockPreferenceError
import ch.protonmail.android.mailsettings.domain.usecase.autolock.GetRemainingAutoLockAttempts
import ch.protonmail.android.mailsettings.domain.usecase.autolock.ObserveAutoLockPinValue
import ch.protonmail.android.mailsettings.domain.usecase.autolock.SaveAutoLockPin
import ch.protonmail.android.mailsettings.domain.usecase.autolock.ToggleAutoLockAttemptPendingStatus
import ch.protonmail.android.mailsettings.domain.usecase.autolock.ToggleAutoLockEnabled
import ch.protonmail.android.mailsettings.domain.usecase.autolock.UpdateLastForegroundMillis
import ch.protonmail.android.mailsettings.domain.usecase.autolock.UpdateRemainingAutoLockAttempts
import ch.protonmail.android.mailsettings.domain.usecase.autolock.biometric.GetCurrentAutoLockBiometricState
import ch.protonmail.android.mailsettings.domain.usecase.autolock.biometric.ObserveAutoLockBiometricsState
import ch.protonmail.android.mailsettings.presentation.R
import ch.protonmail.android.mailsettings.presentation.settings.autolock.helpers.AutoLockTestData
import ch.protonmail.android.mailsettings.presentation.settings.autolock.mapper.pin.AutoLockBiometricPinUiMapper
import ch.protonmail.android.mailsettings.presentation.settings.autolock.mapper.pin.AutoLockBiometricPromptUiMapper
import ch.protonmail.android.mailsettings.presentation.settings.autolock.mapper.pin.AutoLockPinErrorUiMapper
import ch.protonmail.android.mailsettings.presentation.settings.autolock.mapper.pin.AutoLockPinStepUiMapper
import ch.protonmail.android.mailsettings.presentation.settings.autolock.mapper.pin.AutoLockSuccessfulOperationUiMapper
import ch.protonmail.android.mailsettings.presentation.settings.autolock.model.pin.AutoLockPinState
import ch.protonmail.android.mailsettings.presentation.settings.autolock.model.pin.AutoLockPinViewAction
import ch.protonmail.android.mailsettings.presentation.settings.autolock.model.pin.ConfirmButtonUiModel
import ch.protonmail.android.mailsettings.presentation.settings.autolock.model.pin.InsertedPin
import ch.protonmail.android.mailsettings.presentation.settings.autolock.model.pin.PinInsertionStep
import ch.protonmail.android.mailsettings.presentation.settings.autolock.model.pin.PinInsertionUiModel
import ch.protonmail.android.mailsettings.presentation.settings.autolock.model.pin.PinVerificationRemainingAttempts
import ch.protonmail.android.mailsettings.presentation.settings.autolock.model.pin.SignOutUiModel
import ch.protonmail.android.mailsettings.presentation.settings.autolock.model.pin.TopBarUiModel
import ch.protonmail.android.mailsettings.presentation.settings.autolock.reducer.pin.AutoLockPinReducer
import ch.protonmail.android.mailsettings.presentation.settings.autolock.ui.pin.AutoLockPinScreen
import ch.protonmail.android.mailsettings.presentation.settings.autolock.usecase.ClearPinDataAndForceLogout
import ch.protonmail.android.mailsettings.presentation.settings.autolock.viewmodel.pin.AutoLockPinViewModel
import io.mockk.called
import io.mockk.coEvery
import io.mockk.coVerifySequence
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import me.proton.core.util.kotlin.serialize
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

internal class AutoLockPinViewModelTest {

    private val observeAutoLockBiometricsState = mockk<ObserveAutoLockBiometricsState>()
    private val getCurrentAutoLockBiometricState = mockk<GetCurrentAutoLockBiometricState>()
    private val observeAutoLockPin = mockk<ObserveAutoLockPinValue>()
    private val toggleAutoLockEnabled = mockk<ToggleAutoLockEnabled>()
    private val getRemainingAutoLockAttempts = mockk<GetRemainingAutoLockAttempts>()
    private val updateRemainingAutoLockAttempts = mockk<UpdateRemainingAutoLockAttempts>()
    private val saveAutoLockPin = mockk<SaveAutoLockPin>()
    private val clearPinDataAndForceLogout = mockk<ClearPinDataAndForceLogout>(relaxUnitFun = true)
    private val toggleAutoLockAttemptStatus = mockk<ToggleAutoLockAttemptPendingStatus>()
    private val updateAutoLockLastForegroundMillis = mockk<UpdateLastForegroundMillis>()
    private val savedStateHandle = mockk<SavedStateHandle>()
    private val reducer = AutoLockPinReducer(
        AutoLockPinStepUiMapper(),
        AutoLockSuccessfulOperationUiMapper(),
        AutoLockPinErrorUiMapper(),
        AutoLockBiometricPinUiMapper(),
        AutoLockBiometricPromptUiMapper()
    )

    private val viewModel by lazy {
        AutoLockPinViewModel(
            observeAutoLockPin,
            toggleAutoLockEnabled,
            observeAutoLockBiometricsState,
            getCurrentAutoLockBiometricState,
            getRemainingAutoLockAttempts,
            updateRemainingAutoLockAttempts,
            saveAutoLockPin,
            clearPinDataAndForceLogout,
            toggleAutoLockAttemptStatus,
            updateAutoLockLastForegroundMillis,
            reducer,
            savedStateHandle
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
    fun `should return loaded state when data is fetched from a standalone start`() = runTest {
        // Given
        expectBiometricState()
        expectStandaloneStart()
        expectAttempts()
        expectAttemptStatusToggling()
        val expectedState = AutoLockTestData.BaseLoadedState

        // When + Then
        viewModel.state.test {
            val state = awaitItem()
            assertEquals(expectedState, state)
        }
    }

    @Test
    fun `should not display biometric prompt or pin when user disabled the preference`() = runTest {
        // Given
        expectBiometricState(false)
        expectStandaloneStart()
        expectAttempts()
        expectAttemptStatusToggling()
        val expectedState = AutoLockTestData.BaseLoadedState.copy(
            biometricPinState = AutoLockTestData.biometricPinState.copy(
                shouldDisplayButton = false
            ),
            showBiometricPromptEffect = Effect.empty()
        )

        // When + Then
        viewModel.state.test {
            val state = awaitItem()
            assertEquals(expectedState, state)
        }
    }

    @Test
    fun `should not display biometric prompt or pin given biometric hw not available`() = runTest {
        // Given
        expectBiometricHwError()
        expectStandaloneStart()
        expectAttempts()
        expectAttemptStatusToggling()
        val expectedState = AutoLockTestData.BaseLoadedState.copy(
            biometricPinState = AutoLockTestData.biometricPinState.copy(
                shouldDisplayButton = false
            ),
            showBiometricPromptEffect = Effect.empty()
        )

        // When + Then
        viewModel.state.test {
            val state = awaitItem()
            assertEquals(expectedState, state)
        }
    }

    @Test
    fun `should not display biometric prompt or pin given biometric is not enrolled`() = runTest {
        // Given
        expectBiometricNotEnrolledError()
        expectStandaloneStart()
        expectAttempts()
        expectAttemptStatusToggling()
        val expectedState = AutoLockTestData.BaseLoadedState.copy(
            biometricPinState = AutoLockTestData.biometricPinState.copy(
                shouldDisplayButton = false
            ),
            showBiometricPromptEffect = Effect.empty()
        )

        // When + Then
        viewModel.state.test {
            val state = awaitItem()
            assertEquals(expectedState, state)
        }
    }

    @Test
    fun `should restore pin state when biometric authentication is successfull`() = runTest {
        // Given
        expectBiometricState()
        expectConditionalStart(AutoLockInsertionMode.ChangePin)
        expectAttempts()
        expectAttemptStatusToggling()
        expectValidAutoLockAttemptsUpdate()
        expectLastForegroundReset()

        // When + Then
        viewModel.state.test {
            skipItems(1)

            viewModel.submit(AutoLockPinViewAction.BiometricAuthenticationSucceeded)

            cancelAndConsumeRemainingEvents()
        }

        coVerifySequence {
            updateRemainingAutoLockAttempts(PinVerificationRemainingAttempts.MaxAttempts)
            clearPinDataAndForceLogout wasNot called
        }
    }

    @Test
    fun `should move to previous screen when back action is performed from pin confirmation`() = runTest {
        // Given
        expectBiometricState()
        expectStandaloneStart()
        expectAttempts()
        expectAttemptStatusToggling()

        // When + Then
        viewModel.state.test {
            skipItems(1)

            // Type pin of 4 digits and then confirm
            viewModel.insertPinAndConfirm("1234")
            skipItems(4)

            val intermediateState = awaitItem() as AutoLockPinState.DataLoaded
            assertEquals(PinInsertionStep.PinConfirmation, intermediateState.pinInsertionState.step)

            // Go back
            viewModel.submit(AutoLockPinViewAction.PerformBack)

            assertEquals(AutoLockTestData.BaseLoadedState, awaitItem())
        }
    }

    @Test
    fun `should dismiss the screen when going back from the main pin insertion screen`() = runTest {
        // Given
        expectBiometricState()
        expectStandaloneStart()
        expectAttempts()
        expectAttemptStatusToggling()
        val expectedState = AutoLockTestData.BaseLoadedState.copy(closeScreenEffect = Effect.of(Unit))

        // When + Then
        viewModel.state.test {
            skipItems(1)

            viewModel.submit(AutoLockPinViewAction.PerformBack)

            assertEquals(expectedState, awaitItem())
        }
    }

    @Test
    fun `should dismiss the screen when going back from the main pin change screen`() = runTest {
        // Given
        expectBiometricState()
        expectConditionalStart(AutoLockInsertionMode.ChangePin)
        expectAttempts()
        expectAttemptStatusToggling()
        val expectedCloseEffect = Effect.of(Unit)

        // When + Then
        viewModel.state.test {
            skipItems(1)

            viewModel.submit(AutoLockPinViewAction.PerformBack)

            val actual = awaitItem() as AutoLockPinState.DataLoaded

            assertEquals(expectedCloseEffect, actual.closeScreenEffect)
        }
    }

    @Test
    fun `should not dismiss the screen when going back from the main pin verification screen`() = runTest {
        // Given
        expectBiometricState()
        expectConditionalStart(AutoLockInsertionMode.VerifyPin)
        expectAttempts()
        expectAttemptStatusToggling()

        // When + Then
        viewModel.state.test {
            skipItems(1)

            viewModel.submit(AutoLockPinViewAction.PerformBack)

            expectNoEvents()
        }
    }

    @Test
    fun `should emit new state when pin digit is added`() = runTest {
        // Given
        expectBiometricState()
        expectStandaloneStart()
        expectAttempts()
        expectAttemptStatusToggling()
        val expectedInsertedPin = InsertedPin(listOf(0))

        // When + Then
        viewModel.state.test {
            skipItems(1)

            viewModel.submit(AutoLockPinViewAction.AddPinDigit(0))

            val actual = awaitItem() as AutoLockPinState.DataLoaded

            assertEquals(expectedInsertedPin, actual.pinInsertionState.pinInsertionUiModel.currentPin)
        }
    }

    @Test
    fun `should emit new state when pin digit is removed`() = runTest {
        // Given
        expectBiometricState()
        expectStandaloneStart()
        expectAttempts()
        expectAttemptStatusToggling()
        val expectedInsertedPin = InsertedPin(listOf(1))

        // When + Then
        viewModel.state.test {
            skipItems(1)

            viewModel.submit(AutoLockPinViewAction.AddPinDigit(1))
            viewModel.submit(AutoLockPinViewAction.AddPinDigit(0))

            skipItems(2)

            viewModel.submit(AutoLockPinViewAction.RemovePinDigit)

            val actual = awaitItem() as AutoLockPinState.DataLoaded

            assertEquals(expectedInsertedPin, actual.pinInsertionState.pinInsertionUiModel.currentPin)
        }
    }

    @Test
    fun `should emit error when pins do not match`() = runTest {
        // Given
        expectBiometricState()
        expectStandaloneStart()
        expectAttempts()
        expectAttemptStatusToggling()
        val expectedStep = PinInsertionStep.PinConfirmation
        val expectedError = Effect.of(TextUiModel(R.string.mail_settings_pin_insertion_error_no_match))

        // When + Then
        viewModel.state.test {
            skipItems(1)

            // Insertion and confirmation
            viewModel.insertPinAndConfirm("1234")
            viewModel.insertPinAndConfirm("1233")
            skipItems(9)

            val actual = awaitItem() as AutoLockPinState.DataLoaded

            assertEquals(expectedStep, actual.pinInsertionState.step)
            assertEquals(expectedError.consume(), actual.pinInsertionErrorEffect.consume())
        }
    }

    @Test
    fun `should emit a generic error when pin cannot be saved`() = runTest {
        // Given
        expectBiometricState()
        expectStandaloneStart()
        expectAttempts()
        expectAttemptStatusToggling()
        coEvery {
            saveAutoLockPin(AutoLockTestData.BaseAutoLockPin)
        } returns AutoLockPreferenceError.DataStoreError.left()

        val expectedStep = PinInsertionStep.PinConfirmation
        val expectedError = Effect.of(TextUiModel(R.string.mail_settings_pin_insertion_error_unknown))

        // When + Then
        viewModel.state.test {
            skipItems(1)

            // Insertion and confirmation
            viewModel.insertPinAndConfirm("1234")
            viewModel.insertPinAndConfirm("1234")
            skipItems(9)

            val actual = awaitItem() as AutoLockPinState.DataLoaded

            assertEquals(expectedStep, actual.pinInsertionState.step)
            assertEquals(expectedError.consume(), actual.pinInsertionErrorEffect.consume())
        }
    }

    @Test
    fun `should emit a generic error when auto lock cannot be toggled`() = runTest {
        // Given
        expectBiometricState()
        expectStandaloneStart()
        expectAttempts()
        expectAttemptStatusToggling()
        expectValidPinSaving()
        coEvery { toggleAutoLockEnabled(true) } returns AutoLockPreferenceError.DataStoreError.left()

        val expectedStep = PinInsertionStep.PinConfirmation
        val expectedError = Effect.of(TextUiModel(R.string.mail_settings_pin_insertion_error_unknown))

        // When + Then
        viewModel.state.test {
            skipItems(1)

            // Insertion and confirmation
            viewModel.insertPinAndConfirm("1234")
            viewModel.insertPinAndConfirm("1234")
            skipItems(9)

            val actual = awaitItem() as AutoLockPinState.DataLoaded

            assertEquals(expectedStep, actual.pinInsertionState.step)
            assertEquals(expectedError.consume(), actual.pinInsertionErrorEffect.consume())
        }
    }

    @Test
    fun `should emit a completion event when pin confirmation succeeds`() = runTest {
        // Given
        expectBiometricState()
        expectStandaloneStart()
        expectAttempts()
        expectAttemptStatusToggling()
        expectValidPinSaving()
        expectAutoLockToggling(true)

        val expectedStep = PinInsertionStep.PinConfirmation
        val expectedCloseEffect = Effect.of(Unit)

        // When + Then
        viewModel.state.test {
            skipItems(1)

            // Insertion and confirmation
            viewModel.insertPinAndConfirm("1234")
            viewModel.insertPinAndConfirm("1234")
            skipItems(9)

            val actual = awaitItem() as AutoLockPinState.DataLoaded

            assertEquals(expectedStep, actual.pinInsertionState.step)
            assertEquals(expectedCloseEffect, actual.closeScreenEffect)
            assertEquals(Effect.empty(), actual.pinInsertionErrorEffect)
        }
    }

    @Test
    fun `should restore pin attempts when the pin change verification has success`() = runTest {
        // Given
        expectBiometricState()
        expectConditionalStart(AutoLockInsertionMode.ChangePin)
        expectAttempts()
        expectAttemptStatusToggling()
        expectExistingPin("1234")
        expectValidAutoLockAttemptsUpdate()
        expectLastForegroundReset()

        // When + Then
        viewModel.state.test {
            skipItems(1)

            viewModel.insertPinAndConfirm("1234")

            cancelAndConsumeRemainingEvents()
        }

        coVerifySequence {
            updateRemainingAutoLockAttempts(PinVerificationRemainingAttempts.MaxAttempts)
            clearPinDataAndForceLogout wasNot called
        }
    }

    @Test
    fun `should decrement pin attempts when the pin change verification fails`() = runTest {
        // Given
        expectBiometricState()
        expectConditionalStart(AutoLockInsertionMode.ChangePin)
        expectAttempts(value = PinVerificationRemainingAttempts.MaxAttempts)
        expectAttemptStatusToggling()
        expectExistingPin("1234")
        expectValidAutoLockAttemptsUpdate()

        // When + Then
        viewModel.state.test {
            skipItems(1)

            viewModel.submit(AutoLockPinViewAction.AddPinDigit(1))
            viewModel.submit(AutoLockPinViewAction.PerformConfirm)

            cancelAndConsumeRemainingEvents()
        }

        coVerifySequence {
            updateRemainingAutoLockAttempts(PinVerificationRemainingAttempts.MaxAttempts - 1)
            clearPinDataAndForceLogout wasNot called
        }
    }

    @Test
    fun `should call use case to force logout and clear auto lock to defaults when no attempts are left`() = runTest {
        // Given
        expectBiometricState()
        expectConditionalStart(AutoLockInsertionMode.ChangePin)
        expectAttempts(value = 1)
        expectAttemptStatusToggling()
        expectExistingPin("1234")
        expectValidAutoLockAttemptsUpdate()
        expectValidReset()
        val expectedCloseEffect = Effect.of(Unit)

        // When + Then
        viewModel.state.test {
            skipItems(1)

            viewModel.submit(AutoLockPinViewAction.AddPinDigit(1))
            skipItems(1)

            viewModel.submit(AutoLockPinViewAction.PerformConfirm)
            val actual = awaitItem() as AutoLockPinState.DataLoaded

            assertEquals(expectedCloseEffect, actual.closeScreenEffect)
        }

        coVerifySequence {
            updateRemainingAutoLockAttempts wasNot called
            clearPinDataAndForceLogout()
        }
    }

    @Test
    fun `should close the screen when verification is completed without continuation`() = runTest {
        // Given
        expectBiometricState()
        expectConditionalStart(AutoLockInsertionMode.VerifyPin)
        expectAttempts()
        expectAttemptStatusToggling()
        expectExistingPin("1234")
        expectValidAutoLockAttemptsUpdate()
        expectLastForegroundReset()

        val expectedCloseEffect = Effect.of(Unit)

        // When + Then
        viewModel.state.test {
            skipItems(1)

            viewModel.insertPinAndConfirm("1234")
            skipItems(4)

            val actual = awaitItem() as AutoLockPinState.DataLoaded

            assertEquals(expectedCloseEffect, actual.closeScreenEffect)
        }
    }

    @Test
    fun `should update the sign out confirmation dialog when the button is pressed`() = runTest {
        // Given
        expectBiometricState()
        val expectedSignOutState = AutoLockPinState.SignOutButtonState(
            SignOutUiModel(isDisplayed = true, isRequested = true)
        )
        expectConditionalStart(AutoLockInsertionMode.VerifyPin)
        expectAttempts()
        expectAttemptStatusToggling()
        expectExistingPin("1234")
        expectValidAutoLockAttemptsUpdate()
        expectLastForegroundReset()

        // When + Then
        viewModel.state.test {
            skipItems(1)

            viewModel.submit(AutoLockPinViewAction.RequestSignOut)
            val actual = awaitItem() as AutoLockPinState.DataLoaded

            assertEquals(expectedSignOutState, actual.signOutButtonState)
        }
    }

    @Test
    fun `should update the sign out confirmation dialog when the dialog is dismissed`() = runTest {
        // Given
        expectBiometricState()
        val expectedSignOutState = AutoLockPinState.SignOutButtonState(
            SignOutUiModel(isDisplayed = true, isRequested = false)
        )
        expectConditionalStart(AutoLockInsertionMode.VerifyPin)
        expectAttempts()
        expectAttemptStatusToggling()
        expectExistingPin("1234")
        expectValidAutoLockAttemptsUpdate()
        expectLastForegroundReset()

        // When + Then
        viewModel.state.test {
            skipItems(1)

            viewModel.submit(AutoLockPinViewAction.RequestSignOut)
            skipItems(1)

            viewModel.submit(AutoLockPinViewAction.CancelSignOut)
            val actual = awaitItem() as AutoLockPinState.DataLoaded

            assertEquals(expectedSignOutState, actual.signOutButtonState)
        }
    }

    @Test
    fun `should update the sign out confirmation dialog when the dialog is confirmed`() = runTest {
        // Given
        expectBiometricState()
        val expectedSignOutState = AutoLockPinState.SignOutButtonState(
            SignOutUiModel(isDisplayed = true, isRequested = true)
        )
        expectConditionalStart(AutoLockInsertionMode.VerifyPin)
        expectAttempts()
        expectAttemptStatusToggling()
        expectExistingPin("1234")
        expectValidAutoLockAttemptsUpdate()
        expectLastForegroundReset()
        expectValidReset()

        // When + Then
        viewModel.state.test {
            skipItems(1)

            viewModel.submit(AutoLockPinViewAction.RequestSignOut)
            skipItems(1)

            viewModel.submit(AutoLockPinViewAction.ConfirmSignOut)
            val actual = awaitItem() as AutoLockPinState.DataLoaded

            assertEquals(expectedSignOutState, actual.signOutButtonState)
        }
    }

    @Test
    fun `should not emit the snackbar confirmation when the starting flow is verification`() = runTest {
        // Given
        expectBiometricState()
        expectConditionalStart(AutoLockInsertionMode.VerifyPin)
        expectAttempts()
        expectAttemptStatusToggling()
        expectExistingPin("1234")
        expectValidAutoLockAttemptsUpdate()
        expectLastForegroundReset()

        val expectedSnackbarEffect = Effect.empty<TextUiModel>()

        // When + Then
        viewModel.state.test {
            skipItems(1)

            viewModel.insertPinAndConfirm("1234")
            skipItems(4)

            val actual = awaitItem() as AutoLockPinState.DataLoaded

            assertEquals(expectedSnackbarEffect, actual.snackbarSuccessEffect)
        }
    }

    @Test
    fun `should emit a snackbar state when completing the change pin flow`() = runTest {
        // Given
        expectBiometricState()
        expectConditionalStart(AutoLockInsertionMode.ChangePin)
        expectAttempts()
        expectAttemptStatusToggling()
        expectValidAutoLockAttemptsUpdate()
        expectLastForegroundReset()
        expectValidPinSaving()
        expectAutoLockToggling(true)

        coEvery { observeAutoLockPin() } returns flowOf(AutoLockPin("1234").right())
        val expectedEffect = Effect.of(TextUiModel(R.string.mail_settings_pin_insertion_changed_success))
        val expectedState = AutoLockTestData.BaseLoadedState.copy(
            pinInsertionState = AutoLockPinState.PinInsertionState(
                startingStep = PinInsertionStep.PinChange,
                step = PinInsertionStep.PinConfirmation,
                remainingAttempts = PinVerificationRemainingAttempts(10),
                pinInsertionUiModel = PinInsertionUiModel(InsertedPin(listOf(1, 2, 3, 3)))
            ),
            topBarState = AutoLockPinState.TopBarState(
                TopBarUiModel(showBackButton = true, R.string.mail_settings_pin_insertion_confirm_title)
            ),
            confirmButtonState = AutoLockPinState.ConfirmButtonState(
                ConfirmButtonUiModel(isEnabled = true, R.string.mail_settings_pin_insertion_button_create)
            ),
            snackbarSuccessEffect = expectedEffect,
            closeScreenEffect = Effect.of(Unit)
        )

        // When + Then
        viewModel.state.test {
            skipItems(1)

            // Insertion and confirmation
            viewModel.insertPinAndConfirm("1234")
            viewModel.insertPinAndConfirm("1233")
            viewModel.insertPinAndConfirm("1233")
            skipItems(14)

            val actual = awaitItem() as AutoLockPinState.DataLoaded

            assertEquals(expectedState, actual)
        }
    }

    private fun expectBiometricNotEnrolledError() {
        coEvery { getCurrentAutoLockBiometricState() } returns
            AutoLockBiometricsState.BiometricsAvailable.BiometricsNotEnrolled
        coEvery { observeAutoLockBiometricsState() } returns flowOf()

    }

    private fun expectBiometricHwError() {
        coEvery { getCurrentAutoLockBiometricState() } returns AutoLockBiometricsState.BiometricsNotAvailable
        coEvery { observeAutoLockBiometricsState() } returns flowOf()

    }

    private fun expectBiometricState(isEnabled: Boolean = true) {
        val defaultBiometricState = AutoLockBiometricsState.BiometricsAvailable.BiometricsEnrolled(isEnabled)
        coEvery { getCurrentAutoLockBiometricState() } returns defaultBiometricState
        coEvery { observeAutoLockBiometricsState() } returns flowOf()
    }

    private fun expectConditionalStart(mode: AutoLockInsertionMode) {
        every {
            savedStateHandle.get<String>(AutoLockPinScreen.AutoLockPinModeKey)
        } returns mode.serialize()
    }

    private fun expectStandaloneStart() {
        every { savedStateHandle.get<String>(AutoLockPinScreen.AutoLockPinModeKey) } returns ""
    }

    private fun expectAttempts(value: Int = 10) {
        coEvery { getRemainingAutoLockAttempts() } returns AutoLockRemainingAttempts(value).right()
    }

    private fun expectAutoLockToggling(newValue: Boolean) {
        coEvery { toggleAutoLockEnabled(newValue) } returns Unit.right()

    }

    private fun expectValidPinSaving() {
        coEvery { saveAutoLockPin(AutoLockTestData.BaseAutoLockPin) } returns Unit.right()
        coEvery { saveAutoLockPin(AutoLockTestData.BaseAutoLockUpdatedPin) } returns Unit.right()
    }

    private fun expectAttemptStatusToggling() {
        coEvery { toggleAutoLockAttemptStatus(true) } returns Unit.right()
        coEvery { toggleAutoLockAttemptStatus(false) } returns Unit.right()
    }

    private fun expectValidAutoLockAttemptsUpdate() {
        coEvery { updateRemainingAutoLockAttempts(any()) } returns Unit.right()
    }

    private fun expectExistingPin(pin: String) {
        every { observeAutoLockPin() } returns flowOf(AutoLockPin(pin).right())
    }

    private fun expectLastForegroundReset() {
        coEvery { updateAutoLockLastForegroundMillis(Long.MAX_VALUE) } returns Unit.right()
    }

    private fun expectValidReset() {
        coEvery { clearPinDataAndForceLogout().await() } just runs
    }

    private fun AutoLockPinViewModel.insertPinAndConfirm(pin: String) {
        pin.toCharArray().forEach { submit(AutoLockPinViewAction.AddPinDigit(it.digitToInt())) }
        submit(AutoLockPinViewAction.PerformConfirm)
    }
}
