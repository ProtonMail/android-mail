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

import ch.protonmail.android.mailcomposer.domain.model.SenderEmail
import ch.protonmail.android.mailcomposer.domain.repository.AttachmentRepository
import ch.protonmail.android.mailmessage.domain.model.MessageId
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import javax.inject.Inject

class DeleteAllAttachments @Inject constructor(
    private val localDraft: GetLocalDraft,
    private val attachmentRepository: AttachmentRepository
) {

    suspend operator fun invoke(
        userId: UserId,
        senderEmail: SenderEmail,
        messageId: MessageId
    ) {
        val localDraft = localDraft(userId, messageId, senderEmail).getOrNull()

        if (localDraft == null) {
            Timber.e("Failed to load local draft")
            return
        }

        localDraft.messageBody.attachments.forEach {
            attachmentRepository.deleteAttachment(userId, localDraft.message.messageId, it.attachmentId)
        }
    }
}
