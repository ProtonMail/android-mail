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
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.model.NetworkError
import ch.protonmail.android.mailcontact.domain.model.ContactGroup
import ch.protonmail.android.maillabel.domain.usecase.ObserveLabels
import ch.protonmail.android.testdata.contact.ContactIdTestData
import ch.protonmail.android.testdata.contact.ContactTestData
import ch.protonmail.android.testdata.label.LabelTestData
import ch.protonmail.android.testdata.user.UserIdTestData
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.contact.domain.entity.ContactEmail
import me.proton.core.contact.domain.entity.ContactEmailId
import me.proton.core.contact.domain.repository.ContactRepository
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.arch.ResponseSource
import me.proton.core.label.domain.entity.LabelId
import me.proton.core.label.domain.entity.LabelType
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ObserveContactGroupTest {

    private val label = LabelTestData.buildLabel(
        "LabelId1",
        UserIdTestData.userId,
        LabelType.ContactGroup,
        "Label 1"
    )
    private val labels = listOf(label)
    private val observeLabels = mockk<ObserveLabels> {
        every { this@mockk.invoke(UserIdTestData.userId, LabelType.ContactGroup) } returns flowOf(
            Either.Right(
                labels
            )
        )
    }

    private val contact = ContactTestData.buildContactWith(
        contactEmails = listOf(
            ContactEmail(
                UserIdTestData.userId,
                ContactEmailId("contact email id 1"),
                "First name from contact email",
                "test1+alias@protonmail.com",
                0,
                0,
                ContactIdTestData.contactId1,
                "test1@protonmail.com",
                listOf("LabelId1"),
                true,
                lastUsedTime = 0
            ),
            ContactEmail(
                UserIdTestData.userId,
                ContactEmailId("contact email id 2"),
                "First name from contact email",
                "test2+alias@protonmail.com",
                0,
                0,
                ContactIdTestData.contactId1,
                "test2@protonmail.com",
                emptyList(),
                true,
                lastUsedTime = 0
            )
        )
    )
    private val contacts = listOf(contact)
    private val contactRepository = mockk<ContactRepository> {
        every { this@mockk.observeAllContacts(UserIdTestData.userId) } returns flowOf(
            DataResult.Success(
                ResponseSource.Remote,
                contacts
            )
        )
    }

    private val contactGroup = ContactGroup(
        UserIdTestData.userId,
        label.labelId,
        label.name,
        label.color,
        listOf(contact.contactEmails[0])
    )

    private val observeContactGroup = ObserveContactGroup(observeLabels, contactRepository)

    @Test
    fun `when repository returns labels and contacts they are successfully mapped and emitted`() = runTest {
        // When
        observeContactGroup(UserIdTestData.userId, LabelId("LabelId1")).test {
            // Then
            val actual = assertIs<Either.Right<ContactGroup>>(awaitItem())
            assertEquals(contactGroup, actual.value)
            awaitComplete()
        }
    }

    @Test
    fun `when label repository returns any data error then emit get contact groups error`() = runTest {
        // Given
        every { observeLabels(UserIdTestData.userId, LabelType.ContactGroup) } returns flowOf(
            DataError.Remote.Http(NetworkError.Unauthorized, "").left()
        )
        // When
        observeContactGroup(UserIdTestData.userId, LabelId("LabelId1")).test {
            // Then
            assertIs<Either.Left<GetContactGroupError.GetLabelsError>>(awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `when contact repository returns any data error then emit get contact groups error`() = runTest {
        // Given
        every { contactRepository.observeAllContacts(UserIdTestData.userId) } returns flowOf(
            DataResult.Error.Remote(message = "Unauthorised", cause = null, httpCode = 401)
        )
        // When
        observeContactGroup(UserIdTestData.userId, LabelId("LabelId1")).test {
            // Then
            assertIs<Either.Left<GetContactGroupError.GetContactsError>>(awaitItem())
            awaitComplete()
        }
    }
}
