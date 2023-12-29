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
import ch.protonmail.android.mailsettings.domain.model.autolock.AutoLockAttemptPendingStatus
import ch.protonmail.android.mailsettings.domain.repository.AutoLockPreferenceError
import ch.protonmail.android.mailsettings.domain.repository.AutoLockRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

internal class ToggleAutoLockAttemptPendingStatusTest {

    private val autoLockRepository = mockk<AutoLockRepository>()
    private val toggleAutoLockAttemptStatus = ToggleAutoLockAttemptPendingStatus(autoLockRepository)

    @Test
    fun `should propagate the value when the repo returns a success`() = runTest {
        // Given
        val newPreference = AutoLockAttemptPendingStatus(true)
        val expectedValue = Unit.right()
        coEvery { autoLockRepository.updateAutoLockAttemptPendingStatus(newPreference) } returns expectedValue

        // When
        val actual = toggleAutoLockAttemptStatus(newPreference.value)

        // Then
        assertEquals(expectedValue, actual)
    }

    @Test
    fun `should propagate the error when the value cannot be updated`() = runTest {
        // Given
        val newPreference = AutoLockAttemptPendingStatus(true)
        val expectedValue = AutoLockPreferenceError.DataStoreError.left()
        coEvery { autoLockRepository.updateAutoLockAttemptPendingStatus(newPreference) } returns expectedValue

        // When
        val actual = toggleAutoLockAttemptStatus(newPreference.value)

        // Then
        assertEquals(expectedValue, actual)
    }
}
