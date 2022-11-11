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

    fun toUiModel(message: Message, contacts: List<Contact>): MessageDetailHeaderUiModel {
        val senderResolvedName = resolveParticipantName(message.sender, contacts)
        val messageLocation = messageLocationUiModelMapper(message.labelIds)

        return MessageDetailHeaderUiModel(
            avatar = detailAvatarUiModelMapper(message, senderResolvedName),
            sender = participantUiModelMapper.toUiModel(message.sender, contacts),
            shouldShowTrackerProtectionIcon = false,
            shouldShowAttachmentIcon = message.hasNonCalendarAttachments(),
            shouldShowStar = message.isStarred(),
            locationIcon = messageLocation.icon,
            location = messageLocation.name,
            time = formatShortTime(message.time.toDuration(DurationUnit.MILLISECONDS)),
            extendedTime = formatExtendedTime(message.time.toDuration(DurationUnit.MILLISECONDS)),
            shouldShowUndisclosedRecipients = message.hasUndisclosedRecipients(),
            allRecipients = message.allRecipients(contacts),
            toRecipients = message.toList.map { participantUiModelMapper.toUiModel(it, contacts) },
            ccRecipients = message.ccList.map { participantUiModelMapper.toUiModel(it, contacts) },
            bccRecipients = message.bccList.map { participantUiModelMapper.toUiModel(it, contacts) },
            labels = emptyList(),
            size = Formatter.formatShortFileSize(null, message.size),
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
