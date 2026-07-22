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

package ch.protonmail.android.mailmessage.domain.model

import ch.protonmail.android.mailattachments.domain.model.AttachmentCount
import ch.protonmail.android.mailattachments.domain.model.AttachmentMetadata
import ch.protonmail.android.mailcommon.domain.model.AvatarInformation
import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.maillabel.domain.model.CategoryLocation
import ch.protonmail.android.maillabel.domain.model.ExclusiveLocation
import ch.protonmail.android.maillabel.domain.model.Label
import ch.protonmail.android.mailsnooze.domain.model.ConversationSnoozeStatus
import kotlinx.serialization.Serializable
import me.proton.core.user.domain.entity.AddressId
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Serializable
data class MessageId(val id: String)

@Serializable
@JvmInline
value class RemoteMessageId(val id: String)

/**
 * @property expirationTime is epoch time in seconds.
 *  0 means no expiration time.
 *  @see expirationTimeOrNull
 */
data class Message(
    val messageId: MessageId,
    val conversationId: ConversationId,
    val time: Long,
    val size: Long,
    val order: Long,
    val subject: String,
    val isUnread: Boolean,
    val sender: Sender,
    val toList: List<Recipient>,
    val ccList: List<Recipient>,
    val bccList: List<Recipient>,
    val expirationTime: Long,
    val isReplied: Boolean,
    val isRepliedAll: Boolean,
    val isForwarded: Boolean,
    val isStarred: Boolean,
    val addressId: AddressId,
    val numAttachments: Int,
    val flags: Long,
    val attachmentCount: AttachmentCount,
    // Limited set of attachments (up to 10) for mailbox preview purposes
    val attachmentPreviews: List<AttachmentMetadata>,
    val customLabels: List<Label>,
    val avatarInformation: AvatarInformation,
    val exclusiveLocation: ExclusiveLocation,
    val category: CategoryLocation,
    val isDraft: Boolean,
    val isScheduled: Boolean,
    val isReplyAllowed: Boolean,
    val snoozeInformation: ConversationSnoozeStatus
) {
    val allRecipients = toList + ccList + bccList

    fun expirationTimeOrNull(): Duration? = expirationTime.takeIf { it > 0 }?.seconds

    fun isPhishingAuto() = flags.and(FLAG_PHISHING_AUTO) == FLAG_PHISHING_AUTO

    fun isHamManual() = flags.and(FLAG_HAM_MANUAL) == FLAG_HAM_MANUAL

    companion object {

        const val FLAG_PHISHING_AUTO = 1_073_741_824L
        const val FLAG_HAM_MANUAL = 134_217_728L
    }
}
