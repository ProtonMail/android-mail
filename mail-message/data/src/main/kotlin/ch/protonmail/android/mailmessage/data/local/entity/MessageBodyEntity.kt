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

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.model.Recipient
import kotlinx.serialization.Serializable
import me.proton.core.domain.entity.UserId
import me.proton.core.user.data.entity.UserEntity

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
        ),
        /*
        Update cascade as we want messageId to be updated when updated on messageEntity (local draft becoming remote)
         */
        ForeignKey(
            entity = MessageEntity::class,
            parentColumns = ["userId", "messageId"],
            childColumns = ["userId", "messageId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ]
)
data class MessageBodyEntity(
    val userId: UserId,
    val messageId: MessageId,
    val body: String?, // If null -> file.
    val header: String,
    val mimeType: MimeTypeEntity,
    val spamScore: String,
    val replyTo: Recipient,
    val replyTos: List<Recipient>,
    val unsubscribeMethodsEntity: UnsubscribeMethodsEntity?
)

enum class MimeTypeEntity(val value: String) {
    PlainText("text/plain"),
    Html("text/html"),
    MultipartMixed("multipart/mixed");

    companion object {
        fun from(value: String) = values().find { it.value == value } ?: PlainText
    }
}

@Serializable
data class UnsubscribeMethodsEntity(
    val httpClient: String?,
    val oneClick: String?,
    val mailToEntity: MailToEntity?
)

@Serializable
data class MailToEntity(
    val toList: List<String>,
    val subject: String?,
    val body: String?
)
