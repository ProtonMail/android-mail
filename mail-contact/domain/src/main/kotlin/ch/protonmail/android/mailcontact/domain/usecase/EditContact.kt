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
import arrow.core.getOrElse
import arrow.core.left
import ch.protonmail.android.mailcontact.domain.model.DecryptedContact
import me.proton.core.contact.domain.entity.ContactId
import me.proton.core.contact.domain.repository.ContactRepository
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

class EditContact @Inject constructor(
    private val contactRepository: ContactRepository,
    private val encryptAndSignContactCards: EncryptAndSignContactCards
) {

    suspend operator fun invoke(
        userId: UserId,
        decryptedContact: DecryptedContact,
        contactId: ContactId
    ): Either<EditContactErrors, Unit> {
        val contactCards = encryptAndSignContactCards(
            userId,
            decryptedContact.takeIf { it.id != null } ?: decryptedContact.copy(
                id = contactId
            )
        ).getOrElse {
            return EditContactErrors.FailedToEncryptAndSignContactCards.left()
        }
        return Either.catch {
            contactRepository.updateContact(userId, contactId, contactCards)
        }.mapLeft {
            EditContactErrors.FailedToEditContact
        }
    }

    sealed interface EditContactErrors {
        object FailedToEncryptAndSignContactCards : EditContactErrors
        object FailedToEditContact : EditContactErrors
    }
}
