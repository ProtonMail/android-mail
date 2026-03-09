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

package ch.protonmail.android.mailmessage.data.local

import arrow.core.Either
import ch.protonmail.android.mailcommon.data.mapper.LocalAttachmentData
import ch.protonmail.android.mailcommon.data.mapper.LocalMessageId
import ch.protonmail.android.mailcommon.data.mapper.LocalPrivacyLock
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailmessage.domain.model.AttachmentDataError
import ch.protonmail.android.mailmessage.domain.model.MessageBody
import ch.protonmail.android.mailmessage.domain.model.MessageBodyTransformations
import ch.protonmail.android.mailmessage.domain.model.RawMessageData
import me.proton.core.domain.entity.UserId
import uniffi.mail_uniffi.ImagePolicy

interface MessageBodyDataSource {

    suspend fun getMessageBody(
        userId: UserId,
        messageId: LocalMessageId,
        transformations: MessageBodyTransformations
    ): Either<DataError, MessageBody>

    suspend fun loadImage(
        userId: UserId,
        messageId: LocalMessageId,
        url: String,
        imagePolicy: ImagePolicy
    ): Either<AttachmentDataError, LocalAttachmentData>

    suspend fun getRawHeaders(userId: UserId, messageId: LocalMessageId): Either<DataError, RawMessageData>

    suspend fun getRawBody(userId: UserId, messageId: LocalMessageId): Either<DataError, RawMessageData>

    suspend fun getPrivacyLock(userId: UserId, messageId: LocalMessageId): Either<DataError, LocalPrivacyLock?>

    suspend fun unsubscribeFromNewsletter(userId: UserId, messageId: LocalMessageId): Either<DataError, Unit>
}
