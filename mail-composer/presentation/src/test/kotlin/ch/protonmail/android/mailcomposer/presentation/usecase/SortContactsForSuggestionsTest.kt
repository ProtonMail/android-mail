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
import ch.protonmail.android.mailcomposer.presentation.model.ContactSuggestionUiModel
import ch.protonmail.android.mailcontact.domain.model.ContactGroup
import ch.protonmail.android.mailcontact.domain.model.DeviceContact
import ch.protonmail.android.testdata.contact.ContactEmailSample
import ch.protonmail.android.testdata.contact.ContactEmailSample.contactEmailLastUsedLongTimeAgo
import ch.protonmail.android.testdata.contact.ContactEmailSample.contactEmailLastUsedRecently
import ch.protonmail.android.testdata.contact.ContactSample
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

class SortContactsForSuggestionsTest {

    private val sut = SortContactsForSuggestions()

    @Test
    fun `returns correctly sorted UiModels`() = runTest {
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
                contacts[1].contactEmails[1].name,
                contacts[1].contactEmails[1].email
            ),
            ContactSuggestionUiModel.Contact(
                contacts[1].contactEmails[0].name,
                contacts[1].contactEmails[0].email
            ),
            ContactSuggestionUiModel.Contact(
                contacts[0].contactEmails[0].name,
                contacts[0].contactEmails[0].email
            ),
            ContactSuggestionUiModel.ContactGroup(
                contactGroups[2].name,
                contactGroups[2].members.map { it.email }
            ),
            ContactSuggestionUiModel.Contact(
                deviceContacts[1].name,
                deviceContacts[1].email
            ),
            ContactSuggestionUiModel.Contact(
                deviceContacts[0].name,
                deviceContacts[0].email
            ),
            ContactSuggestionUiModel.ContactGroup(
                contactGroups[1].name,
                contactGroups[1].members.map { it.email }
            )
        )

        assertEquals(expected, actual)
    }

}
