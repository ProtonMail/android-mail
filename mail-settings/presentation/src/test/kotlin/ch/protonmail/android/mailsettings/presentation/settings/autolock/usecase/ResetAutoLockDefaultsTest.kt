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

package ch.protonmail.android.mailsettings.presentation.settings.autolock.usecase

import arrow.core.right
import ch.protonmail.android.mailsettings.domain.model.autolock.AutoLockPin
import ch.protonmail.android.mailsettings.domain.usecase.autolock.SaveAutoLockPin
import ch.protonmail.android.mailsettings.domain.usecase.autolock.ToggleAutoLockAttemptPendingStatus
import ch.protonmail.android.mailsettings.domain.usecase.autolock.ToggleAutoLockEnabled
import ch.protonmail.android.mailsettings.domain.usecase.autolock.UpdateRemainingAutoLockAttempts
import io.mockk.coEvery
import io.mockk.coVerifySequence
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class ResetAutoLockDefaultsTest {

    private val toggleAutoLockEnabled = mockk<ToggleAutoLockEnabled>()
    private val updateRemainingAutoLockAttempts = mockk<UpdateRemainingAutoLockAttempts>()
    private val saveAutoLockPin = mockk<SaveAutoLockPin>()
    private val toggleAutoLockPendingAttemptStatus = mockk<ToggleAutoLockAttemptPendingStatus>()

    private val resetAutoLockDefaults = ResetAutoLockDefaults(
        toggleAutoLockEnabled,
        updateRemainingAutoLockAttempts,
        saveAutoLockPin,
        toggleAutoLockPendingAttemptStatus
    )

    @Test
    fun `should toggle preference, restore the auto lock attempts and save an empty pin`() = runTest {
        // Given
        coEvery { toggleAutoLockEnabled(ExpectedDefaults.AutoLockPreference) } returns Unit.right()
        coEvery { updateRemainingAutoLockAttempts(ExpectedDefaults.AutoLockRemainingAttempts) } returns Unit.right()
        coEvery { saveAutoLockPin(ExpectedDefaults.AutoLockPin) } returns Unit.right()
        coEvery { toggleAutoLockPendingAttemptStatus(ExpectedDefaults.AutoLockPendingStatus) } returns Unit.right()

        // When
        resetAutoLockDefaults()

        // Then
        coVerifySequence {
            toggleAutoLockEnabled(ExpectedDefaults.AutoLockPreference)
            saveAutoLockPin(ExpectedDefaults.AutoLockPin)
            updateRemainingAutoLockAttempts(ExpectedDefaults.AutoLockRemainingAttempts)
            toggleAutoLockPendingAttemptStatus(ExpectedDefaults.AutoLockPendingStatus)
        }
    }

    private object ExpectedDefaults {

        const val AutoLockPreference = false
        val AutoLockPin = AutoLockPin("")
        const val AutoLockRemainingAttempts = 10
        const val AutoLockPendingStatus = false
    }
}
