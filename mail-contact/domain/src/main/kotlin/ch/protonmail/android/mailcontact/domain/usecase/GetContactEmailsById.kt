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
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcontact.domain.model.GetContactError
import kotlinx.coroutines.flow.first
import me.proton.core.contact.domain.entity.ContactEmail
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

class GetContactEmailsById @Inject constructor(
    private val observeContacts: ObserveContacts
) {

    suspend operator fun invoke(
        userId: UserId,
        selectedContactEmailIds: List<String>
    ): Either<GetContactError, List<ContactEmail>> {
        return observeContacts(userId).first().getOrNull()?.flatMap { contact ->
            contact.contactEmails.mapNotNull { contactEmail ->
                contactEmail.takeIf { selectedContactEmailIds.contains(contactEmail.id.id) }
            }
        }?.right() ?: GetContactError.left()
    }
}
