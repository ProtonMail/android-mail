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

import android.util.Log
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.sample.UserAddressSample
import ch.protonmail.android.test.utils.TestTree
import ch.protonmail.android.testdata.user.UserIdTestData
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.BeforeTest
import timber.log.Timber
import kotlin.test.assertEquals

class ResolveUserAddressTest {

    private val testTree = TestTree()
    private val userId = UserIdTestData.userId
    private val userAddresses = listOf(UserAddressSample.PrimaryAddress, UserAddressSample.AliasAddress)

    private val observeUserAddresses = mockk<ObserveUserAddresses> {
        every { this@mockk(userId) } returns flowOf(userAddresses)
    }

    private val resolveUserAddress = ResolveUserAddress(observeUserAddresses)

    @BeforeTest
    fun setUp() {
        Timber.plant(testTree)
    }

    @Test
    fun `returns user address by email when found in user addresses`() = runTest {
        // Given
        val expectedUserAddress = UserAddressSample.AliasAddress

        // When
        val actual = resolveUserAddress(userId, expectedUserAddress.email)

        // Then
        assertEquals(UserAddressSample.AliasAddress.right(), actual)
    }

    @Test
    fun `returns error when user address not found`() = runTest {
        // Given
        val expectedResult = ResolveUserAddress.Error.UserAddressNotFound
        val notFoundUserAddress = UserAddressSample.DisabledAddress

        // When
        val actual = resolveUserAddress(userId, notFoundUserAddress.email)

        // Then
        assertEquals(expectedResult.left(), actual)
        assertErrorLogged("Could not resolve user address for ${notFoundUserAddress.email}")
    }

    private fun assertErrorLogged(message: String) {
        val expectedLog = TestTree.Log(Log.ERROR, null, message, null)
        assertEquals(expectedLog, testTree.logs.lastOrNull())
    }
}
