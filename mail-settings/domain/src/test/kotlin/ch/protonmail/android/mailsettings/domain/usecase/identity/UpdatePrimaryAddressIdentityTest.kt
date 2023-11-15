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
import ch.protonmail.android.mailcommon.domain.usecase.GetPrimaryAddress
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailsettings.domain.model.DisplayName
import ch.protonmail.android.mailsettings.domain.model.Signature
import ch.protonmail.android.mailsettings.domain.model.SignatureValue
import ch.protonmail.android.mailsettings.domain.repository.AddressIdentityRepository
import io.mockk.coEvery
import io.mockk.confirmVerified
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

internal class UpdatePrimaryAddressIdentityTest {

    private val observePrimaryUserId = mockk<ObservePrimaryUserId>()
    private val getPrimaryAddress = mockk<GetPrimaryAddress>()
    private val addressIdentityRepository = mockk<AddressIdentityRepository>()
    private val updatePrimaryAddressIdentity = UpdatePrimaryAddressIdentity(
        observePrimaryUserId,
        getPrimaryAddress,
        addressIdentityRepository
    )

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `should return an error when the user id cannot be fetched`() = runTest {
        // Given
        val expectedError = UpdatePrimaryAddressIdentity.Error.UserIdNotFound.left()
        coEvery { observePrimaryUserId() } returns flowOf(null)

        // When
        val result = updatePrimaryAddressIdentity(BaseDisplayName, BaseSignature)

        // Then
        assertEquals(expectedError, result)
        confirmVerified(addressIdentityRepository)
    }

    @Test
    fun `should return an error when the primary address cannot be fetched`() = runTest {
        // Given
        val expectedError = UpdatePrimaryAddressIdentity.Error.PrimaryAddressNotFound.left()
        coEvery { observePrimaryUserId() } returns flowOf(BaseUserId)
        coEvery { getPrimaryAddress(BaseUserId) } returns DataError.AddressNotFound.left()

        // When
        val result = updatePrimaryAddressIdentity(BaseDisplayName, BaseSignature)

        // Then
        assertEquals(expectedError, result)
        confirmVerified(addressIdentityRepository)
    }

    @Test
    fun `should return an error when the update fails`() = runTest {
        // Given
        val expectedError = UpdatePrimaryAddressIdentity.Error.UpdateFailure.left()
        coEvery { observePrimaryUserId() } returns flowOf(BaseUserId)
        coEvery { getPrimaryAddress(BaseUserId) } returns BaseAddress.right()
        coEvery {
            addressIdentityRepository.updateAddressIdentity(
                BaseUserId,
                BaseAddress.addressId,
                BaseDisplayName,
                BaseSignature
            )
        } returns DataError.Local.Unknown.left()

        // When
        val result = updatePrimaryAddressIdentity(BaseDisplayName, BaseSignature)

        // Then
        assertEquals(expectedError, result)
    }

    @Test
    fun `should call the repository when updating the display name and signature`() = runTest {
        // Given
        coEvery { observePrimaryUserId() } returns flowOf(BaseUserId)
        coEvery { getPrimaryAddress(BaseUserId) } returns BaseAddress.right()
        coEvery {
            addressIdentityRepository.updateAddressIdentity(
                BaseUserId,
                BaseAddress.addressId,
                BaseDisplayName,
                BaseSignature
            )
        } returns Unit.right()

        // When
        val result = updatePrimaryAddressIdentity(BaseDisplayName, BaseSignature)

        // Then
        assertTrue(result.isRight())
    }

    private companion object {

        val BaseUserId = UserId("000")
        val BaseDisplayName = DisplayName("123")
        val BaseAddress = UserAddressSample.PrimaryAddress
        val BaseSignature = Signature(enabled = true, value = SignatureValue("456"))
    }
}
