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
import ch.protonmail.android.composer.data.local.entity.toAttachmentStateEntity
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcomposer.domain.model.AttachmentState
import ch.protonmail.android.mailmessage.domain.model.AttachmentId
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

    override suspend fun save(state: AttachmentState): Either<DataError, Unit> {
        return Either.catch {
            attachmentStateDao.insertOrUpdate(state.toAttachmentStateEntity())
        }.mapLeft {
            Timber.e(it, "Unexpected error writing attachment state to DB")
            DataError.Local.Unknown
        }
    }

    override suspend fun delete(state: AttachmentState) {
        attachmentStateDao.delete(state.toAttachmentStateEntity())
    }
}
