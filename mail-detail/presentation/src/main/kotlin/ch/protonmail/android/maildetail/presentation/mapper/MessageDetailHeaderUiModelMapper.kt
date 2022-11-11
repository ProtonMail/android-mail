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

package ch.protonmail.android.maildetail.presentation.mapper

import android.text.format.Formatter
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcommon.presentation.usecase.FormatExtendedTime
import ch.protonmail.android.mailcommon.presentation.usecase.FormatShortTime
import ch.protonmail.android.maildetail.domain.model.MessageDetailItem
import ch.protonmail.android.maildetail.presentation.R
import ch.protonmail.android.maildetail.presentation.model.MessageDetailHeaderUiModel
import ch.protonmail.android.mailmessage.domain.usecase.ResolveParticipantName
import ch.protonmail.android.maildetail.presentation.model.MessageDetailMetadataUiModel
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.mailmessage.domain.entity.Message
import me.proton.core.contact.domain.entity.Contact
import me.proton.core.domain.arch.Mapper
import javax.inject.Inject
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class MessageDetailHeaderUiModelMapper @Inject constructor(
    private val detailAvatarUiModelMapper: DetailAvatarUiModelMapper,
    private val formatExtendedTime: FormatExtendedTime,
    private val formatShortTime: FormatShortTime,
    private val messageLocationUiModelMapper: MessageLocationUiModelMapper,
    private val participantUiModelMapper: ParticipantUiModelMapper,
    private val resolveParticipantName: ResolveParticipantName
) : Mapper<Message, MessageDetailMetadataUiModel> {

    fun toUiModel(messageDetailItem: MessageDetailItem, contacts: List<Contact>): MessageDetailHeaderUiModel {
        val senderResolvedName = resolveParticipantName(messageDetailItem.message.sender, contacts)

        return MessageDetailHeaderUiModel(
            avatar = detailAvatarUiModelMapper(messageDetailItem.message, senderResolvedName),
            sender = participantUiModelMapper.toUiModel(messageDetailItem.message.sender, contacts),
            shouldShowTrackerProtectionIcon = false,
            shouldShowAttachmentIcon = messageDetailItem.message.hasNonCalendarAttachments(),
            shouldShowStar = messageDetailItem.message.isStarred(),
            location = messageLocationUiModelMapper(messageDetailItem.message.labelIds, messageDetailItem.labels),
            time = formatShortTime(messageDetailItem.message.time.toDuration(DurationUnit.MILLISECONDS)),
            extendedTime = formatExtendedTime(messageDetailItem.message.time.toDuration(DurationUnit.MILLISECONDS)),
            shouldShowUndisclosedRecipients = messageDetailItem.message.hasUndisclosedRecipients(),
            allRecipients = messageDetailItem.message.allRecipients(contacts),
            toRecipients = messageDetailItem.message.toList.map { participantUiModelMapper.toUiModel(it, contacts) },
            ccRecipients = messageDetailItem.message.ccList.map { participantUiModelMapper.toUiModel(it, contacts) },
            bccRecipients = messageDetailItem.message.bccList.map { participantUiModelMapper.toUiModel(it, contacts) },
            labels = emptyList(),
            size = Formatter.formatShortFileSize(null, messageDetailItem.message.size),
            encryptionPadlock = R.drawable.ic_proton_lock,
            encryptionInfo = "End-to-end encrypted and signed message"
        )
    }

    private fun Message.hasNonCalendarAttachments() = numAttachments > attachmentCount.calendar

    private fun Message.isStarred() = labelIds.any { it == MailLabelId.System.Starred.labelId }

    private fun Message.hasUndisclosedRecipients() = (toList + ccList + bccList).isEmpty()

    private fun Message.allRecipients(contacts: List<Contact>): TextUiModel {
        val allRecipientsList = toList + ccList + bccList

        return if (allRecipientsList.isNotEmpty()) {
            TextUiModel.Text(allRecipientsList.joinToString { resolveParticipantName(it, contacts) })
        } else {
            TextUiModel.TextRes(R.string.undisclosed_recipients)
        }
    }
}
