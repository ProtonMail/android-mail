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

package ch.protonmail.android.mailconversation.data.mapper

import arrow.core.Either
import arrow.core.right
import ch.protonmail.android.mailattachments.data.mapper.getCalendarAttachmentCount
import ch.protonmail.android.mailattachments.data.mapper.toAttachmentMetadata
import ch.protonmail.android.mailattachments.domain.model.AttachmentCount
import ch.protonmail.android.mailcommon.data.mapper.LocalAttachmentDisposition
import ch.protonmail.android.mailcommon.data.mapper.LocalConversation
import ch.protonmail.android.mailcommon.data.mapper.LocalConversationId
import ch.protonmail.android.mailcommon.data.mapper.LocalHiddenMessagesBanner
import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailconversation.domain.entity.Conversation
import ch.protonmail.android.mailconversation.domain.entity.ConversationDetailEntryPoint
import ch.protonmail.android.mailconversation.domain.entity.ConversationError
import ch.protonmail.android.mailconversation.domain.entity.HiddenMessagesBanner
import ch.protonmail.android.maillabel.data.mapper.toExclusiveLocation
import ch.protonmail.android.maillabel.data.mapper.toLabel
import ch.protonmail.android.mailmessage.data.mapper.toAvatarInformation
import ch.protonmail.android.mailmessage.data.mapper.toMessage
import ch.protonmail.android.mailmessage.data.mapper.toMessageId
import ch.protonmail.android.mailmessage.data.mapper.toParticipant
import ch.protonmail.android.mailmessage.data.model.LocalConversationMessages
import ch.protonmail.android.mailmessage.domain.model.ConversationMessages
import ch.protonmail.android.mailsnooze.data.mapper.toSnoozeInformation
import uniffi.mail_uniffi.OpenConversationOrigin

fun LocalConversation.toConversation() = Conversation(
    conversationId = this.id.toConversationId(),
    order = this.displayOrder.toLong(),
    subject = this.subject,
    senders = this.senders.map { it.toParticipant() },
    recipients = this.recipients.map { it.toParticipant() },
    expirationTime = this.expirationTime.toLong(),
    numMessages = this.totalMessages.toInt(),
    numUnread = this.numUnread.toInt(),
    numAttachments = this.numAttachments.toInt(),
    attachmentCount = AttachmentCount(
        calendar = this.attachmentsMetadata.getCalendarAttachmentCount()
    ),
    attachments = this.attachmentsMetadata
        .filter { it.disposition == LocalAttachmentDisposition.ATTACHMENT }
        .map { it.toAttachmentMetadata() },
    isStarred = this.isStarred,
    time = time.toLong(),
    size = size.toLong(),
    customLabels = this.customLabels.map { it.toLabel() },
    avatarInformation = this.avatar.toAvatarInformation(),
    exclusiveLocation = this.locations.map { it.toExclusiveLocation() },
    snoozeInformation = this.toSnoozeInformation(),
    hiddenMessagesBanner = this.hiddenMessagesBanner?.toHiddenMessagesBanner()
)

private fun LocalConversationId.toConversationId(): ConversationId = ConversationId(this.value.toString())

fun LocalConversationMessages.toConversationMessagesWithMessageToOpen():
    Either<ConversationError, ConversationMessages> {
    val messages = messages.toList().map { it.toMessage() }

    return ConversationMessages(
        messages = messages,
        messageIdToOpen = messageIdToOpen?.toMessageId()
    ).right()
}

fun ConversationDetailEntryPoint.toOrigin() = when (this) {
    ConversationDetailEntryPoint.PushNotification -> OpenConversationOrigin.PUSH_NOTIFICATION
    ConversationDetailEntryPoint.Mailbox -> OpenConversationOrigin.DEFAULT
}

fun LocalHiddenMessagesBanner.toHiddenMessagesBanner() = when (this) {
    LocalHiddenMessagesBanner.CONTAINS_TRASHED_MESSAGES -> HiddenMessagesBanner.ContainsTrashedMessages
    LocalHiddenMessagesBanner.CONTAINS_NON_TRASHED_MESSAGES -> HiddenMessagesBanner.ContainsNonTrashedMessages
}
