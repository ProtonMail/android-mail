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

import androidx.compose.ui.graphics.Color
import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailcommon.presentation.model.AvatarUiModel
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.maillabel.presentation.model.LabelUiModel
import ch.protonmail.android.mailmailbox.domain.model.MailboxItemType
import ch.protonmail.android.mailmailbox.presentation.R
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxItemLocationUiModel
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxItemUiModel
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.ParticipantUiModel
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.ParticipantsUiModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import me.proton.core.domain.entity.UserId

object MailboxItemUiModelPreviewData {

    val AccuWeatherAvatar = AvatarUiModel.ParticipantInitial("A")
    const val AccuWeatherName = "AccuWeather"
    val UserId = UserId("user")
    val WeatherForecastConversationId = ConversationId("WeatherForecasts")

    object Conversation {

        val DroidConLondon = MailboxItemUiModel(
            avatar = AvatarUiModel.ParticipantInitial("D"),
            type = MailboxItemType.Conversation,
            id = "DroidConLondon",
            userId = UserId,
            conversationId = ConversationId("DroidConLondon"),
            time = TextUiModel.Text("Aug 20th 2022"),
            isRead = true,
            labels = persistentListOf(),
            subject = "DroidCon London",
            participants = ParticipantsUiModel.Participants(
                listOf(ParticipantUiModel(name = "DroidCon", shouldShowOfficialBadge = false)).toImmutableList()
            ),
            shouldShowRepliedIcon = false,
            shouldShowRepliedAllIcon = false,
            shouldShowForwardedIcon = true,
            numMessages = 2,
            showStar = true,
            locations = persistentListOf(MailboxItemLocationUiModel(R.drawable.ic_proton_archive_box)),
            shouldShowAttachmentIcon = true,
            shouldShowExpirationLabel = false,
            shouldShowCalendarIcon = true,
            shouldOpenInComposer = false
        )

        val DroidConLondonWithZeroMessages = MailboxItemUiModel(
            avatar = AvatarUiModel.ParticipantInitial("D"),
            type = MailboxItemType.Conversation,
            id = "DroidConLondon",
            userId = UserId,
            conversationId = ConversationId("DroidConLondon"),
            time = TextUiModel.Text("Aug 20th 2022"),
            isRead = true,
            labels = persistentListOf(),
            subject = "DroidCon London",
            participants = ParticipantsUiModel.Participants(
                listOf(ParticipantUiModel(name = "DroidCon", shouldShowOfficialBadge = false)).toImmutableList()
            ),
            shouldShowRepliedIcon = false,
            shouldShowRepliedAllIcon = false,
            shouldShowForwardedIcon = false,
            numMessages = null,
            showStar = true,
            locations = persistentListOf(),
            shouldShowAttachmentIcon = false,
            shouldShowExpirationLabel = false,
            shouldShowCalendarIcon = false,
            shouldOpenInComposer = false
        )

        val WeatherForecast = MailboxItemUiModel(
            avatar = AccuWeatherAvatar,
            type = MailboxItemType.Conversation,
            id = "WeatherForecast",
            userId = UserId,
            conversationId = WeatherForecastConversationId,
            time = Message.WeatherForecastSep.time,
            isRead = false,
            labels = persistentListOf(),
            subject = "Weather Forecast",
            participants = ParticipantsUiModel.Participants(
                listOf(ParticipantUiModel(name = AccuWeatherName, shouldShowOfficialBadge = false)).toImmutableList()
            ),
            shouldShowRepliedIcon = false,
            shouldShowRepliedAllIcon = false,
            shouldShowForwardedIcon = false,
            numMessages = 2,
            showStar = true,
            locations = persistentListOf(
                MailboxItemLocationUiModel(R.drawable.ic_proton_inbox),
                MailboxItemLocationUiModel(R.drawable.ic_proton_trash)
            ),
            shouldShowAttachmentIcon = true,
            shouldShowExpirationLabel = false,
            shouldShowCalendarIcon = false,
            shouldOpenInComposer = false
        )

        val MultipleRecipientWithLabel = MailboxItemUiModel(
            avatar = AvatarUiModel.ParticipantInitial("D"),
            type = MailboxItemType.Conversation,
            id = "DroidConLondon",
            userId = UserId,
            conversationId = ConversationId("DroidConLondon"),
            time = TextUiModel.Text("Aug 20th 2022"),
            isRead = true,
            labels = persistentListOf(
                LabelUiModel("Long Test", Color.Red, id = "longTest"),
                LabelUiModel("Test", Color.Blue, id = "test"),
                LabelUiModel("Even Longer Test", Color.Cyan, id = "evenLongerTest"),
                LabelUiModel("Short", Color.Blue, id = "short"),
                LabelUiModel("1234567890123", Color.Blue, id = "random"),
                LabelUiModel("Very important mail label", Color.Green, id = "important")
            ),
            subject = "DroidCon London",
            participants = ParticipantsUiModel.Participants(
                listOf(
                    ParticipantUiModel(name = "FirstRecipient", shouldShowOfficialBadge = false),
                    ParticipantUiModel(name = "SecondRecipient", shouldShowOfficialBadge = false),
                    ParticipantUiModel(name = "ThirdRecipient", shouldShowOfficialBadge = false)
                ).toImmutableList()
            ),
            shouldShowRepliedIcon = true,
            shouldShowRepliedAllIcon = true,
            shouldShowForwardedIcon = true,
            numMessages = 2,
            showStar = true,
            locations = persistentListOf(),
            shouldShowAttachmentIcon = true,
            shouldShowExpirationLabel = true,
            shouldShowCalendarIcon = true,
            shouldOpenInComposer = false
        )

        val LongSubjectWithIcons = MailboxItemUiModel(
            avatar = AccuWeatherAvatar,
            type = MailboxItemType.Conversation,
            id = "WeatherForecast",
            userId = UserId,
            conversationId = WeatherForecastConversationId,
            time = Message.WeatherForecastSep.time,
            isRead = false,
            labels = persistentListOf(),
            subject = "This is a really long subject without any information",
            participants = ParticipantsUiModel.Participants(
                listOf(ParticipantUiModel(name = AccuWeatherName, shouldShowOfficialBadge = false)).toImmutableList()
            ),
            shouldShowRepliedIcon = false,
            shouldShowRepliedAllIcon = false,
            shouldShowForwardedIcon = false,
            numMessages = 2,
            showStar = true,
            locations = persistentListOf(
                MailboxItemLocationUiModel(R.drawable.ic_proton_inbox),
                MailboxItemLocationUiModel(R.drawable.ic_proton_trash)
            ),
            shouldShowAttachmentIcon = false,
            shouldShowExpirationLabel = false,
            shouldShowCalendarIcon = false,
            shouldOpenInComposer = false
        )

        val LongSubjectWithoutIcons = MailboxItemUiModel(
            avatar = AccuWeatherAvatar,
            type = MailboxItemType.Conversation,
            id = "WeatherForecast",
            userId = UserId,
            conversationId = WeatherForecastConversationId,
            time = Message.WeatherForecastSep.time,
            isRead = false,
            labels = persistentListOf(),
            subject = "This is a really long subject without any information",
            participants = ParticipantsUiModel.Participants(
                listOf(ParticipantUiModel(name = AccuWeatherName, shouldShowOfficialBadge = false)).toImmutableList()
            ),
            shouldShowRepliedIcon = false,
            shouldShowRepliedAllIcon = false,
            shouldShowForwardedIcon = false,
            numMessages = 2,
            showStar = false,
            locations = persistentListOf(),
            shouldShowAttachmentIcon = false,
            shouldShowExpirationLabel = false,
            shouldShowCalendarIcon = false,
            shouldOpenInComposer = false
        )

        val NoParticipant = MailboxItemUiModel(
            avatar = AccuWeatherAvatar,
            type = MailboxItemType.Conversation,
            id = "WeatherForecast",
            userId = UserId,
            conversationId = WeatherForecastConversationId,
            time = Message.WeatherForecastSep.time,
            isRead = false,
            labels = persistentListOf(),
            subject = "This is a really long subject without any information",
            participants = ParticipantsUiModel.NoParticipants(message = TextUiModel(R.string.mailbox_default_sender)),
            shouldShowRepliedIcon = false,
            shouldShowRepliedAllIcon = false,
            shouldShowForwardedIcon = false,
            numMessages = 2,
            showStar = false,
            locations = persistentListOf(),
            shouldShowAttachmentIcon = false,
            shouldShowExpirationLabel = false,
            shouldShowCalendarIcon = false,
            shouldOpenInComposer = false
        )
    }

    object Message {

        val WeatherForecastAug = MailboxItemUiModel(
            avatar = AccuWeatherAvatar,
            type = MailboxItemType.Message,
            id = "WeatherForecastAugust2022",
            userId = UserId,
            conversationId = WeatherForecastConversationId,
            time = TextUiModel.Text("Jul 30th 2022"),
            isRead = true,
            labels = persistentListOf(),
            subject = "Weather Forecast for August 2022",
            participants = ParticipantsUiModel.Participants(
                listOf(ParticipantUiModel(name = AccuWeatherName, shouldShowOfficialBadge = false)).toImmutableList()
            ),
            shouldShowRepliedIcon = false,
            shouldShowRepliedAllIcon = false,
            shouldShowForwardedIcon = false,
            numMessages = 1,
            showStar = true,
            locations = persistentListOf(),
            shouldShowAttachmentIcon = true,
            shouldShowExpirationLabel = false,
            shouldShowCalendarIcon = false,
            shouldOpenInComposer = false
        )

        val WeatherForecastSep = MailboxItemUiModel(
            avatar = AccuWeatherAvatar,
            type = MailboxItemType.Message,
            id = "WeatherForecastSeptember2022",
            userId = UserId,
            conversationId = WeatherForecastConversationId,
            time = TextUiModel.TextRes(R.string.yesterday),
            isRead = false,
            labels = persistentListOf(),
            subject = "Weather Forecast for September 2022",
            participants = ParticipantsUiModel.Participants(
                listOf(ParticipantUiModel(name = AccuWeatherName, shouldShowOfficialBadge = false)).toImmutableList()
            ),
            shouldShowRepliedIcon = false,
            shouldShowRepliedAllIcon = false,
            shouldShowForwardedIcon = false,
            numMessages = 1,
            showStar = true,
            locations = persistentListOf(),
            shouldShowAttachmentIcon = true,
            shouldShowExpirationLabel = false,
            shouldShowCalendarIcon = false,
            shouldOpenInComposer = false
        )
    }
}
