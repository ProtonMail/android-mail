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
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId
import me.proton.core.label.domain.entity.LabelType
import me.proton.core.label.domain.repository.LabelRepository
import javax.inject.Inject

class DeleteContactGroup @Inject constructor(
    private val labelRepository: LabelRepository
) {

    suspend operator fun invoke(userId: UserId, labelId: LabelId): Either<CreateContactGroupError, Unit> = either {
        Either.catch {
            labelRepository.deleteLabel(
                userId, LabelType.ContactGroup, labelId
            )
        }.getOrElse {
            raise(DeleteContactGroupError.RepositoryError(it))
        }
    }
}

sealed class DeleteContactGroupError {
    data class RepositoryError(val throwable: Throwable) : CreateContactGroupError()
}
