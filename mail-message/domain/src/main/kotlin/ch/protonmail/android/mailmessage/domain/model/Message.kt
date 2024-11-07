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

import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.mailpagination.domain.model.PageItem
import kotlinx.serialization.Serializable
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId
import me.proton.core.user.domain.entity.AddressId
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Serializable
data class MessageId(val id: String)

/**
 * @property expirationTime is epoch time in seconds.
 *  0 means no expiration time.
 *  @see expirationTimeOrNull
 */
data class Message(
    override val userId: UserId,
    val messageId: MessageId,
    val conversationId: ConversationId,
    override val time: Long,
    override val size: Long,
    override val order: Long,
    override val labelIds: List<LabelId>,
    val subject: String,
    val unread: Boolean,
    val sender: Sender,
    val toList: List<Recipient>,
    val ccList: List<Recipient>,
    val bccList: List<Recipient>,
    val expirationTime: Long,
    val isReplied: Boolean,
    val isRepliedAll: Boolean,
    val isForwarded: Boolean,
    val addressId: AddressId,
    val externalId: String?,
    val numAttachments: Int,
    val flags: Long,
    val attachmentCount: AttachmentCount
) : PageItem {

    override val id: String = messageId.id
    override val read: Boolean by lazy { !unread }
    override val keywords: String by lazy { subject + sender + toList + ccList + bccList }

    val allRecipients = toList + ccList + bccList
    val allRecipientsDeduplicated = allRecipients.toSet()

    fun expirationTimeOrNull(): Duration? = expirationTime.takeIf { it > 0 }?.seconds

    fun isDraft() = labelIds.any { it == SystemLabelId.AllDrafts.labelId }

    fun isSent() = labelIds.any { it == SystemLabelId.AllSent.labelId }

    fun isPhishing() = flags.and(FLAG_PHISHING_AUTO) == FLAG_PHISHING_AUTO

    fun isExpirationFrozen() = flags.and(FLAG_EXPIRATION_FROZEN) == FLAG_EXPIRATION_FROZEN

    companion object {

        const val FLAG_PHISHING_AUTO = 1L shl 30
        const val FLAG_EXPIRATION_FROZEN = 1L shl 32
    }
}
