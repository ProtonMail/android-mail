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

package ch.protonmail.android.mailconversation.domain.entity

import ch.protonmail.android.mailcommon.domain.model.AttachmentCount
import ch.protonmail.android.mailpagination.domain.entity.PageItem
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId

data class Conversation(
    override val userId: UserId,
    val conversationId: ConversationId,
    val contextLabelId: LabelId,
    override val order: Long,
    val labels: List<ConversationLabel>,
    val subject: String,
    val senders: List<Recipient>,
    val recipients: List<Recipient>,
    val expirationTime: Long,
    val numMessages: Int,
    val numUnread: Int,
    val numAttachments: Int,
    val attachmentCount: AttachmentCount
) : PageItem {
    private val contextLabel: ConversationLabel by lazy {
        labels.firstOrNull { it.labelId == contextLabelId } ?: labels.first()
    }
    override val id: String = conversationId.id
    override val time: Long by lazy { contextLabel.contextTime }
    override val size: Long by lazy { contextLabel.contextSize }
    override val read: Boolean by lazy { contextLabel.contextNumUnread == 0 }
    override val labelIds: List<LabelId> by lazy { labels.map { it.labelId } }
    override val keywords: String by lazy { subject + senders + recipients }
    val contextNumMessages: Int by lazy { contextLabel.contextNumMessages }
    val contextNumUnread: Int by lazy { contextLabel.contextNumUnread }
    val contextNumAttachments: Int by lazy { contextLabel.contextNumAttachments }
}
