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

package ch.protonmail.android.composer.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import ch.protonmail.android.mailcomposer.domain.model.MessageExpirationTime
import ch.protonmail.android.mailmessage.data.local.entity.MessageEntity
import ch.protonmail.android.mailmessage.domain.model.MessageId
import me.proton.core.domain.entity.UserId
import me.proton.core.user.data.entity.UserEntity
import kotlin.time.Duration.Companion.seconds

@Entity(
    primaryKeys = ["userId", "messageId"],
    indices = [
        Index("userId"),
        Index("userId", "messageId")
    ],
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["userId"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = MessageEntity::class,
            parentColumns = ["userId", "messageId"],
            childColumns = ["userId", "messageId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ]
)
data class MessageExpirationTimeEntity(
    val userId: UserId,
    val messageId: MessageId,
    val expiresInSeconds: Long
) {
    fun toDomainModel() = MessageExpirationTime(userId, messageId, expiresInSeconds.seconds)
}

fun MessageExpirationTime.toEntity() = MessageExpirationTimeEntity(userId, messageId, expiresIn.inWholeSeconds)
