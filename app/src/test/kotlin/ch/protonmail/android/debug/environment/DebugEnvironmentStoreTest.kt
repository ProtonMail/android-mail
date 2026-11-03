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

package ch.protonmail.android.debug.environment

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import ch.protonmail.android.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class DebugEnvironmentStoreTest {

    private val directory: File = createTempDirectory("debug-environment").toFile()
    private val scope = CoroutineScope(UnconfinedTestDispatcher() + SupervisorJob())
    private val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(scope = scope) {
        File(directory, "debug_environment.preferences_pb")
    }

    @AfterTest
    fun tearDown() {
        scope.cancel()
        directory.deleteRecursively()
    }

    @Test
    fun `given no override is stored, when asking for the environment, then it is the build default`() {
        // Given
        val store = DebugEnvironmentStore(dataStore)

        // When
        val host = store.effectiveHost()

        // Then
        assertEquals(BuildConfig.API_HOST, host)
        assertNull(store.host())
    }

    @Test
    fun `given an override is stored, when asking for the environment, then it is the override`() = runTest {
        // Given
        DebugEnvironmentStore(dataStore).save("maury.proton.black")

        // When
        val host = DebugEnvironmentStore(dataStore).effectiveHost()

        // Then
        assertEquals("maury.proton.black", host)
    }


    @Test
    fun `given five recent environments, when saving another, then it comes first and the oldest is dropped`() =
        runTest {
            // Given
            val store = DebugEnvironmentStore(dataStore)
            listOf("e", "d", "c", "b", "a").forEach { store.save("$it.proton.black") }

            // When
            val recentHosts = store.save("maury.proton.black")

            // Then
            val expected = listOf(
                "maury.proton.black",
                "a.proton.black",
                "b.proton.black",
                "c.proton.black",
                "d.proton.black"
            )
            assertEquals(expected, recentHosts)
            assertEquals(expected, DebugEnvironmentStore(dataStore).recentHosts())
        }

    @Test
    fun `given a recent environment, when saving it again, then it moves to the front without a duplicate`() =
        runTest {
            // Given
            val store = DebugEnvironmentStore(dataStore)
            store.save("maury.proton.black")
            store.save("a.proton.black")

            // When
            store.save("maury.proton.black")

            // Then
            assertEquals(
                listOf("maury.proton.black", "a.proton.black"),
                DebugEnvironmentStore(dataStore).recentHosts()
            )
        }

    @Test
    fun `given an override, when clearing it, then the build default is used and recent environments are kept`() =
        runTest {
            // Given
            val store = DebugEnvironmentStore(dataStore)
            store.save("maury.proton.black")

            // When
            store.clear()

            // Then
            val restarted = DebugEnvironmentStore(dataStore)
            assertNull(restarted.host())
            assertEquals(listOf("maury.proton.black"), restarted.recentHosts())
        }

    @Test
    fun `given the store was read, when an override is saved, then this process keeps its starting environment`() =
        runTest {
            // Given
            val store = DebugEnvironmentStore(dataStore)
            assertNull(store.host())

            // When
            dataStore.edit { it[stringPreferencesKey("host")] = "maury.proton.black" }

            // Then
            assertNull(store.host())
            assertEquals("maury.proton.black", dataStore.data.first()[stringPreferencesKey("host")])
        }
}
