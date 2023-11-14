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

import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.sample.UserAddressSample
import ch.protonmail.android.testdata.user.UserIdTestData
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.user.domain.entity.UserAddress
import org.junit.Test
import kotlin.test.assertEquals

internal class GetPrimaryAddressTest {

    private val userId = UserIdTestData.userId

    private val observeUserAddresses = mockk<ObserveUserAddresses>()

    private val getPrimaryAddress = GetPrimaryAddress(observeUserAddresses)

    @Test
    fun `returns primary user address when list contains more than one`() = runTest {
        // Given
        val addresses = listOf(UserAddressSample.AliasAddress, UserAddressSample.PrimaryAddress)
        every { observeUserAddresses.invoke(userId) } returns flowOf(addresses)

        // When
        val actual = getPrimaryAddress(userId)

        // Then
        assertEquals(UserAddressSample.PrimaryAddress.right(), actual)
    }

    @Test
    fun `returns no cached data error when list is empty`() = runTest {
        // Given
        val addresses = emptyList<UserAddress>()
        every { observeUserAddresses.invoke(userId) } returns flowOf(addresses)

        // When
        val actual = getPrimaryAddress(userId)

        // Then
        assertEquals(DataError.Local.NoDataCached.left(), actual)
    }

}
