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

package ch.protonmail.android.mailmailbox.presentation.mailbox.model

import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.maillabel.presentation.model.MailboxItemLabelUiModel
import ch.protonmail.android.mailmailbox.domain.model.MailboxItemType
import ch.protonmail.android.mailcommon.presentation.model.AvatarUiModel
import me.proton.core.domain.entity.UserId

data class MailboxItemUiModel(
    val avatar: AvatarUiModel,
    val type: MailboxItemType,
    val id: String,
    val userId: UserId,
    val conversationId: ConversationId,
    val time: TextUiModel,
    val isRead: Boolean,
    val labels: List<MailboxItemLabelUiModel>,
    val subject: String,
    val participants: List<String>,
    val shouldShowRepliedIcon: Boolean,
    val shouldShowRepliedAllIcon: Boolean,
    val shouldShowForwardedIcon: Boolean,
    val numMessages: Int?,
    val showStar: Boolean,
    val locationIconResIds: List<Int>,
    val shouldShowAttachmentIcon: Boolean,
    val shouldShowExpirationLabel: Boolean,
    val shouldShowCalendarIcon: Boolean
)
