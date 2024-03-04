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
import arrow.core.raise.either
import ch.protonmail.android.mailcontact.domain.repository.ContactGroupRepository
import kotlinx.coroutines.flow.first
import me.proton.core.contact.domain.entity.ContactEmailId
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId
import timber.log.Timber
import javax.inject.Inject

class EditContactGroupMembers @Inject constructor(
    private val observeContactGroup: ObserveContactGroup,
    private val contactGroupRepository: ContactGroupRepository
) {

    suspend operator fun invoke(
        userId: UserId,
        labelId: LabelId,
        contactEmailIds: Set<ContactEmailId>
    ): Either<EditContactGroupMembersError, Unit> = either {

        val contactGroup = observeContactGroup(userId, labelId).first().getOrElse {
            Timber.e("Error while observing contact group by id in EditContactGroupMembers")
            raise(EditContactGroupMembersError.ObservingContactGroup)
        }

        val groupContactEmailIds = contactGroup.members.map { it.id }.toSet()
        val contactEmailIdsToAdd = contactEmailIds.subtract(groupContactEmailIds)
        val contactEmailIdsToRemove = groupContactEmailIds.subtract(contactEmailIds)

        if (contactEmailIdsToAdd.isNotEmpty()) {
            contactGroupRepository.addContactEmailIdsToContactGroup(
                userId,
                labelId,
                contactEmailIdsToAdd
            ).onLeft {
                Timber.e("Error while adding Members in EditContactGroupMembers")
                raise(EditContactGroupMembersError.AddingContactsToGroup)
            }
        }

        if (contactEmailIdsToRemove.isNotEmpty()) {
            contactGroupRepository.removeContactEmailIdsFromContactGroup(
                userId,
                labelId,
                contactEmailIdsToRemove
            ).onLeft {
                Timber.e("Error while removing Members in EditContactGroupMembers")
                raise(EditContactGroupMembersError.RemovingContactsFromGroup)
            }
        }
    }

    sealed interface EditContactGroupMembersError {
        object ObservingContactGroup : EditContactGroupMembersError
        object AddingContactsToGroup : EditContactGroupMembersError
        object RemovingContactsFromGroup : EditContactGroupMembersError
    }

}
