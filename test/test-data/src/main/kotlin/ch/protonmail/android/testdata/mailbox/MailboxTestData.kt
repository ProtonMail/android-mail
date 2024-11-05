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

package ch.protonmail.android.testdata.mailbox

import androidx.compose.ui.graphics.Color
import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailcommon.domain.sample.LabelIdSample
import ch.protonmail.android.mailcommon.domain.sample.LabelSample
import ch.protonmail.android.mailcommon.presentation.model.AvatarUiModel
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.maillabel.presentation.model.LabelUiModel
import ch.protonmail.android.mailmailbox.domain.model.MailboxItem
import ch.protonmail.android.mailmailbox.domain.model.MailboxItemType
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxItemLocationUiModel
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxItemUiModel
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.ParticipantsUiModel
import ch.protonmail.android.mailmessage.domain.model.Recipient
import ch.protonmail.android.mailmessage.domain.model.Sender
import ch.protonmail.android.testdata.R
import ch.protonmail.android.testdata.label.LabelTestData.buildLabel
import ch.protonmail.android.testdata.user.UserIdTestData
import ch.protonmail.android.testdata.user.UserIdTestData.userId
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.Label
import me.proton.core.label.domain.entity.LabelId
import ch.protonmail.android.mailmailbox.R.string as mailboxStrings

object MailboxTestData {

    val unreadMailboxItem = buildMessageMailboxItem("1", isRead = false)
    val readMailboxItem = buildMessageMailboxItem("2", isRead = true)
    val unreadMailboxItemWithLabel =
        unreadMailboxItem.copy(labelIds = listOf(LabelIdSample.Folder2021), labels = listOf(LabelSample.Folder2021))

    val repliedMailboxItem = buildMessageMailboxItem("3", isReplied = true)
    val repliedAllMailboxItem = buildMessageMailboxItem("4", isReplied = true, isRepliedAll = true)
    val allActionsMailboxItem = buildMessageMailboxItem(
        "5", isReplied = true, isRepliedAll = true, isForwarded = true
    )

    val mailboxConversationItem = buildConversationMailboxItem("6")

    fun buildMailboxItem(
        userId: UserId = UserIdTestData.userId,
        id: String = "itemId",
        time: Long = 0,
        labelIds: List<LabelId> = listOf(MailLabelId.System.Inbox.labelId),
        labels: List<Label> = labelIds.map { buildLabel(userId = userId, id = it.id) },
        type: MailboxItemType = MailboxItemType.Message,
        senders: List<Sender> = emptyList(),
        recipients: List<Recipient> = emptyList(),
        numMessages: Int = 1,
        hasAttachments: Boolean = false,
        expirationTime: Long = 0,
        calendarAttachmentCount: Int = 0
    ) = MailboxItem(
        type = type,
        id = id,
        userId = userId,
        time = time,
        size = 0,
        order = 1000,
        read = true,
        labelIds = labelIds,
        conversationId = ConversationId(id),
        labels = labels,
        subject = "subject",
        senders = senders,
        recipients = recipients,
        isReplied = false,
        isRepliedAll = false,
        isForwarded = false,
        numMessages = numMessages,
        hasNonCalendarAttachments = hasAttachments,
        expirationTime = expirationTime,
        calendarAttachmentCount = calendarAttachmentCount
    )

    private fun buildMessageMailboxItem(
        id: String,
        isRead: Boolean = true,
        isReplied: Boolean = false,
        isRepliedAll: Boolean = false,
        isForwarded: Boolean = false,
        labelIds: List<LabelId> = emptyList(),
        labels: List<Label> = emptyList()
    ) = MailboxItem(
        type = MailboxItemType.Message,
        id = id,
        userId = UserId("0"),
        time = 0,
        size = 0,
        order = 0,
        read = isRead,
        labelIds = labelIds,
        conversationId = ConversationId("2"),
        labels = labels,
        subject = "First message",
        senders = listOf(Sender("address", "name")),
        recipients = emptyList(),
        isReplied = isReplied,
        isRepliedAll = isRepliedAll,
        isForwarded = isForwarded,
        numMessages = 1,
        hasNonCalendarAttachments = false,
        expirationTime = 0,
        calendarAttachmentCount = 0
    )

    private fun buildConversationMailboxItem(id: String) = MailboxItem(
        type = MailboxItemType.Conversation,
        id = id,
        userId = UserId("0"),
        time = 0,
        size = 0,
        order = 0,
        read = false,
        labelIds = emptyList(),
        conversationId = ConversationId("2"),
        labels = emptyList(),
        subject = "First message",
        senders = listOf(Sender("address", "name")),
        recipients = emptyList(),
        isReplied = false,
        isRepliedAll = false,
        isForwarded = false,
        numMessages = 3,
        hasNonCalendarAttachments = false,
        expirationTime = 0,
        calendarAttachmentCount = 0
    )

}

object MailboxItemUiModelTestData {

    val unreadMailboxItemUiModel = buildMailboxUiModelItem(
        id = "1",
        type = MailboxItemType.Message,
        subject = "First message",
        isRead = false
    )

    val unreadMailboxItemUiModelWithLabelColored = unreadMailboxItemUiModel.copy(
        locations = persistentListOf(MailboxItemLocationUiModel(R.drawable.ic_proton_folder_filled, Color.Red))
    )

    val unreadMailboxItemUiModelWithLabel = unreadMailboxItemUiModel.copy(
        locations = persistentListOf(MailboxItemLocationUiModel(R.drawable.ic_proton_folder))
    )

    val sentMessage = buildMailboxUiModelItem(
        id = "message-id",
        conversationId = ConversationId("conversation-id"),
        type = MailboxItemType.Conversation
    )

    val readMailboxItemUiModel = buildMailboxUiModelItem(
        id = "2",
        type = MailboxItemType.Message,
        subject = "Second message",
        isRead = true
    )

    val draftMailboxItemUiModel = buildMailboxUiModelItem(
        id = "3",
        type = MailboxItemType.Message,
        subject = "Composing something",
        isRead = true,
        shouldOpenInComposer = true
    )

    fun buildMailboxUiModelItem(
        id: String = "0",
        type: MailboxItemType = MailboxItemType.Message,
        subject: String = id,
        conversationId: ConversationId = ConversationId(id),
        isRead: Boolean = true,
        labels: ImmutableList<LabelUiModel> = persistentListOf(),
        locations: ImmutableList<MailboxItemLocationUiModel> = persistentListOf(),
        shouldOpenInComposer: Boolean = false
    ) = MailboxItemUiModel(
        avatar = AvatarUiModel.ParticipantInitial("T"),
        type = type,
        id = id,
        userId = userId,
        conversationId = conversationId,
        time = TextUiModel.Text("10:42"),
        isRead = isRead,
        labels = labels,
        subject = subject,
        participants = ParticipantsUiModel.NoParticipants(TextUiModel(mailboxStrings.mailbox_default_sender)),
        shouldShowRepliedIcon = false,
        shouldShowRepliedAllIcon = false,
        shouldShowForwardedIcon = false,
        numMessages = null,
        showStar = false,
        locations = locations,
        shouldShowAttachmentIcon = false,
        shouldShowExpirationLabel = false,
        shouldShowCalendarIcon = false,
        shouldOpenInComposer = shouldOpenInComposer
    )
}
