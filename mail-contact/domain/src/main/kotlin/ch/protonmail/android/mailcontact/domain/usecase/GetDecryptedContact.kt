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

package ch.protonmail.android.mailcontact.domain.usecase

import java.time.ZoneId
import arrow.core.Either
import arrow.core.raise.either
import arrow.core.right
import ch.protonmail.android.mailcontact.domain.model.ContactProperty
import ch.protonmail.android.mailcontact.domain.model.DecryptedContact
import ch.protonmail.android.mailcontact.domain.model.GetContactError
import ezvcard.property.Address
import ezvcard.property.Anniversary
import ezvcard.property.Birthday
import ezvcard.property.Email
import ezvcard.property.FormattedName
import ezvcard.property.Gender
import ezvcard.property.Language
import ezvcard.property.Logo
import ezvcard.property.Member
import ezvcard.property.Note
import ezvcard.property.Organization
import ezvcard.property.Photo
import ezvcard.property.Role
import ezvcard.property.StructuredName
import ezvcard.property.Telephone
import ezvcard.property.Timezone
import ezvcard.property.Title
import ezvcard.property.Url
import me.proton.core.contact.domain.entity.ContactId
import me.proton.core.contact.domain.entity.ContactWithCards
import me.proton.core.contact.domain.entity.DecryptedVCard
import me.proton.core.crypto.common.pgp.VerificationStatus
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

/**
 * Decrypts ContactCards and combines all the data into one [DecryptedContact] model.
 */
class GetDecryptedContact @Inject constructor(
    private val decryptContactCards: DecryptContactCards
) {

    suspend operator fun invoke(
        userId: UserId,
        contactWithCards: ContactWithCards
    ): Either<GetContactError, DecryptedContact> = either {

        val decryptedCards = decryptContactCards(userId, contactWithCards).bind().filter {
            it.status == VerificationStatus.Success || it.status == VerificationStatus.NotSigned
        }

        return extractFromVCards(contactWithCards.id, decryptedCards).right()
    }

    @Suppress("LongMethod", "ComplexMethod")
    private fun extractFromVCards(contactId: ContactId, decryptedCards: List<DecryptedVCard>): DecryptedContact {

        var decryptedContact = DecryptedContact(contactId)

        decryptedCards.forEach { decryptedVCard ->
            decryptedVCard.card.properties.forEach {
                decryptedContact = when (it) {
                    is StructuredName -> {
                        decryptedContact.copy(
                            structuredName = ContactProperty.StructuredName(
                                it.family ?: "",
                                it.given ?: ""
                            )
                        )
                    }

                    is FormattedName -> {
                        decryptedContact.copy(
                            formattedName = ContactProperty.FormattedName(
                                it.value ?: ""
                            )
                        )
                    }

                    is Email -> {
                        decryptedContact.copy(
                            emails = decryptedContact.emails + ContactProperty.Email(
                                type = ContactProperty.Email.Type.from(it.types.firstOrNull()?.value),
                                value = it.value ?: ""
                            )
                        )
                    }

                    is Telephone -> {
                        decryptedContact.copy(
                            telephones = decryptedContact.telephones + ContactProperty.Telephone(
                                type = ContactProperty.Telephone.Type.from(it.types.firstOrNull()?.value),
                                text = it.text ?: ""
                            )
                        )
                    }

                    is Address -> {
                        decryptedContact.copy(
                            addresses = decryptedContact.addresses + ContactProperty.Address(
                                type = ContactProperty.Address.Type.from(it.types.firstOrNull()?.value),
                                streetAddress = it.streetAddress ?: "",
                                locality = it.locality ?: "",
                                region = it.region ?: "",
                                postalCode = it.postalCode ?: "",
                                country = it.country ?: ""
                            )
                        )
                    }

                    is Birthday -> {
                        if (it.date != null) {
                            decryptedContact.copy(
                                birthday = ContactProperty.Birthday(
                                    it.date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                                )
                            )
                        } else decryptedContact
                    }

                    is Note -> {
                        decryptedContact.copy(
                            notes = decryptedContact.notes + ContactProperty.Note(
                                it.value ?: ""
                            )
                        )
                    }

                    is Photo -> {
                        decryptedContact.copy(
                            photos = decryptedContact.photos + ContactProperty.Photo(
                                data = it.data ?: ByteArray(0),
                                contentType = it.contentType?.value,
                                mediaType = it.contentType?.mediaType,
                                extension = it.contentType?.extension
                            )
                        )
                    }

                    is Organization -> {
                        decryptedContact.copy(
                            organizations = decryptedContact.organizations + it.values.map {
                                ContactProperty.Organization(it)
                            }
                        )
                    }

                    is Title -> {
                        decryptedContact.copy(
                            titles = decryptedContact.titles + ContactProperty.Title(
                                it.value ?: ""
                            )
                        )
                    }

                    is Role -> {
                        decryptedContact.copy(
                            roles = decryptedContact.roles + ContactProperty.Role(
                                it.value ?: ""
                            )
                        )
                    }

                    is Timezone -> {
                        decryptedContact.copy(
                            timezones = decryptedContact.timezones + ContactProperty.Timezone(
                                it.text ?: ""
                            )
                        )
                    }

                    is Logo -> {
                        decryptedContact.copy(
                            logos = decryptedContact.logos + ContactProperty.Logo(
                                data = it.data ?: ByteArray(0),
                                contentType = it.contentType?.value,
                                mediaType = it.contentType?.mediaType,
                                extension = it.contentType?.extension
                            )
                        )
                    }

                    is Member -> {
                        decryptedContact.copy(
                            members = decryptedContact.members + ContactProperty.Member(
                                it.value ?: ""
                            )
                        )
                    }

                    is Language -> {
                        decryptedContact.copy(
                            languages = decryptedContact.languages + ContactProperty.Language(
                                it.value ?: ""
                            )
                        )
                    }

                    is Gender -> {
                        decryptedContact.copy(
                            gender = ContactProperty.Gender(
                                it.gender ?: ""
                            )
                        )
                    }

                    is Anniversary -> {
                        if (it.date != null) {
                            decryptedContact.copy(
                                anniversary = ContactProperty.Anniversary(
                                    it.date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                                )
                            )
                        } else decryptedContact
                    }

                    is Url -> {
                        decryptedContact.copy(
                            urls = decryptedContact.urls + ContactProperty.Url(
                                it.value ?: ""
                            )
                        )
                    }

                    else -> {
                        decryptedContact
                    }
                }
            }
        }

        return decryptedContact
    }
}
