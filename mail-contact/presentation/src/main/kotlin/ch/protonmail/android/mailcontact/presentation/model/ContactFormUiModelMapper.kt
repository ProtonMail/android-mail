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
import ch.protonmail.android.mailcontact.domain.model.ContactGroup
import ch.protonmail.android.mailcontact.domain.model.ContactProperty
import ch.protonmail.android.mailcontact.domain.model.DecryptedContact
import me.proton.core.contact.domain.entity.ContactId
import javax.inject.Inject

class ContactFormUiModelMapper @Inject constructor(
    private val decodeByteArray: DecodeByteArray
) {

    fun toContactFormUiModel(decryptedContact: DecryptedContact): ContactFormUiModel {
        return ContactFormUiModel(
            id = decryptedContact.id,
            avatar = getAvatar(decryptedContact),
            displayName = decryptedContact.formattedName?.value ?: "",
            firstName = decryptedContact.structuredName?.given ?: "",
            lastName = decryptedContact.structuredName?.family ?: "",
            emails = decryptedContact.emails.map {
                InputField.SingleTyped(
                    value = it.value,
                    selectedType = FieldType.EmailType.valueOf(it.type.name)
                )
            }.toMutableList(),
            telephones = decryptedContact.telephones.map {
                InputField.SingleTyped(
                    value = it.text,
                    selectedType = FieldType.TelephoneType.valueOf(it.type.name)
                )
            }.toMutableList(),
            addresses = decryptedContact.addresses.map {
                InputField.Address(
                    streetAddress = it.streetAddress,
                    postalCode = it.postalCode,
                    city = it.locality,
                    region = it.region,
                    country = it.country,
                    selectedType = FieldType.AddressType.valueOf(it.type.name)
                )
            }.toMutableList(),
            birthday = decryptedContact.birthday?.let {
                InputField.Birthday(value = it.date)
            },
            notes = decryptedContact.notes.map {
                InputField.Note(
                    value = it.value
                )
            }.toMutableList(),
            others = buildOthers(decryptedContact),
            otherTypes = FieldType.OtherType.values().toList()
        )
    }

    @SuppressWarnings("LongMethod", "ComplexMethod")
    fun toDecryptedContact(
        contact: ContactFormUiModel,
        contactGroups: List<ContactGroup>,
        // Remove those fields once they are implemented in form
        photos: List<ContactProperty.Photo>,
        logos: List<ContactProperty.Logo>
    ): DecryptedContact {
        val organizations = listOf<ContactProperty.Organization>()
        val titles = listOf<ContactProperty.Title>()
        val roles = listOf<ContactProperty.Role>()
        val timezones = listOf<ContactProperty.Timezone>()
        val members = listOf<ContactProperty.Member>()
        val languages = listOf<ContactProperty.Language>()
        val urls = listOf<ContactProperty.Url>()
        var gender: ContactProperty.Gender? = null
        var anniversary: ContactProperty.Anniversary? = null
        contact.others.map { other ->
            when (other) {
                is InputField.SingleTyped -> {
                    when (other.selectedType as FieldType.OtherType) {
                        FieldType.OtherType.Organization -> organizations.plus(
                            ContactProperty.Organization(other.value)
                        )
                        FieldType.OtherType.Title -> titles.plus(
                            ContactProperty.Title(other.value)
                        )
                        FieldType.OtherType.Role -> roles.plus(
                            ContactProperty.Role(other.value)
                        )
                        FieldType.OtherType.TimeZone -> timezones.plus(
                            ContactProperty.Timezone(other.value)
                        )
                        FieldType.OtherType.Member -> members.plus(
                            ContactProperty.Member(other.value)
                        )
                        FieldType.OtherType.Language -> languages.plus(
                            ContactProperty.Language(other.value)
                        )
                        FieldType.OtherType.Url -> urls.plus(
                            ContactProperty.Url(other.value)
                        )
                        FieldType.OtherType.Gender -> gender = ContactProperty.Gender(gender = other.value)
                        else -> {
                            // Not applicable for `SingleTyped`
                        }
                    }
                }
                is InputField.DateTyped -> {
                    when (other.selectedType as FieldType.OtherType) {
                        FieldType.OtherType.Anniversary -> anniversary = ContactProperty.Anniversary(date = other.value)
                        else -> {
                            // Not applicable for `DateTyped`
                        }
                    }
                }
                is InputField.ImageTyped -> {
                    // Not yet implemented (photo, logo)
                }
                else -> {
                    // Not applicable to `others` section
                }
            }
        }
        return DecryptedContact(
            id = contact.id ?: ContactId(""),
            contactGroups = contactGroups,
            structuredName = ContactProperty.StructuredName(
                family = contact.lastName,
                given = contact.firstName
            ),
            formattedName = ContactProperty.FormattedName(
                value = contact.displayName
            ),
            emails = contact.emails.map { email ->
                ContactProperty.Email(
                    type = ContactProperty.Email.Type.valueOf(
                        (email.selectedType as FieldType.EmailType).name
                    ),
                    value = email.value
                )
            },
            telephones = contact.telephones.map { telephone ->
                ContactProperty.Telephone(
                    type = ContactProperty.Telephone.Type.valueOf(
                        (telephone.selectedType as FieldType.TelephoneType).name
                    ),
                    text = telephone.value
                )
            },
            addresses = contact.addresses.map { address ->
                ContactProperty.Address(
                    type = ContactProperty.Address.Type.valueOf(
                        (address.selectedType as FieldType.AddressType).name
                    ),
                    streetAddress = address.streetAddress,
                    locality = address.city,
                    region = address.region,
                    postalCode = address.postalCode,
                    country = address.country
                )
            },
            birthday = contact.birthday?.let { birthday ->
                ContactProperty.Birthday(
                    date = birthday.value
                )
            },
            notes = contact.notes.map { note ->
                ContactProperty.Note(
                    value = note.value
                )
            },
            photos = photos,
            organizations = organizations,
            titles = titles,
            roles = roles,
            timezones = timezones,
            logos = logos,
            members = members,
            languages = languages,
            urls = urls,
            gender = gender,
            anniversary = anniversary
        )
    }

    private fun buildOthers(decryptedContact: DecryptedContact): MutableList<InputField> {
        val others = arrayListOf<InputField>()
        decryptedContact.photos.forEachIndexed { index, photo ->
            // Skip first index as we use it for avatar already
            if (index == 0) return@forEachIndexed
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

    private fun getAvatar(decryptedContact: DecryptedContact): ContactFormAvatar {
        if (decryptedContact.photos.isNotEmpty()) {
            val byteArray = decryptedContact.photos.first().data
            decodeByteArray(byteArray)?.let { bitmap ->
                return ContactFormAvatar.Photo(bitmap = bitmap)
            }
        }
        return ContactFormAvatar.Empty
    }
}
