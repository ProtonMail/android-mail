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

package ch.protonmail.android.composer.data.sample

import ch.protonmail.android.composer.data.remote.resource.DraftMessageResource
import ch.protonmail.android.mailmessage.data.remote.resource.RecipientResource
import ch.protonmail.android.mailmessage.domain.model.MimeType
import ch.protonmail.android.mailmessage.domain.sample.RecipientSample

object DraftMessageResourceSample {

    val NewDraftWithSubject = build(subject = "New draft, just typed the subject")

    val NewDraftWithSubjectAndBody = build(
        subject = "New draft, just typed the subject",
        body = "This is the body typed from the user, ENCRYPTED"
    )

    val RemoteDraft = build(subject = "Remote draft, known to the API")

    fun build(subject: String = "", body: String = "") = DraftMessageResource(
        subject = subject,
        unread = 0,
        sender = RecipientResource(RecipientSample.John.address, RecipientSample.John.name),
        toList = emptyList(),
        ccList = emptyList(),
        bccList = emptyList(),
        externalId = null,
        flags = 0L,
        body = body,
        mimeType = MimeType.PlainText.value
    )
}
