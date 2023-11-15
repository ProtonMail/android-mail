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

import androidx.core.text.HtmlCompat
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.sample.UserAddressSample
import ch.protonmail.android.mailcommon.domain.usecase.GetPrimaryAddress
import ch.protonmail.android.mailsettings.domain.model.Signature
import ch.protonmail.android.mailsettings.domain.model.SignaturePreference
import ch.protonmail.android.mailsettings.domain.model.SignatureValue
import ch.protonmail.android.mailsettings.domain.repository.AddressIdentityRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

internal class GetPrimaryAddressSignatureTest {

    private val getPrimaryAddress: GetPrimaryAddress = mockk()
    private val addressIdentityRepository: AddressIdentityRepository = mockk()
    private val getPrimaryAddressSignature = GetPrimaryAddressSignature(getPrimaryAddress, addressIdentityRepository)

    @Before
    fun setup() {
        mockkStatic(HtmlCompat::class)
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `should return a valid signature when the user address has one`() = runTest {
        // Given
        val address = UserAddressSample.PrimaryAddress
        coEvery { getPrimaryAddress(BaseUserId) } returns address.right()
        coEvery {
            addressIdentityRepository.getSignatureEnabled(address.addressId)
        } returns SignaturePreference(true).right()
        every { HtmlCompat.fromHtml(any(), any()).toString() } returns "signature"

        // When
        val result = getPrimaryAddressSignature(BaseUserId)

        // Then
        assertEquals(BaseExpectedSignature, result)
    }

    @Test
    fun `should return an empty signature when the address one is null`() = runTest {
        // Given
        val address = AddressWithNullSignature
        coEvery { getPrimaryAddress(BaseUserId) } returns address.right()
        coEvery {
            addressIdentityRepository.getSignatureEnabled(address.addressId)
        } returns SignaturePreference(true).right()

        // When
        val result = getPrimaryAddressSignature(BaseUserId)

        // Then
        assertEquals(BaseExpectedEmptySignature, result)
    }

    @Test
    fun `should return an error when the primary address cannot be fetched`() = runTest {
        // Given
        val expectedError = DataError.AddressNotFound.left()
        coEvery { getPrimaryAddress(BaseUserId) } returns expectedError

        // When
        val result = getPrimaryAddressSignature(BaseUserId)

        // Then
        assertEquals(expectedError, result)
    }

    @Test
    fun `should return an error when the signature enabled value cannot be fetched`() = runTest {
        // Given
        val address = UserAddressSample.PrimaryAddress
        val expectedError = DataError.Local.NoDataCached.left()
        coEvery { getPrimaryAddress(BaseUserId) } returns address.right()
        coEvery { addressIdentityRepository.getSignatureEnabled(address.addressId) } returns expectedError

        // When
        val result = getPrimaryAddressSignature(BaseUserId)

        // Then
        assertEquals(expectedError, result)
    }

    private companion object {

        val BaseUserId = UserId("123")
        val BaseExpectedSignature = Signature(value = SignatureValue("signature"), enabled = true).right()
        val BaseExpectedEmptySignature = Signature(value = SignatureValue(""), enabled = true).right()
        val AddressWithNullSignature = UserAddressSample.PrimaryAddress.copy(signature = null)
    }
}
