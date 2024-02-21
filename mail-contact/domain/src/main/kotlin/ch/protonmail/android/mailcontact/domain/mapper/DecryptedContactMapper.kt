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

package ch.protonmail.android.mailcontact.domain.mapper

import java.time.ZoneId
import java.util.Date
import ch.protonmail.android.mailcontact.domain.VCARD_PROD_ID
import ch.protonmail.android.mailcontact.domain.model.ContactGroupLabel
import ch.protonmail.android.mailcontact.domain.model.DecryptedContact
import ezvcard.VCard
import ezvcard.VCardVersion
import ezvcard.parameter.AddressType
import ezvcard.parameter.EmailType
import ezvcard.parameter.ImageType
import ezvcard.parameter.TelephoneType
import ezvcard.property.Address
import ezvcard.property.Anniversary
import ezvcard.property.Birthday
import ezvcard.property.Email
import ezvcard.property.FormattedName
import ezvcard.property.Gender
import ezvcard.property.Logo
import ezvcard.property.Member
import ezvcard.property.Organization
import ezvcard.property.Photo
import ezvcard.property.ProductId
import ezvcard.property.StructuredName
import ezvcard.property.Telephone
import ezvcard.property.Timezone
import ezvcard.property.Title
import me.proton.core.util.kotlin.takeIfNotBlank
import me.proton.core.util.kotlin.takeIfNotEmpty
import javax.inject.Inject

class DecryptedContactMapper @Inject constructor() {

    /**
     * We should not generate ClearText ContactCard if CATEGORIES field is empty.
     */
    fun mapToClearTextContactCard(vCard: VCard, contactGroups: List<ContactGroupLabel>? = null): VCard? {

        val contactGroupNames = contactGroups?.map { it.name }?.toTypedArray()

        return if (contactGroupNames?.isNotEmpty() == true || vCard.categories?.values?.takeIfNotEmpty() != null) {
            VCard(VCardVersion.V4_0).apply {
                productId = ProductId(VCARD_PROD_ID)
                if (contactGroupNames != null) {
                    setCategories(*contactGroupNames)
                } else setCategories(vCard.categories)
            }
        } else null
    }

    fun mapToSignedContactCard(
        fallbackName: String,
        decryptedContact: DecryptedContact,
        vCard: VCard
    ): VCard {
        return with(vCard) {

            formattedName = decryptedContact.formattedName?.value?.takeIfNotBlank()?.let {
                FormattedName(it)
            } ?: FormattedName(fallbackName) // API requires every Contact to have FN field

            decryptedContact.emails.let {
                emails?.clear()
                it.forEachIndexed { index, email ->
                    addEmail(
                        Email(email.value).apply {
                            group = "ITEM${index + 1}"
                            if (email.type.value.isNotBlank()) {
                                types.add(EmailType.get(email.type.value))
                            }
                            pref = index + 1
                        }
                    )
                }
            }

            this
        }
    }

    @Suppress("LongMethod", "ComplexMethod")
    fun mapToEncryptedAndSignedContactCard(decryptedContact: DecryptedContact, vCard: VCard): VCard {
        return with(vCard) {
            structuredName = decryptedContact.structuredName?.let {
                StructuredName().apply {
                    family = it.family
                    given = it.given
                }
            }

            decryptedContact.telephones.let {
                telephoneNumbers?.clear()
                it.forEachIndexed { index, telephone ->
                    addTelephoneNumber(
                        Telephone(telephone.text).apply {
                            if (telephone.type.value.isNotBlank()) {
                                types.add(TelephoneType.get(telephone.type.value))
                            }
                            pref = index + 1
                        }
                    )
                }
            }

            decryptedContact.addresses.let {
                addresses?.clear()
                it.forEachIndexed { index, address ->
                    addAddress(
                        Address().apply {
                            if (address.type.value.isNotBlank()) {
                                types.add(AddressType.get(address.type.value))
                            }
                            streetAddress = address.streetAddress
                            locality = address.locality
                            region = address.region
                            postalCode = address.postalCode
                            country = address.country
                            pref = index + 1
                        }
                    )
                }
            }

            birthday = decryptedContact.birthday?.let {
                Birthday(Date.from(it.date.atStartOfDay(ZoneId.systemDefault()).toInstant()))
            }

            decryptedContact.notes.let {
                notes?.clear()
                it.forEach { note ->
                    addNote(note.value)
                }
            }

            decryptedContact.photos.let {
                photos?.clear()
                it.forEachIndexed { index, photo ->
                    addPhoto(
                        Photo(
                            photo.data,
                            ImageType.get(photo.contentType, photo.mediaType, photo.extension)
                        ).apply {
                            pref = index + 1
                        }
                    )
                }
            }

            decryptedContact.organizations.let {
                organizations?.clear()
                it.forEach { organization ->
                    addOrganization(
                        Organization().apply {
                            values.add(organization.value)
                        }
                    )
                }
            }

            decryptedContact.titles.let {
                titles?.clear()
                it.forEach { title ->
                    addTitle(Title(title.value))
                }
            }

            decryptedContact.roles.let {
                roles?.clear()
                it.forEach { role ->
                    addRole(role.value)
                }
            }

            decryptedContact.timezones.let {
                timezones?.clear()
                it.forEach { timezone ->
                    addTimezone(Timezone(timezone.text))
                }
            }

            decryptedContact.logos.let {
                logos?.clear()
                it.forEach { logo ->
                    addLogo(
                        Logo(
                            logo.data,
                            ImageType.get(logo.contentType, logo.mediaType, logo.extension)
                        )
                    )
                }
            }

            decryptedContact.members.let {
                members?.clear()
                it.forEach { member ->
                    addMember(Member(member.value))
                }
            }

            decryptedContact.languages.let {
                languages?.clear()
                it.forEach { language ->
                    addLanguage(language.value)
                }
            }

            gender = decryptedContact.gender?.let {
                Gender(it.gender)
            }

            anniversary = decryptedContact.anniversary?.let {
                Anniversary(Date.from(it.date.atStartOfDay(ZoneId.systemDefault()).toInstant()))
            }

            decryptedContact.urls.let {
                urls?.clear()
                it.forEach { url ->
                    addUrl(url.value)
                }
            }

            this
        }
    }

}
