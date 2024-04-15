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

package ch.protonmail.android.mailconversation.domain.sample

import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailcommon.domain.sample.ConversationIdSample
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailconversation.domain.entity.Conversation
import ch.protonmail.android.mailconversation.domain.entity.ConversationLabel
import ch.protonmail.android.mailmessage.domain.model.AttachmentCount
import ch.protonmail.android.mailmessage.domain.model.Recipient
import ch.protonmail.android.mailmessage.domain.model.Sender
import ch.protonmail.android.mailmessage.domain.sample.AttachmentCountSample
import ch.protonmail.android.mailmessage.domain.sample.RecipientSample
import me.proton.core.domain.entity.UserId

object ConversationSample {

    val WeatherForecast = build(
        conversationId = ConversationIdSample.WeatherForecast,
        labels = listOf(
            ConversationLabelSample.WeatherForecast.AllMail,
            ConversationLabelSample.WeatherForecast.News
        ),
        subject = "Weather Forecast"
    )

    val AlphaAppFeedback = build(
        conversationId = ConversationIdSample.AlphaAppFeedback,
        labels = listOf(
            ConversationLabelSample.AlphaAppFeedback.Inbox,
            ConversationLabelSample.AlphaAppFeedback.Archive
        ),
        subject = "Request for feedback on the new alpha app",
        numMessages = 4
    )

    val Newsletter = build(
        conversationId = ConversationIdSample.Newsletter,
        labels = listOf(
            ConversationLabelSample.Newsletter.Archive,
            ConversationLabelSample.Newsletter.AllMail
        ),
        subject = "Android weekly: android tips",
        numMessages = 1
    )

    val AppointmentReminder = build(
        conversationId = ConversationIdSample.AppointmentReminder,
        labels = listOf(
            ConversationLabelSample.AppointmentReminder.Archive,
            ConversationLabelSample.AppointmentReminder.AllMail
        ),
        subject = "Reminder of your appointment",
        numMessages = 1
    )


    fun build(
        attachmentCount: AttachmentCount = AttachmentCountSample.build(),
        conversationId: ConversationId = ConversationIdSample.build(),
        labels: List<ConversationLabel> = listOf(ConversationLabelSample.build()),
        recipients: List<Recipient> = listOf(RecipientSample.Doe),
        senders: List<Sender> = listOf(RecipientSample.John),
        subject: String = "subject",
        userId: UserId = UserIdSample.Primary,
        numMessages: Int = 0
    ) = Conversation(
        attachmentCount = attachmentCount,
        conversationId = conversationId,
        expirationTime = 0,
        labels = labels,
        numAttachments = 0,
        numMessages = numMessages,
        numUnread = 0,
        order = 0,
        recipients = recipients,
        senders = senders,
        subject = subject,
        userId = userId
    )
}
