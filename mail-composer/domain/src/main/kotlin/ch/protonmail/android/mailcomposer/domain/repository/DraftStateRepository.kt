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

package ch.protonmail.android.mailcomposer.domain.repository

import arrow.core.Either
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcomposer.domain.model.DraftAction
import ch.protonmail.android.mailcomposer.domain.model.DraftState
import ch.protonmail.android.mailmessage.domain.entity.MessageId
import kotlinx.coroutines.flow.Flow
import me.proton.core.domain.entity.UserId

interface DraftStateRepository {

    suspend fun observe(userId: UserId, messageId: MessageId): Flow<Either<DataError, DraftState>>

    suspend fun saveSyncedState(
        userId: UserId,
        messageId: MessageId,
        remoteDraftId: MessageId
    ): Either<DataError, Unit>

    suspend fun saveLocalState(
        userId: UserId,
        messageId: MessageId,
        action: DraftAction
    ): Either<DataError, Unit>
}
