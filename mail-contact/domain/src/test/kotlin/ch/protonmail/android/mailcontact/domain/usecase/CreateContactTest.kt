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

package ch.protonmail.android.mailcontact.domain.usecase

import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcontact.domain.model.ContactProperty
import ch.protonmail.android.mailcontact.domain.model.DecryptedContact
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.contact.domain.entity.ContactCard
import me.proton.core.contact.domain.repository.ContactRepository
import me.proton.core.domain.entity.UserId
import me.proton.core.network.domain.ApiException
import me.proton.core.network.domain.ApiResult
import org.junit.Test
import kotlin.test.assertEquals

class CreateContactTest {

    private val userId = UserId("userId")

    private val contactRepository = mockk<ContactRepository>()
    private val encryptAndSignContactCards = mockk<EncryptAndSignContactCards>()
    val createContact = CreateContact(contactRepository, encryptAndSignContactCards)

    @Test
    fun `should return unit when create contact was successful`() = runTest {
        // Given
        val decryptedContact = DecryptedContact(
            id = null,
            formattedName = ContactProperty.FormattedName(value = "Mario_ClearText@protonmail.com")
        )
        coEvery { contactRepository.createContact(userId, any()) } returns Unit
        coEvery { encryptAndSignContactCards(userId, decryptedContact) } returns listOf<ContactCard>().right()

        // When
        val result = createContact(userId, decryptedContact)

        // Then
        assertEquals(Unit.right(), result)
    }

    @Test
    fun `should return FailedToCreateContact when create contact was not successful`() = runTest {
        // Given
        val decryptedContact = DecryptedContact(
            id = null,
            formattedName = ContactProperty.FormattedName(value = "Mario_ClearText@protonmail.com")
        )
        coEvery { contactRepository.createContact(userId, any()) } throws Exception("Test")
        coEvery { encryptAndSignContactCards(userId, decryptedContact) } returns listOf<ContactCard>().right()

        // When
        val result = createContact(userId, decryptedContact)

        // Then
        assertEquals(CreateContact.CreateContactErrors.FailedToCreateContact.left(), result)
    }

    @Test
    fun `should return NumberOfContactsReached when create contact failed because of limit`() = runTest {
        // Given
        val decryptedContact = DecryptedContact(
            id = null,
            formattedName = ContactProperty.FormattedName(value = "Mario_ClearText@protonmail.com")
        )
        coEvery { contactRepository.createContact(userId, any()) } throws ApiException(
            ApiResult.Error.Http(
                httpCode = 422,
                message = "",
                proton = ApiResult.Error.ProtonData(
                    code = 2024,
                    error = "Number of contacts limit reached"
                )
            )
        )
        coEvery { encryptAndSignContactCards(userId, decryptedContact) } returns listOf<ContactCard>().right()

        // When
        val result = createContact(userId, decryptedContact)

        // Then
        assertEquals(CreateContact.CreateContactErrors.MaximumNumberOfContactsReached.left(), result)
    }

    @Test
    fun `should return FailedToEncryptAndSignContactCards when encrypt contact was not successful`() = runTest {
        // Given
        val decryptedContact = DecryptedContact(
            id = null,
            formattedName = ContactProperty.FormattedName(value = "Mario_ClearText@protonmail.com")
        )
        coEvery { contactRepository.createContact(userId, any()) } returns Unit
        coEvery {
            encryptAndSignContactCards(userId, decryptedContact)
        } returns EncryptingContactCardsError.DecryptingContactCardError.left()

        // When
        val result = createContact(userId, decryptedContact)

        // Then
        assertEquals(CreateContact.CreateContactErrors.FailedToEncryptAndSignContactCards.left(), result)
    }
}
