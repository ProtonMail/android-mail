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
import ch.protonmail.android.mailcontact.domain.model.ContactGroup
import ch.protonmail.android.maillabel.domain.usecase.ObserveLabels
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import me.proton.core.contact.domain.entity.ContactEmail
import me.proton.core.contact.domain.repository.ContactRepository
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId
import me.proton.core.label.domain.entity.LabelType
import javax.inject.Inject

class ObserveContactGroup @Inject constructor(
    private val observeLabels: ObserveLabels,
    private val contactRepository: ContactRepository
) {

    operator fun invoke(userId: UserId, labelId: LabelId): Flow<Either<GetContactGroupError, ContactGroup>> {
        return combine(
            observeLabels(userId, LabelType.ContactGroup),
            contactRepository.observeAllContacts(userId).mapToEither()
        ) { labels, contacts ->
            either {
                val label = labels.getOrNull()?.firstOrNull {
                    it.labelId == labelId
                } ?: raise(GetContactGroupError.GetLabelsError)

                val contactGroupMembers = arrayListOf<ContactEmail>()
                contacts.getOrNull()?.forEach { contact ->
                    contact.contactEmails.forEach { contactEmail ->
                        if (contactEmail.labelIds.contains(labelId.id)) contactGroupMembers.add(contactEmail)
                    }
                } ?: raise(GetContactGroupError.GetContactsError)

                ContactGroup(
                    userId = label.userId,
                    labelId = labelId,
                    name = label.name,
                    color = label.color,
                    members = contactGroupMembers
                )
            }
        }
    }
}

sealed interface GetContactGroupError {
    object GetLabelsError : GetContactGroupError
    object GetContactsError : GetContactGroupError
}
