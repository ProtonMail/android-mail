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
import ch.protonmail.android.mailmessage.data.local.entity.UnsubscribeMethodsEntity
import ch.protonmail.android.mailmessage.data.local.relation.MessageWithBodyEntity
import ch.protonmail.android.mailmessage.domain.entity.MailTo
import ch.protonmail.android.mailmessage.domain.entity.Message
import ch.protonmail.android.mailmessage.domain.entity.MessageBody
import ch.protonmail.android.mailmessage.domain.entity.MessageWithBody
import ch.protonmail.android.mailmessage.domain.entity.UnsubscribeMethods
import javax.inject.Inject

class MessageWithBodyEntityMapper @Inject constructor() {

    fun toMessageWithBody(messageWithBodyEntity: MessageWithBodyEntity) = with(messageWithBodyEntity) {
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
                    attachments = emptyList(), // We don't save the attachments to DB yet
                    mimeType = mimeType,
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
            mimeType = mimeType,
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
