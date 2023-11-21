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

package ch.protonmail.android.mailsettings.domain.usecase.identity

import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.sample.UserAddressSample
import ch.protonmail.android.mailcommon.domain.usecase.ResolveUserAddress
import ch.protonmail.android.mailsettings.domain.model.Signature
import ch.protonmail.android.mailsettings.domain.model.SignaturePreference
import ch.protonmail.android.mailsettings.domain.model.SignatureValue
import ch.protonmail.android.mailsettings.domain.repository.AddressIdentityRepository
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import org.junit.After
import org.junit.Test
import kotlin.test.assertEquals

internal class GetAddressSignatureTest {

    private val resolveUserAddress = mockk<ResolveUserAddress>()
    private val addressIdentityRepository = mockk<AddressIdentityRepository>()
    private val getAddressSignature = GetAddressSignature(resolveUserAddress, addressIdentityRepository)

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `should return an error when the address cannot be found`() = runTest {
        // Given
        coEvery {
            resolveUserAddress(BaseUserId, BaseRawAddress)
        } returns ResolveUserAddress.Error.UserAddressNotFound.left()

        // When
        val result = getAddressSignature(BaseUserId, BaseRawAddress)

        // Then
        assertEquals(DataError.AddressNotFound.left(), result)
    }

    @Test
    fun `should return an error when the preference cannot be fetched`() = runTest {
        // Given
        coEvery {
            resolveUserAddress(BaseUserId, BaseRawAddress)
        } returns BaseAddress.right()

        coEvery {
            addressIdentityRepository.getSignatureEnabledPreferenceValue(BaseAddress.addressId)
        } returns DataError.Local.NoDataCached.left()

        // When
        val result = getAddressSignature(BaseUserId, BaseRawAddress)

        // Then
        assertEquals(DataError.Local.Unknown.left(), result)
    }

    @Test
    fun `should return the signature when all data can be fetched`() = runTest {
        // Given
        coEvery {
            resolveUserAddress(BaseUserId, BaseRawAddress)
        } returns BaseAddress.right()

        coEvery {
            addressIdentityRepository.getSignatureEnabledPreferenceValue(BaseAddress.addressId)
        } returns BaseSignaturePreference.right()

        val expectedSignature = Signature(enabled = false, SignatureValue("signature"))

        // When
        val result = getAddressSignature(BaseUserId, BaseRawAddress)

        // Then
        assertEquals(expectedSignature.right(), result)
    }

    private companion object {

        val BaseUserId = UserId("userId")
        val BaseAddress = UserAddressSample.PrimaryAddress
        const val BaseRawAddress = "raw@example.com"
        val BaseSignaturePreference = SignaturePreference(enabled = false)
    }
}
