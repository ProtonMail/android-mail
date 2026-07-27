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
import androidx.compose.ui.graphics.Color
import arrow.core.getOrElse
import ch.protonmail.android.mailcommon.presentation.mapper.ColorMapper
import ch.protonmail.android.mailcommon.presentation.usecase.FormatExtendedTime
import ch.protonmail.android.mailcommon.presentation.usecase.FormatShortTime
import ch.protonmail.android.maildetail.presentation.R
import ch.protonmail.android.maildetail.presentation.model.MessageDetailHeaderUiModel
import ch.protonmail.android.maildetail.presentation.model.MessageIdUiModel
import ch.protonmail.android.maillabel.domain.model.Label
import ch.protonmail.android.maillabel.domain.model.LabelType
import ch.protonmail.android.maillabel.presentation.model.LabelUiModel
import ch.protonmail.android.mailmessage.domain.model.AvatarImageState
import ch.protonmail.android.mailmessage.domain.model.Message
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.model.MessageTheme
import ch.protonmail.android.mailmessage.presentation.mapper.AvatarImageUiModelMapper
import ch.protonmail.android.mailmessage.presentation.model.ViewModePreference
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

class MessageDetailHeaderUiModelMapper @Inject constructor(
    private val colorMapper: ColorMapper,
    @ApplicationContext private val context: Context,
    private val detailAvatarUiModelMapper: DetailAvatarUiModelMapper,
    private val formatExtendedTime: FormatExtendedTime,
    private val formatShortTime: FormatShortTime,
    private val messageLocationUiModelMapper: MessageLocationUiModelMapper,
    private val participantUiModelMapper: ParticipantUiModelMapper,
    private val avatarImageUiModelMapper: AvatarImageUiModelMapper
) {

    fun toUiModel(
        message: Message,
        primaryUserAddress: String?,
        avatarImageState: AvatarImageState,
        viewModePreference: ViewModePreference,
        hasMoreActionsEnabled: Boolean
    ): MessageDetailHeaderUiModel {
        return MessageDetailHeaderUiModel(
            avatar = detailAvatarUiModelMapper(message.isDraft, message.avatarInformation, message.sender),
            avatarImage = avatarImageUiModelMapper.toUiModel(avatarImageState),
            sender = participantUiModelMapper.senderToUiModel(
                message.sender,
                isPhishing = message.isPhishingAuto() && message.isHamManual().not()
            ),
            shouldShowTrackerProtectionIcon = true,
            shouldShowAttachmentIcon = message.hasNonCalendarAttachments(),
            shouldShowStar = message.isStarred,
            location = messageLocationUiModelMapper(message.exclusiveLocation),
            category = messageLocationUiModelMapper(message.category),
            time = formatShortTime(message.time.seconds),
            extendedTime = formatExtendedTime(message.time.seconds),
            shouldShowUndisclosedRecipients = message.hasUndisclosedRecipients(),
            allRecipients = (message.toList + message.ccList + message.bccList).map {
                participantUiModelMapper.recipientToUiModel(it, primaryUserAddress)
            }.toImmutableList(),
            toRecipients = message.toList.map {
                participantUiModelMapper.recipientToUiModel(it, primaryUserAddress)
            }.toImmutableList(),
            ccRecipients = message.ccList.map {
                participantUiModelMapper.recipientToUiModel(it, primaryUserAddress)
            }.toImmutableList(),
            bccRecipients = message.bccList.map {
                participantUiModelMapper.recipientToUiModel(it, primaryUserAddress)
            }.toImmutableList(),
            labels = toLabelUiModels(message.customLabels),
            size = Formatter.formatShortFileSize(context, message.size),
            encryptionPadlock = R.drawable.ic_proton_lock,
            encryptionInfo = "End-to-end encrypted and signed message",
            messageIdUiModel = toMessageUiModel(message.messageId),
            themeOverride = viewModePreference.toThemeOverride(),
            shouldShowQuickReply = message.isReplyAllowed,
            hasMoreActions = hasMoreActionsEnabled,
            subject = message.subject
        )
    }

    private fun ViewModePreference.toThemeOverride(): MessageTheme? {
        return when (this) {
            ViewModePreference.ThemeDefault -> null
            ViewModePreference.LightMode -> MessageTheme.Light
            ViewModePreference.DarkMode -> MessageTheme.Dark
        }
    }

    private fun Message.hasNonCalendarAttachments() = numAttachments > attachmentCount.calendar

    private fun Message.hasUndisclosedRecipients() = (toList + ccList + bccList).isEmpty()

    private fun toLabelUiModels(labels: List<Label>): ImmutableList<LabelUiModel> =
        labels.filter { it.type == LabelType.MessageLabel }.map { label ->
            LabelUiModel(
                name = label.name,
                color = colorMapper.toColor(label.color).getOrElse { Color.Unspecified },
                id = label.labelId.id
            )
        }.toImmutableList()

    private fun toMessageUiModel(messageId: MessageId) = MessageIdUiModel(messageId.id)
}
