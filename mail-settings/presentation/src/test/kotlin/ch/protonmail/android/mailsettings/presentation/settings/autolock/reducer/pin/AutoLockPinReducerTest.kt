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

package ch.protonmail.android.mailsettings.presentation.settings.autolock.reducer.pin

import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailsettings.presentation.settings.autolock.helpers.AutoLockTestData
import ch.protonmail.android.mailsettings.presentation.settings.autolock.mapper.pin.AutoLockPinErrorUiMapper
import ch.protonmail.android.mailsettings.presentation.settings.autolock.mapper.pin.AutoLockPinStepUiMapper
import ch.protonmail.android.mailsettings.presentation.settings.autolock.model.pin.AutoLockPinEvent
import ch.protonmail.android.mailsettings.presentation.settings.autolock.model.pin.AutoLockPinState
import ch.protonmail.android.mailsettings.presentation.settings.autolock.model.pin.PinInsertionStep
import ch.protonmail.android.mailsettings.presentation.settings.autolock.model.pin.PinInsertionUiModel
import ch.protonmail.android.mailsettings.presentation.settings.autolock.model.pin.PinVerificationRemainingAttempts
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
internal class AutoLockPinReducerTest(private val testInput: TestInput) {

    private val reducer = AutoLockPinReducer(stepMapper, errorMapper)

    @Test
    fun `should map the step to the appropriate top bar ui model`() = with(testInput) {
        // When
        val actual = reducer.newStateFrom(state, event)

        // Then
        assertEquals(expected, actual)
    }

    private companion object {

        val stepMapper = AutoLockPinStepUiMapper()
        val errorMapper = AutoLockPinErrorUiMapper()

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = arrayOf(
            TestInput(
                state = AutoLockPinState.Loading,
                event = AutoLockPinEvent.Data.Loaded(
                    PinInsertionStep.PinInsertion,
                    PinVerificationRemainingAttempts.Default
                ),
                expected = AutoLockTestData.BaseLoadedState
            ),
            TestInput(
                state = AutoLockPinState.Loading,
                event = AutoLockPinEvent.Data.Loaded(
                    PinInsertionStep.PinInsertion,
                    AutoLockTestData.OneRemainingAttempt
                ),
                expected = AutoLockTestData.BaseLoadedState.copy(
                    pinInsertionState = AutoLockTestData.BasePinInsertionState.copy(
                        remainingAttempts = AutoLockTestData.OneRemainingAttempt
                    ),
                    pinInsertionErrorEffect = Effect.of(errorMapper.toUiModel(AutoLockTestData.OneRemainingAttempt)!!)
                )
            ),
            TestInput(
                state = AutoLockTestData.BaseLoadedState,
                event = AutoLockPinEvent.Update.Error.NotMatchingPins,
                expected = AutoLockTestData.BaseLoadedState.copy(
                    pinInsertionErrorEffect = Effect.of(
                        errorMapper.toUiModel(AutoLockPinEvent.Update.Error.NotMatchingPins)
                    )
                )
            ),
            TestInput(
                state = AutoLockTestData.BaseLoadedState,
                event = AutoLockPinEvent.Update.Error.UnknownError,
                expected = AutoLockTestData.BaseLoadedState.copy(
                    pinInsertionErrorEffect = Effect.of(
                        errorMapper.toUiModel(AutoLockPinEvent.Update.Error.UnknownError)
                    )
                )
            ),
            TestInput(
                state = AutoLockTestData.BaseLoadedState,
                event = AutoLockPinEvent.Update.Error.WrongPinCode(
                    remainingAttempts = AutoLockTestData.NineRemainingAttempts
                ),
                expected = AutoLockTestData.BaseLoadedState.copy(
                    pinInsertionState = AutoLockTestData.BasePinInsertionState.copy(
                        remainingAttempts = AutoLockTestData.NineRemainingAttempts
                    ),
                    pinInsertionErrorEffect = Effect.of(errorMapper.toUiModel(AutoLockTestData.NineRemainingAttempts)!!)
                )
            ),
            TestInput(
                state = AutoLockTestData.BaseLoadedState,
                event = AutoLockPinEvent.Update.PinValueChanged(AutoLockTestData.BaseValidPinInserted),
                expected = AutoLockTestData.BaseLoadedState.copy(
                    pinInsertionState = AutoLockTestData.BasePinInsertionState.copy(
                        pinInsertionUiModel = PinInsertionUiModel(AutoLockTestData.BaseValidPinInserted)
                    ),
                    confirmButtonState = AutoLockTestData.BaseConfirmButtonState.copy(
                        AutoLockTestData.BaseConfirmButtonUiModel.copy(isEnabled = true)
                    )
                )
            ),
            TestInput(
                state = AutoLockTestData.BaseLoadedState,
                event = AutoLockPinEvent.Update.PinValueChanged(AutoLockTestData.BaseInvalidPinInserted),
                expected = AutoLockTestData.BaseLoadedState.copy(
                    pinInsertionState = AutoLockTestData.BasePinInsertionState.copy(
                        pinInsertionUiModel = PinInsertionUiModel(AutoLockTestData.BaseInvalidPinInserted)
                    ),
                    confirmButtonState = AutoLockTestData.BaseConfirmButtonState.copy(
                        AutoLockTestData.BaseConfirmButtonUiModel.copy(isEnabled = false)
                    )
                )
            ),
            TestInput(
                state = AutoLockTestData.BaseLoadedState,
                event = AutoLockPinEvent.Update.OperationCompleted,
                expected = AutoLockTestData.BaseLoadedState.copy(
                    closeScreenEffect = Effect.of(Unit)
                )
            ),
            TestInput(
                state = AutoLockTestData.BaseLoadedState,
                event = AutoLockPinEvent.Update.OperationAborted,
                expected = AutoLockTestData.BaseLoadedState.copy(
                    closeScreenEffect = Effect.of(Unit)
                )
            ),
            TestInput(
                state = AutoLockTestData.BaseLoadedState,
                event = AutoLockPinEvent.Update.VerificationCompleted(),
                expected = AutoLockTestData.BaseLoadedState.copy(
                    closeScreenEffect = Effect.of(Unit)
                )
            ),
            TestInput(
                state = AutoLockTestData.BaseLoadedState,
                event = AutoLockPinEvent.Update.VerificationCompleted(AutoLockTestData.BaseContinuationDestination),
                expected = AutoLockTestData.BaseLoadedState.copy(
                    navigateEffect = Effect.of(
                        AutoLockTestData.BaseContinuationDestination.destination.toDecodedValue()
                    )
                )
            ),

            TestInput(
                state = AutoLockTestData.BaseLoadedState,
                event = AutoLockPinEvent.Update.MovedToStep(PinInsertionStep.PinConfirmation),
                expected = AutoLockTestData.BaseLoadedState.copy(
                    topBarState = AutoLockTestData.BaseTopBarState.copy(
                        stepMapper.toTopBarUiModel(PinInsertionStep.PinConfirmation)
                    ),
                    pinInsertionState = AutoLockTestData.BasePinInsertionState.copy(
                        step = PinInsertionStep.PinConfirmation
                    ),
                    confirmButtonState = AutoLockTestData.BaseConfirmButtonState.copy(
                        stepMapper.toConfirmButtonUiModel(isEnabled = false, PinInsertionStep.PinConfirmation)
                    )
                )
            ),
            TestInput(
                state = AutoLockTestData.BaseLoadedState.copy(
                    pinInsertionErrorEffect = Effect.of(AutoLockTestData.PlaceholderTextUiModel)
                ),
                event = AutoLockPinEvent.Update.MovedToStep(PinInsertionStep.PinConfirmation),
                expected = AutoLockTestData.BaseLoadedState.copy(
                    topBarState = AutoLockTestData.BaseTopBarState.copy(
                        stepMapper.toTopBarUiModel(PinInsertionStep.PinConfirmation)
                    ),
                    pinInsertionState = AutoLockTestData.BasePinInsertionState.copy(
                        step = PinInsertionStep.PinConfirmation
                    ),
                    confirmButtonState = AutoLockTestData.BaseConfirmButtonState.copy(
                        stepMapper.toConfirmButtonUiModel(isEnabled = false, PinInsertionStep.PinConfirmation)
                    ),
                    pinInsertionErrorEffect = Effect.empty()
                )
            )
        )
    }

    data class TestInput(
        val state: AutoLockPinState,
        val event: AutoLockPinEvent,
        val expected: AutoLockPinState
    )
}
