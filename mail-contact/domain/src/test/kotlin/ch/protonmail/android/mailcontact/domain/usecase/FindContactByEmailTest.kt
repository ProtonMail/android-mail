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
import ch.protonmail.android.testdata.contact.ContactTestData
import ch.protonmail.android.testdata.user.UserIdTestData
import io.mockk.coEvery
import io.mockk.mockk
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.test.assertNotNull

class FindContactByEmailTest {

    val userId = UserIdTestData.userId
    val contact = ContactTestData.buildContactWith(
        userId = userId,
        name = "Test User", // <-- match
        contactEmails = listOf(
            ContactTestData.buildContactEmailWith(
                name = "Test User 1",
                address = "test1@proton.ch"
            ),
            ContactTestData.buildContactEmailWith(
                name = "Test User 2",
                address = "Test2@protonmail.ch"
            )
        )
    )
    private val contactList = ContactTestData.contacts + contact

    private val getContacts = mockk<GetContacts> {
        coEvery { this@mockk.invoke(userId) } returns Either.Right(contactList)
    }

    private val findContactByEmail = FindContactByEmail(getContacts)

    @Test
    fun `should find and return the existing contact`() = runTest {
        // Given
        val participantEmail = "teST2@protonmail.ch"

        // When
        val result = findContactByEmail(userId, participantEmail)

        // Then
        assertNotNull(result)
        assertTrue(result.contactEmails.any { it.email.equals(participantEmail, ignoreCase = true) })
    }

    @Test
    fun `should return null when there is no contact with that email`() = runTest {
        // Given
        val participantEmail = "notexist@proton.ch"

        // When
        val result = findContactByEmail(userId, participantEmail)

        // Then
        assertNull(result)
    }
}
