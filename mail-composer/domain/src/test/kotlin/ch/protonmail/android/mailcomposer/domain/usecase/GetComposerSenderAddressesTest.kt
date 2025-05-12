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
import ch.protonmail.android.mailcommon.domain.usecase.IsPaidMailUser
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
import javax.inject.Provider

class GetComposerSenderAddressesTest {

    private val userId = UserIdTestData.userId
    private val addresses = listOf(UserAddressSample.PrimaryAddress, UserAddressSample.AliasAddress)

    private val observePrimaryUserId = mockk<ObservePrimaryUserId> {
        every { this@mockk.invoke() } returns flowOf(userId)
    }
    private val isPaidMailUser = mockk<IsPaidMailUser>()
    private val observeUserAddresses = mockk<ObserveUserAddresses> {
        coEvery { this@mockk(userId) } returns flowOf(addresses)
    }

    private val provideIsExternalAddressesEnabled = mockk<Provider<Boolean>>()

    private val getComposerSenderAddresses by lazy {
        GetComposerSenderAddresses(
            observePrimaryUserId = observePrimaryUserId,
            isPaidUser = isPaidMailUser,
            observeUserAddresses = observeUserAddresses,
            externalSendingEnabled = provideIsExternalAddressesEnabled.get()
        )
    }

    @Test
    fun `returns paid feature error when user account has 1 send enabled address only and no paid mail subscription`() =
        runTest {
            // Given
            externalAddressesEnabled(true)
            coEvery { isPaidMailUser(userId) } returns false.right()

            val addresses = listOf(
                UserAddressSample.PrimaryAddress,
                UserAddressSample.AliasAddress.copy(canSend = false),
                UserAddressSample.ExternalAddressWithoutSend
            )

            every { observeUserAddresses(userId) } returns flowOf(addresses)


            // When
            val actual = getComposerSenderAddresses()

            // Then
            Assert.assertEquals(GetComposerSenderAddresses.Error.UpgradeToChangeSender.left(), actual)
        }

    @Test
    fun `returns list of addresses to free user when addresses are send-enabled`() = runTest {
        // Given
        externalAddressesEnabled(true)
        coEvery { isPaidMailUser(userId) } returns false.right()

        // When
        val actual = getComposerSenderAddresses()

        // Then
        Assert.assertEquals(addresses.right(), actual)
    }

    @Test
    fun `returns list of user addresses when user has a paid mail subscription`() = runTest {
        // Given
        externalAddressesEnabled(true)
        coEvery { isPaidMailUser(userId) } returns true.right()

        // When
        val actual = getComposerSenderAddresses()

        // Then
        Assert.assertEquals(addresses.right(), actual)
    }

    @Test
    fun `excludes disabled and includes external accounts with send permission`() = runTest {
        // Given
        externalAddressesEnabled(true)
        val allUserAddresses = listOf(
            UserAddressSample.PrimaryAddress,
            UserAddressSample.DisabledAddress,
            UserAddressSample.ExternalAddressWithSend,
            UserAddressSample.ExternalAddressWithoutSend
        )
        coEvery { isPaidMailUser(userId) } returns true.right()
        coEvery { observeUserAddresses(userId) } returns flowOf(allUserAddresses)

        // When
        val actual = getComposerSenderAddresses()

        // Then
        val expected = listOf(
            UserAddressSample.PrimaryAddress,
            UserAddressSample.ExternalAddressWithSend
        )
        Assert.assertEquals(expected.right(), actual)
    }


    @Test
    fun `excludes external accounts if FF disabled`() = runTest {
        // Given
        externalAddressesEnabled(false)
        val allUserAddresses = listOf(
            UserAddressSample.PrimaryAddress,
            UserAddressSample.DisabledAddress,
            UserAddressSample.ExternalAddressWithSend,
            UserAddressSample.ExternalAddressWithoutSend
        )
        coEvery { isPaidMailUser(userId) } returns true.right()
        coEvery { observeUserAddresses(userId) } returns flowOf(allUserAddresses)

        // When
        val actual = getComposerSenderAddresses()

        // Then
        val expected = listOf(
            UserAddressSample.PrimaryAddress
        )
        Assert.assertEquals(expected.right(), actual)
    }


    @Test
    fun `allows free users to switch to their external address`() = runTest {
        // Given
        externalAddressesEnabled(true)
        val allUserAddresses = listOf(
            UserAddressSample.PrimaryAddress,
            UserAddressSample.ExternalAddressWithSend
        )
        coEvery { isPaidMailUser(userId) } returns false.right()
        coEvery { observeUserAddresses(userId) } returns flowOf(allUserAddresses)

        // When
        val actual = getComposerSenderAddresses()

        // Then
        val expected = listOf(
            UserAddressSample.PrimaryAddress,
            UserAddressSample.ExternalAddressWithSend
        )
        Assert.assertEquals(expected.right(), actual)
    }

    @Test
    fun `returns failed getting primary user error when no primary user exists`() = runTest {
        // Given
        externalAddressesEnabled(true)
        every { observePrimaryUserId() } returns flowOf(null)

        // When
        val actual = getComposerSenderAddresses()

        // Then
        Assert.assertEquals(GetComposerSenderAddresses.Error.FailedGettingPrimaryUser.left(), actual)
    }

    @Test
    fun `returns failed determining user subscription when is paid user fails`() = runTest {
        // Given
        externalAddressesEnabled(true)
        coEvery { isPaidMailUser(userId) } returns DataError.Local.NoDataCached.left()

        // When
        val actual = getComposerSenderAddresses()

        // Then
        Assert.assertEquals(GetComposerSenderAddresses.Error.FailedDeterminingUserSubscription.left(), actual)
    }

    private fun externalAddressesEnabled(value: Boolean) {
        every {
            provideIsExternalAddressesEnabled.get()
        } returns value
    }
}
