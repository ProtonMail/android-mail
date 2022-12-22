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

package ch.protonmail.android.mailcommon.data.mapper

import java.io.IOException
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.PreferencesError
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

internal class DataStoreEitherMappingsTest {

    private val dataStore: DataStore<Preferences> = mockk()
    private val intKey = intPreferencesKey("key")

    @Test
    fun `when edit succeed`() = runTest {
        // given
        val preferences: Preferences = mockk()
        coEvery { dataStore.updateData(any()) } returns preferences

        // when
        val result = dataStore.safeEdit {
            it[intKey] = 1
        }

        // then
        assertEquals(preferences.right(), result)
    }

    @Test
    fun `when edit fails`() = runTest {
        // given
        coEvery { dataStore.updateData(any()) } throws IOException()

        // when
        val result = dataStore.safeEdit {
            it[intKey] = 1
        }

        // then
        assertEquals(PreferencesError.left(), result)
    }
}
