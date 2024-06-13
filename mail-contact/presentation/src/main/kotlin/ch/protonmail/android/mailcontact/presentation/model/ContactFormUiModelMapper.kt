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
import ch.protonmail.android.mailcontact.domain.model.ContactGroupLabel
import ch.protonmail.android.mailcontact.domain.model.ContactProperty
import ch.protonmail.android.mailcontact.domain.model.DecryptedContact
import me.proton.core.util.kotlin.takeIfNotBlank
import javax.inject.Inject

class ContactFormUiModelMapper @Inject constructor(
    private val decodeByteArray: DecodeByteArray
) {

    private var incrementalUniqueFieldId = 0

    fun toContactFormUiModel(decryptedContact: DecryptedContact): ContactFormUiModel {
        incrementalUniqueFieldId = 0 // Make sure to reset the field ID
        val emails = decryptedContact.emails.map {
            InputField.SingleTyped(
                fieldId = getIncrementalUniqueFieldId(),
                value = it.value,
                selectedType = FieldType.EmailType.valueOf(it.type.name)
            )
        }
        val telephones = decryptedContact.telephones.map {
            InputField.SingleTyped(
                fieldId = getIncrementalUniqueFieldId(),
                value = it.text,
                selectedType = FieldType.TelephoneType.valueOf(it.type.name)
            )
        }
        val addresses = decryptedContact.addresses.map {
            InputField.Address(
                fieldId = getIncrementalUniqueFieldId(),
                streetAddress = it.streetAddress,
                postalCode = it.postalCode,
                city = it.locality,
                region = it.region,
                country = it.country,
                selectedType = FieldType.AddressType.valueOf(it.type.name)
            )
        }
        val birthday = decryptedContact.birthday?.let {
            InputField.Birthday(
                fieldId = getIncrementalUniqueFieldId(),
                value = it.date
            )
        }
        val notes = decryptedContact.notes.map {
            InputField.Note(
                fieldId = getIncrementalUniqueFieldId(),
                value = it.value
            )
        }
        val others = buildOthers(decryptedContact)
        return ContactFormUiModel(
            id = decryptedContact.id,
            avatar = getAvatar(decryptedContact),
            displayName = decryptedContact.formattedName?.value ?: "",
            firstName = decryptedContact.structuredName?.given ?: "",
            lastName = decryptedContact.structuredName?.family ?: "",
            emails = emails,
            telephones = telephones,
            addresses = addresses,
            birthday = birthday,
            notes = notes,
            others = others,
            otherTypes = FieldType.OtherType.values().filterOutUnsupportedFields().toList(),
            incrementalUniqueFieldId = incrementalUniqueFieldId
        )
    }

    @SuppressWarnings("LongMethod")
    private fun buildOthers(decryptedContact: DecryptedContact): MutableList<InputField> {
        val others = arrayListOf<InputField>()
        decryptedContact.photos.forEachIndexed { index, photo ->
            // Skip first index as we use it for avatar already
            if (index == 0) return@forEachIndexed
            decodeByteArray(photo.data)?.let { bitmap ->
                others.add(
                    InputField.ImageTyped(
                        fieldId = getIncrementalUniqueFieldId(),
                        value = bitmap,
                        selectedType = FieldType.OtherType.Photo
                    )
                )
            }
        }
        decryptedContact.organizations.forEach { organization ->
            others.add(
                InputField.SingleTyped(
                    fieldId = getIncrementalUniqueFieldId(),
                    value = organization.value,
                    selectedType = FieldType.OtherType.Organization
                )
            )
        }
        decryptedContact.titles.forEach { title ->
            others.add(
                InputField.SingleTyped(
                    fieldId = getIncrementalUniqueFieldId(),
                    value = title.value,
                    selectedType = FieldType.OtherType.Title
                )
            )
        }
        decryptedContact.roles.forEach { role ->
            others.add(
                InputField.SingleTyped(
                    fieldId = getIncrementalUniqueFieldId(),
                    value = role.value,
                    selectedType = FieldType.OtherType.Role
                )
            )
        }
        decryptedContact.timezones.forEach { timeZone ->
            others.add(
                InputField.SingleTyped(
                    fieldId = getIncrementalUniqueFieldId(),
                    value = timeZone.text,
                    selectedType = FieldType.OtherType.TimeZone
                )
            )
        }
        decryptedContact.logos.forEach { logo ->
            decodeByteArray(logo.data)?.let { bitmap ->
                others.add(
                    InputField.ImageTyped(
                        fieldId = getIncrementalUniqueFieldId(),
                        value = bitmap,
                        selectedType = FieldType.OtherType.Logo
                    )
                )
            }
        }
        decryptedContact.members.forEach { member ->
            others.add(
                InputField.SingleTyped(
                    fieldId = getIncrementalUniqueFieldId(),
                    value = member.value,
                    selectedType = FieldType.OtherType.Member
                )
            )
        }
        decryptedContact.languages.forEach { language ->
            others.add(
                InputField.SingleTyped(
                    fieldId = getIncrementalUniqueFieldId(),
                    value = language.value,
                    selectedType = FieldType.OtherType.Language
                )
            )
        }
        decryptedContact.urls.forEach { url ->
            others.add(
                InputField.SingleTyped(
                    fieldId = getIncrementalUniqueFieldId(),
                    value = url.value,
                    selectedType = FieldType.OtherType.Url
                )
            )
        }
        decryptedContact.gender?.let { gender ->
            others.add(
                InputField.SingleTyped(
                    fieldId = getIncrementalUniqueFieldId(),
                    value = gender.gender,
                    selectedType = FieldType.OtherType.Gender
                )
            )
        }
        decryptedContact.anniversary?.let { anniversary ->
            others.add(
                InputField.DateTyped(
                    fieldId = getIncrementalUniqueFieldId(),
                    value = anniversary.date,
                    selectedType = FieldType.OtherType.Anniversary
                )
            )
        }
        return others
    }

    private fun getIncrementalUniqueFieldId(): String {
        val fieldId = incrementalUniqueFieldId.toString()
        incrementalUniqueFieldId++
        return fieldId
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

    fun toDecryptedContact(
        contact: ContactFormUiModel,
        contactGroupLabels: List<ContactGroupLabel>,
        // Remove those fields once they are implemented in form
        photos: List<ContactProperty.Photo>,
        logos: List<ContactProperty.Logo>
    ): DecryptedContact {
        return DecryptedContact(
            id = contact.id,
            contactGroupLabels = contactGroupLabels,
            structuredName = contact.getStructuredNameContactProperty(),
            formattedName = contact.getFormattedNameContactProperty(),
            emails = contact.getEmailContactPropertyList(),
            telephones = contact.getTelephoneContactPropertyList(),
            addresses = contact.getAddressContactPropertyList(),
            birthday = contact.getBirthdayContactProperty(),
            notes = contact.getNoteContactPropertyList(),
            photos = photos,
            organizations = contact.getOrganizationContactPropertyList(),
            titles = contact.getTitleContactPropertyList(),
            roles = contact.getRoleContactPropertyList(),
            timezones = contact.getTimezoneContactPropertyList(),
            logos = logos,
            members = contact.getMemberContactPropertyList(),
            languages = contact.getLanguageContactPropertyList(),
            urls = contact.getUrlContactPropertyList(),
            gender = contact.getGenderContactProperty(),
            anniversary = contact.getAnniversaryContactProperty()
        )
    }

    private fun ContactFormUiModel.getFormattedNameContactProperty(): ContactProperty.FormattedName {
        return ContactProperty.FormattedName(
            value = this.displayName.takeIfNotBlank()?.trim() ?: run {
                // Formatted name is mandatory in contacts. Fallback to first and last name combo.
                (this.firstName.takeIfNotBlank()?.trim() ?: "").plus(
                    this.lastName.takeIfNotBlank()?.let { " ${it.trim()}" } ?: ""
                ).trim()
            }
        )
    }

    private fun ContactFormUiModel.getStructuredNameContactProperty(): ContactProperty.StructuredName? {
        return if (this.lastName.isBlank() && this.firstName.isBlank()) {
            null
        } else {
            ContactProperty.StructuredName(
                family = this.lastName.trim(),
                given = this.firstName.trim()
            )
        }
    }

    private fun ContactFormUiModel.getEmailContactPropertyList(): List<ContactProperty.Email> {
        return this.emails.mapNotNull { email ->
            if (email.value.isNotBlank()) {
                ContactProperty.Email(
                    type = ContactProperty.Email.Type.valueOf(
                        (email.selectedType as FieldType.EmailType).name
                    ),
                    value = email.value
                )
            } else null
        }
    }

    private fun ContactFormUiModel.getTelephoneContactPropertyList(): List<ContactProperty.Telephone> {
        return this.telephones.mapNotNull { telephone ->
            if (telephone.value.isNotBlank()) {
                ContactProperty.Telephone(
                    type = ContactProperty.Telephone.Type.valueOf(
                        (telephone.selectedType as FieldType.TelephoneType).name
                    ),
                    text = telephone.value
                )
            } else null
        }
    }

    @SuppressWarnings("ComplexCondition")
    private fun ContactFormUiModel.getAddressContactPropertyList(): List<ContactProperty.Address> {
        return this.addresses.mapNotNull { address ->
            if (address.streetAddress.isNotBlank() ||
                address.city.isNotBlank() ||
                address.region.isNotBlank() ||
                address.postalCode.isNotBlank() ||
                address.country.isNotBlank()
            ) {
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
            } else null
        }
    }

    private fun ContactFormUiModel.getBirthdayContactProperty(): ContactProperty.Birthday? {
        return this.birthday?.let { birthday ->
            ContactProperty.Birthday(
                date = birthday.value
            )
        }
    }

    private fun ContactFormUiModel.getNoteContactPropertyList(): List<ContactProperty.Note> {
        return this.notes.mapNotNull { note ->
            if (note.value.isNotBlank()) {
                ContactProperty.Note(
                    value = note.value
                )
            } else null
        }
    }

    private fun ContactFormUiModel.getOrganizationContactPropertyList(): List<ContactProperty.Organization> {
        return this.others.mapNotNull { other ->
            if (other is InputField.SingleTyped &&
                other.selectedType == FieldType.OtherType.Organization &&
                other.value.isNotBlank()
            ) {
                ContactProperty.Organization(
                    value = other.value
                )
            } else null
        }
    }

    private fun ContactFormUiModel.getTitleContactPropertyList(): List<ContactProperty.Title> {
        return this.others.mapNotNull { other ->
            if (other is InputField.SingleTyped &&
                other.selectedType == FieldType.OtherType.Title &&
                other.value.isNotBlank()
            ) {
                ContactProperty.Title(
                    value = other.value
                )
            } else null
        }
    }

    private fun ContactFormUiModel.getRoleContactPropertyList(): List<ContactProperty.Role> {
        return this.others.mapNotNull { other ->
            if (other is InputField.SingleTyped &&
                other.selectedType == FieldType.OtherType.Role &&
                other.value.isNotBlank()
            ) {
                ContactProperty.Role(
                    value = other.value
                )
            } else null
        }
    }

    private fun ContactFormUiModel.getTimezoneContactPropertyList(): List<ContactProperty.Timezone> {
        return this.others.mapNotNull { other ->
            if (other is InputField.SingleTyped &&
                other.selectedType == FieldType.OtherType.TimeZone &&
                other.value.isNotBlank()
            ) {
                ContactProperty.Timezone(
                    text = other.value
                )
            } else null
        }
    }

    private fun ContactFormUiModel.getMemberContactPropertyList(): List<ContactProperty.Member> {
        return this.others.mapNotNull { other ->
            if (other is InputField.SingleTyped &&
                other.selectedType == FieldType.OtherType.Member &&
                other.value.isNotBlank()
            ) {
                ContactProperty.Member(
                    value = other.value
                )
            } else null
        }
    }

    private fun ContactFormUiModel.getLanguageContactPropertyList(): List<ContactProperty.Language> {
        return this.others.mapNotNull { other ->
            if (other is InputField.SingleTyped &&
                other.selectedType == FieldType.OtherType.Language &&
                other.value.isNotBlank()
            ) {
                ContactProperty.Language(
                    value = other.value
                )
            } else null
        }
    }

    private fun ContactFormUiModel.getUrlContactPropertyList(): List<ContactProperty.Url> {
        return this.others.mapNotNull { other ->
            if (other is InputField.SingleTyped &&
                other.selectedType == FieldType.OtherType.Url &&
                other.value.isNotBlank()
            ) {
                ContactProperty.Url(
                    value = other.value
                )
            } else null
        }
    }

    private fun ContactFormUiModel.getGenderContactProperty(): ContactProperty.Gender? {
        val genderInputField = this.others.find {
            it is InputField.SingleTyped && it.selectedType == FieldType.OtherType.Gender
        }
        return genderInputField?.takeIf {
            (genderInputField as InputField.SingleTyped).value.isNotBlank()
        }?.let {
            ContactProperty.Gender(
                gender = (genderInputField as InputField.SingleTyped).value
            )
        }
    }

    private fun ContactFormUiModel.getAnniversaryContactProperty(): ContactProperty.Anniversary? {
        val anniversaryInputField = this.others.find {
            it is InputField.DateTyped && it.selectedType == FieldType.OtherType.Anniversary
        }
        return anniversaryInputField?.let {
            ContactProperty.Anniversary(
                date = (anniversaryInputField as InputField.DateTyped).value
            )
        }
    }
}
