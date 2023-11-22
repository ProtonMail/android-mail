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

package ch.protonmail.android.composer.data.local

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailmessage.data.local.entity.toAttachmentStateEntity
import ch.protonmail.android.mailmessage.domain.model.AttachmentId
import ch.protonmail.android.mailmessage.domain.model.AttachmentState
import ch.protonmail.android.mailmessage.domain.model.MessageId
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import javax.inject.Inject

class AttachmentStateLocalDataSourceImpl @Inject constructor(
    draftStateDatabase: DraftStateDatabase
) : AttachmentStateLocalDataSource {

    private val attachmentStateDao = draftStateDatabase.attachmentStateDao()

    override suspend fun getAttachmentState(
        userId: UserId,
        messageId: MessageId,
        attachmentId: AttachmentId
    ): Either<DataError, AttachmentState> = attachmentStateDao.getAttachmentState(userId, messageId, attachmentId)
        .let { attachmentState -> attachmentState?.toAttachmentState()?.right() ?: DataError.Local.NoDataCached.left() }

    override suspend fun getAllAttachmentStatesForMessage(userId: UserId, messageId: MessageId): List<AttachmentState> =
        attachmentStateDao.getAllAttachmentStatesForMessage(userId, messageId)
            .map { attachmentStateEntity -> attachmentStateEntity.toAttachmentState() }

    override suspend fun createOrUpdate(state: AttachmentState) = createOrUpdate(listOf(state))

    override suspend fun createOrUpdate(states: List<AttachmentState>): Either<DataError, Unit> {
        return Either.catch {
            val mappedStates = states.map { it.toAttachmentStateEntity() }
            attachmentStateDao.insertOrUpdate(*mappedStates.toTypedArray())
        }.mapLeft {
            Timber.e(it, "Unexpected error writing attachment states to DB")
            DataError.Local.Unknown
        }
    }

    override suspend fun delete(state: AttachmentState) {
        attachmentStateDao.delete(state.toAttachmentStateEntity())
    }
}
