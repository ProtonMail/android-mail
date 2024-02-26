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

package ch.protonmail.android.mailmessage.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.model.Recipient
import ch.protonmail.android.mailmessage.domain.model.Sender
import me.proton.core.domain.entity.UserId
import me.proton.core.user.data.entity.UserEntity
import me.proton.core.user.domain.entity.AddressId

@Entity(
    primaryKeys = ["userId", "messageId"],
    indices = [
        Index("userId"),
        Index("messageId")
    ],
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["userId"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class MessageEntity(
    val userId: UserId,
    val messageId: MessageId,
    val conversationId: ConversationId,
    val order: Long,
    val subject: String,
    val unread: Boolean,
    @Embedded(prefix = "sender_")
    val sender: Sender,
    val toList: List<Recipient>,
    val ccList: List<Recipient>,
    val bccList: List<Recipient>,
    val time: Long,
    val size: Long,
    val expirationTime: Long,
    val isReplied: Boolean,
    val isRepliedAll: Boolean,
    val isForwarded: Boolean,
    val addressId: AddressId,
    val externalId: String?,
    val numAttachments: Int,
    /* Bitmap of message flags:
     * Received = 1 (2^0)
     * Sent = 2 (2^1)
     * Internal = 4 (2^2)x
     * E2E = 8 (2^3)
     * Auto = 16 (2^4)
     * Replied = 32 (2^5)
     * RepliedAll = 64 (2^6)
     * Forwarded = 128 (2^7)
     * Auto replied = 256 (2^8)
     * Imported = 512 (2^9)
     * Opened = 1024 (2^10)
     * Receipt Sent = 2048 (2^11)
     * Notified = 4096 (2^12)
     * Touched = 8192 (2^13)
     * Receipt = 16384 (2^14)
     * Proton = 32768 (2^15)
     * Receipt request = 65536 (2^16)
     * Public key = 131072 (2^17)
     * Sign = 262144 (2^18)
     * Unsubscribed = 524288 (2^19)
     * SPF fail = 16777216 (2^24)
     * DKIM fail = 33554432 (2^25)
     * DMARC fail = 67108864 (2^26)
     * Ham manual = 134217728 (2^27)
     * Spam auto = 268435456 (2^28)
     * Spam manual = 536870912 (2^29)
     * Phishing auto = 1073741824 (2^30)
     * Phishing manual = 2147483648 (2^31)
     */
    val flags: Long,
    val attachmentCount: AttachmentCountEntity
)

