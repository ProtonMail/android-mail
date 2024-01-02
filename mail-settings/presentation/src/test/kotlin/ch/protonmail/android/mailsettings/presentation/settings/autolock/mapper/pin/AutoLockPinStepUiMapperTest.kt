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

package ch.protonmail.android.mailsettings.presentation.settings.autolock.mapper.pin

import ch.protonmail.android.mailsettings.presentation.R
import ch.protonmail.android.mailsettings.presentation.settings.autolock.model.pin.ConfirmButtonUiModel
import ch.protonmail.android.mailsettings.presentation.settings.autolock.model.pin.PinInsertionStep
import ch.protonmail.android.mailsettings.presentation.settings.autolock.model.pin.SignOutUiModel
import ch.protonmail.android.mailsettings.presentation.settings.autolock.model.pin.TopBarUiModel
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertEquals

@RunWith(Enclosed::class)
internal class AutoLockPinStepUiMapperTest {

    @RunWith(Parameterized::class)
    internal class AutoLockPinStepTopBarUiMapperTest(private val testInput: TestInput) {

        private val mapper = AutoLockPinStepUiMapper()

        @Test
        fun `should map the step to the appropriate top bar ui model`() = with(testInput) {
            // When
            val actual = mapper.toTopBarUiModel(step)

            // Then
            assertEquals(expectedValue, actual)
        }

        private companion object {

            @JvmStatic
            @Parameterized.Parameters(name = "{0}")
            fun data() = arrayOf(
                TestInput(
                    PinInsertionStep.PinChange,
                    TopBarUiModel(true, R.string.mail_settings_pin_insertion_input_title)
                ),
                TestInput(
                    PinInsertionStep.PinInsertion,
                    TopBarUiModel(true, R.string.mail_settings_pin_insertion_set_title)
                ),
                TestInput(
                    PinInsertionStep.PinConfirmation,
                    TopBarUiModel(true, R.string.mail_settings_pin_insertion_confirm_title)
                ),
                TestInput(
                    PinInsertionStep.PinVerification,
                    TopBarUiModel(false, R.string.mail_settings_pin_insertion_input_title)
                )
            )
        }

        data class TestInput(
            val step: PinInsertionStep,
            val expectedValue: TopBarUiModel
        )
    }

    @RunWith(Parameterized::class)
    internal class AutoLockPinStepConfirmationButtonUiMapperTest(private val testInput: TestInput) {

        private val mapper = AutoLockPinStepUiMapper()

        @Test
        fun `should map to the appropriate confirmation button ui model`() = with(testInput) {
            // When
            val actual = mapper.toConfirmButtonUiModel(isEnabled, step)

            // Then
            assertEquals(expectedValue, actual)
        }

        companion object {

            @JvmStatic
            @Parameterized.Parameters(name = "{0}")
            fun data() = arrayOf(
                TestInput(
                    isEnabled = false,
                    PinInsertionStep.PinChange,
                    ConfirmButtonUiModel(false, R.string.mail_settings_pin_insertion_button_confirm)
                ),
                TestInput(
                    isEnabled = false,
                    PinInsertionStep.PinInsertion,
                    ConfirmButtonUiModel(false, R.string.mail_settings_pin_insertion_button_confirm)
                ),
                TestInput(
                    isEnabled = false,
                    PinInsertionStep.PinVerification,
                    ConfirmButtonUiModel(false, R.string.mail_settings_pin_insertion_button_confirm)
                ),
                TestInput(
                    isEnabled = false,
                    PinInsertionStep.PinConfirmation,
                    ConfirmButtonUiModel(false, R.string.mail_settings_pin_insertion_button_create)
                ),
                TestInput(
                    isEnabled = true,
                    PinInsertionStep.PinConfirmation,
                    ConfirmButtonUiModel(true, R.string.mail_settings_pin_insertion_button_create)
                )
            )
        }

        data class TestInput(
            val isEnabled: Boolean,
            val step: PinInsertionStep,
            val expectedValue: ConfirmButtonUiModel
        )
    }

    @RunWith(Parameterized::class)
    internal class AutoLockPinStepSignOutButtonUiMapperTest(private val testInput: TestInput) {

        private val mapper = AutoLockPinStepUiMapper()

        @Test
        fun `should map to the appropriate sign out button ui model`() = with(testInput) {
            // When
            val actual = mapper.toSignOutUiModel(step)

            // Then
            assertEquals(expectedValue, actual)
        }

        companion object {

            @JvmStatic
            @Parameterized.Parameters(name = "{0}")
            fun data() = arrayOf(
                TestInput(
                    PinInsertionStep.PinInsertion,
                    SignOutUiModel(isDisplayed = false, isRequested = false)
                ),
                TestInput(
                    PinInsertionStep.PinConfirmation,
                    SignOutUiModel(isDisplayed = false, isRequested = false)
                ),
                TestInput(
                    PinInsertionStep.PinChange,
                    SignOutUiModel(isDisplayed = false, isRequested = false)
                ),
                TestInput(
                    PinInsertionStep.PinVerification,
                    SignOutUiModel(isDisplayed = true, isRequested = false)
                )
            )
        }

        data class TestInput(
            val step: PinInsertionStep,
            val expectedValue: SignOutUiModel
        )
    }
}
