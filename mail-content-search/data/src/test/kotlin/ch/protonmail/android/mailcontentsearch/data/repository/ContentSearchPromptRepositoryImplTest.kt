/*
 * Copyright (c) 2026 Proton Technologies AG
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
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import app.cash.turbine.test
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.PreferencesError
import ch.protonmail.android.mailcontentsearch.data.local.ContentSearchDataStoreProvider
import ch.protonmail.android.mailcontentsearch.domain.model.ContentSearchPrompt
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals

internal class ContentSearchPromptRepositoryImplTest {

    private val userId = UserId("user-1")
    private val prompt = ContentSearchPrompt.SetUpOffer
    private val dismissedKey = booleanPreferencesKey("contentSearchPromptDismissed_SetUpOffer_user-1")

    private val preferences = mockk<Preferences>()
    private val dataStore = mockk<DataStore<Preferences>>()
    private val dataStoreProvider = mockk<ContentSearchDataStoreProvider> {
        every { this@mockk.contentSearchDataStore } returns dataStore
    }

    private val repository = ContentSearchPromptRepositoryImpl(dataStoreProvider)

    @Test
    fun `should report not dismissed when the account has no stored answer`() = runTest {
        // Given
        every { preferences[dismissedKey] } returns null
        every { dataStore.data } returns flowOf(preferences)

        // When
        repository.observeDismissed(userId, prompt).test {
            // Then
            assertEquals(false.right(), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `should report dismissed when the account has answered`() = runTest {
        // Given
        every { preferences[dismissedKey] } returns true
        every { dataStore.data } returns flowOf(preferences)

        // When
        repository.observeDismissed(userId, prompt).test {
            // Then
            assertEquals(true.right(), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `should report an error when the store cannot be read`() = runTest {
        // Given
        every { dataStore.data } returns flow { throw IOException() }

        // When
        repository.observeDismissed(userId, prompt).test {
            // Then
            assertEquals(PreferencesError.left(), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `should store the answer under the account's own key`() = runTest {
        // Given
        val mutablePreferences = mockk<MutablePreferences>(relaxUnitFun = true)
        val transform = slot<suspend (Preferences) -> Preferences>()
        every { preferences.toMutablePreferences() } returns mutablePreferences
        coEvery { dataStore.updateData(capture(transform)) } returns preferences

        // When
        val result = repository.dismiss(userId, prompt)

        // Then
        assertEquals(Unit.right(), result)
        transform.captured(preferences)
        verify { mutablePreferences[dismissedKey] = true }
    }

    @Test
    fun `should forget the answer under the account's own key on reset`() = runTest {
        // Given
        val mutablePreferences = mockk<MutablePreferences> {
            every { remove(dismissedKey) } returns true
        }
        val transform = slot<suspend (Preferences) -> Preferences>()
        every { preferences.toMutablePreferences() } returns mutablePreferences
        coEvery { dataStore.updateData(capture(transform)) } returns preferences

        // When
        val result = repository.reset(userId, prompt)

        // Then
        assertEquals(Unit.right(), result)
        transform.captured(preferences)
        verify { mutablePreferences.remove(dismissedKey) }
    }

    @Test
    fun `should keep each banner's answer under its own key`() = runTest {
        // Given
        val otherKey = booleanPreferencesKey("contentSearchPromptDismissed_IndexingComplete_user-1")
        every { preferences[dismissedKey] } returns true
        every { preferences[otherKey] } returns null
        every { dataStore.data } returns flowOf(preferences)

        // When
        repository.observeDismissed(userId, ContentSearchPrompt.IndexingComplete).test {
            // Then
            assertEquals(false.right(), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `should report an error when the answer cannot be stored`() = runTest {
        // Given
        coEvery { dataStore.updateData(any()) } throws IOException()

        // When
        val result = repository.dismiss(userId, prompt)

        // Then
        assertEquals(PreferencesError.left(), result)
    }
}
