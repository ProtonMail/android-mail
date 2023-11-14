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
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.sample.UserAddressSample
import ch.protonmail.android.mailcommon.domain.usecase.IsPaidUser
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailcommon.domain.usecase.ObserveUserAddresses
import ch.protonmail.android.testdata.user.UserIdTestData
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Test

class GetComposerSenderAddressesTest {

    private val userId = UserIdTestData.userId
    private val addresses = listOf(UserAddressSample.PrimaryAddress, UserAddressSample.AliasAddress)

    private val observePrimaryUserId = mockk<ObservePrimaryUserId> {
        every { this@mockk.invoke() } returns flowOf(userId)
    }
    private val isPaidUser = mockk<IsPaidUser>()
    private val observeUserAddresses = mockk<ObserveUserAddresses> {
        coEvery { this@mockk(userId) } returns flowOf(addresses)
    }

    private val getComposerSenderAddresses = GetComposerSenderAddresses(
        observePrimaryUserId,
        isPaidUser,
        observeUserAddresses
    )

    @Test
    fun `returns paid feature error when user account is free`() = runTest {
        // Given
        coEvery { isPaidUser(userId) } returns false.right()

        // When
        val actual = getComposerSenderAddresses()

        // Then
        Assert.assertEquals(GetComposerSenderAddresses.Error.UpgradeToChangeSender.left(), actual)
    }

    @Test
    fun `returns list of user addresses when user has a paid subscription`() = runTest {
        // Given
        coEvery { isPaidUser(userId) } returns true.right()

        // When
        val actual = getComposerSenderAddresses()

        // Then
        Assert.assertEquals(addresses.right(), actual)
    }

    @Test
    fun `excludes disabled and external accounts from the returned list`() = runTest {
        // Given
        val disabledExternalAddresses = listOf(
            UserAddressSample.PrimaryAddress,
            UserAddressSample.DisabledAddress,
            UserAddressSample.ExternalAddress
        )
        coEvery { isPaidUser(userId) } returns true.right()
        coEvery { observeUserAddresses(userId) } returns flowOf(disabledExternalAddresses)

        // When
        val actual = getComposerSenderAddresses()

        // Then
        val expected = listOf(UserAddressSample.PrimaryAddress)
        Assert.assertEquals(expected.right(), actual)
    }

    @Test
    fun `returns failed getting primary user error when no primary user exists`() = runTest {
        // Given
        every { observePrimaryUserId() } returns flowOf(null)

        // When
        val actual = getComposerSenderAddresses()

        // Then
        Assert.assertEquals(GetComposerSenderAddresses.Error.FailedGettingPrimaryUser.left(), actual)
    }

    @Test
    fun `returns failed determining user subscription when is paid user fails`() = runTest {
        // Given
        coEvery { isPaidUser(userId) } returns DataError.Local.NoDataCached.left()

        // When
        val actual = getComposerSenderAddresses()

        // Then
        Assert.assertEquals(GetComposerSenderAddresses.Error.FailedDeterminingUserSubscription.left(), actual)
    }
}
