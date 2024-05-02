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
import ch.protonmail.android.mailcontact.domain.mapper.isAlreadyExistsApiError
import ch.protonmail.android.maillabel.domain.model.ColorRgbHex
import me.proton.core.contact.domain.entity.ContactEmailId
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelType
import me.proton.core.label.domain.entity.NewLabel
import me.proton.core.label.domain.repository.LabelRepository
import javax.inject.Inject

class CreateContactGroup @Inject constructor(
    private val labelRepository: LabelRepository,
    private val editContactGroupMembers: EditContactGroupMembers
) {

    suspend operator fun invoke(
        userId: UserId,
        name: String,
        color: ColorRgbHex,
        contactEmailIds: List<ContactEmailId>
    ): Either<CreateContactGroupError, Unit> = either {

        val label = NewLabel(
            name = name.trim(),
            color = color.hex,
            isNotified = null,
            isExpanded = null,
            isSticky = null,
            parentId = null,
            type = LabelType.ContactGroup
        )

        runCatching {
            labelRepository.createLabel(userId, label)
        }.getOrElse {
            if (it.isAlreadyExistsApiError()) {
                raise(CreateContactGroupError.GroupNameDuplicate)
            } else raise(CreateContactGroupError.CreatingLabelError)
        }

        val createdLabel = labelRepository.getLabels(userId, LabelType.ContactGroup, refresh = true).find {
            it.name == label.name
        } ?: raise(CreateContactGroupError.CreatingLabelError)

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
    object GroupNameDuplicate : CreateContactGroupError()
    object EditingMembersError : CreateContactGroupError()
}
