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

package ch.protonmail.android.mailmessage.domain.sample

import java.util.UUID
import ch.protonmail.android.mailmessage.domain.model.MessageId

object MessageIdSample {

    val AugWeatherForecast = MessageId("aug_weather_forecast")
    val EmptyDraft = MessageId("empty_draft")
    val NewDraftWithSubject = MessageId("new_draft_with_subject_only")
    val NewDraftWithSubjectAndBody = MessageId("new_draft_with_subject_and_body")
    val RemoteDraft = MessageId(
        "j3AabCJkO7V4T9aJA81ilVFs2HYzJYhwRDPH_dm2O8twgGjRqUZ0-9XX7ZGP8ehEgtIm5o2J8N5svjFuVfu0GQ=="
    )
    val LocalDraft = MessageId(UUID.randomUUID().toString())
    val Invoice = MessageId("invoice")
    val HtmlInvoice = MessageId("html_invoice")
    val OctWeatherForecast = MessageId("oct_weather_forecast")
    val SepWeatherForecast = MessageId("sep_weather_forecast")
    val AlphaAppQAReport = MessageId("QA_testing_report")
    val AlphaAppInfoRequest = MessageId("alpha_app_info_request")
    val PlainTextMessage = MessageId("plain_text_message")
    val MessageWithAttachments = MessageId("Message_with_attachments")
    val PgpMimeMessage = MessageId("pgp_mime_message")
    val CalendarInvite = MessageId("calendar_invite")
    val ReadMessageMayFirst = MessageId("ReadMessageMayFirst")
    val ReadMessageMaySecond = MessageId("ReadMessageMaySecond")
    val UnreadMessageMayFirst = MessageId("UnreadMessageMayFirst")
    val UnreadMessageMaySecond = MessageId("UnreadMessageMaySecond")
    val UnreadMessageMayThird = MessageId("UnreadMessageMayThird")

    fun build() = MessageId("message")
}
