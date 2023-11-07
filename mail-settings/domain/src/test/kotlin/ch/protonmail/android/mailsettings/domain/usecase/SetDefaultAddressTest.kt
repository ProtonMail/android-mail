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

package ch.protonmail.android.mailsettings.domain.usecase

import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.sample.UserAddressSample
import ch.protonmail.android.mailcommon.domain.usecase.IsPaidUser
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import me.proton.core.user.domain.UserAddressManager
import me.proton.core.user.domain.entity.AddressId
import org.junit.Assert.assertEquals
import org.junit.Test

internal class SetDefaultAddressTest {

    private val userAddressManager = mockk<UserAddressManager>()
    private val isPaidUser = mockk<IsPaidUser>()
    private val setDefaultEmailAddress = SetDefaultAddress(userAddressManager, isPaidUser)

    @Test
    fun `when unable to determine a subscription upon updating, an error is raised`() = runTest {
        // Given
        coEvery { isPaidUser(userId) } returns DataError.Local.Unknown.left()
        val expectedResult = SetDefaultAddress.Error.FailedDeterminingUserSubscription.left()

        // When
        val result = setDefaultEmailAddress(userId, addressId)

        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `when addresses list is somehow empty upon updating, an error is raised`() = runTest {
        // Given
        coEvery { isPaidUser(userId) } returns true.right()
        coEvery { userAddressManager.observeAddresses(userId, false) } returns flowOf(emptyList())
        val expectedResult = SetDefaultAddress.Error.AddressNotFound.left()

        // When
        val result = setDefaultEmailAddress(userId, addressId)

        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `when user tries to set a non existing address as default, an error is raised`() = runTest {
        // Given
        coEvery { isPaidUser(userId) } returns true.right()
        coEvery { userAddressManager.observeAddresses(userId, false) } returns flowOf(
            listOf(UserAddressSample.build(AddressId("random-id")))
        )
        val expectedResult = SetDefaultAddress.Error.AddressNotFound.left()

        // When
        val result = setDefaultEmailAddress(userId, addressId)

        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `when free user tries to set a pm address as default, a subscription related error is raised`() = runTest {
        // Given
        coEvery { isPaidUser(userId) } returns false.right()
        coEvery { userAddressManager.observeAddresses(userId, false) } returns flowOf(
            listOf(UserAddressSample.build(addressId, email = "email@pm.me"))
        )
        val expectedResult = SetDefaultAddress.Error.UpgradeRequired.left()

        // When
        val result = setDefaultEmailAddress(userId, addressId)

        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `when free user tries to set a non-pm address as default, the updated list is returned`() = runTest {
        // Given
        coEvery { isPaidUser(userId) } returns false.right()
        coEvery { userAddressManager.observeAddresses(userId, false) } returns flowOf(baseAddresses)
        coEvery {
            userAddressManager.updateOrder(userId, listOf(secondAddressId, firstAddressId))
        } returns updatedAddresses
        val expectedResult = updatedAddresses.right()

        // When
        val result = setDefaultEmailAddress(userId, secondAddressId)

        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `when setting an address as default, the order is forwarded correctly`() = runTest {
        // Given
        coEvery { isPaidUser(userId) } returns false.right()
        coEvery { userAddressManager.observeAddresses(userId, false) } returns flowOf(baseAddresses)
        coEvery {
            userAddressManager.updateOrder(userId, listOf(secondAddressId, firstAddressId))
        } returns updatedAddresses
        val expectedResult = updatedAddresses.right()
        val expectedOrder = listOf(secondAddressId, firstAddressId)

        // When
        val result = setDefaultEmailAddress(userId, secondAddressId)

        // Then
        assertEquals(expectedResult, result)
        coVerify(exactly = 1) {
            userAddressManager.updateOrder(userId, expectedOrder)
        }
    }

    private companion object {

        val userId = UserId("dummy-id")
        val addressId = AddressId("address-id")
        val firstAddressId = AddressId("address-id1")
        val secondAddressId = AddressId("address-id2")
        val baseAddresses = listOf(
            UserAddressSample.build(order = 1, addressId = firstAddressId, email = "email@proton.me"),
            UserAddressSample.build(order = 2, addressId = secondAddressId, email = "email2@proton.me")
        )
        val updatedAddresses = listOf(
            UserAddressSample.build(order = 1, addressId = secondAddressId, email = "email2@proton.me"),
            UserAddressSample.build(order = 2, addressId = firstAddressId, email = "email@proton.me")
        )
    }
}
