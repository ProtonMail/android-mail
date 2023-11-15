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

package ch.protonmail.android.mailmessage.data.remote

import arrow.core.Either
import ch.protonmail.android.mailcommon.data.mapper.toEither
import ch.protonmail.android.mailcommon.data.worker.Enqueuer
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailmessage.data.remote.worker.GetAttachmentWorker
import ch.protonmail.android.mailmessage.domain.model.AttachmentId
import ch.protonmail.android.mailmessage.domain.model.MessageId
import me.proton.core.domain.entity.UserId
import me.proton.core.network.data.ApiProvider
import javax.inject.Inject

class AttachmentRemoteDataSourceImpl @Inject constructor(
    private val apiProvider: ApiProvider,
    private val enqueuer: Enqueuer
) : AttachmentRemoteDataSource {

    override suspend fun enqueueGetAttachmentWorker(
        userId: UserId,
        messageId: MessageId,
        attachmentId: AttachmentId
    ) {
        enqueuer.enqueueUniqueWork<GetAttachmentWorker>(
            userId = userId,
            workerId = attachmentId.id,
            params = GetAttachmentWorker.params(userId, messageId, attachmentId),
            constraints = null
        )
    }

    override suspend fun getAttachment(
        userId: UserId,
        messageId: MessageId,
        attachmentId: AttachmentId
    ): Either<DataError.Remote, ByteArray> {
        return apiProvider.get<AttachmentApi>(userId).invoke {
            getAttachment(attachmentId = attachmentId.id).bytes()
        }.toEither()
    }
}
