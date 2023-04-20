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

package ch.protonmail.android.mailmailbox.presentation.mailbox.previewdata

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.paging.PagingData
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxItemUiModel
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxState
import kotlinx.coroutines.flow.flowOf

object MailboxPreviewData {

    val Loading = MailboxPreview(
        mailboxState = MailboxStateSampleData.Loading,
        mailboxItems = MailboxItemsPreviewData.Empty
    )

    val EmptyInbox = MailboxPreview(
        mailboxState = MailboxStateSampleData.Inbox,
        mailboxItems = MailboxItemsPreviewData.Empty
    )

    val InboxConversations = MailboxPreview(
        mailboxState = MailboxStateSampleData.Inbox,
        mailboxItems = MailboxItemsPreviewData.Conversations
    )

    val AllMailMessages = MailboxPreview(
        mailboxState = MailboxStateSampleData.AllMail,
        mailboxItems = MailboxItemsPreviewData.Messages
    )
}

data class MailboxPreview(
    private val mailboxState: MailboxState,
    private val mailboxItems: List<MailboxItemUiModel>
) {

    val state = mailboxState
    val items = flowOf(PagingData.from(mailboxItems))
}

class MailboxPreviewProvider : PreviewParameterProvider<MailboxPreview> {

    override val values: Sequence<MailboxPreview>
        get() = sequenceOf(
            MailboxPreviewData.Loading,
            MailboxPreviewData.EmptyInbox,
            MailboxPreviewData.InboxConversations,
            MailboxPreviewData.AllMailMessages
        )
}
