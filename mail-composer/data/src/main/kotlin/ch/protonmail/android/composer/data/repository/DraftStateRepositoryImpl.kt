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
import arrow.core.raise.either
import arrow.core.getOrElse
import ch.protonmail.android.composer.data.local.DraftStateLocalDataSource
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailmessage.domain.model.DraftAction
import ch.protonmail.android.mailmessage.domain.model.DraftState
import ch.protonmail.android.mailmessage.domain.model.DraftSyncState
import ch.protonmail.android.mailmessage.domain.repository.DraftStateRepository
import ch.protonmail.android.mailmessage.domain.model.MessageId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

class DraftStateRepositoryImpl @Inject constructor(
    private val localDataSource: DraftStateLocalDataSource
) : DraftStateRepository {

    override fun observe(userId: UserId, messageId: MessageId): Flow<Either<DataError, DraftState>> =
        localDataSource.observe(userId, messageId)

    override fun observeAll(userId: UserId): Flow<List<DraftState>> = localDataSource.observeAll(userId)

    override suspend fun createOrUpdateLocalState(
        userId: UserId,
        messageId: MessageId,
        action: DraftAction
    ): Either<DataError, Unit> = either {
        val draftState = localDataSource.observe(userId, messageId).first().getOrElse {
            DraftState(userId, messageId, null, DraftSyncState.Local, action)
        }
        val updatedState = draftState.copy(state = DraftSyncState.Local)
        localDataSource.save(updatedState)
    }

    override suspend fun updateDraftSyncState(
        userId: UserId,
        messageId: MessageId,
        syncState: DraftSyncState
    ): Either<DataError, Unit> = either {
        val draftState = localDataSource.observe(userId, messageId).first().bind()
        localDataSource.save(draftState.copy(state = syncState))
    }

    override suspend fun deleteDraftState(userId: UserId, messageId: MessageId): Either<DataError, Unit> = either {
        val draftState = localDataSource.observe(userId, messageId).first().bind()
        localDataSource.delete(draftState)
    }

    override suspend fun updateApiMessageIdAndSetSyncedState(
        userId: UserId,
        messageId: MessageId,
        apiMessageId: MessageId
    ): Either<DataError, Unit> = either {
        val draftState = localDataSource.observe(userId, messageId).first().bind()
        val updatedState = draftState.copy(
            apiMessageId = apiMessageId,
            state = DraftSyncState.Synchronized
        )
        localDataSource.save(updatedState)
    }

}
