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

import ch.protonmail.android.mailcommon.presentation.usecase.GetInitials
import me.proton.core.contact.domain.entity.Contact
import me.proton.core.contact.domain.entity.ContactEmailId
import javax.inject.Inject

class ManageMembersUiModelMapper @Inject constructor(
    private val getInitials: GetInitials
) {

    fun toManageMembersUiModelList(
        contacts: List<Contact>,
        selectedContactEmailIds: List<ContactEmailId>
    ): List<ManageMembersUiModel> {
        return contacts.flatMap { contact ->
            contact.contactEmails.map { contactEmail ->
                ManageMembersUiModel(
                    id = contactEmail.id,
                    name = contactEmail.name,
                    email = contactEmail.email,
                    initials = getInitials(contactEmail.name),
                    isSelected = selectedContactEmailIds.any { it == contactEmail.id },
                    isDisplayed = true
                )
            }
        }
    }
}
