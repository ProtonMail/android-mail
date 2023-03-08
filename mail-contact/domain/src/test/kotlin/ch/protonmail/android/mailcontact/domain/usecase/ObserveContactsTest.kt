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
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.contact.domain.entity.Contact
import me.proton.core.contact.domain.repository.ContactRepository
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.arch.ResponseSource
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ObserveContactsTest {

    private val repository = mockk<ContactRepository> {
        every { this@mockk.observeAllContacts(UserIdTestData.userId) } returns flowOf(
            DataResult.Success(ResponseSource.Remote, ContactTestData.contacts)
        )
    }

    private val observeContacts = ObserveContacts(repository)

    @Test
    fun `when repository returns contacts they are successfully emitted`() = runTest {
        // When
        observeContacts(UserIdTestData.userId).test {
            // Then
            val actual = assertIs<Either.Right<List<Contact>>>(awaitItem())
            assertEquals(ContactTestData.contacts, actual.value)
            awaitComplete()
        }
    }

    @Test
    fun `when repository returns any data error then emit get contacts error`() = runTest {
        // Given
        every { repository.observeAllContacts(UserIdTestData.userId) } returns flowOf(
            DataResult.Error.Remote(message = "Unauthorised", cause = null, httpCode = 401)
        )
        // When
        observeContacts(UserIdTestData.userId).test {
            // Then
            assertIs<Either.Left<GetContactError>>(awaitItem())
            awaitComplete()
        }
    }
}
