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

package ch.protonmail.android.mailcontact.presentation

import java.time.LocalDate
import androidx.compose.ui.graphics.Color
import ch.protonmail.android.mailcontact.domain.model.ContactGroupLabel
import ch.protonmail.android.mailcontact.domain.model.ContactProperty
import ch.protonmail.android.mailcontact.domain.model.DecryptedContact
import ch.protonmail.android.maillabel.presentation.getHexStringFromColor
import ch.protonmail.android.testdata.contact.ContactSample

object ContactDetailsTestData {

    val photoByteArray = ContactImagesSample.Photo
    val logoByteArray = ContactImagesSample.Logo

    val regularContact = DecryptedContact(
        id = ContactSample.Mario.id,
        contactGroupLabels = listOf(
            ContactGroupLabel(
                "Group 1",
                Color.Red.getHexStringFromColor()
            )
        ),
        structuredName = ContactProperty.StructuredName(
            family = "Last", given = "First"
        ),
        formattedName = ContactProperty.FormattedName(value = "Mario@protonmail.com"),
        emails = listOf(
            ContactProperty.Email(type = ContactProperty.Email.Type.Email, value = "Mario@protonmail.com"),
            ContactProperty.Email(
                type = ContactProperty.Email.Type.Home,
                value = "home_email@Mario.protonmail.com"
            ),
            ContactProperty.Email(
                type = ContactProperty.Email.Type.Work,
                value = "work_email@Mario.protonmail.com"
            ),
            ContactProperty.Email(
                type = ContactProperty.Email.Type.Other,
                value = "other_email@Mario.protonmail.com"
            )
        ),
        telephones = listOf(
            ContactProperty.Telephone(type = ContactProperty.Telephone.Type.Telephone, text = "1231231235"),
            ContactProperty.Telephone(
                type = ContactProperty.Telephone.Type.Home,
                text = "23233232323"
            ),
            ContactProperty.Telephone(
                type = ContactProperty.Telephone.Type.Work,
                text = "45454545"
            ),
            ContactProperty.Telephone(type = ContactProperty.Telephone.Type.Other, text = "565656"),
            ContactProperty.Telephone(
                type = ContactProperty.Telephone.Type.Mobile,
                text = "676767"
            ),
            ContactProperty.Telephone(type = ContactProperty.Telephone.Type.Main, text = "787887"),
            ContactProperty.Telephone(
                type = ContactProperty.Telephone.Type.Fax,
                text = "898989"
            ),
            ContactProperty.Telephone(type = ContactProperty.Telephone.Type.Pager, text = "90909090")
        ),
        addresses = listOf(
            ContactProperty.Address(
                type = ContactProperty.Address.Type.Address,
                streetAddress = "Address Street1",
                locality = "City",
                region = "Region",
                postalCode = "123",
                country = "Country"
            ),
            ContactProperty.Address(
                type = ContactProperty.Address.Type.Other,
                streetAddress = "Address Other1",
                locality = "City",
                region = "Region",
                postalCode = "234",
                country = "Country"
            ),
            ContactProperty.Address(
                type = ContactProperty.Address.Type.Home,
                streetAddress = "Home address the rest is empty",
                locality = "",
                region = "",
                postalCode = "",
                country = ""
            ),
            ContactProperty.Address(
                type = ContactProperty.Address.Type.Work,
                streetAddress = "City the rest is empty",
                locality = "",
                region = "",
                postalCode = "",
                country = ""
            )
        ),
        birthday = ContactProperty.Birthday(date = LocalDate.of(2023, 12, 14)),
        notes = listOf(ContactProperty.Note(value = "Note1"), ContactProperty.Note(value = "Note2")),
        photos = listOf(
            ContactProperty.Photo(
                data = photoByteArray,
                contentType = "jpeg",
                mediaType = null,
                extension = null
            )
        ),
        organizations = listOf(
            ContactProperty.Organization(value = "Organization1"),
            ContactProperty.Organization(value = "Organization2")
        ),
        titles = listOf(ContactProperty.Title(value = "Title")),
        roles = listOf(ContactProperty.Role(value = "Role")),
        timezones = listOf(ContactProperty.Timezone(text = "Europe/Paris")),
        logos = listOf(
            ContactProperty.Logo(
                data = logoByteArray,
                contentType = "jpeg",
                mediaType = null,
                extension = null
            )
        ),
        members = listOf(ContactProperty.Member(value = "Member")),
        languages = listOf(ContactProperty.Language(value = "English")),
        urls = listOf(ContactProperty.Url(value = "http://proton.me")),
        gender = ContactProperty.Gender(gender = "Gender"),
        anniversary = ContactProperty.Anniversary(date = LocalDate.of(2023, 12, 6))
    )
}
