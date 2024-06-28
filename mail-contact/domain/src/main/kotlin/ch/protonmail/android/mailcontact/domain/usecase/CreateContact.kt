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
import ch.protonmail.android.mailcontact.domain.mapper.isContactLimitReachedApiError
import ch.protonmail.android.mailcontact.domain.model.DecryptedContact
import me.proton.core.contact.domain.repository.ContactRepository
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

class CreateContact @Inject constructor(
    private val contactRepository: ContactRepository,
    private val encryptAndSignContactCards: EncryptAndSignContactCards
) {

    suspend operator fun invoke(userId: UserId, decryptedContact: DecryptedContact): Either<CreateContactErrors, Unit> {
        val contactCards = encryptAndSignContactCards.invoke(userId, decryptedContact).getOrElse {
            return CreateContactErrors.FailedToEncryptAndSignContactCards.left()
        }
        return Either.catch {
            contactRepository.createContact(userId, contactCards)
        }.mapLeft {
            if (it.isContactLimitReachedApiError()) {
                CreateContactErrors.MaximumNumberOfContactsReached
            } else CreateContactErrors.FailedToCreateContact
        }
    }

    sealed interface CreateContactErrors {
        data object FailedToEncryptAndSignContactCards : CreateContactErrors
        data object FailedToCreateContact : CreateContactErrors
        data object MaximumNumberOfContactsReached : CreateContactErrors
    }
}
