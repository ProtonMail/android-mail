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
import arrow.core.getOrElse
import ch.protonmail.android.composer.data.local.AttachmentStateLocalDataSource
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcomposer.domain.model.AttachmentState
import ch.protonmail.android.mailcomposer.domain.model.AttachmentSyncState
import ch.protonmail.android.mailcomposer.domain.repository.AttachmentStateRepository
import ch.protonmail.android.mailmessage.domain.model.AttachmentId
import ch.protonmail.android.mailmessage.domain.model.MessageId
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

class AttachmentStateRepositoryImpl @Inject constructor(
    private val localDataSource: AttachmentStateLocalDataSource
) : AttachmentStateRepository {

    override suspend fun getAttachmentState(
        userId: UserId,
        messageId: MessageId,
        attachmentId: AttachmentId
    ): Either<DataError, AttachmentState> = localDataSource.getAttachmentState(userId, messageId, attachmentId)

    override suspend fun getAllAttachmentStatesForMessage(userId: UserId, messageId: MessageId): List<AttachmentState> =
        localDataSource.getAllAttachmentStatesForMessage(userId, messageId)

    override suspend fun createOrUpdateLocalState(
        userId: UserId,
        messageId: MessageId,
        attachmentId: AttachmentId
    ): Either<DataError, Unit> = either {
        val attachmentState = localDataSource.getAttachmentState(userId, messageId, attachmentId).getOrElse {
            AttachmentState(userId, messageId, attachmentId, AttachmentSyncState.Local)
        }
        val updatedState = attachmentState.copy(state = AttachmentSyncState.Local)
        localDataSource.save(updatedState)
    }

    override suspend fun updateApiAttachmentIdAndSetSyncedState(
        userId: UserId,
        messageId: MessageId,
        attachmentId: AttachmentId,
        apiAttachmentId: AttachmentId
    ): Either<DataError, Unit> = either {
        val attachmentState = localDataSource.getAttachmentState(userId, messageId, attachmentId).bind()
        localDataSource.save(attachmentState.copy(state = AttachmentSyncState.Uploaded))
    }

    override suspend fun deleteAttachmentState(
        userId: UserId,
        messageId: MessageId,
        attachmentId: AttachmentId
    ): Either<DataError, Unit> = either {
        val attachmentState = localDataSource.getAttachmentState(userId, messageId, attachmentId).bind()
        localDataSource.delete(attachmentState)
    }
}
