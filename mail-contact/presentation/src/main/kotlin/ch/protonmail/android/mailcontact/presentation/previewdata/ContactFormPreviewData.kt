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
        emails = mutableListOf(
            InputField.SingleTyped("email@proton.me", FieldType.EmailType.Email),
            InputField.SingleTyped("email2@proton.me", FieldType.EmailType.Other)
        ),
        telephones = mutableListOf(
            InputField.SingleTyped("0123456789", FieldType.TelephoneType.Telephone),
            InputField.SingleTyped("9876543210", FieldType.TelephoneType.Other)
        ),
        addresses = mutableListOf(
            InputField.Address(
                "Street",
                "Postal Code",
                "City",
                "Region",
                "Country",
                FieldType.AddressType.Address
            ),
            InputField.Address(
                "Street 2",
                "Postal Code 2",
                "City 2",
                "Region 2",
                "Country 2",
                FieldType.AddressType.Home
            )
        ),
        birthday = InputField.Birthday(LocalDate.now()),
        notes = mutableListOf(
            InputField.Note("Notes"),
            InputField.Note("Notes 2")
        ),
        others = mutableListOf(
            InputField.SingleTyped("Title", FieldType.OtherType.Title),
            InputField.SingleTyped("Organization", FieldType.OtherType.Organization),
            InputField.SingleTyped("Language", FieldType.OtherType.Language)
        ),
        otherTypes = FieldType.OtherType.values().toList()
    )
}
