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
import arrow.core.getOrElse
import ch.protonmail.android.mailcommon.presentation.mapper.ColorMapper
import ch.protonmail.android.mailcontact.domain.model.ContactGroup
import javax.inject.Inject

class ContactSearchUiModelMapper @Inject constructor(
    private val colorMapper: ColorMapper
) {

    fun contactGroupsToContactSearchUiModelList(contactGroups: List<ContactGroup>): List<ContactGroupItemUiModel> {
        return contactGroups.map { contactGroup ->
            ContactGroupItemUiModel(
                contactGroup.labelId,
                contactGroup.name,
                contactGroup.members.size,
                colorMapper.toColor(contactGroup.color).getOrElse { Color.Black }
            )
        }
    }
}

