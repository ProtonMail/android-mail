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
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelType
import me.proton.core.label.domain.entity.NewLabel
import arrow.core.raise.either
import me.proton.core.contact.domain.entity.ContactEmailId
import me.proton.core.label.domain.repository.LabelLocalDataSource
import me.proton.core.label.domain.repository.LabelRemoteDataSource
import javax.inject.Inject

class CreateContactGroup @Inject constructor(
    private val labelRemoteDataSource: LabelRemoteDataSource,
    private val labelLocalDataSource: LabelLocalDataSource,
    private val editContactGroupMembers: EditContactGroupMembers
) {

    suspend operator fun invoke(
        userId: UserId,
        name: String,
        color: String,
        contactEmailIds: List<ContactEmailId>
    ): Either<CreateContactGroupError, Unit> = either {

        val label = NewLabel(
            name = name,
            color = color,
            isNotified = null,
            isExpanded = null,
            isSticky = null,
            parentId = null,
            type = LabelType.ContactGroup
        )

        val createdLabel = Either.catch {
            val createdLabel = labelRemoteDataSource.createLabel(userId, label)
            labelLocalDataSource.upsertLabel(listOf(createdLabel))
            createdLabel
        }.getOrNull() ?: raise(CreateContactGroupError.CreatingLabelError)

        return editContactGroupMembers(
            userId,
            createdLabel.labelId,
            contactEmailIds.toSet()
        ).mapLeft {
            CreateContactGroupError.EditingMembersError
        }
    }
}

sealed class CreateContactGroupError {
    object CreatingLabelError : CreateContactGroupError()
    object EditingMembersError : CreateContactGroupError()
}
