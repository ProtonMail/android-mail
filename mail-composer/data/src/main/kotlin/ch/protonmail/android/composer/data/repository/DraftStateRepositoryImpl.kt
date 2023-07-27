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

package ch.protonmail.android.composer.data.repository

import arrow.core.Either
import arrow.core.continuations.either
import ch.protonmail.android.composer.data.local.DraftStateLocalDataSource
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcomposer.domain.model.DraftState
import ch.protonmail.android.mailcomposer.domain.model.DraftSyncState
import ch.protonmail.android.mailcomposer.domain.repository.DraftStateRepository
import ch.protonmail.android.mailmessage.domain.entity.MessageId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

class DraftStateRepositoryImpl @Inject constructor(
    private val localDataSource: DraftStateLocalDataSource
) : DraftStateRepository {

    override suspend fun observe(userId: UserId, messageId: MessageId): Flow<Either<DataError, DraftState>> =
        localDataSource.observe(userId, messageId)

    override suspend fun saveCreatedState(
        userId: UserId,
        messageId: MessageId,
        remoteDraftId: MessageId
    ): Either<DataError, Unit> = either {
        val draftState = localDataSource.observe(userId, messageId).first().bind()
        val updatedState = draftState.copy(
            messageId = remoteDraftId,
            apiMessageId = remoteDraftId,
            state = DraftSyncState.Synchronized
        )
        localDataSource.save(updatedState)
    }

}
