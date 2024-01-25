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
import app.cash.turbine.test
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
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.crypto.common.keystore.KeyStoreCrypto
import me.proton.core.util.kotlin.serialize
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

internal class AutoLockRepositoryFetchImplTest {

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
    fun `should return current data correctly when auto lock biometric preference is requested`() = runTest {
        // Given
        val expectedValue = AutoLockBiometricsPreference(enabled = false)
        coEvery { autoLockLocalDataSource.getAutoLockBiometricEncryptedValue() } returns
            AutoLockBiometricsEncryptedValue(BaseEncryptedDummyValue).right()

        expectSuccessfulDecryption(expectedValue.enabled.toString())

        // When
        val result = autoLockRepository.getCurrentAutoLockBiometricsPreference()

        // Then
        assertEquals(false, result.getOrNull()?.enabled)
    }

    @Test
    fun `should propagate data correctly when auto lock biometric preference is observed`() = runTest {
        // Given
        val expectedValue = AutoLockBiometricsPreference(enabled = false)
        every { autoLockLocalDataSource.observeAutoLockBiometricEncryptedValue() } returns flowOf(
            AutoLockBiometricsEncryptedValue(BaseEncryptedDummyValue).right()
        )

        expectSuccessfulDecryption(expectedValue.enabled.toString())

        // When + Then
        autoLockRepository.observeAutoLockBiometricsPreference().assertValue(expectedValue)
    }

    @Test
    fun `should propagate data store error when auto lock biometric preference cannot be retrieved`() = runTest {
        // Given
        val expectedValue = AutoLockPreferenceError.DataStoreError
        every { autoLockLocalDataSource.observeAutoLockBiometricEncryptedValue() } returns
            flowOf(PreferencesError.left())

        // When + Then
        autoLockRepository.observeAutoLockBiometricsPreference().assertError(expectedValue)
    }

    @Test
    fun `should propagate deserialization error when auto lock biometric preference cannot be decoded`() = runTest {
        // Given
        every { autoLockLocalDataSource.observeAutoLockBiometricEncryptedValue() } returns flowOf(
            AutoLockBiometricsEncryptedValue(BaseEncryptedDummyValue).right()
        )

        expectDecryptionError()

        // When + Then
        autoLockRepository.observeAutoLockBiometricsPreference().assertError(baseDecryptionError)
    }

    @Test
    fun `should propagate data correctly when auto lock enabled is observed`() = runTest {
        // Given
        val expectedValue = AutoLockPreference(isEnabled = false)
        every { autoLockLocalDataSource.observeAutoLockEnabledEncryptedValue() } returns flowOf(
            AutoLockEnabledEncryptedValue(BaseEncryptedDummyValue).right()
        )

        expectSuccessfulDecryption(expectedValue.isEnabled.toString())

        // When + Then
        autoLockRepository.observeAutoLockEnabledValue().assertValue(expectedValue)
    }

    @Test
    fun `should propagate data store error when auto lock enabled cannot be retrieved`() = runTest {
        // Given
        val expectedValue = AutoLockPreferenceError.DataStoreError
        every { autoLockLocalDataSource.observeAutoLockEnabledEncryptedValue() } returns flowOf(PreferencesError.left())

        // When + Then
        autoLockRepository.observeAutoLockEnabledValue().assertError(expectedValue)
    }

    @Test
    fun `should propagate deserialization error when auto lock enabled cannot be decoded`() = runTest {
        // Given
        every { autoLockLocalDataSource.observeAutoLockEnabledEncryptedValue() } returns flowOf(
            AutoLockEnabledEncryptedValue(BaseEncryptedDummyValue).right()
        )

        expectDecryptionError()

        // When + Then
        autoLockRepository.observeAutoLockEnabledValue().assertError(baseDecryptionError)
    }

    @Test
    fun `should propagate data correctly when auto lock interval is observed`() = runTest {
        // Given
        val expectedValue = AutoLockInterval.Immediately
        every { autoLockLocalDataSource.observeAutoLockEncryptedInterval() } returns flowOf(
            AutoLockEncryptedInterval(BaseEncryptedDummyValue).right()
        )

        expectSuccessfulDecryption("\"${expectedValue}\"")

        // When + Then
        autoLockRepository.observeAutoLockInterval().assertValue(expectedValue)
    }

    @Test
    fun `should propagate data store error when last auto lock interval cannot be retrieved`() = runTest {
        // Given
        val expectedValue = AutoLockPreferenceError.DataStoreError
        every { autoLockLocalDataSource.observeAutoLockEncryptedInterval() } returns flowOf(PreferencesError.left())

        // When + Then
        autoLockRepository.observeAutoLockInterval().assertError(expectedValue)
    }

    @Test
    fun `should propagate deserialization error when auto lock interval cannot be decoded`() = runTest {
        // Given
        every { autoLockLocalDataSource.observeAutoLockEncryptedInterval() } returns flowOf(
            AutoLockEncryptedInterval(BaseEncryptedDummyValue).right()
        )

        expectDecryptionError()

        // When + Then
        autoLockRepository.observeAutoLockInterval().assertError(baseDecryptionError)
    }

    @Test
    fun `should propagate data correctly when last foreground timestamp is observed`() = runTest {
        // Given
        val expectedValue = AutoLockLastForegroundMillis(123)
        every { autoLockLocalDataSource.observeLastEncryptedForegroundMillis() } returns flowOf(
            AutoLockEncryptedLastForegroundMillis(BaseEncryptedDummyValue).right()
        )

        expectSuccessfulDecryption(expectedValue.serialize())

        // When + Then
        autoLockRepository.observeAutoLockLastForegroundMillis().assertValue(expectedValue)
    }

    @Test
    fun `should propagate data store error when last foreground timestamp cannot be retrieved`() = runTest {
        // Given
        val expectedValue = AutoLockPreferenceError.DataStoreError
        every { autoLockLocalDataSource.observeLastEncryptedForegroundMillis() } returns flowOf(PreferencesError.left())

        // When + Then
        autoLockRepository.observeAutoLockLastForegroundMillis().assertError(expectedValue)
    }

    @Test
    fun `should propagate deserialization error when last foreground timestamp cannot be decoded`() = runTest {
        // Given
        every { autoLockLocalDataSource.observeLastEncryptedForegroundMillis() } returns flowOf(
            AutoLockEncryptedLastForegroundMillis(BaseEncryptedDummyValue).right()
        )

        expectDecryptionError()

        // When + Then
        autoLockRepository.observeAutoLockLastForegroundMillis().assertError(baseDecryptionError)
    }

    @Test
    fun `should propagate data correctly when pin data is observed`() = runTest {
        // Given
        val expectedValue = AutoLockPin("1234")
        every { autoLockLocalDataSource.observeAutoLockEncryptedPin() } returns flowOf(
            AutoLockEncryptedPin(BaseEncryptedDummyValue).right()
        )

        expectSuccessfulDecryption("\"${expectedValue.value}\"")

        // When + Then
        autoLockRepository.observeAutoLockPin().assertValue(expectedValue)
    }

    @Test
    fun `should propagate data store error when encrypted pin data cannot be retrieved`() = runTest {
        // Given
        val expectedValue = AutoLockPreferenceError.DataStoreError
        every { autoLockLocalDataSource.observeAutoLockEncryptedPin() } returns flowOf(PreferencesError.left())

        // When + Then
        autoLockRepository.observeAutoLockPin().assertError(expectedValue)
    }

    @Test
    fun `should propagate deserialization error when encrypted pin cannot be decoded`() = runTest {
        // Given
        every { autoLockLocalDataSource.observeAutoLockEncryptedPin() } returns flowOf(
            AutoLockEncryptedPin(BaseEncryptedDummyValue).right()
        )

        expectDecryptionError()

        // When + Then
        autoLockRepository.observeAutoLockPin().assertError(baseDecryptionError)
    }

    @Test
    fun `should propagate data correctly when auto lock remaining attempts are observed`() = runTest {
        // Given
        val expectedValue = AutoLockRemainingAttempts(10)
        every { autoLockLocalDataSource.observeAutoLockEncryptedAttemptsLeft() } returns flowOf(
            AutoLockEncryptedRemainingAttempts(BaseEncryptedDummyValue).right()
        )

        expectSuccessfulDecryption("\"${expectedValue.value}\"")

        // When + Then
        autoLockRepository.observeAutoLockRemainingAttempts().assertValue(expectedValue)
    }

    @Test
    fun `should propagate data store error when auto lock remaining attempts cannot be retrieved`() = runTest {
        // Given
        val expectedValue = AutoLockPreferenceError.DataStoreError
        every { autoLockLocalDataSource.observeAutoLockEncryptedAttemptsLeft() } returns flowOf(
            PreferencesError.left()
        )

        // When + Then
        autoLockRepository.observeAutoLockRemainingAttempts().assertError(expectedValue)
    }

    @Test
    fun `should propagate deserialization error when auto lock remaining attempts cannot be decoded`() = runTest {
        // Given
        every { autoLockLocalDataSource.observeAutoLockEncryptedAttemptsLeft() } returns flowOf(
            AutoLockEncryptedRemainingAttempts(BaseEncryptedDummyValue).right()
        )

        expectDecryptionError()

        // When + Then
        autoLockRepository.observeAutoLockRemainingAttempts().assertError(baseDecryptionError)
    }

    @Test
    fun `should propagate data correctly when auto lock pending attempt status is observed`() = runTest {
        // Given
        val expectedValue = AutoLockAttemptPendingStatus(true)
        every { autoLockLocalDataSource.observeAutoLockEncryptedPendingAttempt() } returns flowOf(
            AutoLockEncryptedAttemptPendingStatus(BaseEncryptedDummyValue).right()
        )

        expectSuccessfulDecryption(expectedValue.value.toString())

        // When + Then
        autoLockRepository.observeAutoLockAttemptPendingStatus().assertValue(expectedValue)
    }

    @Test
    fun `should propagate data store error when auto lock pending attempt status cannot be retrieved`() = runTest {
        // Given
        val expectedValue = AutoLockPreferenceError.DataStoreError
        every { autoLockLocalDataSource.observeAutoLockEncryptedPendingAttempt() } returns flowOf(
            PreferencesError.left()
        )

        // When + Then
        autoLockRepository.observeAutoLockAttemptPendingStatus().assertError(expectedValue)
    }

    @Test
    fun `should propagate deserialization error when auto lock pending attempt status cannot be decoded`() = runTest {
        // Given
        every { autoLockLocalDataSource.observeAutoLockEncryptedPendingAttempt() } returns flowOf(
            AutoLockEncryptedAttemptPendingStatus(BaseEncryptedDummyValue).right()
        )

        expectDecryptionError()

        // When + Then
        autoLockRepository.observeAutoLockAttemptPendingStatus().assertError(baseDecryptionError)
    }

    private fun expectSuccessfulDecryption(value: String) {
        coEvery { keyStoreCrypto.decrypt(any<String>()) } returns value
    }

    private fun expectDecryptionError() {
        coEvery { keyStoreCrypto.decrypt(any<String>()) } throws IOException(baseDecryptionError.message)
    }

    private suspend fun <T> Flow<T>.assertError(error: AutoLockPreferenceError) = test {
        val item = awaitItem()
        assertEquals(error.left(), item)
        awaitComplete()
    }

    private suspend fun <T> Flow<T>.assertValue(value: T) = test {
        val item = awaitItem()
        assertEquals(value.right(), item)
        awaitComplete()
    }

    private companion object {

        val baseDecryptionError = AutoLockPreferenceError.DeserializationError("message")
        const val BaseEncryptedDummyValue = "somevalue"
    }
}
