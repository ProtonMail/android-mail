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

package ch.protonmail.android.mailcomposer.presentation.usecase

import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.sample.UserAddressSample
import ch.protonmail.android.mailcommon.domain.usecase.IsPaidUser
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailcomposer.domain.usecase.ObserveUserAddresses
import ch.protonmail.android.testdata.user.UserIdTestData
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetChangeSenderAddressesTest {

    private val userId = UserIdTestData.userId
    private val addresses = listOf(UserAddressSample.primaryAddress, UserAddressSample.secondaryAddress)

    private val observePrimaryUserId = mockk<ObservePrimaryUserId> {
        every { this@mockk.invoke() } returns flowOf(userId)
    }
    private val isPaidUser = mockk<IsPaidUser> {
        coEvery { this@mockk(userId) } returns true.right()
    }
    private val observeUserAddresses = mockk<ObserveUserAddresses> {
        coEvery { this@mockk(userId) } returns flowOf(addresses)
    }

    private val getChangeSenderAddresses = GetChangeSenderAddresses(
        observePrimaryUserId,
        isPaidUser,
        observeUserAddresses
    )

    @Test
    fun `returns paid feature error when user account is free`() = runTest {
        // Given
        coEvery { isPaidUser(userId) } returns false.right()

        // When
        val actual = getChangeSenderAddresses()

        // Then
        assertEquals(GetChangeSenderAddresses.Error.UpgradeToChangeSender.left(), actual)
    }

    @Test
    fun `returns list of user addresses when user has a paid subscription`() = runTest {
        // Given
        coEvery { isPaidUser(userId) } returns true.right()

        // When
        val actual = getChangeSenderAddresses()

        // Then
        assertEquals(addresses.right(), actual)
    }

    @Test
    fun `returns failed getting primary user error when no primary user exists`() = runTest {
        // Given
        every { observePrimaryUserId() } returns flowOf(null)

        // When
        val actual = getChangeSenderAddresses()

        // Then
        assertEquals(GetChangeSenderAddresses.Error.FailedGettingPrimaryUser.left(), actual)
    }

    @Test
    fun `returns failed determining user subscription when is paid user fails`() = runTest {
        // Given
        coEvery { isPaidUser(userId) } returns DataError.Local.NoDataCached.left()

        // When
        val actual = getChangeSenderAddresses()

        // Then
        assertEquals(GetChangeSenderAddresses.Error.FailedDeterminingUserSubscription.left(), actual)
    }
}
