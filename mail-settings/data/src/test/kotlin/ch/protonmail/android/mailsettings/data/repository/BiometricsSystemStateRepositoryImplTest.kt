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

package ch.protonmail.android.mailsettings.data.repository

import ch.protonmail.android.mailsettings.domain.model.autolock.biometric.BiometricsSystemState
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.junit.Assert.assertEquals
import android.content.Context
import androidx.biometric.BiometricManager
import io.mockk.unmockkStatic
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

class BiometricsSystemStateRepositoryImplTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        mockkStatic(BiometricManager::class)
    }

    @After
    fun tearDown() {
        unmockkStatic(BiometricManager::class)
    }

    @Test
    fun `Current state should be biometric enrolled when BiometricManager returns success`() {
        // Given
        every {
            BiometricManager.from(context).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)
        } returns
            BiometricManager.BIOMETRIC_SUCCESS

        // When
        val repository = BiometricsSystemStateRepositoryImpl(context)

        // Then
        assertEquals(BiometricsSystemState.BiometricEnrolled, repository.getCurrentState())
    }

    @Test
    fun `Current state should be biometric not enrolled when BiometricManager returns error non enrolled`() {

        // Given
        every {
            BiometricManager.from(context).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)
        } returns BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED

        // When
        val repository = BiometricsSystemStateRepositoryImpl(context)

        // Then
        assertEquals(BiometricsSystemState.BiometricNotEnrolled, repository.getCurrentState())
    }

    @Test
    fun `Current state should be biometric not available when BiometricManager returns error hw not available`() {
        // Given
        every {
            BiometricManager.from(context).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)
        } returns BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE

        // When
        val repository = BiometricsSystemStateRepositoryImpl(context)

        // Then
        assertEquals(BiometricsSystemState.BiometricNotAvailable, repository.getCurrentState())
    }

    @Test
    fun `should emit biometric enrolled when observing changes given BiometricManager returns success`() = runTest {
        // Given
        every {
            BiometricManager.from(context).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)
        } returns BiometricManager.BIOMETRIC_SUCCESS
        val repository = BiometricsSystemStateRepositoryImpl(context)

        // When
        val actual = repository.observe().first()

        // Then
        assertEquals(BiometricsSystemState.BiometricEnrolled, actual)
    }
}

