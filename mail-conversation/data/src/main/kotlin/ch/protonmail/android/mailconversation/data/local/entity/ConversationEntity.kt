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

package ch.protonmail.android.mailconversation.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import ch.protonmail.android.mailconversation.domain.entity.ConversationId
import ch.protonmail.android.mailconversation.domain.entity.Recipient
import me.proton.core.domain.entity.UserId
import me.proton.core.user.data.entity.UserEntity

@Entity(
    primaryKeys = ["userId", "conversationId"],
    indices = [
        Index("userId"),
        Index("conversationId"),
    ],
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["userId"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        ),
    ]
)
data class ConversationEntity(
    val userId: UserId,
    val conversationId: ConversationId,
    val order: Long,
    val subject: String,
    val senders: List<Recipient>,
    val recipients: List<Recipient>,
    val expirationTime: Long,
    val numMessages: Int,
    val numUnread: Int,
    val numAttachments: Int,
)
