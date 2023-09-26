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

import ch.protonmail.android.composer.data.remote.resource.CreateDraftBody
import ch.protonmail.android.composer.data.remote.resource.DraftMessageResource
import ch.protonmail.android.mailmessage.domain.sample.MessageWithBodySample
import me.proton.core.crypto.common.pgp.Armored

object CreateDraftBodySample {

    val NewDraftWithSubject = build()

    val NewDraftWithInvoiceAttachment = build(
        message = DraftMessageResourceSample.NewDraftWithInvoiceAttachment,
        attachmentKeyPackets = MessageWithBodySample.MessageWithInvoiceAttachment.messageBody.attachments
            .filter { it.keyPackets != null }
            .associate { it.attachmentId.id to it.keyPackets!! }
    )

    val NewDraftWithSubjectAndBody = build(
        message = DraftMessageResourceSample.NewDraftWithSubjectAndBody
    )

    fun build(
        message: DraftMessageResource = DraftMessageResourceSample.NewDraftWithSubject,
        parentId: String? = null,
        action: Int = -1,
        attachmentKeyPackets: Map<String, Armored> = emptyMap()
    ) = CreateDraftBody(
        message = message,
        parentId = parentId,
        action = action,
        attachmentKeyPackets = attachmentKeyPackets
    )
}
