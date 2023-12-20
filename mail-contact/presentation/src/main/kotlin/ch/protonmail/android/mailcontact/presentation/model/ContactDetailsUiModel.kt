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

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import me.proton.core.contact.domain.entity.ContactId

data class ContactDetailsUiModel(
    val id: ContactId,
    val displayName: String,
    val firstName: String = "",
    val lastName: String = "",
    val avatar: Avatar,
    val contactMainDetailsItemList: List<ContactDetailsItem> = emptyList(),
    val contactOtherDetailsItemList: List<ContactDetailsItem> = emptyList(),
    val contactGroups: ContactDetailsGroupsItem
)

data class ContactDetailsItem(
    val displayIcon: Boolean,
    val iconResId: Int,
    val header: TextUiModel,
    val value: TextUiModel
)

data class ContactDetailsGroupsItem(
    val iconResId: Int,
    val groupLabelList: List<ContactGroupLabel> = emptyList()
)

data class ContactGroupLabel(
    val name: String,
    val color: Color
)

sealed interface Avatar {
    data class Initials(
        val value: String
    ) : Avatar

    data class Photo(
        val bitmap: Bitmap
    ) : Avatar
}
