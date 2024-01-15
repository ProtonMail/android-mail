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

import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcommon.presentation.usecase.DecodeByteArray
import ch.protonmail.android.mailcommon.presentation.usecase.FormatLocalDate
import ch.protonmail.android.mailcontact.domain.model.ContactProperty
import ch.protonmail.android.mailcontact.domain.model.DecryptedContact
import ch.protonmail.android.mailcontact.presentation.R
import ch.protonmail.android.mailcontact.presentation.utils.getInitials
import ch.protonmail.android.maillabel.presentation.getColorFromHexString
import javax.inject.Inject

class ContactDetailsUiModelMapper @Inject constructor(
    private val formatLocalDate: FormatLocalDate,
    private val decodeByteArray: DecodeByteArray
) {

    fun toContactDetailsUiModel(decryptedContact: DecryptedContact): ContactDetailsUiModel {
        val groupLabelList = getGroupLabelList(decryptedContact)
        val defaultPhoneNumber = decryptedContact.telephones.firstOrNull()?.text ?: ""
        return ContactDetailsUiModel(
            id = decryptedContact.id,
            defaultPhoneNumber = defaultPhoneNumber,
            displayName = decryptedContact.formattedName?.value ?: "",
            firstName = decryptedContact.structuredName?.given ?: "",
            lastName = decryptedContact.structuredName?.family ?: "",
            avatar = getAvatar(decryptedContact),
            contactMainDetailsItemList = getContactMainDetailsItemList(decryptedContact),
            contactOtherDetailsItemList = getContactOtherDetailsItemList(decryptedContact),
            contactGroups = ContactDetailsGroupsItem(
                displayGroupSection = groupLabelList.isNotEmpty(),
                iconResId = R.drawable.ic_proton_users,
                groupLabelList = groupLabelList
            )
        )
    }

    private fun getAvatar(contact: DecryptedContact): Avatar {
        if (contact.photos.isNotEmpty()) {
            val byteArray = contact.photos.first().data
            decodeByteArray(byteArray)?.let { bitmap ->
                return Avatar.Photo(
                    bitmap = bitmap
                )
            }
        }

        return Avatar.Initials(
            value = getInitials(contact.formattedName?.value ?: "")
        )
    }

    private fun getGroupLabelList(contact: DecryptedContact): List<ContactGroupLabel> {
        return contact.contactGroups.map {
            ContactGroupLabel(it.name, it.color.getColorFromHexString())
        }
    }

    @SuppressWarnings("LongMethod", "ComplexMethod")
    private fun getContactMainDetailsItemList(contact: DecryptedContact): List<ContactDetailsItem> {
        val contactDetailsItem = arrayListOf<ContactDetailsItem>()
        contact.emails.forEachIndexed { index, email ->
            contactDetailsItem.add(
                ContactDetailsItem.Text(
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
                    value = TextUiModel(email.value),
                    type = ContactDetailsItem.ContactDetailType.Triggerable.Email(email.value)
                )
            )
        }
        contact.telephones.forEachIndexed { index, phone ->
            contactDetailsItem.add(
                ContactDetailsItem.Text(
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
                    value = TextUiModel(phone.text),
                    type = ContactDetailsItem.ContactDetailType.Triggerable.Phone(phone.text)
                )
            )
        }
        contact.addresses.forEachIndexed { index, address ->
            val formattedAddress = formattedAddress(address)
            if (formattedAddress.isNotBlank()) {
                contactDetailsItem.add(
                    ContactDetailsItem.Text(
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
                        value = TextUiModel(formattedAddress)
                    )
                )
            }
        }
        contact.birthday?.let { birthday ->
            contactDetailsItem.add(
                ContactDetailsItem.Text(
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
                ContactDetailsItem.Text(
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
        contact.photos.forEachIndexed { index, photo ->
            // Skip first index as we use it for avatar already
            if (index == 0) return@forEachIndexed
            decodeByteArray(photo.data)?.let { bitmap ->
                contactDetailsItem.add(
                    ContactDetailsItem.Image(
                        displayIcon = contactDetailsItem.isEmpty(),
                        iconResId = R.drawable.ic_proton_text_align_left,
                        header = TextUiModel(R.string.contact_property_photo),
                        value = bitmap
                    )
                )
            }
        }
        contact.organizations.forEach { organization ->
            contactDetailsItem.add(
                ContactDetailsItem.Text(
                    displayIcon = contactDetailsItem.isEmpty(),
                    iconResId = R.drawable.ic_proton_text_align_left,
                    header = TextUiModel(R.string.contact_property_organization),
                    value = TextUiModel(organization.value)
                )
            )
        }
        contact.titles.forEach { title ->
            contactDetailsItem.add(
                ContactDetailsItem.Text(
                    displayIcon = contactDetailsItem.isEmpty(),
                    iconResId = R.drawable.ic_proton_text_align_left,
                    header = TextUiModel(R.string.contact_property_title),
                    value = TextUiModel(title.value)
                )
            )
        }
        contact.roles.forEach { role ->
            contactDetailsItem.add(
                ContactDetailsItem.Text(
                    displayIcon = contactDetailsItem.isEmpty(),
                    iconResId = R.drawable.ic_proton_text_align_left,
                    header = TextUiModel(R.string.contact_property_role),
                    value = TextUiModel(role.value)
                )
            )
        }
        contact.timezones.forEach { timezone ->
            contactDetailsItem.add(
                ContactDetailsItem.Text(
                    displayIcon = contactDetailsItem.isEmpty(),
                    iconResId = R.drawable.ic_proton_text_align_left,
                    header = TextUiModel(R.string.contact_property_time_zone),
                    value = TextUiModel(timezone.text)
                )
            )
        }
        contact.logos.forEach { logo ->
            decodeByteArray(logo.data)?.let { bitmap ->
                contactDetailsItem.add(
                    ContactDetailsItem.Image(
                        displayIcon = contactDetailsItem.isEmpty(),
                        iconResId = R.drawable.ic_proton_text_align_left,
                        header = TextUiModel(R.string.contact_property_logo),
                        value = bitmap
                    )
                )
            }
        }
        contact.members.forEach { member ->
            contactDetailsItem.add(
                ContactDetailsItem.Text(
                    displayIcon = contactDetailsItem.isEmpty(),
                    iconResId = R.drawable.ic_proton_text_align_left,
                    header = TextUiModel(R.string.contact_property_member),
                    value = TextUiModel(member.value)
                )
            )
        }
        contact.languages.forEach { language ->
            contactDetailsItem.add(
                ContactDetailsItem.Text(
                    displayIcon = contactDetailsItem.isEmpty(),
                    iconResId = R.drawable.ic_proton_text_align_left,
                    header = TextUiModel(R.string.contact_property_language),
                    value = TextUiModel(language.value)
                )
            )
        }
        contact.urls.forEach { url ->
            contactDetailsItem.add(
                ContactDetailsItem.Text(
                    displayIcon = contactDetailsItem.isEmpty(),
                    iconResId = R.drawable.ic_proton_text_align_left,
                    header = TextUiModel(R.string.contact_property_url),
                    value = TextUiModel(url.value)
                )
            )
        }
        contact.gender?.let { gender ->
            contactDetailsItem.add(
                ContactDetailsItem.Text(
                    displayIcon = contactDetailsItem.isEmpty(),
                    iconResId = R.drawable.ic_proton_text_align_left,
                    header = TextUiModel(R.string.contact_property_gender),
                    value = TextUiModel(gender.gender)
                )
            )
        }
        contact.anniversary?.let { anniversary ->
            contactDetailsItem.add(
                ContactDetailsItem.Text(
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

    private fun formattedAddress(address: ContactProperty.Address): String {
        var formattedAddress = ""
        if (address.streetAddress.isNotBlank()) {
            formattedAddress = formattedAddress.plus(address.streetAddress)
        }
        if (address.postalCode.isNotBlank()) {
            val prefix = if (formattedAddress.isNotBlank()) "\n" else ""
            formattedAddress = formattedAddress.plus("$prefix${address.postalCode}")
        }
        if (address.locality.isNotBlank()) {
            val prefix = if (address.postalCode.isNotBlank()) ", " else ""
            formattedAddress = formattedAddress.plus("$prefix${address.locality}")
        }
        if (address.region.isNotBlank()) {
            val prefix = if (formattedAddress.isNotBlank()) "\n" else ""
            formattedAddress = formattedAddress.plus("$prefix${address.region}")
        }
        if (address.country.isNotBlank()) {
            val prefix = if (address.region.isNotBlank()) ", " else ""
            formattedAddress = formattedAddress.plus("$prefix${address.country}")
        }
        return formattedAddress
    }
}
