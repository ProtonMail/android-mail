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

package ch.protonmail.android.mailmessage.data.mapper

import ch.protonmail.android.mailmessage.data.local.entity.MailToEntity
import ch.protonmail.android.mailmessage.data.local.entity.MessageBodyEntity
import ch.protonmail.android.mailmessage.data.local.entity.MimeTypeEntity
import ch.protonmail.android.mailmessage.data.local.entity.UnsubscribeMethodsEntity
import ch.protonmail.android.mailmessage.data.local.relation.MessageWithBodyEntity
import ch.protonmail.android.mailmessage.domain.model.MailTo
import ch.protonmail.android.mailmessage.domain.model.Message
import ch.protonmail.android.mailmessage.domain.model.MessageAttachment
import ch.protonmail.android.mailmessage.domain.model.MessageBody
import ch.protonmail.android.mailmessage.domain.model.MessageWithBody
import ch.protonmail.android.mailmessage.domain.model.MimeType
import ch.protonmail.android.mailmessage.domain.model.UnsubscribeMethods
import javax.inject.Inject

class MessageWithBodyEntityMapper @Inject constructor() {

    fun toMessageWithBody(messageWithBodyEntity: MessageWithBodyEntity, attachments: List<MessageAttachment>) =
        with(messageWithBodyEntity) {
            MessageWithBody(
                message = with(message) {
                    Message(
                        userId = userId,
                        messageId = messageId,
                        conversationId = conversationId,
                        time = time,
                        size = size,
                        order = order,
                        labelIds = labelIds,
                        subject = subject,
                        unread = unread,
                        sender = sender,
                        toList = toList,
                        ccList = ccList,
                        bccList = bccList,
                        expirationTime = expirationTime,
                        isReplied = isReplied,
                        isRepliedAll = isRepliedAll,
                        isForwarded = isForwarded,
                        addressId = addressId,
                        externalId = externalId,
                        numAttachments = numAttachments,
                        flags = flags,
                        attachmentCount = attachmentCount.toDomainModel()
                    )
                },
                messageBody = with(messageBody) {
                    MessageBody(
                        userId = userId,
                        messageId = messageId,
                        body = body.orEmpty(),
                        header = header,
                        attachments = attachments,
                        mimeType = MimeType.valueOf(mimeType.name),
                        spamScore = spamScore,
                        replyTo = replyTo,
                        replyTos = replyTos,
                        unsubscribeMethods = unsubscribeMethodsEntity?.toDomainModel()
                    )
                }
            )
        }

    fun toMessageBodyEntity(messageBody: MessageBody) = with(messageBody) {
        MessageBodyEntity(
            userId = userId,
            messageId = messageId,
            body = body,
            header = header,
            mimeType = MimeTypeEntity.valueOf(mimeType.name),
            spamScore = spamScore,
            replyTo = replyTo,
            replyTos = replyTos,
            unsubscribeMethodsEntity = unsubscribeMethods?.toEntity()
        )
    }

    private fun UnsubscribeMethodsEntity.toDomainModel() =
        UnsubscribeMethods(this.httpClient, this.oneClick, this.mailToEntity?.toDomainModel())

    private fun MailToEntity.toDomainModel() = MailTo(this.toList, this.subject, this.body)

    private fun UnsubscribeMethods.toEntity() =
        UnsubscribeMethodsEntity(this.httpClient, this.oneClick, this.mailTo?.toEntity())

    private fun MailTo.toEntity() = MailToEntity(this.toList, this.subject, this.body)
}
