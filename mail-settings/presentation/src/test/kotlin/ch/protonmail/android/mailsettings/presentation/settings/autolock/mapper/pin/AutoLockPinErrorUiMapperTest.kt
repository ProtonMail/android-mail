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

import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailsettings.presentation.R
import ch.protonmail.android.mailsettings.presentation.settings.autolock.helpers.AutoLockTestData
import ch.protonmail.android.mailsettings.presentation.settings.autolock.model.pin.AutoLockPinEvent
import ch.protonmail.android.mailsettings.presentation.settings.autolock.model.pin.PinVerificationRemainingAttempts
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertEquals

@RunWith(Enclosed::class)
internal class AutoLockPinErrorUiMapperTest {

    @RunWith(Parameterized::class)
    internal class AutoLockPinErrorUiMapperUpdateErrorTest(private val testInput: TestInput) {

        private val autoLockPinSettingsErrorUiMapper = AutoLockPinErrorUiMapper()

        @Test
        fun `should map the update error to the appropriate ui model`() = with(testInput) {
            // When
            val actual = autoLockPinSettingsErrorUiMapper.toUiModel(updateError)

            // Then
            assertEquals(expectedValue, actual)
        }

        private companion object {

            @JvmStatic
            @Parameterized.Parameters(name = "{0}")
            fun data() = arrayOf(
                TestInput(
                    AutoLockPinEvent.Update.Error.NotMatchingPins,
                    TextUiModel(R.string.mail_settings_pin_insertion_error_no_match)
                ),
                TestInput(
                    AutoLockPinEvent.Update.Error.UnknownError,
                    TextUiModel(R.string.mail_settings_pin_insertion_error_unknown)
                ),
                TestInput(
                    AutoLockPinEvent.Update.Error.WrongPinCode(AutoLockTestData.OneRemainingAttempt),
                    TextUiModel.PluralisedText(
                        R.plurals.mail_settings_pin_insertion_error_wrong_code_ultimatum,
                        AutoLockTestData.OneRemainingAttempt.value
                    )
                ),
                TestInput(
                    AutoLockPinEvent.Update.Error.WrongPinCode(AutoLockTestData.NineRemainingAttempts),
                    TextUiModel.PluralisedText(
                        R.plurals.mail_settings_pin_insertion_error_wrong_code,
                        AutoLockTestData.NineRemainingAttempts.value
                    )
                )
            )
        }

        data class TestInput(
            val updateError: AutoLockPinEvent.Update.Error,
            val expectedValue: TextUiModel
        )
    }

    @RunWith(Parameterized::class)
    internal class AutoLockPinErrorUiMapperRemainingAttemptsTest(private val testInput: TestInput) {

        private val autoLockPinSettingsErrorUiMapper = AutoLockPinErrorUiMapper()

        @Test
        fun `should map the remaining attempts to the appropriate error ui model`() = with(testInput) {
            // When
            val actual = autoLockPinSettingsErrorUiMapper.toUiModel(remainingAttempts)

            // Then
            assertEquals(expectedValue, actual)
        }

        companion object {

            @JvmStatic
            @Parameterized.Parameters(name = "{0}")
            fun data() = arrayOf(
                TestInput(PinVerificationRemainingAttempts.Default, null),
                TestInput(
                    PinVerificationRemainingAttempts(1),
                    TextUiModel.PluralisedText(R.plurals.mail_settings_pin_insertion_error_wrong_code_ultimatum, 1)
                ),
                TestInput(
                    PinVerificationRemainingAttempts(6),
                    TextUiModel.PluralisedText(R.plurals.mail_settings_pin_insertion_error_wrong_code, 6)
                )
            )
        }

        data class TestInput(
            val remainingAttempts: PinVerificationRemainingAttempts,
            val expectedValue: TextUiModel?
        )
    }
}
