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

import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.model.PreferencesError
import ch.protonmail.android.mailsettings.data.repository.local.AddressIdentityLocalDataSource
import ch.protonmail.android.mailsettings.data.repository.remote.AddressIdentityRemoteDataSource
import ch.protonmail.android.mailsettings.domain.model.DisplayName
import ch.protonmail.android.mailsettings.domain.model.Signature
import ch.protonmail.android.mailsettings.domain.model.SignaturePreference
import ch.protonmail.android.mailsettings.domain.model.SignatureValue
import io.mockk.called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import me.proton.core.user.domain.entity.AddressId
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

internal class AddressIdentityRepositoryImplTest {

    private val addressIdentityLocalDataSource = mockk<AddressIdentityLocalDataSource>()
    private val addressIdentityRemoteDataSource = mockk<AddressIdentityRemoteDataSource>()
    private val addressIdentityRepository =
        AddressIdentityRepositoryImpl(addressIdentityLocalDataSource, addressIdentityRemoteDataSource)

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `should propagate the display name when it is correctly observed`() = runTest {
        // Given
        coEvery {
            addressIdentityLocalDataSource.observeDisplayName(BaseAddressId)
        } returns flowOf(BaseDisplayName.right())

        // When
        val result = addressIdentityRepository.getDisplayName(BaseAddressId)

        // Then
        assertEquals(BaseDisplayName.right(), result)
    }

    @Test
    fun `should return an error when the display name cannot be observed`() = runTest {
        // Given
        coEvery {
            addressIdentityLocalDataSource.observeDisplayName(BaseAddressId)
        } returns flowOf(DataError.Local.NoDataCached.left())

        // When
        val result = addressIdentityRepository.getDisplayName(BaseAddressId)

        // Then
        assertEquals(DataError.Local.NoDataCached.left(), result)
    }

    @Test
    fun `should propagate the signature preference when it is correctly observed`() = runTest {
        // Given
        coEvery {
            addressIdentityLocalDataSource.observeSignaturePreference(BaseAddressId)
        } returns flowOf(BaseSignaturePreference.right())

        // When
        val result = addressIdentityRepository.getSignatureEnabledPreferenceValue(BaseAddressId)

        // Then
        assertEquals(BaseSignaturePreference.right(), result)
    }

    @Test
    fun `should propagate the signature value when it is correctly observed`() = runTest {
        // Given
        coEvery {
            addressIdentityLocalDataSource.observeSignatureValue(BaseAddressId)
        } returns flowOf(BaseSignatureValue.right())

        // When
        val result = addressIdentityRepository.getSignatureValue(BaseAddressId)

        // Then
        assertEquals(BaseSignatureValue.right(), result)
    }

    @Test
    fun `should return an error when the display signature preference cannot be observed`() = runTest {
        // Given
        coEvery {
            addressIdentityLocalDataSource.observeSignaturePreference(BaseAddressId)
        } returns flowOf(PreferencesError.left())

        // When
        val result = addressIdentityRepository.getSignatureEnabledPreferenceValue(BaseAddressId)

        // Then
        assertEquals(DataError.Local.NoDataCached.left(), result)
    }

    @Test
    fun `should return an error when the signature value cannot be observed`() = runTest {
        // Given
        coEvery {
            addressIdentityLocalDataSource.observeSignatureValue(BaseAddressId)
        } returns flowOf(DataError.Local.NoDataCached.left())

        // When
        val result = addressIdentityRepository.getSignatureValue(BaseAddressId)

        // Then
        assertEquals(DataError.Local.NoDataCached.left(), result)
    }

    @Test
    fun `should return an error when the signature cannot be toggled`() = runTest {
        // Given
        coEvery {
            addressIdentityLocalDataSource.updateSignatureEnabledState(BaseAddressId, false)
        } returns PreferencesError.left()

        // When
        val result = addressIdentityRepository.updateAddressIdentity(
            BaseUserId, BaseAddressId, BaseDisplayName, BaseSignature
        )

        // Then
        assertEquals(DataError.Local.Unknown.left(), result)
        coVerify(exactly = 0) {
            addressIdentityLocalDataSource.updateAddressIdentity(any(), any(), any())
        }
        verify {
            addressIdentityRemoteDataSource wasNot called
        }
    }

    @Test
    fun `should return an error when display name and signature cannot be updated`() = runTest {
        // Given
        coEvery {
            addressIdentityLocalDataSource.updateSignatureEnabledState(BaseAddressId, false)
        } returns Unit.right()
        coEvery {
            addressIdentityLocalDataSource.updateAddressIdentity(BaseAddressId, BaseDisplayName, BaseSignature)
        } returns DataError.Local.NoDataCached.left()

        // When
        val result = addressIdentityRepository.updateAddressIdentity(
            BaseUserId, BaseAddressId, BaseDisplayName, BaseSignature
        )

        // Then
        assertEquals(DataError.Local.Unknown.left(), result)
        verify {
            addressIdentityRemoteDataSource wasNot called
        }
    }

    @Test
    fun `should call the remote data source only when all local updates have succeeded`() = runTest {
        // Given
        coEvery {
            addressIdentityLocalDataSource.updateSignatureEnabledState(BaseAddressId, false)
        } returns Unit.right()
        coEvery {
            addressIdentityLocalDataSource.updateAddressIdentity(BaseAddressId, BaseDisplayName, BaseSignature)
        } returns Unit.right()
        coEvery {
            addressIdentityRemoteDataSource.updateAddressIdentity(
                BaseUserId,
                BaseAddressId,
                BaseDisplayName,
                BaseSignature.value
            )
        } just runs

        // When
        val result = addressIdentityRepository.updateAddressIdentity(
            BaseUserId, BaseAddressId, BaseDisplayName, BaseSignature
        )

        // Then
        assertEquals(Unit.right(), result)
        verify(exactly = 1) {
            addressIdentityRemoteDataSource.updateAddressIdentity(
                BaseUserId, BaseAddressId, BaseDisplayName, BaseSignature.value
            )
        }
    }

    private companion object {

        val BaseUserId = UserId("userId")
        val BaseAddressId = AddressId("123")
        val BaseDisplayName = DisplayName("displayName")
        val BaseSignaturePreference = SignaturePreference(false)
        val BaseSignatureValue = SignatureValue("signature")
        val BaseSignature = Signature(false, BaseSignatureValue)
    }
}
