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

package ch.protonmail.android.mailcomposer.domain.usecase

import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.sample.UserAddressSample
import ch.protonmail.android.mailcommon.domain.usecase.ObserveUserAddresses
import ch.protonmail.android.mailcommon.domain.usecase.ResolveUserAddress
import ch.protonmail.android.test.utils.rule.LoggingTestRule
import ch.protonmail.android.testdata.user.UserIdTestData
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals

class ResolveUserAddressTest {

    @get:Rule
    val loggingTestRule = LoggingTestRule()

    private val userId = UserIdTestData.userId
    private val userAddresses = listOf(UserAddressSample.PrimaryAddress, UserAddressSample.AliasAddress)

    private val observeUserAddresses = mockk<ObserveUserAddresses> {
        every { this@mockk(userId) } returns flowOf(userAddresses)
    }

    private val resolveUserAddress = ResolveUserAddress(observeUserAddresses)

    @Test
    fun `returns user address by email when found in user addresses`() = runTest {
        // Given
        val expectedUserAddress = UserAddressSample.AliasAddress

        // When
        val actual = resolveUserAddress(userId, expectedUserAddress.email)

        // Then
        assertEquals(expectedUserAddress.right(), actual)
    }

    @Test
    fun `returns error when user address was not found by email`() = runTest {
        // Given
        val expectedResult = ResolveUserAddress.Error.UserAddressNotFound
        val notFoundUserAddress = UserAddressSample.DisabledAddress

        // When
        val actual = resolveUserAddress(userId, notFoundUserAddress.email)

        // Then
        assertEquals(expectedResult.left(), actual)
        loggingTestRule.assertErrorLogged("Could not resolve user address for email: ${notFoundUserAddress.email}")
    }

    @Test
    fun `returns user address by address ID when found in user addresses`() = runTest {
        // Given
        val expectedUserAddress = UserAddressSample.AliasAddress

        // When
        val actual = resolveUserAddress(userId, expectedUserAddress.addressId)

        // Then
        assertEquals(expectedUserAddress.right(), actual)
    }

    @Test
    fun `returns error when user address was not found by address ID`() = runTest {
        // Given
        val expectedResult = ResolveUserAddress.Error.UserAddressNotFound
        val notFoundUserAddress = UserAddressSample.DisabledAddress

        // When
        val actual = resolveUserAddress(userId, notFoundUserAddress.addressId)

        // Then
        assertEquals(expectedResult.left(), actual)
        loggingTestRule
            .assertErrorLogged("Could not resolve user address for address ID: ${notFoundUserAddress.addressId.id}")
    }

}
