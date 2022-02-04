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

package ch.protonmail.android.mailmessage.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import ch.protonmail.android.mailmessage.domain.entity.MessageId
import ch.protonmail.android.mailmessage.domain.entity.Sender
import kotlinx.serialization.json.JsonElement
import me.proton.core.domain.entity.UserId
import me.proton.core.user.data.entity.UserEntity

@Entity(
    primaryKeys = ["userId", "messageId"],
    indices = [
        Index("userId"),
        Index("messageId"),
    ],
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["userId"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        ),
        /*
        No foreign key for messageId.
        We want to keep MessageBodyEntity without MessageEntity.
        ForeignKey(
            entity = MessageEntity::class,
            parentColumns = ["userId", "messageId"],
            childColumns = ["userId", "messageId"],
            onDelete = ForeignKey.CASCADE
        ),*/
    ]
)
data class MessageBodyEntity(
    val userId: UserId,
    val messageId: MessageId,
    val body: String?, // If null -> file.
    val header: String,
    val parsedHeaders: Map<String, JsonElement>,
    val mimeType: String,
    val spamScore: String,
    val replyTo: Sender,
    val replyTos: List<Sender>,
    val unsubscribeMethods: List<UnsubscribeMethod>?,
)

data class UnsubscribeMethod(
    val httpClient: String?,
    val oneClick: String?,
    val mailTo: MailTo?,
)

data class MailTo(
    val toList: List<String>,
    val subject: String,
    val body: String,
)
