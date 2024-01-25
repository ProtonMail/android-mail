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
import ch.protonmail.android.mailsettings.presentation.settings.autolock.model.pin.PinInsertionStep
import org.junit.Assert.assertEquals
import org.junit.Test

class AutoLockBiometricPinUiMapperTest {

    @Test
    fun `should display biometric pin when state is biometric enrolled with enabled preference`() {
        // Given
        val pinStep = PinInsertionStep.PinVerification
        val mapper = AutoLockBiometricPinUiMapper()
        val state = AutoLockBiometricsState.BiometricsAvailable.BiometricsEnrolled(enabled = true)

        // When
        val uiModel = mapper.toUiModel(state, pinStep)

        // Then
        assertEquals(true, uiModel.shouldDisplayButton)
    }

    @Test
    fun `should not display biometric pin when pin step is not pin verification`() {
        // Given
        val pinStep = PinInsertionStep.PinInsertion
        val mapper = AutoLockBiometricPinUiMapper()
        val state = AutoLockBiometricsState.BiometricsAvailable.BiometricsEnrolled(enabled = true)

        // When
        val uiModel = mapper.toUiModel(state, pinStep)

        // Then
        assertEquals(false, uiModel.shouldDisplayButton)
    }

    @Test
    fun `should not display biometric pin when state is biometric enrolled with disabled preference`() {
        // Given
        val pinStep = PinInsertionStep.PinVerification
        val mapper = AutoLockBiometricPinUiMapper()
        val state = AutoLockBiometricsState.BiometricsAvailable.BiometricsEnrolled(enabled = false)

        // When
        val uiModel = mapper.toUiModel(state, pinStep)

        // Then
        assertEquals(false, uiModel.shouldDisplayButton)
    }

    @Test
    fun `should not display biometric pin when state is biometric not available`() {
        // Given
        val pinStep = PinInsertionStep.PinVerification
        val mapper = AutoLockBiometricPinUiMapper()
        val state = AutoLockBiometricsState.BiometricsNotAvailable

        // When
        val uiModel = mapper.toUiModel(state, pinStep)

        // Then
        assertEquals(false, uiModel.shouldDisplayButton)
    }

    @Test
    fun `should not display biometric pin button when state is biometric not enrolled`() {
        // Given
        val pinStep = PinInsertionStep.PinVerification
        val mapper = AutoLockBiometricPinUiMapper()
        val state = AutoLockBiometricsState.BiometricsAvailable.BiometricsNotEnrolled

        // When
        val uiModel = mapper.toUiModel(state, pinStep)

        // Then
        assertEquals(false, uiModel.shouldDisplayButton)
    }

    @Test
    fun `should not display biometric pin button when biometric is not available`() {
        // Given
        val pinStep = PinInsertionStep.PinVerification
        val mapper = AutoLockBiometricPinUiMapper()
        val state = AutoLockBiometricsState.BiometricsNotAvailable

        // When
        val uiModel = mapper.toUiModel(state, pinStep)

        // Then
        assertEquals(false, uiModel.shouldDisplayButton)
    }
}
