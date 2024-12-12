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

package ch.protonmail.android.mailcomposer.presentation.usecase

import ch.protonmail.android.mailcommon.domain.sample.LabelIdSample
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailcommon.presentation.usecase.GetInitials
import ch.protonmail.android.mailcomposer.presentation.model.ContactSuggestionUiModel
import ch.protonmail.android.mailcontact.domain.model.ContactGroup
import ch.protonmail.android.mailcontact.domain.model.DeviceContact
import ch.protonmail.android.testdata.contact.ContactEmailSample
import ch.protonmail.android.testdata.contact.ContactEmailSample.contactEmailLastUsedLongTimeAgo
import ch.protonmail.android.testdata.contact.ContactEmailSample.contactEmailLastUsedRecently
import ch.protonmail.android.testdata.contact.ContactSample
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class SortContactsForSuggestionsTest {

    private val getInitials = mockk<GetInitials>()
    private val sut = SortContactsForSuggestions(getInitials)

    @Before
    fun mockInitials() {
        every { getInitials(any()) } returns BaseInitials
    }

    @Test
    fun `should return correctly sorted UI models`() = runTest {
        // Given
        val contacts = listOf(
            ContactSample.Stefano,
            ContactSample.Doe.copy(
                contactEmails = listOf(
                    contactEmailLastUsedLongTimeAgo,
                    contactEmailLastUsedRecently
                )
            )
        )
        val deviceContacts = listOf(
            DeviceContact(
                "device contact 2",
                "device@email 2"
            ),
            DeviceContact(
                "device contact 1",
                "device@email 1"
            )
        )
        val contactGroups = listOf(
            ContactGroup(
                UserIdSample.Primary,
                LabelIdSample.LabelCoworkers,
                "z group",
                "#AABBCC",
                listOf(ContactEmailSample.contactEmail1)
            ),
            ContactGroup(
                UserIdSample.Primary,
                LabelIdSample.LabelCoworkers,
                "x group",
                "#AABBCC",
                listOf(ContactEmailSample.contactEmail1)
            ),
            ContactGroup(
                UserIdSample.Primary,
                LabelIdSample.LabelCoworkers,
                "a group",
                "#AABBCC",
                listOf(ContactEmailSample.contactEmail1)
            )
        )

        // When
        val actual = sut(
            contacts,
            deviceContacts,
            contactGroups,
            7 // one fewer than total
        )

        // Then
        val expected = listOf(
            ContactSuggestionUiModel.Contact(
                name = contacts[1].contactEmails[1].name,
                initial = BaseInitials,
                email = contacts[1].contactEmails[1].email
            ),
            ContactSuggestionUiModel.Contact(
                name = contacts[1].contactEmails[0].name,
                initial = BaseInitials,
                email = contacts[1].contactEmails[0].email
            ),
            ContactSuggestionUiModel.Contact(
                name = contacts[0].contactEmails[0].name,
                initial = BaseInitials,
                email = contacts[0].contactEmails[0].email
            ),
            ContactSuggestionUiModel.ContactGroup(
                name = contactGroups[2].name,
                emails = contactGroups[2].members.map { it.email },
                color = "#AABBCC"
            ),
            ContactSuggestionUiModel.Contact(
                name = deviceContacts[1].name,
                initial = BaseInitials,
                email = deviceContacts[1].email
            ),
            ContactSuggestionUiModel.Contact(
                name = deviceContacts[0].name,
                initial = BaseInitials,
                email = deviceContacts[0].email
            ),
            ContactSuggestionUiModel.ContactGroup(
                contactGroups[1].name,
                contactGroups[1].members.map { it.email },
                color = "#AABBCC"
            )
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `should remove duplicates when email exists both in Proton and device contacts`() = runTest {
        // Given
        val firstEmail = ContactEmailSample.contactEmail1.copy(email = "email1@proton.me")
        val secondEmail = ContactEmailSample.contactEmail1.copy(email = "email2@proton.me")
        val thirdEmail = ContactEmailSample.contactEmail1.copy(email = "email3@proton.me")
        val fourthEmail = ContactEmailSample.contactEmail1.copy(email = "email4@proton.me")
        val fifthEmail = "email5@proton.me"

        val contacts = listOf(
            ContactSample.Doe.copy(contactEmails = listOf(firstEmail, secondEmail)),
            ContactSample.Doe.copy(contactEmails = listOf(thirdEmail))
        )
        val deviceContacts = listOf(
            DeviceContact("First Email equivalent", firstEmail.email),
            DeviceContact("Second Email equivalent", secondEmail.email),
            DeviceContact("New contact", fourthEmail.email)
        )

        val groupsSuggestions = listOf(
            ContactGroup(
                UserIdSample.Primary,
                LabelIdSample.LabelCoworkers,
                "A group",
                "#AABBCC",
                listOf(ContactEmailSample.contactEmail1.copy(email = fifthEmail))
            )
        )

        val expectedSuggestionsResult = listOf(
            ContactSuggestionUiModel.Contact(
                name = contacts[0].contactEmails[0].name,
                initial = BaseInitials,
                email = contacts[0].contactEmails[0].email
            ),
            ContactSuggestionUiModel.Contact(
                name = contacts[0].contactEmails[1].name,
                initial = BaseInitials,
                email = contacts[0].contactEmails[1].email
            ),
            ContactSuggestionUiModel.Contact(
                name = contacts[1].contactEmails[0].name,
                initial = BaseInitials,
                email = contacts[1].contactEmails[0].email
            ),
            ContactSuggestionUiModel.ContactGroup(
                name = groupsSuggestions[0].name,
                emails = groupsSuggestions[0].members.map { it.email },
                color = "#AABBCC"
            ),
            ContactSuggestionUiModel.Contact(
                name = deviceContacts[2].name,
                initial = BaseInitials,
                email = deviceContacts[2].email
            )
        )

        // When
        val actual = sut(
            contacts,
            deviceContacts,
            groupsSuggestions,
            50
        )

        // Then
        assertEquals(actual, expectedSuggestionsResult)
    }

    @Test
    fun `should not remove duplicates from contact groups when email exists both in group and device contacts`() =
        runTest {
            // Given
            val firstEmail = ContactEmailSample.contactEmail1.copy(email = "email1@proton.me")
            val secondEmail = ContactEmailSample.contactEmail1.copy(email = "email2@proton.me")
            val thirdEmail = ContactEmailSample.contactEmail1.copy(email = "email3@proton.me")
            val fourthEmail = ContactEmailSample.contactEmail1.copy(email = "email4@proton.me")

            val contacts = listOf(
                ContactSample.Doe.copy(contactEmails = listOf(firstEmail, secondEmail)),
                ContactSample.Doe.copy(contactEmails = listOf(thirdEmail))
            )
            val deviceContacts = listOf(
                DeviceContact("First Email equivalent", firstEmail.email),
                DeviceContact("Second Email equivalent", secondEmail.email),
                DeviceContact("New contact", fourthEmail.email)
            )

            val groupsSuggestions = listOf(
                ContactGroup(
                    UserIdSample.Primary,
                    LabelIdSample.LabelCoworkers,
                    "A group",
                    "#AABBCC",
                    listOf(ContactEmailSample.contactEmail1.copy(email = firstEmail.email))
                )
            )

            val expectedSuggestionsResult = listOf(
                ContactSuggestionUiModel.Contact(
                    name = contacts[0].contactEmails[0].name,
                    initial = BaseInitials,
                    email = contacts[0].contactEmails[0].email
                ),
                ContactSuggestionUiModel.Contact(
                    name = contacts[0].contactEmails[1].name,
                    initial = BaseInitials,
                    email = contacts[0].contactEmails[1].email
                ),
                ContactSuggestionUiModel.Contact(
                    name = contacts[1].contactEmails[0].name,
                    initial = BaseInitials,
                    email = contacts[1].contactEmails[0].email
                ),
                ContactSuggestionUiModel.ContactGroup(
                    groupsSuggestions[0].name,
                    groupsSuggestions[0].members.map { it.email },
                    color = "#AABBCC"
                ),
                ContactSuggestionUiModel.Contact(
                    name = deviceContacts[2].name,
                    initial = BaseInitials,
                    email = deviceContacts[2].email
                )
            )

            // When
            val actual = sut(
                contacts,
                deviceContacts,
                groupsSuggestions,
                50
            )

            // Then
            assertEquals(actual, expectedSuggestionsResult)
        }

    private companion object {

        const val BaseInitials = "AB"
    }
}
