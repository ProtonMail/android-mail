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

import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailmessage.data.local.entity.AttachmentStateEntity
import ch.protonmail.android.mailmessage.domain.model.AttachmentId
import ch.protonmail.android.mailmessage.domain.model.AttachmentSyncState
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import me.proton.core.domain.entity.UserId

object AttachmentStateEntitySample {

    val LocalAttachmentState = build()

    val RemoteAttachmentState = build(
        messageId = MessageIdSample.RemoteDraft,
        state = AttachmentSyncState.Uploaded
    )


    fun build(
        userId: UserId = UserIdSample.Primary,
        messageId: MessageId = MessageIdSample.RemoteDraft,
        attachmentId: AttachmentId = AttachmentId("attachment_id"),
        state: AttachmentSyncState = AttachmentSyncState.Local
    ) = AttachmentStateEntity(
        userId = userId,
        messageId = messageId,
        attachmentId = attachmentId,
        state = state
    )

}
