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

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import app.cash.turbine.test
import ch.protonmail.android.mailsettings.data.MailSettingsDataStoreProvider
import ch.protonmail.android.mailsettings.domain.model.Theme
import ch.protonmail.android.mailsettings.domain.repository.ThemeRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ThemeRepositoryImplTest {
    private val preferences = mockk<Preferences>()
    private val themeDataStoreSpy = spyk<DataStore<Preferences>> {
        every { this@spyk.data } returns flowOf(preferences)
    }
    private val dataStoreProvider = mockk<MailSettingsDataStoreProvider> {
        every { this@mockk.themeDataStore } returns themeDataStoreSpy
    }

    private lateinit var themeRepository: ThemeRepository

    @Before
    fun setUp() {
        themeRepository = ThemeRepositoryImpl(dataStoreProvider)
    }

    @Test
    fun `returns System Default when no preference is stored locally`() = runTest {
        // Given
        coEvery { preferences.get<String>(any()) } returns null
        // When
        themeRepository.observe().test {
            // Then
            assertEquals(Theme.SYSTEM_DEFAULT, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `returns locally stored preference from data store when available`() = runTest {
        // Given
        coEvery {
            preferences[stringPreferencesKey("themeEnumNamePrefKey")]
        } returns Theme.LIGHT.name
        // When
        themeRepository.observe().test {
            // Then
            assertEquals(Theme.LIGHT, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `fallback to System Default when saved preference cannot be mapped to a Theme enum constant`() = runTest {
        // Given
        coEvery {
            preferences[stringPreferencesKey("themeEnumNamePrefKey")]
        } returns "InvalidThemeName"
        // When
        themeRepository.observe().test {
            // Then
            assertEquals(Theme.SYSTEM_DEFAULT, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `update value stored in data store when update is called`() = runTest {
        // When
        themeRepository.update(Theme.DARK)

        // Then
        coVerify { themeDataStoreSpy.updateData(any()) }
    }
}
