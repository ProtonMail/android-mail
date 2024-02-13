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

import androidx.compose.ui.graphics.Color
import ch.protonmail.android.mailcontact.domain.model.ContactGroup
import ch.protonmail.android.mailcontact.presentation.utils.getInitials
import ch.protonmail.android.maillabel.presentation.getColorFromHexString
import javax.inject.Inject

class ContactGroupFormUiModelMapper @Inject constructor() {

    fun toContactGroupFormUiModel(contactGroup: ContactGroup, colors: List<Color>): ContactGroupFormUiModel {
        return ContactGroupFormUiModel(
            id = contactGroup.labelId,
            name = contactGroup.name,
            color = contactGroup.color.getColorFromHexString(),
            colors = colors,
            memberCount = contactGroup.members.size,
            members = contactGroup.members.map { contactEmail ->
                ContactGroupFormMember(
                    id = contactEmail.id,
                    initials = getInitials(contactEmail.name),
                    name = contactEmail.name,
                    email = contactEmail.email
                )
            }
        )
    }
}