/*
 * Copyright (c) 2025 Proton Technologies AG
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

package ch.protonmail.android.mailcontentsearch.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.model.PreferencesError
import ch.protonmail.android.mailcontentsearch.data.local.ContentSearchDataStoreProvider
import ch.protonmail.android.mailsettings.domain.model.AllowMobileDataForContentSearchIndexing
import ch.protonmail.android.mailsettings.domain.model.AppSettings
import ch.protonmail.android.mailsettings.domain.repository.AppSettingsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class ContentSearchPreferencesRepositoryImplTest {

    private val appSettingsRepository = mockk<AppSettingsRepository>()
    private val dataStore = mockk<DataStore<Preferences>>()
    private val dataStoreProvider = mockk<ContentSearchDataStoreProvider> {
        every { contentSearchDataStore } returns dataStore
    }
    private val repository = ContentSearchPreferencesRepositoryImpl(appSettingsRepository, dataStoreProvider)

    @Test
    fun `returns the allow mobile data value stored in the rust app settings`() = runTest {
        // Given
        val appSettings = AppSettings.default().copy(
            useMobileDataForContentSearchIndexing = AllowMobileDataForContentSearchIndexing.NotEnabled
        )
        every { appSettingsRepository.observeAppSettings() } returns flowOf(appSettings)

        // When
        val result = repository.getAllowMobileData()

        // Then
        assertFalse(result.getOrNull()!!)
    }

    @Test
    fun `defaults to allowing mobile data when the rust app settings enable it`() = runTest {
        // Given
        val appSettings = AppSettings.default().copy(
            useMobileDataForContentSearchIndexing = AllowMobileDataForContentSearchIndexing.Enabled
        )
        every { appSettingsRepository.observeAppSettings() } returns flowOf(appSettings)

        // When
        val result = repository.getAllowMobileData()

        // Then
        assertTrue(result.getOrNull()!!)
    }

    @Test
    fun `persists the allow mobile data value through the rust app settings repository`() = runTest {
        // Given
        coEvery { appSettingsRepository.updateUseMobileDataForContentSearch(true) } returns Unit.right()

        // When
        val result = repository.setAllowMobileData(true)

        // Then
        coVerify { appSettingsRepository.updateUseMobileDataForContentSearch(true) }
        assertEquals(Unit.right(), result)
    }

    @Test
    fun `returns a preferences error when the rust app settings update fails`() = runTest {
        // Given
        coEvery {
            appSettingsRepository.updateUseMobileDataForContentSearch(true)
        } returns DataError.Local.NotFound.left()

        // When
        val result = repository.setAllowMobileData(true)

        // Then
        assertEquals(PreferencesError.left(), result)
    }

    @Test
    fun `reports auto-enable as applied when the user id is stored`() = runTest {
        // Given
        val preferences = mockk<Preferences> {
            every { get(any<Preferences.Key<Set<String>>>()) } returns setOf(TestUserId.id)
        }
        every { dataStore.data } returns flowOf(preferences)

        // When
        val result = repository.hasAutoEnableBeenApplied(TestUserId)

        // Then
        assertTrue(result.getOrNull()!!)
    }

    @Test
    fun `reports auto-enable as not applied when nothing is stored`() = runTest {
        // Given
        val preferences = mockk<Preferences> {
            every { get(any<Preferences.Key<Set<String>>>()) } returns null
        }
        every { dataStore.data } returns flowOf(preferences)

        // When
        val result = repository.hasAutoEnableBeenApplied(TestUserId)

        // Then
        assertFalse(result.getOrNull()!!)
    }

    @Test
    fun `persists the auto-enable applied marker through the data store`() = runTest {
        // Given
        coEvery { dataStore.updateData(any()) } returns mockk()

        // When
        repository.markAutoEnableApplied(TestUserId)

        // Then
        coVerify { dataStore.updateData(any()) }
    }

    @Test
    fun `clears the auto-enable applied marker through the data store`() = runTest {
        // Given
        coEvery { dataStore.updateData(any()) } returns mockk()

        // When
        repository.clearAutoEnableApplied(TestUserId)

        // Then
        coVerify { dataStore.updateData(any()) }
    }

    private companion object {
        val TestUserId = UserId("user-1")
    }
}
