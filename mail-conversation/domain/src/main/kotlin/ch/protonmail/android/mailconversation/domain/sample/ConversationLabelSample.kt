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
import ch.protonmail.android.mailcommon.domain.sample.LabelIdSample
import ch.protonmail.android.mailconversation.domain.entity.ConversationLabel
import me.proton.core.label.domain.entity.LabelId

object ConversationLabelSample {

    object WeatherForecast {

        val AllDrafts = build(
            conversationId = ConversationIdSample.WeatherForecast,
            labelId = LabelIdSample.AllDraft
        )

        val AllMail = build(
            conversationId = ConversationIdSample.WeatherForecast,
            labelId = LabelIdSample.AllMail
        )

        val AllSent = build(
            conversationId = ConversationIdSample.WeatherForecast,
            labelId = LabelIdSample.AllSent
        )

        val Inbox = build(
            conversationId = ConversationIdSample.WeatherForecast,
            labelId = LabelIdSample.Inbox
        )

        val News = build(
            conversationId = ConversationIdSample.WeatherForecast,
            labelId = LabelIdSample.News
        )

        val Trash = build(
            conversationId = ConversationIdSample.WeatherForecast,
            labelId = LabelIdSample.Trash
        )

        val Spam = build(
            conversationId = ConversationIdSample.WeatherForecast,
            labelId = LabelIdSample.Spam
        )
    }

    object AlphaAppFeedback {

        val AllMail = build(
            conversationId = ConversationIdSample.AlphaAppFeedback,
            labelId = LabelIdSample.AllMail
        )

        val Inbox = build(
            conversationId = ConversationIdSample.AlphaAppFeedback,
            labelId = LabelIdSample.Inbox,
            numMessages = 1
        )

        val Archive = build(
            conversationId = ConversationIdSample.AlphaAppFeedback,
            labelId = LabelIdSample.Archive,
            numMessages = 3
        )
    }

    object Newsletter {

        val AllMail = build(
            conversationId = ConversationIdSample.Newsletter,
            labelId = LabelIdSample.AllMail
        )

        val Archive = build(
            conversationId = ConversationIdSample.Newsletter,
            labelId = LabelIdSample.Archive,
            numMessages = 1
        )
    }

    object AppointmentReminder {

        val AllMail = build(
            conversationId = ConversationIdSample.AppointmentReminder,
            labelId = LabelIdSample.AllMail
        )

        val Archive = build(
            conversationId = ConversationIdSample.AppointmentReminder,
            labelId = LabelIdSample.Archive,
            numMessages = 1
        )
    }


    fun build(
        conversationId: ConversationId = ConversationIdSample.build(),
        labelId: LabelId = LabelIdSample.build(),
        numMessages: Int = 0,
        numUnread: Int = 0
    ) = ConversationLabel(
        conversationId = conversationId,
        contextNumAttachments = 0,
        contextNumMessages = numMessages,
        contextNumUnread = numUnread,
        contextSize = 0,
        contextTime = 0,
        labelId = labelId
    )
}
