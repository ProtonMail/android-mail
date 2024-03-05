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

package ch.protonmail.android.mailcontact.data.local

import ch.protonmail.android.mailcommon.domain.sample.LabelSample
import ch.protonmail.android.testdata.contact.ContactEmailSample
import ch.protonmail.android.testdata.contact.ContactIdTestData
import ch.protonmail.android.testdata.contact.ContactTestData.buildContactWith
import ch.protonmail.android.testdata.user.UserIdTestData
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.contact.domain.entity.ContactEmail
import me.proton.core.contact.domain.repository.ContactLocalDataSource
import kotlin.test.Test

@Suppress("MaxLineLength")
class ContactGroupLocalDataSourceImplTest {

    private val contactLocalDataSource = mockk<ContactLocalDataSource>(relaxUnitFun = true) {
        every { this@mockk.observeAllContacts(UserIdTestData.userId) } returns flowOf(
            listOf(
                buildContactWith(
                    UserIdTestData.userId,
                    ContactIdTestData.contactId1,
                    listOf(
                        ContactEmailSample.contactEmail1
                    ),
                    "contact with label: LabelCoworkers"
                )
            )
        )
    }

    private val contactGroupLocalDataSourceImpl by lazy { ContactGroupLocalDataSourceImpl(contactLocalDataSource) }

    private val userId = UserIdTestData.userId
    private val contactEmailIds = setOf(
        ContactEmailSample.contactEmail1.id
    )

    @Test
    fun `addContactEmailIdsToContactGroup should not call local repository if Contact is already in a Group`() =
        runTest {
            // Given
            val assignedContactGroupWhichIsADuplicate = LabelSample.GroupCoworkers.labelId

            val expectedContactEmails = emptyList<ContactEmail>()

            // When
            contactGroupLocalDataSourceImpl.addContactEmailIdsToContactGroup(
                userId,
                assignedContactGroupWhichIsADuplicate,
                contactEmailIds
            )

            // Then
            coVerify(exactly = 0) { contactLocalDataSource.upsertContactEmails(any()) }
        }

    @Test
    fun `addContactEmailIdsToContactGroup should call local repository if Contact is not in a newly added Group`() =
        runTest {
            // Given
            val assignedContactGroupWhichIsNew = LabelSample.GroupFriends.labelId

            val expectedContactEmails = listOf(
                ContactEmailSample.contactEmail1.copy(
                    labelIds = ContactEmailSample.contactEmail1.labelIds.plus(assignedContactGroupWhichIsNew.id)
                )
            )

            // When
            contactGroupLocalDataSourceImpl.addContactEmailIdsToContactGroup(
                userId,
                assignedContactGroupWhichIsNew,
                contactEmailIds
            )

            // Then
            coVerify(exactly = 1) { contactLocalDataSource.upsertContactEmails(*expectedContactEmails.toTypedArray()) }
        }

    @Test
    fun `removeContactEmailIdsFromContactGroup should not call local repository if Contact is not in a newly removed Group`() =
        runTest {
            // Given
            val removedContactGroupButContactIsNotInIt = LabelSample.GroupFriends.labelId

            val expectedContactEmails = emptyList<ContactEmail>()

            // When
            contactGroupLocalDataSourceImpl.removeContactEmailIdsFromContactGroup(
                userId,
                removedContactGroupButContactIsNotInIt,
                contactEmailIds
            )

            // Then
            coVerify(exactly = 0) { contactLocalDataSource.upsertContactEmails(any()) }
        }

    @Test
    fun `removeContactEmailIdsFromContactGroup should call local repository if Contact is in a newly removed Group`() =
        runTest {
            // Given
            val removedContactGroup = LabelSample.GroupCoworkers.labelId

            val expectedContactEmails = listOf(
                ContactEmailSample.contactEmail1.copy(
                    labelIds = ContactEmailSample.contactEmail1.labelIds.minus(removedContactGroup.id)
                )
            )

            // When
            contactGroupLocalDataSourceImpl.removeContactEmailIdsFromContactGroup(
                userId,
                removedContactGroup,
                contactEmailIds
            )

            // Then
            coVerify(exactly = 1) { contactLocalDataSource.upsertContactEmails(*expectedContactEmails.toTypedArray()) }
        }
}
