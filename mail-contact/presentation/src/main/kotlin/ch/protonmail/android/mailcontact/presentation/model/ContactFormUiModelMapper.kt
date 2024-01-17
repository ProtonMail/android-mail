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

import ch.protonmail.android.mailcommon.presentation.usecase.DecodeByteArray
import ch.protonmail.android.mailcontact.domain.model.DecryptedContact
import javax.inject.Inject

class ContactFormUiModelMapper @Inject constructor(
    private val decodeByteArray: DecodeByteArray
) {

    fun toContactFormUiModel(decryptedContact: DecryptedContact): ContactFormUiModel {
        return ContactFormUiModel(
            id = decryptedContact.id,
            displayName = decryptedContact.formattedName?.value ?: "",
            firstName = decryptedContact.structuredName?.given ?: "",
            lastName = decryptedContact.structuredName?.family ?: "",
            emails = decryptedContact.emails.map {
                InputField.SingleTyped(
                    value = it.value,
                    selectedType = FieldType.EmailType.valueOf(it.type.name)
                )
            },
            phones = decryptedContact.telephones.map {
                InputField.SingleTyped(
                    value = it.text,
                    selectedType = FieldType.PhoneType.valueOf(it.type.name)
                )
            },
            addresses = decryptedContact.addresses.map {
                InputField.Address(
                    streetAddress = it.streetAddress,
                    postalCode = it.postalCode,
                    locality = it.locality,
                    region = it.region,
                    country = it.country,
                    selectedType = FieldType.AddressType.valueOf(it.type.name)
                )
            },
            birthday = decryptedContact.birthday?.let {
                InputField.Date(value = it.date)
            },
            notes = decryptedContact.notes.map {
                InputField.Note(
                    value = it.value
                )
            },
            others = buildOthers(decryptedContact),
            otherTypes = FieldType.OtherType.values().toList()
        )
    }

    private fun buildOthers(decryptedContact: DecryptedContact): List<InputField> {
        val others = arrayListOf<InputField>()
        decryptedContact.photos.forEach { photo ->
            decodeByteArray(photo.data)?.let { bitmap ->
                others.add(InputField.ImageTyped(bitmap, FieldType.OtherType.Photo))
            }
        }
        decryptedContact.organizations.forEach { organization ->
            others.add(InputField.SingleTyped(organization.value, FieldType.OtherType.Organization))
        }
        decryptedContact.titles.forEach { title ->
            others.add(InputField.SingleTyped(title.value, FieldType.OtherType.Title))
        }
        decryptedContact.roles.forEach { role ->
            others.add(InputField.SingleTyped(role.value, FieldType.OtherType.Role))
        }
        decryptedContact.timezones.forEach { timeZone ->
            others.add(InputField.SingleTyped(timeZone.text, FieldType.OtherType.TimeZone))
        }
        decryptedContact.logos.forEach { logo ->
            decodeByteArray(logo.data)?.let { bitmap ->
                others.add(InputField.ImageTyped(bitmap, FieldType.OtherType.Logo))
            }
        }
        decryptedContact.members.forEach { member ->
            others.add(InputField.SingleTyped(member.value, FieldType.OtherType.Member))
        }
        decryptedContact.languages.forEach { language ->
            others.add(InputField.SingleTyped(language.value, FieldType.OtherType.Language))
        }
        decryptedContact.urls.forEach { url ->
            others.add(InputField.SingleTyped(url.value, FieldType.OtherType.Url))
        }
        decryptedContact.gender?.let { gender ->
            others.add(InputField.SingleTyped(gender.gender, FieldType.OtherType.Gender))
        }
        decryptedContact.anniversary?.let { anniversary ->
            others.add(InputField.DateTyped(anniversary.date, FieldType.OtherType.Anniversary))
        }
        return others
    }
}
