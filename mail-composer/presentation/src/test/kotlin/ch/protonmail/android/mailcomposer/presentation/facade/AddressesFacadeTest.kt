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

package ch.protonmail.android.mailcomposer.presentation.facade

import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.sample.UserAddressSample
import ch.protonmail.android.mailcommon.domain.usecase.GetPrimaryAddress
import ch.protonmail.android.mailcomposer.domain.model.SenderEmail
import ch.protonmail.android.mailcomposer.domain.usecase.GetComposerSenderAddresses
import ch.protonmail.android.mailcomposer.domain.usecase.ValidateSenderAddress
import ch.protonmail.android.mailcomposer.domain.usecase.ValidateSenderAddress.ValidationFailure
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class AddressesFacadeTest {

    private val getPrimaryAddress = mockk<GetPrimaryAddress>(relaxed = true)
    private val getComposerSenderAddresses = mockk<GetComposerSenderAddresses>(relaxed = true)
    private val validateSenderAddress = mockk<ValidateSenderAddress>(relaxed = true)

    private lateinit var addressesFacade: AddressesFacade

    @BeforeTest
    fun setup() {
        addressesFacade = AddressesFacade(
            getPrimaryAddress,
            getComposerSenderAddresses,
            validateSenderAddress
        )
    }

    @Test
    fun `should proxy getPrimaryAddress accordingly (success)`() = runTest {
        // Given
        val userId = UserId("user-id")
        coEvery { getPrimaryAddress.invoke(userId) } returns UserAddressSample.PrimaryAddress.right()

        // When
        val senderEmail = addressesFacade.getPrimarySenderEmail(userId).getOrNull()

        // Then
        coVerify(exactly = 1) { getPrimaryAddress.invoke(userId) }
        assertEquals(UserAddressSample.PrimaryAddress.email, senderEmail?.value)
    }

    @Test
    fun `should proxy getPrimaryAddress accordingly (failure)`() = runTest {
        // Given
        val userId = UserId("user-id")
        coEvery { getPrimaryAddress.invoke(userId) } returns DataError.Local.Unknown.left()

        // When
        val senderEmail = addressesFacade.getPrimarySenderEmail(userId).getOrNull()

        // Then
        coVerify(exactly = 1) { getPrimaryAddress.invoke(userId) }
        assertNull(senderEmail)
    }

    @Test
    fun `should proxy getSenderAddresses accordingly`() = runTest {
        // When
        addressesFacade.getSenderAddresses()

        // Then
        coVerify(exactly = 1) { getComposerSenderAddresses.invoke() }
    }

    @Test
    fun `should proxy validateSenderAddress accordingly (success)`() = runTest {
        // Given
        val userId = UserId("user-id")
        val senderEmail = SenderEmail("sender-email")
        val expectedResult = ValidateSenderAddress.ValidationResult.Valid(senderEmail)

        coEvery { validateSenderAddress.invoke(userId, senderEmail) } returns expectedResult.right()

        // When
        val result = addressesFacade.validateSenderAddress(userId, senderEmail).getOrNull()

        // Then
        coVerify(exactly = 1) { validateSenderAddress.invoke(userId, senderEmail) }
        assertEquals(expectedResult, result)
    }

    @Test
    fun `should proxy validateSenderAddress accordingly (failure with fallback)`() = runTest {
        // Given
        val userId = UserId("user-id")
        val senderEmail = SenderEmail(UserAddressSample.PrimaryAddress.email)
        val fallbackEmail = SenderEmail("fallback@pm.me")
        val expectedResult = ValidateSenderAddress.ValidationResult.Invalid(
            validAddress = fallbackEmail,
            invalid = senderEmail,
            reason = ValidateSenderAddress.ValidationError.GenericError
        )

        coEvery { validateSenderAddress.invoke(userId, senderEmail) } returns ValidationFailure.CouldNotValidate.left()
        coEvery {
            getPrimaryAddress.invoke(userId)
        } returns UserAddressSample.PrimaryAddress.copy(email = fallbackEmail.value).right()

        // When
        val result = addressesFacade.validateSenderAddress(userId, senderEmail).getOrNull()

        // Then
        coVerify(exactly = 1) { validateSenderAddress.invoke(userId, senderEmail) }
        assertEquals(expectedResult, result)
    }
}
