/*
 * Copyright (c) 2021 Proton Technologies AG
 * This file is part of Proton Technologies AG and ProtonMail.
 *
 * ProtonMail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ProtonMail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ProtonMail.  If not, see <https://www.gnu.org/licenses/>.
 */

package ch.protonmail.android.mailmessage.domain.entity

import ch.protonmail.android.mailpagination.domain.entity.PageItem
import ch.protonmail.android.mailconversation.domain.ConversationId
import kotlinx.serialization.Serializable
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId
import me.proton.core.user.domain.entity.AddressId

data class MessageId(val id: String)

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
    val flags: Int,
) : PageItem {
    override val id: String = messageId.id
    override val read: Boolean = !unread
    override val keywords: String = subject + sender + toList + ccList + bccList
}

@Serializable
data class Sender(
    val address: String,
    val name: String,
)

@Serializable
data class Recipient(
    val address: String,
    val name: String,
    val group: String,
)
