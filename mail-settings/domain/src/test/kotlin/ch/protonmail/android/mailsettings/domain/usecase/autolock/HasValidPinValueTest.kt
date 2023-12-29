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

package ch.protonmail.android.mailsettings.domain.usecase.autolock

import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailsettings.domain.model.autolock.AutoLockPin
import ch.protonmail.android.mailsettings.domain.repository.AutoLockPreferenceError
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.test.assertFalse

internal class HasValidPinValueTest {

    private val observeValidPinValue = mockk<ObserveAutoLockPinValue>()
    private val hasValidPinValue = HasValidPinValue(observeValidPinValue)

    @Test
    fun `should return true when the pin is valid`() = runTest {
        // Given
        every { observeValidPinValue() } returns flowOf(AutoLockPin("1234").right())

        // When
        val actual = hasValidPinValue()

        // Then
        assertTrue(actual)
    }

    @Test
    fun `should return false when the observer returns an error`() = runTest {
        // Given
        every { observeValidPinValue() } returns flowOf(AutoLockPreferenceError.DataStoreError.left())

        // When
        val actual = hasValidPinValue()

        // Then
        assertFalse(actual)
    }

    @Test
    fun `should return false when the pin is empty`() = runTest {
        // Given
        every { observeValidPinValue() } returns flowOf(AutoLockPin("").right())

        // When
        val actual = hasValidPinValue()

        // Then
        assertFalse(actual)
    }
}
