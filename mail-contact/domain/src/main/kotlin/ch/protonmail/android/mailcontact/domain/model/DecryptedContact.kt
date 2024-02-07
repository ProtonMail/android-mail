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

package ch.protonmail.android.mailcontact.domain.model

import me.proton.core.contact.domain.entity.ContactId

data class DecryptedContact(
    val id: ContactId?,
    val contactGroupLabels: List<ContactGroupLabel> = emptyList(),
    val structuredName: ContactProperty.StructuredName? = null,
    val formattedName: ContactProperty.FormattedName? = null,
    val emails: List<ContactProperty.Email> = emptyList(),
    val telephones: List<ContactProperty.Telephone> = emptyList(),
    val addresses: List<ContactProperty.Address> = emptyList(),
    val birthday: ContactProperty.Birthday? = null,
    val notes: List<ContactProperty.Note> = emptyList(),
    val photos: List<ContactProperty.Photo> = emptyList(),
    val organizations: List<ContactProperty.Organization> = emptyList(),
    val titles: List<ContactProperty.Title> = emptyList(),
    val roles: List<ContactProperty.Role> = emptyList(),
    val timezones: List<ContactProperty.Timezone> = emptyList(),
    val logos: List<ContactProperty.Logo> = emptyList(),
    val members: List<ContactProperty.Member> = emptyList(),
    val languages: List<ContactProperty.Language> = emptyList(),
    val urls: List<ContactProperty.Url> = emptyList(),
    val gender: ContactProperty.Gender? = null,
    val anniversary: ContactProperty.Anniversary? = null
)
