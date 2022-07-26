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

package ch.protonmail.android.mailmailbox.presentation.mailbox.mapper

import ch.protonmail.android.mailmailbox.domain.model.MailboxItem
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxItemUiModel
import me.proton.core.domain.arch.Mapper
import javax.inject.Inject

class MailboxItemUiModelMapper @Inject constructor() : Mapper<MailboxItem, MailboxItemUiModel> {

    fun toUiModel(mailboxItem: MailboxItem): MailboxItemUiModel =
        MailboxItemUiModel(
            type = mailboxItem.type,
            id = mailboxItem.id,
            userId = mailboxItem.userId,
            conversationId = mailboxItem.conversationId,
            time = mailboxItem.time,
            read = mailboxItem.read,
            labels = mailboxItem.labels,
            subject = mailboxItem.subject,
            senders = mailboxItem.senders,
            recipients = mailboxItem.recipients,
            showRepliedIcon = showRepliedIcon(mailboxItem),
            showRepliedAllIcon = mailboxItem.isRepliedAll
        )

    private fun showRepliedIcon(mailboxItem: MailboxItem) =
        if (mailboxItem.isRepliedAll) {
            false
        } else {
            mailboxItem.isReplied
        }
}
