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
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.model.NetworkError
import ch.protonmail.android.mailcommon.domain.sample.LabelSample
import ch.protonmail.android.mailcontact.domain.model.ContactGroupLabel
import ch.protonmail.android.mailcontact.domain.model.DecryptedContact
import ch.protonmail.android.maillabel.domain.usecase.ObserveLabels
import ch.protonmail.android.testdata.contact.ContactWithCardsSample
import ch.protonmail.android.testdata.user.UserIdTestData
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.contact.domain.repository.ContactRepository
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.arch.ResponseSource
import me.proton.core.label.domain.entity.LabelType
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ObserveDecryptedContactTest {

    private val contactRepositoryMock = mockk<ContactRepository> {
        every {
            this@mockk.observeContactWithCards(
                UserIdTestData.Primary,
                ContactWithCardsSample.Mario.contact.id
            )
        } returns flowOf(
            DataResult.Success(ResponseSource.Local, ContactWithCardsSample.Mario)
        )
        every {
            this@mockk.observeContactWithCards(
                UserIdTestData.Primary,
                ContactWithCardsSample.Stefano.contact.id
            )
        } returns flowOf(
            DataResult.Success(ResponseSource.Local, ContactWithCardsSample.Stefano)
        )
    }

    private val observeLabels = mockk<ObserveLabels> {
        every {
            this@mockk.invoke(
                UserIdTestData.Primary,
                LabelType.ContactGroup
            )
        } returns flowOf(
            Either.Right(listOf(LabelSample.GroupCoworkers, LabelSample.GroupFriends))
        )
    }

    private val getDecryptedContactMock = mockk<GetDecryptedContact> {
        coEvery {
            this@mockk.invoke(UserIdTestData.Primary, ContactWithCardsSample.Mario)
        } returns DecryptedContact(ContactWithCardsSample.Mario.contact.id).right()
        coEvery {
            this@mockk.invoke(UserIdTestData.Primary, ContactWithCardsSample.Stefano)
        } returns DecryptedContact(ContactWithCardsSample.Stefano.contact.id).right()
    }

    private val observeDecryptedContact = ObserveDecryptedContact(
        contactRepositoryMock,
        getDecryptedContactMock,
        observeLabels
    )

    @Test
    fun `returns Contact with injected ContactGroups if there were any`() = runTest {
        // Given
        val expectedContactGroupLabels = listOf(
            ContactGroupLabel(
                LabelSample.GroupCoworkers.name,
                LabelSample.GroupCoworkers.color
            )
        )

        // When
        observeDecryptedContact(UserIdTestData.Primary, ContactWithCardsSample.Mario.contact.id).test {
            // Then
            val actual = assertIs<Either.Right<DecryptedContact>>(awaitItem())
            assertEquals(expectedContactGroupLabels, actual.value.contactGroupLabels)
            awaitComplete()
        }
    }

    @Test
    fun `returns Contact with no injected ContactGroups if there were none`() = runTest {
        // Given
        val expectedContactGroupLabels = emptyList<ContactGroupLabel>()

        // When
        observeDecryptedContact(UserIdTestData.Primary, ContactWithCardsSample.Stefano.contact.id).test {
            // Then
            val actual = assertIs<Either.Right<DecryptedContact>>(awaitItem())
            assertEquals(expectedContactGroupLabels, actual.value.contactGroupLabels)
            awaitComplete()
        }
    }

    @Test
    fun `propagates error when contactRepository fails`() = runTest {
        // Given
        every { contactRepositoryMock.observeContactWithCards(any(), any()) } returns flowOf(
            DataResult.Error.Remote(
                message = "",
                cause = null,
                httpCode = 404
            )
        )
        val expectedError = DataError.Remote.Http(
            networkError = NetworkError.NotFound,
            apiErrorInfo = ""
        )

        // When
        observeDecryptedContact(UserIdTestData.Primary, ContactWithCardsSample.Stefano.contact.id).test {
            // Then
            val actual = assertIs<Either.Left<DataError.Remote.Http>>(awaitItem())
            assertEquals(expectedError, actual.value)
            awaitComplete()
        }
    }

    @Test
    fun `propagates error when labelRepository fails`() = runTest {
        // Given
        every { observeLabels(any(), any()) } returns flowOf(
            DataError.Remote.Http(NetworkError.NotFound, "info").left()
        )
        val expectedError = DataError.Remote.Http(
            networkError = NetworkError.NotFound,
            apiErrorInfo = "info"
        )

        // When
        observeDecryptedContact(UserIdTestData.Primary, ContactWithCardsSample.Stefano.contact.id).test {
            // Then
            val actual = assertIs<Either.Left<DataError.Remote.Http>>(awaitItem())
            assertEquals(expectedError, actual.value)
            awaitComplete()
        }
    }
}
