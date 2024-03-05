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

import kotlinx.coroutines.flow.firstOrNull
import me.proton.core.contact.domain.entity.ContactEmailId
import me.proton.core.contact.domain.repository.ContactLocalDataSource
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId
import javax.inject.Inject

class ContactGroupLocalDataSourceImpl @Inject constructor(
    private val contactLocalDataSource: ContactLocalDataSource
) : ContactGroupLocalDataSource {

    override suspend fun addContactEmailIdsToContactGroup(
        userId: UserId,
        labelId: LabelId,
        contactEmailIds: Set<ContactEmailId>
    ) {
        val contactEmailsNotInGroup = contactLocalDataSource.observeAllContacts(userId).firstOrNull()?.map { contact ->
            contact.contactEmails.filter { contactEmail ->
                contactEmail.id in contactEmailIds && labelId.id !in contactEmail.labelIds
            }
        }?.flatten() ?: emptyList()

        val contactEmailsWithAddedLabelId = contactEmailsNotInGroup.map {
            it.copy(labelIds = it.labelIds.plus(labelId.id))
        }

        if (contactEmailsWithAddedLabelId.isNotEmpty()) {
            contactLocalDataSource.upsertContactEmails(
                *contactEmailsWithAddedLabelId.toTypedArray()
            )
        }
    }

    override suspend fun removeContactEmailIdsFromContactGroup(
        userId: UserId,
        labelId: LabelId,
        contactEmailIds: Set<ContactEmailId>
    ) {
        val contactEmailsInGroup = contactLocalDataSource.observeAllContacts(userId).firstOrNull()?.map { contact ->
            contact.contactEmails.filter { contactEmail ->
                contactEmail.id in contactEmailIds && labelId.id in contactEmail.labelIds
            }
        }?.flatten() ?: emptyList()

        val contactEmailsWithRemovedLabelId = contactEmailsInGroup.map {
            it.copy(labelIds = it.labelIds.minus(labelId.id))
        }

        if (contactEmailsWithRemovedLabelId.isNotEmpty()) {
            contactLocalDataSource.upsertContactEmails(
                *contactEmailsWithRemovedLabelId.toTypedArray()
            )
        }
    }
}
