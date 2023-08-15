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

import me.proton.core.domain.entity.UserId

data class MessageBody(
    val userId: UserId,
    val messageId: MessageId,
    val body: String,
    val header: String,
    val attachments: List<MessageAttachment>,
    val mimeType: MimeType,
    val spamScore: String,
    val replyTo: Recipient,
    val replyTos: List<Recipient>,
    val unsubscribeMethods: UnsubscribeMethods?
)

enum class MimeType(val value: String) {
    PlainText("text/plain"),
    Html("text/html"),
    MultipartMixed("multipart/mixed");

    companion object {
        fun from(value: String) = values().find { it.value == value } ?: PlainText
    }
}

data class UnsubscribeMethods(
    val httpClient: String?,
    val oneClick: String?,
    val mailTo: MailTo?
)

data class MailTo(
    val toList: List<String>,
    val subject: String?,
    val body: String?
)
