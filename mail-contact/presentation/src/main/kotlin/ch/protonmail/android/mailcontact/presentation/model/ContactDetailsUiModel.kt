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
    val defaultPhoneNumber: String = "",
    val defaultEmail: String = "",
    val nameHeader: String,
    val nameSubText: String = "",
    val avatar: Avatar,
    val contactMainDetailsItemList: List<ContactDetailsItem> = emptyList(),
    val contactOtherDetailsItemList: List<ContactDetailsItem> = emptyList(),
    val contactGroups: ContactDetailsGroupsItem
) {
    fun isCallActionEnabled() = defaultPhoneNumber.isNotEmpty()
    fun isEmailActionEnabled() = defaultEmail.isNotEmpty()
}

sealed interface ContactDetailsItem {

    sealed interface ContactDetailType {
        sealed interface Triggerable : ContactDetailType {
            data class Phone(val phoneNumber: String) : Triggerable
            data class Email(val email: String) : Triggerable
        }
        object Undefined : ContactDetailType
    }

    val displayIcon: Boolean
    val iconResId: Int
    val header: TextUiModel

    data class Text(
        override val displayIcon: Boolean,
        override val iconResId: Int,
        override val header: TextUiModel,
        val value: TextUiModel,
        val type: ContactDetailType = ContactDetailType.Undefined
    ) : ContactDetailsItem

    data class Image(
        override val displayIcon: Boolean,
        override val iconResId: Int,
        override val header: TextUiModel,
        val value: Bitmap
    ) : ContactDetailsItem
}

data class ContactDetailsGroupsItem(
    val displayGroupSection: Boolean,
    val iconResId: Int,
    val groupLabelList: List<ContactDetailsGroupLabel> = emptyList()
)

data class ContactDetailsGroupLabel(
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
