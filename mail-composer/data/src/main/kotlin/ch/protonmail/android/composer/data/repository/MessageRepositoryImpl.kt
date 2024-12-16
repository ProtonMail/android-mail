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

import androidx.work.ExistingWorkPolicy
import arrow.core.Either
import arrow.core.right
import ch.protonmail.android.composer.data.remote.SendMessageWorker
import ch.protonmail.android.composer.data.remote.UploadAttachmentsWorker
import ch.protonmail.android.composer.data.remote.UploadDraftWorker
import ch.protonmail.android.mailcommon.data.worker.Enqueuer
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcomposer.domain.repository.MessageRepository
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.mailmessage.data.local.MessageLocalDataSource
import ch.protonmail.android.mailmessage.domain.model.MessageId
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import javax.inject.Inject

class MessageRepositoryImpl @Inject constructor(
    private val messageLocalDataSource: MessageLocalDataSource,
    private val enqueuer: Enqueuer
) : MessageRepository {

    override suspend fun send(userId: UserId, messageId: MessageId) {
        Timber.d("MessageRepository send $messageId")

        enqueuer.enqueueInChain<UploadDraftWorker, UploadAttachmentsWorker, SendMessageWorker>(
            userId = userId,
            uniqueWorkId = UploadDraftWorker.sendId(messageId),
            params1 = UploadDraftWorker.params(userId, messageId),
            params2 = UploadAttachmentsWorker.params(userId, messageId),
            params3 = SendMessageWorker.params(userId, messageId),
            existingWorkPolicy = ExistingWorkPolicy.KEEP
        )
    }

    override suspend fun moveMessageFromDraftsToSent(userId: UserId, messageId: MessageId): Either<DataError, Unit> {
        // optimistically move message to "Sent folder", but only in local DB (for the time of sending)
        return messageLocalDataSource.relabelMessages(
            userId,
            listOf(messageId),
            labelIdsToAdd = setOf(SystemLabelId.Sent.labelId, SystemLabelId.AllSent.labelId),
            labelIdsToRemove = setOf(SystemLabelId.Drafts.labelId, SystemLabelId.AllDrafts.labelId)
        ).map { Unit.right() }
    }

    override suspend fun moveMessageBackFromSentToDrafts(userId: UserId, messageId: MessageId) {

        // move message back from "Sent folder" to "Drafts", but only in local DB (to rollback the optimistic move
        // to "Sent")
        messageLocalDataSource.relabelMessages(
            userId,
            listOf(messageId),
            labelIdsToAdd = setOf(SystemLabelId.Drafts.labelId, SystemLabelId.AllDrafts.labelId),
            labelIdsToRemove = setOf(SystemLabelId.Sent.labelId, SystemLabelId.AllSent.labelId)
        )
    }
}
