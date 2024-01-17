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

import java.time.LocalDate
import ch.protonmail.android.mailcontact.presentation.model.ContactFormAvatar
import ch.protonmail.android.mailcontact.presentation.model.ContactFormUiModel
import ch.protonmail.android.mailcontact.presentation.model.FieldType
import ch.protonmail.android.mailcontact.presentation.model.InputField
import me.proton.core.contact.domain.entity.ContactId

object ContactFormPreviewData {

    val contactFormSampleData = ContactFormUiModel(
        id = ContactId("Id"),
        avatar = ContactFormAvatar.Empty,
        displayName = "displayName",
        firstName = "firstName",
        lastName = "lastName",
        emails = listOf(
            InputField.SingleTyped("email@proton.me", FieldType.EmailType.Email)
        ),
        phones = listOf(
            InputField.SingleTyped("0123456789", FieldType.PhoneType.Phone)
        ),
        addresses = listOf(
            InputField.Address(
                "Street",
                "Postal Code",
                "City",
                "Region",
                "Country",
                FieldType.AddressType.Address
            )
        ),
        birthday = InputField.Date(LocalDate.now()),
        notes = listOf(
            InputField.Note("Notes")
        ),
        others = listOf(
            InputField.SingleTyped("Title", FieldType.OtherType.Title),
            InputField.SingleTyped("Organization", FieldType.OtherType.Organization),
            InputField.SingleTyped("Language", FieldType.OtherType.Language)
        ),
        otherTypes = FieldType.OtherType.values().toList()
    )
}
