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

package ch.protonmail.android.mailcontact.presentation.model

import ch.protonmail.android.maillabel.presentation.getColorFromHexString
import me.proton.core.contact.domain.entity.Contact
import me.proton.core.label.domain.entity.Label
import javax.inject.Inject

class ContactGroupItemUiModelMapper @Inject constructor() {

    fun toContactGroupItemUiModel(contactList: List<Contact>, labels: List<Label>): List<ContactGroupItemUiModel> {
        val labelMembersCount = hashMapOf<String, Int>()
        contactList.forEach { contact ->
            contact.contactEmails.forEach { contactEmail ->
                contactEmail.labelIds.forEach { labelId ->
                    labelMembersCount[labelId] = labelMembersCount[labelId]?.plus(1) ?: 1
                }
            }
        }
        val contactGroups = labels.map { label ->
            ContactGroupItemUiModel(
                labelId = label.labelId,
                name = label.name,
                memberCount = labelMembersCount[label.labelId.id] ?: 0,
                color = label.color.getColorFromHexString()
            )
        }
        return contactGroups
    }
}
