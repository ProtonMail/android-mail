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

package ch.protonmail.android.mailsettings.domain.usecase.autolock.biometric

import arrow.core.right
import ch.protonmail.android.mailsettings.domain.model.autolock.biometric.AutoLockBiometricsPreference
import ch.protonmail.android.mailsettings.domain.model.autolock.biometric.AutoLockBiometricsState
import ch.protonmail.android.mailsettings.domain.model.autolock.biometric.BiometricsSystemState
import ch.protonmail.android.mailsettings.domain.repository.AutoLockRepository
import ch.protonmail.android.mailsettings.domain.repository.BiometricsSystemStateRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.Test

class GetCurrentAutoLockBiometricStateTest {

    @Test
    fun `should get the current biometric user preference and current system state to return current state`() =
        runBlocking {
            // Given
            val autoLockRepository = mockk<AutoLockRepository>()
            val biometricsSystemStateRepository = mockk<BiometricsSystemStateRepository>()
            val getCurrentAutoLockBiometricState = GetCurrentAutoLockBiometricState(
                autoLockRepository, biometricsSystemStateRepository
            )

            coEvery { autoLockRepository.getCurrentAutoLockBiometricsPreference() } returns
                AutoLockBiometricsPreference(enabled = true).right()
            coEvery { biometricsSystemStateRepository.getCurrentState() } returns
                BiometricsSystemState.BiometricEnrolled

            // When
            val result = getCurrentAutoLockBiometricState()

            // Then
            coVerify { autoLockRepository.getCurrentAutoLockBiometricsPreference() }
            coVerify { biometricsSystemStateRepository.getCurrentState() }
            assertTrue(result is AutoLockBiometricsState.BiometricsAvailable.BiometricsEnrolled && result.enabled)
        }
}
