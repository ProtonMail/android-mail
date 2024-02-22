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

    fun contactFormSampleData() = ContactFormUiModel(
        id = ContactId("Id"),
        avatar = ContactFormAvatar.Empty,
        displayName = "displayName",
        firstName = "firstName",
        lastName = "lastName",
        emails = mutableListOf(
            InputField.SingleTyped("1", "email@proton.me", FieldType.EmailType.Email),
            InputField.SingleTyped("2", "email2@proton.me", FieldType.EmailType.Other)
        ),
        telephones = mutableListOf(
            InputField.SingleTyped("3", "0123456789", FieldType.TelephoneType.Telephone),
            InputField.SingleTyped("4", "9876543210", FieldType.TelephoneType.Other)
        ),
        addresses = mutableListOf(
            InputField.Address(
                "5",
                "Street",
                "Postal Code",
                "City",
                "Region",
                "Country",
                FieldType.AddressType.Address
            ),
            InputField.Address(
                "6",
                "Street 2",
                "Postal Code 2",
                "City 2",
                "Region 2",
                "Country 2",
                FieldType.AddressType.Home
            )
        ),
        birthday = InputField.Birthday("7", LocalDate.now()),
        notes = mutableListOf(
            InputField.Note("8", "Notes"),
            InputField.Note("9", "Notes 2")
        ),
        others = mutableListOf(
            InputField.SingleTyped("10", "Title", FieldType.OtherType.Title),
            InputField.SingleTyped("11", "Organization", FieldType.OtherType.Organization),
            InputField.SingleTyped("12", "Language", FieldType.OtherType.Language)
        ),
        otherTypes = FieldType.OtherType.values().toList(),
        incrementalUniqueFieldId = 12
    )
}
