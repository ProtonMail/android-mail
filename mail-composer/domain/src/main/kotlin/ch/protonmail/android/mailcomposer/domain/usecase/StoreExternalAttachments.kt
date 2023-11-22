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

package ch.protonmail.android.mailcomposer.domain.usecase

import ch.protonmail.android.mailcomposer.domain.repository.AttachmentStateRepository
import ch.protonmail.android.mailmessage.domain.model.AttachmentSyncState
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import javax.inject.Inject

class StoreExternalAttachments @Inject constructor(
    private val messageRepository: MessageRepository,
    private val attachmentStateRepository: AttachmentStateRepository
) {

    suspend operator fun invoke(
        userId: UserId,
        messageId: MessageId,
        syncState: AttachmentSyncState = AttachmentSyncState.ExternalUploaded
    ) {
        val messageBody = messageRepository.getMessageWithBody(userId, messageId).getOrNull()?.messageBody

        if (messageBody == null) {
            Timber.e("Failed to get message with body")
            return
        }

        messageBody.attachments.let { attachments ->
            val states = attachmentStateRepository.getAllAttachmentStatesForMessage(userId, messageId)
            attachments
                .filterNot { attachment -> attachment.attachmentId in states.map { it.attachmentId } }
                .takeIf { it.isNotEmpty() }
                ?.map { it.attachmentId }
                ?.let { attachmentIds ->
                    attachmentStateRepository.createOrUpdateLocalStates(userId, messageId, attachmentIds, syncState)
                        .onLeft { Timber.e("Failed to create or update local states: $it") }
                }
        }

    }
}
