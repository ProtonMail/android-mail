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

package ch.protonmail.android.mailcommon.domain.usecase

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.UndoableOperation
import javax.inject.Inject

class UndoLastOperation @Inject constructor(
    private val getUndoableOperation: GetUndoableOperation
) {

    suspend operator fun invoke(): Either<Error, Unit> {
        val operation = getUndoableOperation() ?: return Error.NoOperationToUndo.left()

        when (operation) {
            is UndoableOperation.MoveConversations -> Error.NotImplemented.left()
            is UndoableOperation.MoveMessages -> Error.NotImplemented.left()
        }

        return Unit.right()
    }

    sealed interface Error {
        data object NoOperationToUndo : Error
        data object NotImplemented : Error
    }
}
