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

package ch.protonmail.android.mailcomposer.domain.usecase

import java.time.Instant
import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.mailmessage.domain.model.AttachmentCount
import ch.protonmail.android.mailmessage.domain.model.Message
import ch.protonmail.android.mailmessage.domain.model.MessageBody
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.model.MessageWithBody
import ch.protonmail.android.mailmessage.domain.model.MimeType
import ch.protonmail.android.mailmessage.domain.model.Recipient
import ch.protonmail.android.mailmessage.domain.model.Sender
import me.proton.core.domain.entity.UserId
import me.proton.core.user.domain.entity.UserAddress
import me.proton.core.util.kotlin.EMPTY_STRING
import javax.inject.Inject

class CreateEmptyDraft @Inject constructor() {

    operator fun invoke(
        messageId: MessageId,
        userId: UserId,
        userAddress: UserAddress
    ) = MessageWithBody(
        message = Message(
            userId = userId,
            messageId = messageId,
            conversationId = ConversationId(EMPTY_STRING),
            order = 0,
            subject = EMPTY_STRING,
            unread = false,
            sender = Sender(userAddress.email, userAddress.displayName.orEmpty()),
            toList = emptyList(),
            ccList = emptyList(),
            bccList = emptyList(),
            time = Instant.now().epochSecond,
            size = 0L,
            expirationTime = 0L,
            isReplied = false,
            isRepliedAll = false,
            isForwarded = false,
            addressId = userAddress.addressId,
            externalId = null,
            numAttachments = 0,
            flags = 0L,
            attachmentCount = AttachmentCount(0),
            labelIds = listOf(
                SystemLabelId.Drafts.labelId,
                SystemLabelId.AllDrafts.labelId,
                SystemLabelId.AllMail.labelId
            )
        ),
        messageBody = MessageBody(
            userId = userId,
            messageId = messageId,
            body = EMPTY_STRING,
            header = EMPTY_STRING,
            attachments = emptyList(),
            mimeType = MimeType.PlainText,
            spamScore = EMPTY_STRING,
            replyTo = Recipient(
                address = userAddress.email,
                name = userAddress.displayName ?: EMPTY_STRING,
                group = null
            ),
            replyTos = emptyList(),
            unsubscribeMethods = null
        )
    )
}
