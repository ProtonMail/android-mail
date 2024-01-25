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

import ch.protonmail.android.mailsettings.domain.model.autolock.biometric.AutoLockBiometricsState
import org.junit.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class AutoLockBiometricPromptUiMapperTest {

    @Test
    fun `should return effect to show biometric prompt when biometric enrolled with enabled preference`() {
        // Given
        val mapper = AutoLockBiometricPromptUiMapper()
        val state = AutoLockBiometricsState.BiometricsAvailable.BiometricsEnrolled(enabled = true)

        // When
        val uiModel = mapper.toUiModel(state)

        // Then
        assertNotNull(uiModel.consume())
    }

    @Test
    fun `should return empty effect not to show biometric prompt when biometric enrolled with disabled preference`() {
        // Given
        val mapper = AutoLockBiometricPromptUiMapper()
        val state = AutoLockBiometricsState.BiometricsAvailable.BiometricsEnrolled(enabled = false)

        // When
        val uiModel = mapper.toUiModel(state)

        // Then
        assertNull(uiModel.consume())
    }

    @Test
    fun `should return empty effect not to show biometric prompt when biometric is not available`() {
        // Given
        val mapper = AutoLockBiometricPromptUiMapper()
        val state = AutoLockBiometricsState.BiometricsNotAvailable

        // When
        val uiModel = mapper.toUiModel(state)

        // Then
        assertNull(uiModel.consume())
    }

    @Test
    fun `should return empty effect not to show biometric prompt when biometric is not enrolled`() {
        // Given
        val mapper = AutoLockBiometricPromptUiMapper()
        val state = AutoLockBiometricsState.BiometricsAvailable.BiometricsNotEnrolled

        // When
        val uiModel = mapper.toUiModel(state)

        // Then
        assertNull(uiModel.consume())
    }

}
