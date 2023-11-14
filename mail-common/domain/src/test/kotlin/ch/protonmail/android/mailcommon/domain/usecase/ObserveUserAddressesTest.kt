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

package ch.protonmail.android.mailcommon.domain.usecase

import app.cash.turbine.test
import ch.protonmail.android.mailcommon.domain.sample.UserAddressSample
import ch.protonmail.android.testdata.user.UserIdTestData
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import me.proton.core.user.domain.UserManager
import me.proton.core.user.domain.entity.UserAddress
import org.junit.Test
import kotlin.test.assertEquals

internal class ObserveUserAddressesTest {

    private val userId = UserIdTestData.userId

    private val addressesFlow = MutableSharedFlow<List<UserAddress>>()
    private val userManager = mockk<UserManager> {
        every { this@mockk.observeAddresses(userId) } returns addressesFlow
    }

    private val observeUserAddresses = ObserveUserAddresses(userManager)

    @Test
    fun `returns user addresses when user manager returns addresses`() = runTest {
        observeUserAddresses.invoke(userId).test {
            // Given
            val addresses = listOf(UserAddressSample.PrimaryAddress, UserAddressSample.AliasAddress)
            userManagerSuccessfullyReturns(addresses)

            // Then
            assertEquals(addresses, awaitItem())
        }
    }

    private suspend fun userManagerSuccessfullyReturns(addresses: List<UserAddress>) {
        addressesFlow.emit(addresses)
    }
}
