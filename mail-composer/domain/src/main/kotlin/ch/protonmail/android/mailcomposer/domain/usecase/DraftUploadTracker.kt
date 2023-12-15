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

import java.util.concurrent.ConcurrentHashMap
import ch.protonmail.android.mailmessage.domain.model.DraftSyncState
import ch.protonmail.android.mailmessage.domain.model.Message
import ch.protonmail.android.mailmessage.domain.model.MessageBody
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.model.MessageWithBody
import ch.protonmail.android.mailmessage.domain.model.Recipient
import ch.protonmail.android.mailmessage.domain.repository.DraftStateRepository
import kotlinx.coroutines.flow.firstOrNull
import me.proton.core.domain.entity.UserId
import javax.inject.Inject
import javax.inject.Singleton

@Suppress("UnnecessaryParentheses", "ComplexMethod")
@Singleton
class DraftUploadTracker @Inject constructor(
    private val findLocalDraft: FindLocalDraft,
    private val draftStateRepository: DraftStateRepository
) {

    private val lastUploadedDrafts: MutableMap<MessageId, MessageWithBody> = ConcurrentHashMap()

    suspend fun uploadRequired(userId: UserId, messageId: MessageId): Boolean {

        // Upload may be skipped only in Synchronised state
        draftStateRepository.observe(userId, messageId).firstOrNull()
            ?.takeIf { draftState ->
                draftState.fold(ifRight = { it.state != DraftSyncState.Synchronized }, ifLeft = { true })
            }
            ?.let { return true }

        return lastUploadedDrafts[messageId]?.let { lastUploadedDraft ->
            findLocalDraft(userId, messageId)?.let { localDraft ->
                !localDraft.equalTo(lastUploadedDraft)
            } ?: true // If localDraft is null, upload is required
        } ?: true // If lastSyncedRemoteCopy is null, upload is required
    }

    private fun MessageWithBody.equalTo(other: MessageWithBody): Boolean = this.message.equalTo(other.message) &&
        this.messageBody.equalTo(other.messageBody)

    // Following fields are not compared since they are not related to message content
    // [isReplied] / [isRepliedAll] / [isForwarded] / [unread] / [read] / [time]
    //
    // [id] / [keywords] are not compared since it is calculated from other fields
    private fun Message.equalTo(other: Message): Boolean = this.userId == other.userId &&
        this.messageId == other.messageId &&
        this.subject == other.subject &&
        this.sender.equalTo(other.sender) &&
        this.toList.equalTo(other.toList) &&
        this.ccList.equalTo(other.ccList) &&
        this.bccList.equalTo(other.bccList) &&
        this.order == other.order &&
        this.flags == other.flags &&
        this.labelIds == other.labelIds &&
        this.size == other.size &&
        this.conversationId == other.conversationId &&
        this.expirationTime == other.expirationTime &&
        this.addressId == other.addressId &&
        this.externalId == other.externalId &&
        this.numAttachments == other.numAttachments &&
        this.attachmentCount == other.attachmentCount

    // Following fields are not compared since they do not have any effect on the message content
    // [replyTo] / [replyTos] / [unsubscribeMethods]
    private fun MessageBody.equalTo(other: MessageBody): Boolean = this.messageId == other.messageId &&
        this.userId == other.userId &&
        this.body == other.body &&
        this.spamScore == other.spamScore &&
        this.header == other.header &&
        this.attachments == other.attachments &&
        this.mimeType == other.mimeType &&
        this.replyTo.equalTo(other.replyTo)

    private fun List<Recipient>.equalTo(other: List<Recipient>): Boolean =
        this.size == other.size && this.all { recipient1 ->
            other.any { recipient2 -> recipient1.equalTo(recipient2) }
        }

    private fun Recipient.equalTo(other: Recipient): Boolean = this.address == other.address &&
        this.name == other.name &&
        this.isProton == other.isProton &&
        this.group.equalTo(other.group)

    private fun String?.equalTo(other: String?): Boolean =
        (this.isNullOrEmpty() && other.isNullOrEmpty()) || this == other

    fun notifyUploadedDraft(messageId: MessageId, messageWithBody: MessageWithBody) {
        lastUploadedDrafts[messageId] = messageWithBody
    }

    fun notifySentMessages(sentMessageList: Set<MessageId>) {
        lastUploadedDrafts.keys.removeAll(sentMessageList.toSet())
    }
}
