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

import android.content.Context
import android.text.format.Formatter
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcommon.presentation.usecase.FormatExtendedTime
import ch.protonmail.android.mailcommon.presentation.usecase.FormatShortTime
import ch.protonmail.android.maildetail.domain.model.MessageWithLabels
import ch.protonmail.android.maildetail.presentation.R
import ch.protonmail.android.maildetail.presentation.model.MessageDetailHeaderUiModel
import ch.protonmail.android.mailmessage.domain.usecase.ResolveParticipantName
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.mailmessage.domain.entity.Message
import dagger.hilt.android.qualifiers.ApplicationContext
import me.proton.core.contact.domain.entity.Contact
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

class MessageDetailHeaderUiModelMapper @Inject constructor(
    @ApplicationContext
    private val context: Context,
    private val detailAvatarUiModelMapper: DetailAvatarUiModelMapper,
    private val formatExtendedTime: FormatExtendedTime,
    private val formatShortTime: FormatShortTime,
    private val messageLocationUiModelMapper: MessageLocationUiModelMapper,
    private val participantUiModelMapper: ParticipantUiModelMapper,
    private val resolveParticipantName: ResolveParticipantName
) {

    fun toUiModel(messageWithLabels: MessageWithLabels, contacts: List<Contact>): MessageDetailHeaderUiModel {
        val senderResolvedName = resolveParticipantName(messageWithLabels.message.sender, contacts)

        return MessageDetailHeaderUiModel(
            avatar = detailAvatarUiModelMapper(messageWithLabels.message, senderResolvedName),
            sender = participantUiModelMapper.senderToUiModel(messageWithLabels.message.sender, contacts),
            shouldShowTrackerProtectionIcon = true,
            shouldShowAttachmentIcon = messageWithLabels.message.hasNonCalendarAttachments(),
            shouldShowStar = messageWithLabels.message.isStarred(),
            location = messageLocationUiModelMapper(messageWithLabels.message.labelIds, messageWithLabels.labels),
            time = formatShortTime(messageWithLabels.message.time.seconds),
            extendedTime = formatExtendedTime(messageWithLabels.message.time.seconds),
            shouldShowUndisclosedRecipients = messageWithLabels.message.hasUndisclosedRecipients(),
            allRecipients = messageWithLabels.message.allRecipients(contacts),
            toRecipients = messageWithLabels.message.toList.map {
                participantUiModelMapper.recipientToUiModel(it, contacts)
            },
            ccRecipients = messageWithLabels.message.ccList.map {
                participantUiModelMapper.recipientToUiModel(it, contacts)
            },
            bccRecipients = messageWithLabels.message.bccList.map {
                participantUiModelMapper.recipientToUiModel(it, contacts)
            },
            labels = emptyList(),
            size = Formatter.formatShortFileSize(context, messageWithLabels.message.size),
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
