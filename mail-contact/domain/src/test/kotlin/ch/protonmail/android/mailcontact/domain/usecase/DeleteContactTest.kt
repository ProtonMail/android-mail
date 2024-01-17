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
import ch.protonmail.android.mailcontact.domain.repository.ContactDetailRepository
import ch.protonmail.android.mailcontact.domain.repository.ContactDetailRepository.ContactDetailErrors.ContactDetailLocalDataSourceError
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.contact.domain.entity.ContactId
import me.proton.core.domain.entity.UserId
import kotlin.test.Test
import kotlin.test.assertEquals

class DeleteContactTest {

    private val userId = UserId("userId")
    private val contactId = ContactId("contactId")

    private val contactDetailRepository = mockk<ContactDetailRepository>()
    val deleteContact = DeleteContact(contactDetailRepository)

    @Test
    fun `should return unit when delete contact was successful`() = runTest {
        // Given
        coEvery { contactDetailRepository.deleteContact(userId, contactId) } returns Unit.right()


        // When
        val result = deleteContact(userId, contactId)

        // Then
        assertEquals(Unit.right(), result)
    }

    @Test
    fun `should return FailedToDeleteContact when delete contact was not successful`() = runTest {
        // Given
        coEvery {
            contactDetailRepository.deleteContact(userId, contactId)
        } returns ContactDetailLocalDataSourceError.left()

        // When
        val result = deleteContact(userId, contactId)

        // Then
        assertEquals(DeleteContact.DeleteContactErrors.FailedToDeleteContact.left(), result)
    }
}
