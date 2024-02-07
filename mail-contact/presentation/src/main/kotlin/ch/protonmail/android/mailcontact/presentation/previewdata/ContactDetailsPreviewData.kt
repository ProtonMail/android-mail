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

package ch.protonmail.android.mailcontact.presentation.previewdata

import androidx.compose.ui.graphics.Color
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcontact.presentation.R
import ch.protonmail.android.mailcontact.presentation.model.Avatar
import ch.protonmail.android.mailcontact.presentation.model.ContactDetailsGroupsItem
import ch.protonmail.android.mailcontact.presentation.model.ContactDetailsItem
import ch.protonmail.android.mailcontact.presentation.model.ContactDetailsUiModel
import ch.protonmail.android.mailcontact.presentation.model.ContactDetailsGroupLabel
import me.proton.core.contact.domain.entity.ContactId

object ContactDetailsPreviewData {

    val contactDetailsSampleData = ContactDetailsUiModel(
        id = ContactId("Id"),
        nameHeader = "John Doe",
        avatar = Avatar.Initials("JD"),
        contactMainDetailsItemList = listOf(
            ContactDetailsItem.Text(
                displayIcon = true,
                iconResId = R.drawable.ic_proton_at,
                header = TextUiModel(R.string.contact_type_email),
                value = TextUiModel("johndoe@proton.me")
            ),
            ContactDetailsItem.Text(
                displayIcon = false,
                iconResId = R.drawable.ic_proton_at,
                header = TextUiModel(R.string.contact_type_work),
                value = TextUiModel("johndoe2@proton.me")
            ),
            ContactDetailsItem.Text(
                displayIcon = true,
                iconResId = R.drawable.ic_proton_phone,
                header = TextUiModel(R.string.contact_type_phone),
                value = TextUiModel("01234567890")
            ),
            ContactDetailsItem.Text(
                displayIcon = false,
                iconResId = R.drawable.ic_proton_phone,
                header = TextUiModel(R.string.contact_type_pager),
                value = TextUiModel("0987654321")
            ),
            ContactDetailsItem.Text(
                displayIcon = true,
                iconResId = R.drawable.ic_proton_map_pin,
                header = TextUiModel(R.string.contact_type_home),
                value = TextUiModel("Lettensteg 10, 8037 Zürich")
            ),
            ContactDetailsItem.Text(
                displayIcon = false,
                iconResId = R.drawable.ic_proton_map_pin,
                header = TextUiModel(R.string.contact_type_work),
                value = TextUiModel("Hello world, Earth")
            )
        ),
        contactGroups = ContactDetailsGroupsItem(
            displayGroupSection = true,
            iconResId = R.drawable.ic_proton_users,
            groupLabelList = listOf(
                ContactDetailsGroupLabel(
                    name = "Short",
                    color = Color.Red
                ),
                ContactDetailsGroupLabel(
                    name = "Group very with long name",
                    color = Color.Blue
                ),
                ContactDetailsGroupLabel(
                    name = "Medium length",
                    color = Color.Green
                )
            )
        )
    )
}

