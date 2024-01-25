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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

class ObserveAutoLockBiometricsStateTest {

    @Test
    fun `should get biometrics user preference and system state when observing changes`() = runTest {
        // Given
        val autoLockRepository = mockk<AutoLockRepository>()
        val biometricsSystemStateRepository = mockk<BiometricsSystemStateRepository>()
        val preference = AutoLockBiometricsPreference(enabled = true).right()
        val systemState = BiometricsSystemState.BiometricEnrolled
        coEvery { autoLockRepository.observeAutoLockBiometricsPreference() } returns flowOf(preference)
        coEvery { biometricsSystemStateRepository.observe() } returns flowOf(systemState)

        // When
        val observeAutoLockBiometricsState =
            ObserveAutoLockBiometricsState(autoLockRepository, biometricsSystemStateRepository)
        val result = observeAutoLockBiometricsState().first()

        // Then
        assertEquals(AutoLockBiometricsState.BiometricsAvailable.BiometricsEnrolled(true), result)
        coVerify { autoLockRepository.observeAutoLockBiometricsPreference() }
        coVerify { biometricsSystemStateRepository.observe() }
    }
}
