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

import app.cash.turbine.test
import arrow.core.Either
import ch.protonmail.android.mailcontact.domain.model.GetContactError
import ch.protonmail.android.testdata.contact.ContactTestData
import ch.protonmail.android.testdata.user.UserIdTestData
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.contact.domain.entity.Contact
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@Suppress("MaxLineLength")
class SearchContactsTest {

    private val observeContacts = mockk<ObserveContacts> {
        coEvery { this@mockk.invoke(UserIdTestData.userId) } returns flowOf(Either.Right(ContactTestData.contacts))
    }

    private val searchContacts = SearchContacts(observeContacts)

    @Test
    fun `when there are multiple matching contacts, they are emitted`() = runTest {
        // Given
        val query = "cont"

        // When
        searchContacts(UserIdTestData.userId, query).test {
            // Then
            val actual = assertIs<Either.Right<List<Contact>>>(awaitItem())
            assertEquals(ContactTestData.contacts, actual.value)
            awaitComplete()
        }
    }

    @Test
    fun `when there is contact matched only by name, it is emitted with all ContactEmails`() = runTest {
        // Given
        val query = "impo"

        val contact = ContactTestData.buildContactWith(
            userId = UserIdTestData.userId,
            name = "important contact display name", // <-- match
            contactEmails = listOf(
                ContactTestData.buildContactEmailWith(
                    name = "name 1",
                    address = "address1@proton.ch"
                ),
                ContactTestData.buildContactEmailWith(
                    name = "name 2",
                    address = "address2@protonmail.ch"
                )
            )
        )
        val contacts = ContactTestData.contacts + contact
        coEvery { observeContacts(UserIdTestData.userId) } returns flowOf(Either.Right(contacts))

        // When
        searchContacts(UserIdTestData.userId, query).test {
            // Then
            val actual = assertIs<Either.Right<List<Contact>>>(awaitItem())
            assertEquals(listOf(contact), actual.value)
            awaitComplete()
        }
    }

    @Test
    fun `when there is contact matched only by ContactEmail, it is emitted with only matching ContactEmails if onlyMatchingContactEmails = true`() =
        runTest {
            // Given
            val query = "mail"

            val contact = ContactTestData.buildContactWith(
                userId = UserIdTestData.userId,
                name = "important contact display name",
                contactEmails = listOf(
                    ContactTestData.buildContactEmailWith(
                        name = "name 1",
                        address = "address1@proton.ch"
                    ),
                    ContactTestData.buildContactEmailWith(
                        name = "name 2",
                        address = "address2@protonmail.ch" // <-- match
                    )
                )
            )
            val contacts = ContactTestData.contacts + contact
            coEvery { observeContacts(UserIdTestData.userId) } returns flowOf(Either.Right(contacts))

            // When
            searchContacts(UserIdTestData.userId, query, onlyMatchingContactEmails = true).test {
                // Then
                val actual = assertIs<Either.Right<List<Contact>>>(awaitItem())
                assertTrue(actual.value.size == 1)

                val matchedContact = actual.value.first()

                assertEquals(contact.userId, matchedContact.userId)
                assertEquals(contact.id, matchedContact.id)
                assertEquals(contact.name, matchedContact.name)

                assertTrue(matchedContact.contactEmails.size == 1)
                assertEquals(
                    listOf(contact.contactEmails[1]), // return only 2nd ContactEmail
                    listOf(matchedContact.contactEmails.first())
                )
                awaitComplete()
            }
        }

    @Test
    fun `when there is contact matched only by ContactEmail, it is emitted with all ContactEmails if onlyMatchingContactEmails = false`() =
        runTest {
            // Given
            val query = "mail"

            val contact = ContactTestData.buildContactWith(
                userId = UserIdTestData.userId,
                name = "important contact display name",
                contactEmails = listOf(
                    ContactTestData.buildContactEmailWith(
                        name = "name 1",
                        address = "address1@proton.ch"
                    ),
                    ContactTestData.buildContactEmailWith(
                        name = "name 2",
                        address = "address2@protonmail.ch" // <-- match
                    )
                )
            )
            val contacts = ContactTestData.contacts + contact
            coEvery { observeContacts(UserIdTestData.userId) } returns flowOf(Either.Right(contacts))

            // When
            searchContacts(UserIdTestData.userId, query, onlyMatchingContactEmails = false).test {
                // Then
                val actual = assertIs<Either.Right<List<Contact>>>(awaitItem())
                assertTrue(actual.value.size == 1)

                val matchedContact = actual.value.first()

                assertEquals(contact.userId, matchedContact.userId)
                assertEquals(contact.id, matchedContact.id)
                assertEquals(contact.name, matchedContact.name)

                assertTrue(matchedContact.contactEmails.size == 2)
                assertEquals(
                    listOf(contact.contactEmails[0]),
                    listOf(matchedContact.contactEmails[0])
                )
                assertEquals(
                    listOf(contact.contactEmails[1]),
                    listOf(matchedContact.contactEmails[1])
                )
                awaitComplete()
            }
        }

    @Test
    fun `when there are no matching contacts, empty list is emitted`() = runTest {
        // Given
        val query = "there is no contact like this"

        // When
        searchContacts(UserIdTestData.userId, query).test {
            // Then
            val actual = assertIs<Either.Right<List<Contact>>>(awaitItem())
            assertEquals(emptyList(), actual.value)
            awaitComplete()
        }
    }

    @Test
    fun `when observe contacts returns any error, this error is emitted`() = runTest {
        // Given
        val query = "cont"
        coEvery { observeContacts(UserIdTestData.userId) } returns flowOf(Either.Left(GetContactError))

        // When
        searchContacts(UserIdTestData.userId, query).test {
            // Then
            assertIs<Either.Left<GetContactError>>(awaitItem())
            awaitComplete()
        }
    }
}
