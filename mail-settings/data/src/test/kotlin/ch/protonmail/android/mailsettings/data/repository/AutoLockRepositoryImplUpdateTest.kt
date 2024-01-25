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

import java.io.IOException
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.PreferencesError
import ch.protonmail.android.mailsettings.data.repository.local.AutoLockLocalDataSource
import ch.protonmail.android.mailsettings.data.usecase.DecryptSerializableValue
import ch.protonmail.android.mailsettings.data.usecase.EncryptSerializableValue
import ch.protonmail.android.mailsettings.domain.model.autolock.AutoLockAttemptPendingStatus
import ch.protonmail.android.mailsettings.domain.model.autolock.AutoLockEnabledEncryptedValue
import ch.protonmail.android.mailsettings.domain.model.autolock.AutoLockEncryptedAttemptPendingStatus
import ch.protonmail.android.mailsettings.domain.model.autolock.AutoLockEncryptedInterval
import ch.protonmail.android.mailsettings.domain.model.autolock.AutoLockEncryptedLastForegroundMillis
import ch.protonmail.android.mailsettings.domain.model.autolock.AutoLockEncryptedPin
import ch.protonmail.android.mailsettings.domain.model.autolock.AutoLockEncryptedRemainingAttempts
import ch.protonmail.android.mailsettings.domain.model.autolock.AutoLockInterval
import ch.protonmail.android.mailsettings.domain.model.autolock.AutoLockLastForegroundMillis
import ch.protonmail.android.mailsettings.domain.model.autolock.AutoLockPin
import ch.protonmail.android.mailsettings.domain.model.autolock.AutoLockPreference
import ch.protonmail.android.mailsettings.domain.model.autolock.AutoLockRemainingAttempts
import ch.protonmail.android.mailsettings.domain.model.autolock.biometric.AutoLockBiometricsEncryptedValue
import ch.protonmail.android.mailsettings.domain.model.autolock.biometric.AutoLockBiometricsPreference
import ch.protonmail.android.mailsettings.domain.repository.AutoLockPreferenceError
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import me.proton.core.crypto.common.keystore.KeyStoreCrypto
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

internal class AutoLockRepositoryImplUpdateTest {

    private val autoLockLocalDataSource = mockk<AutoLockLocalDataSource>()
    private val keyStoreCrypto = mockk<KeyStoreCrypto>()

    // Use cases here expose inline functions, cannot be mocked/spied.
    private val encryptSerializableValue = EncryptSerializableValue(keyStoreCrypto)
    private val decryptSerializableValue = DecryptSerializableValue(keyStoreCrypto)

    private val autoLockRepository = AutoLockRepositoryImpl(
        autoLockLocalDataSource,
        encryptSerializableValue,
        decryptSerializableValue
    )

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `should return unit when auto lock biometric preference update is successful`() = runTest {
        // Given
        val biometricPreference = AutoLockBiometricsPreference(true)
        val biometricEncryptedPreference = AutoLockBiometricsEncryptedValue("encrypted")
        coEvery {
            autoLockLocalDataSource.updateAutoLockBiometricEncryptedValue(biometricEncryptedPreference)
        } returns Unit.right()
        expectSuccessfulEncryption()

        // When
        val result = autoLockRepository.updateAutoLockBiometricsPreference(biometricPreference)

        // Then
        assertEquals(Unit.right(), result)
    }

    @Test
    fun `should return unit when auto lock enabled update is successful`() = runTest {
        // Given
        val autoLockPreference = AutoLockPreference(true)
        val autoLockEncryptedPreference = AutoLockEnabledEncryptedValue("encrypted")
        coEvery {
            autoLockLocalDataSource.updateAutoLockEnabledEncryptedValue(autoLockEncryptedPreference)
        } returns Unit.right()
        expectSuccessfulEncryption()

        // When
        val result = autoLockRepository.updateAutoLockEnabledValue(autoLockPreference)

        // Then
        assertEquals(Unit.right(), result)
    }

    @Test
    fun `should propagate data store error when auto lock value update is not successful`() = runTest {
        // Given
        val autoLockPreference = AutoLockPreference(true)
        val autoLockEncryptedPreference = AutoLockEnabledEncryptedValue("encrypted")
        coEvery {
            autoLockLocalDataSource.updateAutoLockEnabledEncryptedValue(autoLockEncryptedPreference)
        } returns PreferencesError.left()

        expectSuccessfulEncryption()

        // When
        val result = autoLockRepository.updateAutoLockEnabledValue(autoLockPreference)

        // Then
        assertEquals(AutoLockPreferenceError.DataStoreError.left(), result)
    }

    @Test
    fun `should propagate encryption error when auto lock value encryption is not successful`() = runTest {
        // Given
        val autoLockPreference = AutoLockPreference(true)
        expectEncryptionError()

        // When
        val result = autoLockRepository.updateAutoLockEnabledValue(autoLockPreference)

        // Then
        assertEquals(baseEncryptionError.left(), result)
    }

    @Test
    fun `should return unit when auto lock interval update is successful`() = runTest {
        // Given
        val autoLockIntervalPreference = AutoLockInterval.FiveMinutes
        val autoLockIntervalEncryptedPreference = AutoLockEncryptedInterval("encrypted")
        coEvery {
            autoLockLocalDataSource.updateAutoLockEncryptedInterval(autoLockIntervalEncryptedPreference)
        } returns Unit.right()

        expectSuccessfulEncryption()

        // When
        val result = autoLockRepository.updateAutoLockInterval(autoLockIntervalPreference)

        // Then
        assertEquals(Unit.right(), result)
    }

    @Test
    fun `should propagate data store error when auto lock interval update is not successful`() = runTest {
        // Given
        val autoLockIntervalPreference = AutoLockInterval.FiveMinutes
        val autoLockIntervalEncryptedPreference = AutoLockEncryptedInterval("encrypted")
        coEvery {
            autoLockLocalDataSource.updateAutoLockEncryptedInterval(autoLockIntervalEncryptedPreference)
        } returns PreferencesError.left()

        expectSuccessfulEncryption()

        // When
        val result = autoLockRepository.updateAutoLockInterval(autoLockIntervalPreference)

        // Then
        assertEquals(AutoLockPreferenceError.DataStoreError.left(), result)
    }

    @Test
    fun `should propagate encryption error when auto lock interval update encryption is not successful`() = runTest {
        // Given
        val autoLockIntervalPreference = AutoLockInterval.FiveMinutes
        expectEncryptionError()

        // When
        val result = autoLockRepository.updateAutoLockInterval(autoLockIntervalPreference)

        // Then
        assertEquals(baseEncryptionError.left(), result)
    }

    @Test
    fun `should return unit when auto lock foreground timestamp update is successful`() = runTest {
        // Given
        val autoLockLastForegroundTimestamp = AutoLockLastForegroundMillis(123L)
        val autoLockLastForegroundEncryptedTimestamp = AutoLockEncryptedLastForegroundMillis("encrypted")
        coEvery {
            autoLockLocalDataSource.updateLastEncryptedForegroundMillis(autoLockLastForegroundEncryptedTimestamp)
        } returns Unit.right()

        expectSuccessfulEncryption()

        // When
        val result = autoLockRepository.updateLastForegroundMillis(autoLockLastForegroundTimestamp)

        // Then
        assertEquals(Unit.right(), result)
    }

    @Test
    fun `should propagate data store error when auto lock foreground timestamp update is not successful`() = runTest {
        // Given
        val autoLockLastForegroundTimestamp = AutoLockLastForegroundMillis(123L)
        val autoLockLastForegroundEncryptedTimestamp = AutoLockEncryptedLastForegroundMillis("encrypted")
        coEvery {
            autoLockLocalDataSource.updateLastEncryptedForegroundMillis(autoLockLastForegroundEncryptedTimestamp)
        } returns PreferencesError.left()

        expectSuccessfulEncryption()

        // When
        val result = autoLockRepository.updateLastForegroundMillis(autoLockLastForegroundTimestamp)

        // Then
        assertEquals(AutoLockPreferenceError.DataStoreError.left(), result)
    }

    @Test
    fun `should propagate encryption error when auto lock foreground timestamp update encryption is not successful`() =
        runTest {
            // Given
            val autoLockLastForegroundTimestamp = AutoLockLastForegroundMillis(123L)
            expectEncryptionError()

            // When
            val result = autoLockRepository.updateLastForegroundMillis(autoLockLastForegroundTimestamp)

            // Then
            assertEquals(baseEncryptionError.left(), result)
        }

    @Test
    fun `should return unit when auto lock pin update is successful`() = runTest {
        // Given
        val autoLockPin = AutoLockPin("123")
        val autoLockPinEncrypted = AutoLockEncryptedPin("encrypted")
        coEvery {
            autoLockLocalDataSource.updateAutoLockEncryptedPin(autoLockPinEncrypted)
        } returns Unit.right()

        expectSuccessfulEncryption()

        // When
        val result = autoLockRepository.updateAutoLockPin(autoLockPin)

        // Then
        assertEquals(Unit.right(), result)
    }

    @Test
    fun `should propagate data store error when auto lock pin update is not successful`() = runTest {
        // Given
        val autoLockPin = AutoLockPin("123")
        val autoLockPinEncrypted = AutoLockEncryptedPin("encrypted")
        coEvery {
            autoLockLocalDataSource.updateAutoLockEncryptedPin(autoLockPinEncrypted)
        } returns PreferencesError.left()

        expectSuccessfulEncryption()

        // When
        val result = autoLockRepository.updateAutoLockPin(autoLockPin)

        // Then
        assertEquals(AutoLockPreferenceError.DataStoreError.left(), result)
    }

    @Test
    fun `should propagate encryption error when auto lock pin update encryption is not successful`() = runTest {
        // Given
        val autoLockPin = AutoLockPin("123")
        expectEncryptionError()

        // When
        val result = autoLockRepository.updateAutoLockPin(autoLockPin)

        // Then
        assertEquals(baseEncryptionError.left(), result)
    }

    @Test
    fun `should return unit when auto lock remaining attempts update is successful`() = runTest {
        // Given
        val remainingAttempts = AutoLockRemainingAttempts(10)
        val autoLockEncryptedAttempts = AutoLockEncryptedRemainingAttempts("encrypted")
        coEvery {
            autoLockLocalDataSource.updateAutoLockAttemptsLeft(autoLockEncryptedAttempts)
        } returns Unit.right()

        expectSuccessfulEncryption()

        // When
        val result = autoLockRepository.updateAutoLockRemainingAttempts(remainingAttempts)

        // Then
        assertEquals(Unit.right(), result)
    }

    @Test
    fun `should propagate data store error if auto lock remaining attempts update is not successful`() = runTest {
        // Given
        val remainingAttempts = AutoLockRemainingAttempts(10)
        val autoLockEncryptedAttempts = AutoLockEncryptedRemainingAttempts("encrypted")
        coEvery {
            autoLockLocalDataSource.updateAutoLockAttemptsLeft(autoLockEncryptedAttempts)
        } returns PreferencesError.left()

        expectSuccessfulEncryption()

        // When
        val result = autoLockRepository.updateAutoLockRemainingAttempts(remainingAttempts)

        // Then
        assertEquals(AutoLockPreferenceError.DataStoreError.left(), result)
    }

    @Test
    fun `should propagate encryption error when auto lock remaining attempts update is not successful`() = runTest {
        // Given
        val remainingAttempts = AutoLockRemainingAttempts(10)
        expectEncryptionError()

        // When
        val result = autoLockRepository.updateAutoLockRemainingAttempts(remainingAttempts)

        // Then
        assertEquals(baseEncryptionError.left(), result)
    }

    @Test
    fun `should return unit when auto lock pending status update is successful`() = runTest {
        // Given
        val pendingAttemptStatus = AutoLockAttemptPendingStatus(true)
        val encryptedPendingAttempt = AutoLockEncryptedAttemptPendingStatus("encrypted")
        coEvery {
            autoLockLocalDataSource.updateAutoLockPendingAttempt(encryptedPendingAttempt)
        } returns Unit.right()

        expectSuccessfulEncryption()

        // When
        val result = autoLockRepository.updateAutoLockAttemptPendingStatus(pendingAttemptStatus)

        // Then
        assertEquals(Unit.right(), result)
    }

    @Test
    fun `should propagate data store error when auto lock pending status update is not successful`() = runTest {
        // Given
        val pendingAttemptStatus = AutoLockAttemptPendingStatus(true)
        val encryptedPendingAttempt = AutoLockEncryptedAttemptPendingStatus("encrypted")
        coEvery {
            autoLockLocalDataSource.updateAutoLockPendingAttempt(encryptedPendingAttempt)
        } returns PreferencesError.left()

        expectSuccessfulEncryption()

        // When
        val result = autoLockRepository.updateAutoLockAttemptPendingStatus(pendingAttemptStatus)

        // Then
        assertEquals(AutoLockPreferenceError.DataStoreError.left(), result)
    }

    @Test
    fun `should propagate encryption error when auto lock pending status update is not successful`() = runTest {
        // Given
        val pendingAttemptStatus = AutoLockAttemptPendingStatus(true)
        expectEncryptionError()

        // When
        val result = autoLockRepository.updateAutoLockAttemptPendingStatus(pendingAttemptStatus)

        // Then
        assertEquals(baseEncryptionError.left(), result)
    }


    private fun expectSuccessfulEncryption() {
        coEvery { keyStoreCrypto.encrypt(any<String>()) } returns "encrypted"
    }

    private fun expectEncryptionError() {
        coEvery { keyStoreCrypto.encrypt(any<String>()) } throws IOException(baseEncryptionError.message)
    }

    private companion object {

        val baseEncryptionError = AutoLockPreferenceError.SerializationError("message")
    }
}
