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

package ch.protonmail.android.mailmessage.domain.usecase

import ch.protonmail.android.mailmessage.domain.entity.Recipient
import ch.protonmail.android.testdata.contact.ContactTestData
import ch.protonmail.android.testdata.user.UserIdTestData
import org.junit.Test
import kotlin.test.assertEquals

class ResolveParticipantNameTest {

    private val resolveParticipantName = ResolveParticipantName()

    @Test
    fun `when a participant exists as contact then contact name is returned`() {
        // Given
        val contact = ContactTestData.buildContactWith(
            userId = UserIdTestData.userId,
            contactEmails = listOf(
                ContactTestData.buildContactEmailWith(
                    name = "contact email name",
                    address = "sender@proton.ch"
                )
            )
        )
        val userContacts = listOf(contact, ContactTestData.contact2)
        val participant = Recipient("sender@proton.ch", "")
        // When
        val actual = resolveParticipantName(participant, userContacts)
        // Then
        val expected = "contact email name"
        assertEquals(expected, actual)
    }

    @Test
    fun `when a participant has no display name defined address is returned`() {
        // Given
        val participant = Recipient("sender@proton.ch", "")
        // When
        val actual = resolveParticipantName(participant, ContactTestData.contacts)
        // Then
        val expected = "sender@proton.ch"
        assertEquals(expected, actual)
    }
}
