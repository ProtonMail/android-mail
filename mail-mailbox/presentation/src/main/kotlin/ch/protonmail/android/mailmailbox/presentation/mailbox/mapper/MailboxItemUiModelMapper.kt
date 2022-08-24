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

import androidx.compose.ui.graphics.Color
import arrow.core.getOrElse
import ch.protonmail.android.mailcommon.presentation.mapper.ColorMapper
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.maillabel.presentation.model.MailboxItemLabelUiModel
import ch.protonmail.android.mailmailbox.domain.model.MailboxItem
import ch.protonmail.android.mailmailbox.domain.model.MailboxItemType
import ch.protonmail.android.mailmailbox.domain.usecase.GetParticipantsResolvedNames
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxItemUiModel
import ch.protonmail.android.mailmailbox.presentation.mailbox.usecase.FormatMailboxItemTime
import ch.protonmail.android.mailmailbox.presentation.mailbox.usecase.GetAvatarUiModel
import ch.protonmail.android.mailmailbox.presentation.mailbox.usecase.GetMailboxItemLocationIcons
import me.proton.core.contact.domain.entity.Contact
import me.proton.core.domain.arch.Mapper
import me.proton.core.label.domain.entity.Label
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

class MailboxItemUiModelMapper @Inject constructor(
    private val colorMapper: ColorMapper,
    private val formatMailboxItemTime: FormatMailboxItemTime,
    private val getAvatarUiModel: GetAvatarUiModel,
    private val getMailboxItemLocationIcons: GetMailboxItemLocationIcons,
    private val getParticipantsResolvedNames: GetParticipantsResolvedNames
) : Mapper<MailboxItem, MailboxItemUiModel> {

    fun toUiModel(mailboxItem: MailboxItem, contacts: List<Contact>): MailboxItemUiModel {
        val participantsResolvedNames = getParticipantsResolvedNames(mailboxItem, contacts)

        return MailboxItemUiModel(
            avatar = getAvatarUiModel(mailboxItem, participantsResolvedNames),
            type = mailboxItem.type,
            id = mailboxItem.id,
            userId = mailboxItem.userId,
            conversationId = mailboxItem.conversationId,
            time = formatMailboxItemTime(mailboxItem.time.seconds),
            isRead = mailboxItem.read,
            labels = toLabelUiModels(mailboxItem.labels),
            subject = mailboxItem.subject,
            participants = participantsResolvedNames,
            shouldShowRepliedIcon = shouldShowRepliedIcon(mailboxItem),
            shouldShowRepliedAllIcon = shouldShowRepliedAllIcon(mailboxItem),
            shouldShowForwardedIcon = shouldShowForwardedIcon(mailboxItem),
            numMessages = mailboxItem.numMessages.takeIf { it >= 2 },
            showStar = mailboxItem.labelIds.contains(SystemLabelId.Starred.labelId),
            locationIconResIds = getLocationIconsToDisplay(mailboxItem),
            shouldShowAttachmentIcon = mailboxItem.hasAttachments
        )
    }

    private fun getLocationIconsToDisplay(mailboxItem: MailboxItem) =
        when (val icons = getMailboxItemLocationIcons(mailboxItem)) {
            is GetMailboxItemLocationIcons.Result.None -> emptyList()
            is GetMailboxItemLocationIcons.Result.Icons -> listOfNotNull(icons.first, icons.second, icons.third)
        }

    private fun shouldShowRepliedIcon(mailboxItem: MailboxItem): Boolean {
        if (mailboxItem.type == MailboxItemType.Conversation) {
            return false
        }

        return if (mailboxItem.isRepliedAll) {
            false
        } else {
            mailboxItem.isReplied
        }
    }

    private fun shouldShowRepliedAllIcon(mailboxItem: MailboxItem) =
        if (mailboxItem.type == MailboxItemType.Conversation) {
            false
        } else {
            mailboxItem.isRepliedAll
        }

    private fun shouldShowForwardedIcon(mailboxItem: MailboxItem) =
        if (mailboxItem.type == MailboxItemType.Conversation) {
            false
        } else {
            mailboxItem.isForwarded
        }

    private fun toLabelUiModels(labels: List<Label>): List<MailboxItemLabelUiModel> =
        labels.map { label ->
            MailboxItemLabelUiModel(
                name = label.name,
                color = colorMapper.toColor(label.color).getOrElse { Color.Unspecified }
            )
        }
}
