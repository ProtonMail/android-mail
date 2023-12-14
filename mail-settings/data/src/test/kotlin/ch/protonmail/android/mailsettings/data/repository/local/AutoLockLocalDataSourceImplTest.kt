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

package ch.protonmail.android.mailsettings.data.repository.local

import java.io.IOException
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import app.cash.turbine.test
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.PreferencesError
import ch.protonmail.android.mailsettings.data.MailSettingsDataStoreProvider
import ch.protonmail.android.mailsettings.domain.model.autolock.AutoLockEnabledEncryptedValue
import ch.protonmail.android.mailsettings.domain.model.autolock.AutoLockEncryptedInterval
import ch.protonmail.android.mailsettings.domain.model.autolock.AutoLockEncryptedPin
import ch.protonmail.android.mailsettings.domain.model.autolock.AutoLockLastEncryptedForegroundMillis
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

internal class AutoLockLocalDataSourceImplTest {

    private val preferences = mockk<Preferences>()
    private val autoLockDataStoreSpy = spyk<DataStore<Preferences>>()
    private val dataStoreProvider = mockk<MailSettingsDataStoreProvider> {
        every { this@mockk.autoLockDataStore } returns autoLockDataStoreSpy
    }

    private val autoLockLocalDataSource = AutoLockLocalDataSourceImpl(dataStoreProvider)

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `should return locally stored preference from data store for auto lock enabled when available`() = runTest {
        // Given
        expectValidPreference(AutoLockPrefKey)

        // When + Then
        autoLockLocalDataSource
            .observeAutoLockEnabledEncryptedValue()
            .assertValue(AutoLockEnabledEncryptedValue(StringPlaceholder))
    }

    @Test
    fun `should return an error when no encrypted auto lock enabled preference is stored locally`() = runTest {
        // Given
        expectEmptyPreferences()

        // When + Then
        autoLockLocalDataSource.observeAutoLockEncryptedPin().assertError()
    }

    @Test
    fun `should return success when auto lock enabled preference is updated`() = runTest {
        // When
        val result = autoLockLocalDataSource.updateAutoLockEnabledEncryptedValue(
            AutoLockEnabledEncryptedValue(StringPlaceholder)
        )

        // Then
        coVerify { autoLockDataStoreSpy.updateData(any()) }
        assertTrue(result.isRight())
    }

    @Test
    fun `should return an error when auto lock enabled preference cannot be updated`() = runTest {
        // Given
        coEvery { autoLockDataStoreSpy.updateData(any()) } throws IOException()

        // When
        val result = autoLockLocalDataSource.updateAutoLockEnabledEncryptedValue(
            AutoLockEnabledEncryptedValue(StringPlaceholder)
        )

        // Then
        coVerify { autoLockDataStoreSpy.updateData(any()) }
        assertTrue(result.isLeft())
    }

    @Test
    fun `should return locally stored preference from data store for auto lock interval when available`() = runTest {
        // Given
        expectValidPreference(AutoLockIntervalKey)

        // When + Then
        autoLockLocalDataSource
            .observeAutoLockEncryptedInterval()
            .assertValue(AutoLockEncryptedInterval(StringPlaceholder))
    }

    @Test
    fun `should return an error when no encrypted auto lock interval preference is stored locally`() = runTest {
        // Given
        expectEmptyPreferences()

        // When + Then
        autoLockLocalDataSource.observeAutoLockEnabledEncryptedValue().assertError()
    }

    @Test
    fun `should return success when auto lock enabled interval is updated`() = runTest {
        // When
        val result = autoLockLocalDataSource.updateAutoLockEncryptedInterval(
            AutoLockEncryptedInterval(StringPlaceholder)
        )

        // Then
        coVerify { autoLockDataStoreSpy.updateData(any()) }
        assertTrue(result.isRight())
    }

    @Test
    fun `should return an error when auto lock enabled interval cannot be updated`() = runTest {
        // Given
        coEvery { autoLockDataStoreSpy.updateData(any()) } throws IOException()

        // When
        val result = autoLockLocalDataSource.updateAutoLockEncryptedInterval(
            AutoLockEncryptedInterval(StringPlaceholder)
        )

        // Then
        coVerify { autoLockDataStoreSpy.updateData(any()) }
        assertTrue(result.isLeft())
    }

    @Test
    fun `should return locally stored preference from data store for last foreground millis when available`() =
        runTest {
            // Given
            expectValidPreference(LastForegroundMillisKey)

            // When + Then
            autoLockLocalDataSource
                .observeLastEncryptedForegroundMillis()
                .assertValue(AutoLockLastEncryptedForegroundMillis(StringPlaceholder))
        }

    @Test
    fun `should return an error when no encrypted auto lock last foreground millis is stored locally`() = runTest {
        // Given
        expectEmptyPreferences()

        // When + Then
        autoLockLocalDataSource.observeLastEncryptedForegroundMillis().assertError()
    }

    @Test
    fun `should return success when auto lock last foreground millis is updated`() = runTest {
        // When
        val result = autoLockLocalDataSource.updateLastEncryptedForegroundMillis(
            AutoLockLastEncryptedForegroundMillis(StringPlaceholder)
        )

        // Then
        coVerify { autoLockDataStoreSpy.updateData(any()) }
        assertTrue(result.isRight())
    }

    @Test
    fun `should return an error when auto lock last foreground millis cannot be updated`() = runTest {
        // Given
        coEvery { autoLockDataStoreSpy.updateData(any()) } throws IOException()

        // When
        val result = autoLockLocalDataSource.updateLastEncryptedForegroundMillis(
            AutoLockLastEncryptedForegroundMillis(StringPlaceholder)
        )

        // Then
        coVerify { autoLockDataStoreSpy.updateData(any()) }
        assertTrue(result.isLeft())
    }


    @Test
    fun `should return locally stored preference from data store for pin value when available`() =
        runTest {
            // Given
            expectValidPreference(pinKey)

            // When + Then
            autoLockLocalDataSource
                .observeAutoLockEncryptedPin()
                .assertValue(AutoLockEncryptedPin(StringPlaceholder))
        }

    @Test
    fun `should return an error when no encrypted auto lock last pin value is stored locally`() = runTest {
        // Given
        expectEmptyPreferences()

        // When + Then
        autoLockLocalDataSource.observeAutoLockEncryptedPin().assertError()
    }

    @Test
    fun `should return success when encrypted auto lock last pin value is updated`() = runTest {
        // When
        val result = autoLockLocalDataSource.updateAutoLockEncryptedPin(
            AutoLockEncryptedPin(StringPlaceholder)
        )

        // Then
        coVerify { autoLockDataStoreSpy.updateData(any()) }
        assertTrue(result.isRight())
    }

    @Test
    fun `should return an error when encrypted auto lock last pin value cannot be updated`() = runTest {
        // Given
        coEvery { autoLockDataStoreSpy.updateData(any()) } throws IOException()

        // When
        val result = autoLockLocalDataSource.updateAutoLockEncryptedPin(
            AutoLockEncryptedPin(StringPlaceholder)
        )

        // Then
        coVerify { autoLockDataStoreSpy.updateData(any()) }
        assertTrue(result.isLeft())
    }

    private fun expectValidPreference(key: Preferences.Key<String>) {
        every { preferences[key] } returns StringPlaceholder
        every { autoLockDataStoreSpy.data } returns flowOf(preferences)
    }

    private fun expectEmptyPreferences() {
        coEvery { preferences.get<String>(any()) } returns null
        every { autoLockDataStoreSpy.data } returns flowOf(preferences)
    }

    private suspend fun <T> Flow<T>.assertError() = test {
        val item = awaitItem()
        assertEquals(PreferencesError.left(), item)
        awaitComplete()
    }

    private suspend fun <T> Flow<T>.assertValue(value: T) = test {
        val item = awaitItem()
        assertEquals(value.right(), item)
        awaitComplete()
    }

    private companion object {

        const val StringPlaceholder = "stringPlaceholder"
        val AutoLockPrefKey = stringPreferencesKey("hasAutoLockPrefKey")
        val AutoLockIntervalKey = stringPreferencesKey("autoLockIntervalPrefKey")
        val LastForegroundMillisKey = stringPreferencesKey("lastForegroundTimestampPrefKey")
        val pinKey = stringPreferencesKey("pinCodePrefKey")
    }
}
