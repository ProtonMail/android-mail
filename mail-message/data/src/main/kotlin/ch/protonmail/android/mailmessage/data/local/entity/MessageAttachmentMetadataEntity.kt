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

import android.net.Uri
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import ch.protonmail.android.mailmessage.domain.entity.AttachmentId
import ch.protonmail.android.mailmessage.domain.entity.AttachmentWorkerStatus
import ch.protonmail.android.mailmessage.domain.entity.MessageId
import me.proton.core.domain.entity.UserId

@Entity(
    primaryKeys = ["userId", "messageId", "attachmentId"],
    indices = [
        Index("userId"),
        Index("messageId"),
        Index("attachmentId")
    ],
    foreignKeys = [
        ForeignKey(
            entity = MessageAttachmentEntity::class,
            parentColumns = ["userId", "messageId", "attachmentId"],
            childColumns = ["userId", "messageId", "attachmentId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class MessageAttachmentMetadataEntity(
    val userId: UserId,
    val messageId: MessageId,
    val attachmentId: AttachmentId,
    val uri: Uri?,
    val status: AttachmentWorkerStatus
)
