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

import android.net.Uri
import arrow.core.Either
import arrow.core.continuations.either
import ch.protonmail.android.mailcomposer.domain.Transactor
import ch.protonmail.android.mailcomposer.domain.model.SenderEmail
import ch.protonmail.android.mailcomposer.domain.repository.AttachmentStateRepository
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.repository.AttachmentRepository
import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import javax.inject.Inject

class StoreAttachments @Inject constructor(
    private val messageRepository: MessageRepository,
    private val attachmentRepository: AttachmentRepository,
    private val attachmentStateRepository: AttachmentStateRepository,
    private val getLocalDraft: GetLocalDraft,
    private val saveDraft: SaveDraft,
    private val provideNewAttachmentId: ProvideNewAttachmentId,
    private val transactor: Transactor
) {

    suspend operator fun invoke(
        userId: UserId,
        messageId: MessageId,
        senderEmail: SenderEmail,
        uriList: List<Uri>
    ): Either<StoreDraftWithAttachmentError, Unit> = either {
        if (uriList.isEmpty()) {
            shift<StoreDraftWithAttachmentError>(StoreDraftWithAttachmentError.AttachmentsMissing)
        }
        transactor.performTransaction {
            val draft = getLocalDraft(userId, messageId, senderEmail)
                .mapLeft { StoreDraftWithAttachmentError.FailedReceivingDraft }
                .bind()

            // Verify that draft exists in db, if not create it
            val messageWithBody = messageRepository.getLocalMessageWithBody(userId, messageId)
            Timber.d("Draft exists in db: ${messageWithBody != null}")
            if (messageWithBody == null) {
                val success = saveDraft(draft, userId)
                if (!success) {
                    Timber.d("Failed to save draft")
                    shift<StoreDraftWithAttachmentError>(StoreDraftWithAttachmentError.FailedReceivingDraft)
                }
            }
            var attachmentFailedToStore = false
            uriList.forEach {
                val attachmentId = provideNewAttachmentId()
                attachmentRepository.saveAttachment(userId, draft.message.messageId, attachmentId, it)
                    .onLeft { attachmentFailedToStore = true }
                    .onRight { attachmentStateRepository.createOrUpdateLocalState(userId, messageId, attachmentId) }
            }
            if (attachmentFailedToStore) {
                shift<StoreDraftWithAttachmentError>(StoreDraftWithAttachmentError.FailedToStoreAttachments)
            }
        }
    }
}

sealed interface StoreDraftWithAttachmentError {
    object AttachmentsMissing : StoreDraftWithAttachmentError
    object FailedReceivingDraft : StoreDraftWithAttachmentError
    object FailedToStoreAttachments : StoreDraftWithAttachmentError
}
