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

import java.io.IOException
import arrow.core.Either
import ch.protonmail.android.testdata.contact.ContactTestData
import ch.protonmail.android.testdata.user.UserIdTestData
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.contact.domain.entity.Contact
import me.proton.core.contact.domain.repository.ContactRepository
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class GetContactsTest {

    private val repository = mockk<ContactRepository> {
        coEvery { this@mockk.getAllContacts(UserIdTestData.userId) } returns ContactTestData.contacts
    }

    private val getContacts = GetContacts(repository)

    @Test
    fun `when repository returns contacts they are successfully emitted`() = runTest {
        // When
        val actual = getContacts(UserIdTestData.userId)
        // Then
        assertIs<Either.Right<List<Contact>>>(actual)
        assertEquals(ContactTestData.contacts, actual.value)
    }

    @Test
    fun `when repository returns any data error then emit get contacts error`() = runTest {
        // Given
        coEvery { repository.getAllContacts(UserIdTestData.userId) } throws IOException()
        // When
        val actual = getContacts(UserIdTestData.userId)
        // Then
        assertIs<Either.Left<ObserveContacts.GetContactError>>(actual)
    }
}
