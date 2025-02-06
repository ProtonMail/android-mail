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

import arrow.core.Either
import arrow.core.raise.either
import ch.protonmail.android.mailcommon.domain.mapper.mapToEither
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcontact.domain.model.ContactGroupLabel
import ch.protonmail.android.mailcontact.domain.model.DecryptedContact
import ch.protonmail.android.maillabel.domain.usecase.ObserveLabels
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import me.proton.core.contact.domain.entity.ContactId
import me.proton.core.contact.domain.entity.ContactWithCards
import me.proton.core.contact.domain.repository.ContactRepository
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.Label
import me.proton.core.label.domain.entity.LabelType
import javax.inject.Inject

class ObserveDecryptedContact @Inject constructor(
    private val contactRepository: ContactRepository,
    private val getDecryptedContact: GetDecryptedContact,
    private val observeLabels: ObserveLabels
) {

    operator fun invoke(
        userId: UserId,
        contactId: ContactId,
        refresh: Boolean = false
    ): Flow<Either<DataError, DecryptedContact>> {
        return combine(
            contactRepository.observeContactWithCards(userId, contactId, refresh).mapToEither(),
            observeLabels(userId, LabelType.ContactGroup)
        ) { contactWithCardsEither, labelsEither ->
            either {

                val contactWithCards = contactWithCardsEither.bind()
                val allContactLabels = labelsEither.bind()

                getDecryptedContact(userId, contactWithCards).getOrNull()?.copy(
                    contactGroupLabels = createContactGroups(contactWithCards, allContactLabels)
                ) ?: raise(DataError.Local.DecryptionError)
            }
        }
    }

    private fun createContactGroups(
        contactWithCards: ContactWithCards,
        allContactLabels: List<Label>
    ): List<ContactGroupLabel> {
        val allLabelIds = contactWithCards.contactEmails.flatMap { it.labelIds }

        return allContactLabels.filter {
            allLabelIds.contains(it.labelId.id)
        }.map {
            ContactGroupLabel(
                name = it.name,
                color = it.color
            )
        }
    }
}
