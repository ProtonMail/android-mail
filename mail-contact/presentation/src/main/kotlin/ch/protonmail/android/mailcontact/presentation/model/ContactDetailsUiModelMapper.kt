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

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import android.graphics.BitmapFactory
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcontact.domain.model.ContactProperty
import ch.protonmail.android.mailcontact.domain.model.DecryptedContact
import ch.protonmail.android.mailcontact.presentation.R
import ch.protonmail.android.mailcontact.presentation.utils.getInitials
import me.proton.core.contact.domain.entity.ContactId

fun DecryptedContact.toContactDetailsUiModel(): ContactDetailsUiModel {
    val displayName = this.formattedName?.value ?: ""
    val firstName = this.structuredName?.given ?: ""
    val lastName = this.structuredName?.family ?: ""
    return ContactDetailsUiModel(
        id = this.id,
        displayName = displayName,
        firstName = firstName,
        lastName = lastName,
        avatar = getAvatar(this),
        contactMainDetailsItemList = getContactMainDetailsItemList(this),
        contactOtherDetailsItemList = getContactOtherDetailsItemList(this),
        contactGroups = ContactDetailsGroupsItem(
            iconResId = R.drawable.ic_proton_users,
            groupLabelList = emptyList() // TODO
        )
    )
}

private fun getAvatar(contact: DecryptedContact): Avatar {
    return if (contact.photos.isNotEmpty()) {
        val byteArray = contact.photos.first().data
        Avatar.Photo(
            bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
        )
    } else {
        Avatar.Initials(
            value = getInitials(contact.formattedName?.value ?: "")
        )
    }
}

@SuppressWarnings("LongMethod", "ComplexMethod")
private fun getContactMainDetailsItemList(contact: DecryptedContact): List<ContactDetailsItem> {
    val contactDetailsItem = arrayListOf<ContactDetailsItem>()
    contact.emails.forEachIndexed { index, email ->
        contactDetailsItem.add(
            ContactDetailsItem(
                displayIcon = index == 0,
                iconResId = R.drawable.ic_proton_at,
                header = TextUiModel(
                    when (email.type) {
                        ContactProperty.Email.Type.Email -> R.string.contact_type_email
                        ContactProperty.Email.Type.Home -> R.string.contact_type_home
                        ContactProperty.Email.Type.Work -> R.string.contact_type_work
                        ContactProperty.Email.Type.Other -> R.string.contact_type_other
                    }
                ),
                value = TextUiModel(email.value)
            )
        )
    }
    contact.telephones.forEachIndexed { index, phone ->
        contactDetailsItem.add(
            ContactDetailsItem(
                displayIcon = index == 0,
                iconResId = R.drawable.ic_proton_phone,
                header = TextUiModel(
                    when (phone.type) {
                        ContactProperty.Telephone.Type.Telephone -> R.string.contact_type_phone
                        ContactProperty.Telephone.Type.Home -> R.string.contact_type_home
                        ContactProperty.Telephone.Type.Work -> R.string.contact_type_work
                        ContactProperty.Telephone.Type.Other -> R.string.contact_type_other
                        ContactProperty.Telephone.Type.Mobile -> R.string.contact_type_mobile
                        ContactProperty.Telephone.Type.Main -> R.string.contact_type_main
                        ContactProperty.Telephone.Type.Fax -> R.string.contact_type_fax
                        ContactProperty.Telephone.Type.Pager -> R.string.contact_type_pager
                    }
                ),
                value = TextUiModel(phone.text)
            )
        )
    }
    contact.addresses.forEachIndexed { index, address ->
        contactDetailsItem.add(
            ContactDetailsItem(
                displayIcon = index == 0,
                iconResId = R.drawable.ic_proton_map_pin,
                header = TextUiModel(
                    when (address.type) {
                        ContactProperty.Address.Type.Address -> R.string.contact_type_address
                        ContactProperty.Address.Type.Home -> R.string.contact_type_home
                        ContactProperty.Address.Type.Work -> R.string.contact_type_work
                        ContactProperty.Address.Type.Other -> R.string.contact_type_other
                    }
                ),
                value = TextUiModel(
                    R.string.contact_details_address,
                    address.streetAddress,
                    address.postalCode,
                    address.locality,
                    address.region,
                    address.country
                )
            )
        )
    }
    contact.birthday?.let { birthday ->
        contactDetailsItem.add(
            ContactDetailsItem(
                displayIcon = true,
                iconResId = R.drawable.ic_proton_calendar_day,
                header = TextUiModel(R.string.contact_property_birthday),
                value = TextUiModel(
                    formatLocalDate(birthday.date)
                )
            )
        )
    }
    contact.notes.forEachIndexed { index, note ->
        contactDetailsItem.add(
            ContactDetailsItem(
                displayIcon = index == 0,
                iconResId = R.drawable.ic_proton_note,
                header = TextUiModel(R.string.contact_property_note),
                value = TextUiModel(note.value)
            )
        )
    }
    return contactDetailsItem
}

@SuppressWarnings("LongMethod", "ComplexMethod")
private fun getContactOtherDetailsItemList(contact: DecryptedContact): List<ContactDetailsItem> {
    val contactDetailsItem = arrayListOf<ContactDetailsItem>()
    contact.photos.forEach { photo ->
        // TODO
    }
    contact.organizations.forEach { organization ->
        contactDetailsItem.add(
            ContactDetailsItem(
                displayIcon = contactDetailsItem.isEmpty(),
                iconResId = R.drawable.ic_proton_text_align_left,
                header = TextUiModel(R.string.contact_property_organization),
                value = TextUiModel(organization.value)
            )
        )
    }
    contact.titles.forEach { title ->
        contactDetailsItem.add(
            ContactDetailsItem(
                displayIcon = contactDetailsItem.isEmpty(),
                iconResId = R.drawable.ic_proton_text_align_left,
                header = TextUiModel(R.string.contact_property_title),
                value = TextUiModel(title.value)
            )
        )
    }
    contact.roles.forEach { role ->
        contactDetailsItem.add(
            ContactDetailsItem(
                displayIcon = contactDetailsItem.isEmpty(),
                iconResId = R.drawable.ic_proton_text_align_left,
                header = TextUiModel(R.string.contact_property_role),
                value = TextUiModel(role.value)
            )
        )
    }
    contact.timezones.forEach { timezone ->
        contactDetailsItem.add(
            ContactDetailsItem(
                displayIcon = contactDetailsItem.isEmpty(),
                iconResId = R.drawable.ic_proton_text_align_left,
                header = TextUiModel(R.string.contact_property_time_zone),
                value = TextUiModel(timezone.text)
            )
        )
    }
    contact.logos.forEach { _ ->
        // TODO
    }
    contact.members.forEach { member ->
        contactDetailsItem.add(
            ContactDetailsItem(
                displayIcon = contactDetailsItem.isEmpty(),
                iconResId = R.drawable.ic_proton_text_align_left,
                header = TextUiModel(R.string.contact_property_member),
                value = TextUiModel(member.value)
            )
        )
    }
    contact.languages.forEach { language ->
        contactDetailsItem.add(
            ContactDetailsItem(
                displayIcon = contactDetailsItem.isEmpty(),
                iconResId = R.drawable.ic_proton_text_align_left,
                header = TextUiModel(R.string.contact_property_language),
                value = TextUiModel(language.value)
            )
        )
    }
    contact.urls.forEach { url ->
        contactDetailsItem.add(
            ContactDetailsItem(
                displayIcon = contactDetailsItem.isEmpty(),
                iconResId = R.drawable.ic_proton_text_align_left,
                header = TextUiModel(R.string.contact_property_url),
                value = TextUiModel(url.value)
            )
        )
    }
    contact.gender?.let { gender ->
        contactDetailsItem.add(
            ContactDetailsItem(
                displayIcon = contactDetailsItem.isEmpty(),
                iconResId = R.drawable.ic_proton_text_align_left,
                header = TextUiModel(R.string.contact_property_gender),
                value = TextUiModel(gender.gender)
            )
        )
    }
    contact.anniversary?.let { anniversary ->
        contactDetailsItem.add(
            ContactDetailsItem(
                displayIcon = contactDetailsItem.isEmpty(),
                iconResId = R.drawable.ic_proton_text_align_left,
                header = TextUiModel(R.string.contact_property_anniversary),
                value = TextUiModel(
                    formatLocalDate(anniversary.date)
                )
            )
        )
    }
    return contactDetailsItem
}

private fun formatLocalDate(date: LocalDate): String {
    return date.format(
        DateTimeFormatter.ofLocalizedDate(
            FormatStyle.MEDIUM
        ).withLocale(
            Locale.getDefault()
        )
    )
}
