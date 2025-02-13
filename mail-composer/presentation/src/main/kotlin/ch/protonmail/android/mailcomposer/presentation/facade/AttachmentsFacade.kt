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

package ch.protonmail.android.mailcomposer.presentation.facade

import android.net.Uri
import ch.protonmail.android.mailcomposer.domain.model.SenderEmail
import ch.protonmail.android.mailcomposer.domain.usecase.DeleteAllAttachments
import ch.protonmail.android.mailcomposer.domain.usecase.DeleteAttachment
import ch.protonmail.android.mailcomposer.domain.usecase.ObserveMessageAttachments
import ch.protonmail.android.mailcomposer.domain.usecase.ReEncryptAttachments
import ch.protonmail.android.mailcomposer.domain.usecase.StoreAttachments
import ch.protonmail.android.mailcomposer.domain.usecase.StoreExternalAttachments
import ch.protonmail.android.mailmessage.domain.model.AttachmentId
import ch.protonmail.android.mailmessage.domain.model.AttachmentSyncState
import ch.protonmail.android.mailmessage.domain.model.MessageId
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

class AttachmentsFacade @Inject constructor(
    private val observeMessageAttachments: ObserveMessageAttachments,
    private val storeAttachments: StoreAttachments,
    private val storeExternalAttachments: StoreExternalAttachments,
    private val deleteAttachment: DeleteAttachment,
    private val deleteAllAttachments: DeleteAllAttachments,
    private val reEncryptAttachments: ReEncryptAttachments
) {

    fun observeMessageAttachments(userId: UserId, messageId: MessageId) =
        observeMessageAttachments.invoke(userId, messageId)

    suspend fun storeAttachments(
        userId: UserId,
        messageId: MessageId,
        senderEmail: SenderEmail,
        uriList: List<Uri>
    ) = storeAttachments.invoke(userId, messageId, senderEmail, uriList)

    suspend fun storeExternalAttachments(
        userId: UserId,
        messageId: MessageId,
        syncState: AttachmentSyncState = AttachmentSyncState.ExternalUploaded
    ) = storeExternalAttachments.invoke(userId, messageId, syncState)

    suspend fun deleteAttachment(
        userId: UserId,
        messageId: MessageId,
        senderEmail: SenderEmail,
        attachmentId: AttachmentId
    ) = deleteAttachment.invoke(userId, senderEmail, messageId, attachmentId)

    suspend fun deleteAllAttachments(
        userId: UserId,
        senderEmail: SenderEmail,
        messageId: MessageId
    ) = deleteAllAttachments.invoke(userId, senderEmail, messageId)

    suspend fun reEncryptAttachments(
        userId: UserId,
        messageId: MessageId,
        previousSender: SenderEmail,
        newSender: SenderEmail
    ) = reEncryptAttachments.invoke(userId, messageId, previousSender, newSender)
}
