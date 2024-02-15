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
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailcommon.domain.usecase.IsPaidUser
import ch.protonmail.android.mailcommon.domain.usecase.ObserveUserAddresses
import ch.protonmail.android.mailcomposer.domain.model.SenderEmail
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import me.proton.core.user.domain.entity.UserAddress
import org.junit.Test
import kotlin.test.assertEquals

class ValidateSenderAddressTest {

    private val observeUserAddresses = mockk<ObserveUserAddresses>()
    private val isPaidUser = mockk<IsPaidUser>()

    private val validateSenderAddress = ValidateSenderAddress(observeUserAddresses, isPaidUser)

    @Test
    fun `returns could not validate error when failing to get user addresses`() = runTest {
        // Given
        val senderEmail = SenderEmail("any@email.me")
        expectUserAddressesError(userId)

        // When
        val actual = validateSenderAddress(userId, senderEmail)

        // Then
        assertEquals(ValidateSenderAddress.ValidationFailure.CouldNotValidate.left(), actual)
    }

    @Test
    fun `returns could not validate error when failing to get given user address`() = runTest {
        // Given
        val senderEmail = SenderEmail("not-found@email.me")
        expectedUserAddresses(userId) { listOf(UserAddressSample.PrimaryAddress) }
        expectPaidUser(userId)

        // When
        val actual = validateSenderAddress(userId, senderEmail)

        // Then
        assertEquals(ValidateSenderAddress.ValidationFailure.CouldNotValidate.left(), actual)
    }

    @Test
    fun `returns no enabled address error when none of the user address is enabled`() = runTest {
        // Given
        val senderEmail = SenderEmail("disabled@protonmail.ch")
        expectedUserAddresses(userId) { listOf(UserAddressSample.DisabledAddress) }
        expectPaidUser(userId)

        // When
        val actual = validateSenderAddress(userId, senderEmail)

        // Then
        assertEquals(ValidateSenderAddress.ValidationFailure.AllAddressesDisabled.left(), actual)
    }

    @Test
    fun `returns invalid result when given address is disabled and another valid address exists`() = runTest {
        // Given
        val disabledSenderEmail = SenderEmail("disabled@protonmail.ch")
        val validAddress = UserAddressSample.PrimaryAddress
        expectedUserAddresses(userId) { listOf(UserAddressSample.DisabledAddress, validAddress) }
        expectPaidUser(userId)

        // When
        val actual = validateSenderAddress(userId, disabledSenderEmail)

        // Then
        val expected = ValidateSenderAddress.ValidationResult.Invalid(
            SenderEmail(validAddress.email),
            disabledSenderEmail,
            ValidateSenderAddress.ValidationError.DisabledAddress
        ).right()
        assertEquals(expected, actual)
    }

    @Test
    fun `returns invalid result when user is free and given address is @pm me`() = runTest {
        // Given
        val pmMeEmail = SenderEmail("myaddress@pm.me")
        expectedUserAddresses(userId) { listOf(UserAddressSample.PrimaryAddress, UserAddressSample.PmMeAddressAlias) }
        expectFreeUser(userId)

        // When
        val actual = validateSenderAddress(userId, pmMeEmail)

        // Then
        val expected = ValidateSenderAddress.ValidationResult.Invalid(
            SenderEmail(UserAddressSample.PrimaryAddress.email),
            pmMeEmail,
            ValidateSenderAddress.ValidationError.PaidAddress
        ).right()
        assertEquals(expected, actual)
    }

    @Test
    fun `returns valid result when the given address is valid for sending`() = runTest {
        // Given
        val aliasEmail = SenderEmail("alias@protonmail.ch")
        expectedUserAddresses(userId) { listOf(UserAddressSample.PrimaryAddress, UserAddressSample.AliasAddress) }
        expectFreeUser(userId)

        // When
        val actual = validateSenderAddress(userId, aliasEmail)

        // Then
        val expected = ValidateSenderAddress.ValidationResult.Valid(aliasEmail).right()
        assertEquals(expected, actual)
    }

    private fun expectPaidUser(userId: UserId) {
        coEvery { isPaidUser(userId) } returns true.right()
    }

    private fun expectFreeUser(userId: UserId) {
        coEvery { isPaidUser(userId) } returns false.right()
    }

    private fun expectedUserAddresses(userId: UserId, addresses: () -> List<UserAddress>) = addresses().also {
        every { observeUserAddresses.invoke(userId) } returns flowOf(it)
    }

    private fun expectUserAddressesError(userId: UserId) {
        every { observeUserAddresses.invoke(userId) } returns flowOf()
    }

    companion object {
        private val userId = UserIdSample.Primary

    }
}
