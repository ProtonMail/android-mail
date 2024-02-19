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

import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.either
import ch.protonmail.android.mailcommon.domain.util.mapFalse
import ch.protonmail.android.mailcomposer.domain.Transactor
import ch.protonmail.android.mailcomposer.domain.model.MessageWithDecryptedBody
import ch.protonmail.android.mailcomposer.domain.model.SenderEmail
import ch.protonmail.android.mailmessage.domain.model.AttachmentId
import ch.protonmail.android.mailmessage.domain.model.AttachmentSyncState
import ch.protonmail.android.mailmessage.domain.model.DraftAction
import ch.protonmail.android.mailmessage.domain.model.MessageAttachment
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.model.MimeType
import ch.protonmail.android.mailmessage.domain.repository.AttachmentRepository
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import javax.inject.Inject

class StoreDraftWithParentAttachments @Inject constructor(
    private val attachmentRepository: AttachmentRepository,
    private val deleteAllAttachments: DeleteAllAttachments,
    private val getLocalDraft: GetLocalDraft,
    private val saveDraft: SaveDraft,
    private val storeParentAttachmentStates: StoreParentAttachmentStates,
    private val transactor: Transactor
) {

    suspend operator fun invoke(
        userId: UserId,
        messageId: MessageId,
        parentMessage: MessageWithDecryptedBody,
        senderEmail: SenderEmail,
        draftAction: DraftAction
    ): Either<Error, Unit> = either {
        transactor.performTransaction {

            val parentAttachments = getParentAttachments(draftAction, parentMessage.decryptedMessageBody.attachments)
            if (parentAttachments.isEmpty()) {
                Timber.d("No attachments to be stored from parent message")
                raise(Error.NoAttachmentsToBeStored)
            }

            Timber.d("Storing draft for action: $draftAction with parent attachments: $parentAttachments")
            saveDraftWithParentAttachments(userId, messageId, senderEmail, parentAttachments)

            val parentAttachmentIds = parentAttachments.map { it.attachmentId }

            if (parentMessage.messageWithBody.messageBody.mimeType == MimeType.MultipartMixed) {
                copyParentAttachmentsToDraft(
                    userId = userId,
                    senderEmail = senderEmail,
                    parentMessageId = parentMessage.decryptedMessageBody.messageId,
                    draftMessageId = messageId,
                    parentAttachmentIds = parentAttachmentIds
                )
            }

            storeParentAttachmentSyncStates(
                userId = userId,
                messageId = messageId,
                parentMessageMimeType = parentMessage.messageWithBody.messageBody.mimeType,
                parentAttachmentIds = parentAttachmentIds
            )
        }
    }

    private fun Raise<Error>.getParentAttachments(
        draftAction: DraftAction,
        parentMessageAttachments: List<MessageAttachment>
    ): List<MessageAttachment> = when (draftAction) {
        is DraftAction.PrefillForShare -> emptyList()
        is DraftAction.Forward -> parentMessageAttachments
        is DraftAction.Reply,
        is DraftAction.ReplyAll -> parentMessageAttachments.filter { it.disposition == "inline" }

        is DraftAction.Compose,
        is DraftAction.ComposeToAddresses -> {
            Timber.w("Store Draft with parent attachments for a Compose action. This shouldn't happen.")
            raise(Error.ActionWithNoParent)
        }
    }

    private suspend fun Raise<Error>.saveDraftWithParentAttachments(
        userId: UserId,
        messageId: MessageId,
        senderEmail: SenderEmail,
        parentAttachments: List<MessageAttachment>
    ) {
        val draftWithBody = getLocalDraft(userId, messageId, senderEmail)
            .mapLeft { Error.DraftDataError }
            .bind()
        val parentAttachmentsWithoutSignature = parentAttachments.map { it.copy(signature = null, encSignature = null) }
        val updatedDraft = draftWithBody.copy(
            messageBody = draftWithBody.messageBody.copy(
                attachments = draftWithBody.messageBody.attachments + parentAttachmentsWithoutSignature
            )
        )
        saveDraft(updatedDraft, userId)
            .mapFalse { Error.DraftDataError }
            .bind()
    }

    private suspend fun Raise<Error>.copyParentAttachmentsToDraft(
        userId: UserId,
        senderEmail: SenderEmail,
        parentMessageId: MessageId,
        draftMessageId: MessageId,
        parentAttachmentIds: List<AttachmentId>
    ) {
        attachmentRepository.copyMimeAttachmentsToMessage(
            userId = userId,
            sourceMessageId = parentMessageId,
            targetMessageId = draftMessageId,
            attachmentIds = parentAttachmentIds
        ).onLeft {
            deleteAllAttachments(userId, senderEmail, draftMessageId)
            raise(Error.DraftAttachmentError)
        }
    }

    private suspend fun Raise<Error>.storeParentAttachmentSyncStates(
        userId: UserId,
        messageId: MessageId,
        parentMessageMimeType: MimeType,
        parentAttachmentIds: List<AttachmentId>
    ) {
        val syncState = if (parentMessageMimeType == MimeType.MultipartMixed) {
            AttachmentSyncState.Local
        } else {
            AttachmentSyncState.External
        }
        storeParentAttachmentStates(userId, messageId, parentAttachmentIds, syncState)
            .mapLeft { Error.DraftAttachmentError }
            .bind()
    }

    sealed interface Error {
        object DraftDataError : Error
        object DraftAttachmentError : Error
        object ActionWithNoParent : Error
        object NoAttachmentsToBeStored : Error
    }
}
