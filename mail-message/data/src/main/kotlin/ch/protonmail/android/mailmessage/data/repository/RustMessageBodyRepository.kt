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

package ch.protonmail.android.mailmessage.data.repository

import arrow.core.Either
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailmessage.data.local.MessageBodyDataSource
import ch.protonmail.android.mailmessage.data.mapper.toLocalMessageId
import ch.protonmail.android.mailmessage.domain.model.AttachmentDataError
import ch.protonmail.android.mailmessage.domain.model.MessageBody
import ch.protonmail.android.mailmessage.domain.model.MessageBodyImage
import ch.protonmail.android.mailmessage.domain.model.MessageBodyTransformations
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.model.RawMessageData
import ch.protonmail.android.mailmessage.domain.repository.MessageBodyRepository
import me.proton.core.domain.entity.UserId
import uniffi.mail_uniffi.ImagePolicy
import javax.inject.Inject

class RustMessageBodyRepository @Inject constructor(
    private val messageBodyDataSource: MessageBodyDataSource
) : MessageBodyRepository {

    override suspend fun getMessageBody(
        userId: UserId,
        messageId: MessageId,
        transformations: MessageBodyTransformations
    ): Either<DataError, MessageBody> =
        messageBodyDataSource.getMessageBody(userId, messageId.toLocalMessageId(), transformations)

    override suspend fun loadImage(
        userId: UserId,
        messageId: MessageId,
        url: String,
        shouldLoadImagesSafely: Boolean
    ): Either<AttachmentDataError, MessageBodyImage> {
        val imagePolicy = if (shouldLoadImagesSafely) ImagePolicy.SAFE else ImagePolicy.UNSAFE
        return messageBodyDataSource.loadImage(userId, messageId.toLocalMessageId(), url, imagePolicy)
            .map { localAttachmentData ->
                MessageBodyImage(localAttachmentData.data, localAttachmentData.mime)
            }
    }

    override suspend fun getRawHeaders(userId: UserId, messageId: MessageId): Either<DataError, RawMessageData> =
        messageBodyDataSource.getRawHeaders(userId, messageId.toLocalMessageId())

    override suspend fun getRawBody(userId: UserId, messageId: MessageId): Either<DataError, RawMessageData> =
        messageBodyDataSource.getRawBody(userId, messageId.toLocalMessageId())



    override suspend fun unsubscribeFromNewsletter(userId: UserId, messageId: MessageId): Either<DataError, Unit> =
        messageBodyDataSource.unsubscribeFromNewsletter(userId, messageId.toLocalMessageId())
}
