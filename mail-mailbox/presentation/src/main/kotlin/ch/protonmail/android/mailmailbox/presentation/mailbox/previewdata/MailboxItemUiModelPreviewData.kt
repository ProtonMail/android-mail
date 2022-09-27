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

import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailmailbox.domain.model.MailboxItemType
import ch.protonmail.android.mailmailbox.presentation.R
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.AvatarUiModel
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxItemUiModel
import me.proton.core.domain.entity.UserId

object MailboxItemUiModelPreviewData {

    val AccuWeatherAvatar = AvatarUiModel.ParticipantInitial("A")
    const val AccuWeatherName = "AccuWeather"
    val UserId = UserId("user")
    val WeatherForecastConversationId = ConversationId("WeatherForecasts")

    object Conversation {

        val DroidConLondon = MailboxItemUiModel(
            avatar = AvatarUiModel.ParticipantInitial("D"),
            conversationId = ConversationId("DroidConLondon"),
            id = "DroidConLondon",
            isRead = true,
            labels = emptyList(),
            locationIconResIds = emptyList(),
            numMessages = 2,
            participants = listOf("DroidCon"),
            shouldShowAttachmentIcon = true,
            shouldShowCalendarIcon = true,
            shouldShowExpirationLabel = false,
            shouldShowForwardedIcon = false,
            shouldShowRepliedAllIcon = false,
            shouldShowRepliedIcon = false,
            showStar = true,
            subject = "DroidCon London",
            time = TextUiModel.Text("Aug 20th 2022"),
            type = MailboxItemType.Conversation,
            userId = UserId
        )

        val WeatherForecast = MailboxItemUiModel(
            avatar = AccuWeatherAvatar,
            conversationId = WeatherForecastConversationId,
            id = "WeatherForecast",
            isRead = false,
            labels = emptyList(),
            locationIconResIds = emptyList(),
            numMessages = 2,
            participants = listOf(AccuWeatherName),
            shouldShowAttachmentIcon = true,
            shouldShowCalendarIcon = false,
            shouldShowExpirationLabel = false,
            shouldShowForwardedIcon = false,
            shouldShowRepliedAllIcon = false,
            shouldShowRepliedIcon = false,
            showStar = true,
            subject = "Weather Forecast",
            time = Message.WeatherForecastSep.time,
            type = MailboxItemType.Conversation,
            userId = UserId
        )
    }

    object Message {

        val WeatherForecastAug = MailboxItemUiModel(
            avatar = AccuWeatherAvatar,
            conversationId = WeatherForecastConversationId,
            id = "WeatherForecastAugust2022",
            isRead = true,
            labels = emptyList(),
            locationIconResIds = emptyList(),
            numMessages = 1,
            participants = listOf(AccuWeatherName),
            shouldShowAttachmentIcon = true,
            shouldShowCalendarIcon = false,
            shouldShowExpirationLabel = false,
            shouldShowForwardedIcon = false,
            shouldShowRepliedAllIcon = false,
            shouldShowRepliedIcon = false,
            showStar = true,
            subject = "Weather Forecast for August 2022",
            time = TextUiModel.Text("Jul 30th 2022"),
            type = MailboxItemType.Message,
            userId = UserId
        )

        val WeatherForecastSep = MailboxItemUiModel(
            avatar = AccuWeatherAvatar,
            conversationId = WeatherForecastConversationId,
            id = "WeatherForecastSeptember2022",
            isRead = false,
            labels = emptyList(),
            locationIconResIds = emptyList(),
            numMessages = 1,
            participants = listOf(AccuWeatherName),
            shouldShowAttachmentIcon = true,
            shouldShowCalendarIcon = false,
            shouldShowExpirationLabel = false,
            shouldShowForwardedIcon = false,
            shouldShowRepliedAllIcon = false,
            shouldShowRepliedIcon = false,
            showStar = true,
            subject = "Weather Forecast for September 2022",
            time = TextUiModel.TextRes(R.string.yesterday),
            type = MailboxItemType.Message,
            userId = UserId
        )
    }
}
