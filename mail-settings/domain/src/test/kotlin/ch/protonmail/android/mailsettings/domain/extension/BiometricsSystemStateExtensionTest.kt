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

package ch.protonmail.android.mailsettings.domain.extension

import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailsettings.domain.model.autolock.biometric.AutoLockBiometricsPreference
import ch.protonmail.android.mailsettings.domain.model.autolock.biometric.AutoLockBiometricsState
import ch.protonmail.android.mailsettings.domain.model.autolock.biometric.BiometricsSystemState
import ch.protonmail.android.mailsettings.domain.repository.AutoLockPreferenceError
import org.junit.Assert.assertEquals
import org.junit.Test

class BiometricsSystemStateExtensionTest {

    @Test
    fun `should return biometrics is not available given system state is biometrics not available on the device`() {
        // Given
        val biometricsSystemState = BiometricsSystemState.BiometricNotAvailable
        val biometricsPreference = AutoLockBiometricsPreference(enabled = true).right()
        // When
        val result = biometricsSystemState.toAutoLockBiometricsState(biometricsPreference)

        // Then
        assertEquals(AutoLockBiometricsState.BiometricsNotAvailable, result)
    }

    @Test
    fun `should return biometric enrolled with enabled preference, given system state is enrolled and user enabled`() {
        // Given
        val biometricsSystemState = BiometricsSystemState.BiometricEnrolled
        val biometricsPreference = AutoLockBiometricsPreference(enabled = true).right()

        // When
        val result = biometricsSystemState.toAutoLockBiometricsState(biometricsPreference)

        // Then
        assertEquals(AutoLockBiometricsState.BiometricsAvailable.BiometricsEnrolled(true), result)
    }

    //
    @Test
    fun `should return biometric enrolled with disabled, given system state is enrolled and user disabled`() {
        // Given
        val biometricsSystemState = BiometricsSystemState.BiometricEnrolled
        val biometricsPreference = AutoLockBiometricsPreference(enabled = false).right()

        // When
        val result = biometricsSystemState.toAutoLockBiometricsState(biometricsPreference)

        // Then
        assertEquals(AutoLockBiometricsState.BiometricsAvailable.BiometricsEnrolled(false), result)
    }

    @Test
    fun `should return biometric not enrolled, given system state is not enrolled`() {
        // Given
        val biometricsSystemState = BiometricsSystemState.BiometricNotEnrolled
        val biometricsPreference = AutoLockBiometricsPreference(enabled = false).right()

        // When
        val result = biometricsSystemState.toAutoLockBiometricsState(biometricsPreference)

        // Then
        assertEquals(AutoLockBiometricsState.BiometricsAvailable.BiometricsNotEnrolled, result)
    }

    @Test
    fun `should return biometric enrolled with disabled, given system state is enrolled and preference has error`() {
        // Given
        val biometricsSystemState = BiometricsSystemState.BiometricEnrolled
        val biometricsPreference = AutoLockPreferenceError.DataStoreError.left()

        // When
        val result = biometricsSystemState.toAutoLockBiometricsState(biometricsPreference)

        // Then
        assertEquals(AutoLockBiometricsState.BiometricsAvailable.BiometricsEnrolled(false), result)
    }
}
