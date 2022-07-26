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

package ch.protonmail.android.mailconversation.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import ch.protonmail.android.mailcommon.domain.model.ConversationId
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId
import me.proton.core.user.data.entity.UserEntity

@Entity(
    primaryKeys = ["userId", "conversationId", "labelId"],
    indices = [
        Index("userId"),
        Index("labelId"),
        Index("conversationId"),
        // Index("userId", "labelId"), // LabelEntity foreign key.
        Index("userId", "conversationId") // ConversationEntity foreign key.
    ],
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["userId"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        ),
        /*
        ForeignKey(
            entity = LabelEntity::class,
            parentColumns = ["userId", "labelId"],
            childColumns = ["userId", "labelId"],
            onDelete = ForeignKey.CASCADE
        ),*/
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["userId", "conversationId"],
            childColumns = ["userId", "conversationId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ConversationLabelEntity(
    val userId: UserId,
    val conversationId: ConversationId,
    val labelId: LabelId,
    val contextTime: Long,
    val contextSize: Long,
    val contextNumMessages: Int,
    val contextNumUnread: Int,
    val contextNumAttachments: Int
)
