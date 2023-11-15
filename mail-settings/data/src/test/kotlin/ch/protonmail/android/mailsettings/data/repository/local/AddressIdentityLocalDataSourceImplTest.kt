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
import androidx.datastore.preferences.core.booleanPreferencesKey
import app.cash.turbine.test
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.PreferencesError
import ch.protonmail.android.mailsettings.data.MailSettingsDataStoreProvider
import ch.protonmail.android.mailsettings.domain.model.SignaturePreference
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.user.data.db.AddressDatabase
import me.proton.core.user.domain.entity.AddressId
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

class AddressIdentityLocalDataSourceImplTest {

    private val preferences = mockk<Preferences>()
    private val addressIdentityDataStore = spyk<DataStore<Preferences>>()
    private val dataStoreProvider = mockk<MailSettingsDataStoreProvider> {
        every { this@mockk.addressDisplayInfoDataStore } returns addressIdentityDataStore
    }

    private val db = mockk<AddressDatabase>(relaxed = true)
    private val addressIdentityLocalDataSourceImpl = AddressIdentityLocalDataSourceImpl(db, dataStoreProvider)

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `should fetch the signature preference correctly when available`() = runTest {
        // Given
        val expectedValue = SignaturePreference(true).right()
        coEvery { preferences[booleanPreferencesKey("${BaseAddressId.id}-signatureEnabledPrefKey")] } returns true
        every { addressIdentityDataStore.data } returns flowOf(preferences)

        // When
        addressIdentityLocalDataSourceImpl.observeSignaturePreference(BaseAddressId).test {
            // Then
            assertEquals(expectedValue, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `should return success when the update completes`() = runTest {
        // Given
        val expectedResult = Unit.right()

        // When
        val result = addressIdentityLocalDataSourceImpl.updateSignatureEnabledState(BaseAddressId, enabled = true)

        // Then
        coVerify { addressIdentityDataStore.updateData(any()) }
        assertEquals(expectedResult, result)
    }


    @Test
    fun `should return an error when the signature preference cannot be fetched`() = runTest {
        // Given
        every { addressIdentityDataStore.data } returns flow { throw IOException() }

        // When
        addressIdentityLocalDataSourceImpl.observeSignaturePreference(BaseAddressId).test {
            // Then
            assertEquals(PreferencesError.left(), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `should return failure when an exception is thrown while saving preference`() = runTest {
        // Given
        val expectedResult = PreferencesError.left()
        coEvery { addressIdentityDataStore.updateData(any()) } throws IOException()

        // When
        val result = addressIdentityLocalDataSourceImpl.updateSignatureEnabledState(BaseAddressId, enabled = true)

        // Then
        assertEquals(expectedResult, result)
    }

    private companion object {

        val BaseAddressId = AddressId("123")
    }
}
