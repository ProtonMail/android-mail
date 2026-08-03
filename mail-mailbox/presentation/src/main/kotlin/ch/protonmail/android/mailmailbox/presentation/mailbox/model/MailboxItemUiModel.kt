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

import androidx.compose.runtime.Immutable
import ch.protonmail.android.mailattachments.presentation.model.AttachmentMetadataUiModel
import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailcommon.presentation.model.AvatarUiModel
import ch.protonmail.android.mailcommon.presentation.model.SelectableMailItem
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.maillabel.presentation.model.LabelUiModel
import ch.protonmail.android.mailmailbox.domain.model.MailboxItemType
import ch.protonmail.android.mailsnooze.presentation.model.SnoozeStatusUiModel
import kotlinx.collections.immutable.ImmutableList

@Immutable
data class MailboxItemUiModel(
    val avatar: AvatarUiModel,
    val type: MailboxItemType,
    val userId: String,
    override val id: String,
    val conversationId: ConversationId,
    val time: TextUiModel,
    override val isRead: Boolean,
    val labels: ImmutableList<LabelUiModel>,
    val subject: String,
    val participants: ParticipantsUiModel,
    val shouldShowRepliedIcon: Boolean,
    val shouldShowRepliedAllIcon: Boolean,
    val shouldShowForwardedIcon: Boolean,
    val numMessages: Int?,
    override val isStarred: Boolean,
    val locations: ImmutableList<MailboxItemLocationUiModel>,
    val attachments: ImmutableList<AttachmentMetadataUiModel>,
    val expiryInformation: ExpiryInformationUiModel,
    val shouldShowAttachmentIcon: Boolean,
    val shouldShowCalendarIcon: Boolean,
    val shouldOpenInComposer: Boolean,
    val shouldShowScheduleSendTime: Boolean,
    val displaySnoozeReminder: Boolean,
    val snoozedUntil: SnoozeStatusUiModel
) : SelectableMailItem
