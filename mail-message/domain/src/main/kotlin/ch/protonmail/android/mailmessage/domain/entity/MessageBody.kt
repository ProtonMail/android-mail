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

import ch.protonmail.android.mailconversation.domain.entity.Recipient
import kotlinx.serialization.json.JsonElement
import me.proton.core.domain.entity.UserId

data class MessageBody(
    val userId: UserId,
    val messageId: MessageId,
    val body: String,
    val header: String,
    val parsedHeaders: Map<String, JsonElement>,
    val attachments: List<MessageAttachment>,
    val mimeType: String,
    val spamScore: String,
    val replyTo: Recipient,
    val replyTos: List<Recipient>,
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
