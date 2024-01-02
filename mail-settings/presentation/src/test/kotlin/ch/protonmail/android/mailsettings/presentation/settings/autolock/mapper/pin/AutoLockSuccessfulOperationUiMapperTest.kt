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
import ch.protonmail.android.mailsettings.presentation.settings.autolock.model.pin.PinInsertionStep
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
internal class AutoLockSuccessfulOperationUiMapperTest(private val testInput: TestInput) {

    private val autoLockSuccessfulOperationUiMapper = AutoLockSuccessfulOperationUiMapper()

    @Test
    fun `should map the starting step to the to the appropriate snackbar text ui model`() = with(testInput) {
        // When
        val actual = autoLockSuccessfulOperationUiMapper.toTextUiModel(startingStep)

        // Then
        assertEquals(expectedValue, actual)
    }

    private companion object {

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = arrayOf(
            TestInput(
                startingStep = PinInsertionStep.PinChange,
                TextUiModel(R.string.mail_settings_pin_insertion_changed_success)
            ),
            TestInput(
                startingStep = PinInsertionStep.PinInsertion,
                TextUiModel(R.string.mail_settings_pin_insertion_created_success)
            ),
            TestInput(
                startingStep = PinInsertionStep.PinConfirmation,
                null
            ),
            TestInput(
                startingStep = PinInsertionStep.PinVerification,
                null
            )
        )
    }

    data class TestInput(
        val startingStep: PinInsertionStep,
        val expectedValue: TextUiModel?
    )
}
