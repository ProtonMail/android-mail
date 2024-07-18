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

import arrow.core.Either
import ch.protonmail.android.mailcontact.domain.model.GetContactError
import ch.protonmail.android.testdata.user.UserIdTestData
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.contact.domain.entity.Contact
import me.proton.core.contact.domain.entity.ContactEmail
import me.proton.core.contact.domain.entity.ContactEmailId
import me.proton.core.contact.domain.entity.ContactId
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class GetContactEmailsByIdTest {

    private val defaultTestContact = Contact(
        UserIdTestData.userId,
        ContactId("ContactId1"),
        "John Doe",
        listOf(
            ContactEmail(
                UserIdTestData.userId,
                ContactEmailId("ContactEmailId1"),
                "John Doe",
                "johndoe+alias@protonmail.com",
                0,
                0,
                ContactId("ContactId1"),
                "johndoe@protonmail.com",
                emptyList(),
                true,
                lastUsedTime = 0
            ),
            ContactEmail(
                UserIdTestData.userId,
                ContactEmailId("ContactEmailId2"),
                "Jane Doe",
                "janedoe@protonmail.com",
                0,
                0,
                ContactId("ContactId1"),
                "janedoe@protonmail.com",
                emptyList(),
                true,
                lastUsedTime = 0
            )
        )
    )

    private val observeContacts = mockk<ObserveContacts> {
        coEvery { this@mockk.invoke(UserIdTestData.userId) } returns flowOf(Either.Right(listOf(defaultTestContact)))
    }

    private val getContactEmailsById = GetContactEmailsById(observeContacts)

    @Test
    fun `when observe contacts returns contacts they are successfully emitted`() = runTest {
        // When
        val actual = getContactEmailsById(UserIdTestData.userId, listOf("ContactEmailId2"))
        // Then
        assertIs<Either.Right<List<ContactEmail>>>(actual)
        assertEquals(listOf(defaultTestContact.contactEmails[1]), actual.value)
    }

    @Test
    fun `when observe contacts returns any error then emit get contacts error`() = runTest {
        // Given
        coEvery { observeContacts(UserIdTestData.userId) } returns flowOf(Either.Left(GetContactError))
        // When
        val actual = getContactEmailsById(UserIdTestData.userId, listOf("ContactEmailId2"))
        // Then
        assertIs<Either.Left<GetContactError>>(actual)
    }
}
