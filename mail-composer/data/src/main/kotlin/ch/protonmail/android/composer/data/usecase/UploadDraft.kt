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

package ch.protonmail.android.composer.data.usecase

import arrow.core.Either
import arrow.core.raise.either
import ch.protonmail.android.composer.data.remote.DraftRemoteDataSource
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcomposer.domain.Transactor
import ch.protonmail.android.mailcomposer.domain.usecase.CreateOrUpdateParentAttachmentStates
import ch.protonmail.android.mailcomposer.domain.usecase.DraftUploadTracker
import ch.protonmail.android.mailcomposer.domain.usecase.FindLocalDraft
import ch.protonmail.android.mailcomposer.domain.usecase.IsDraftKnownToApi
import ch.protonmail.android.mailmessage.domain.model.DraftState
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.model.MessageWithBody
import ch.protonmail.android.mailmessage.domain.repository.AttachmentRepository
import ch.protonmail.android.mailmessage.domain.repository.DraftStateRepository
import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import kotlinx.coroutines.flow.first
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import javax.inject.Inject

internal class UploadDraft @Inject constructor(
    private val transactor: Transactor,
    private val messageRepository: MessageRepository,
    private val findLocalDraft: FindLocalDraft,
    private val draftStateRepository: DraftStateRepository,
    private val draftRemoteDataSource: DraftRemoteDataSource,
    private val isDraftKnownToApi: IsDraftKnownToApi,
    private val attachmentRepository: AttachmentRepository,
    private val updateParentAttachments: CreateOrUpdateParentAttachmentStates,
    private val draftUploadTracker: DraftUploadTracker
) {

    suspend operator fun invoke(userId: UserId, messageId: MessageId): Either<DataError, Unit> = either {
        Timber.d("Draft: Requested draft upload for $messageId")

        val message = findLocalDraft(userId, messageId)
        if (message == null) {
            Timber.d("Sync draft failure $messageId: No message found")
            shift<MessageWithBody>(DataError.Local.NoDataCached)
            // Return for the compiler's sake (message optionality). shift is causing a left to be returned just above
            return@either
        }
        Timber.d("Draft: Uploading draft for ${message.message.messageId}")

        val draftState = draftStateRepository.observe(userId, messageId).first().onLeft {
            Timber.w("Sync draft failure $messageId: No draft state found")
        }.bind()

        if (isDraftKnownToApi(draftState)) {
            handleUpdateDraft(userId, message, messageId).bind()
        } else {
            handleCreateDraft(userId, message, draftState, messageId).bind()

        }
    }

    private suspend fun handleCreateDraft(
        userId: UserId,
        message: MessageWithBody,
        draftState: DraftState,
        messageId: MessageId
    ) = draftRemoteDataSource.create(userId, message, draftState.action).onRight {
        transactor.performTransaction {
            messageRepository.updateDraftRemoteIds(userId, messageId, it.message.messageId, it.message.conversationId)
            draftStateRepository.updateApiMessageIdAndSetSyncedState(userId, messageId, it.message.messageId)
            updateAttachmentsData(message, it)
        }
    }.onLeft {
        if (it.shouldLogToSentry()) {
            Timber.w("Sync draft failure $messageId: Create API call error $it")
        }
        Timber.d("Sync draft error $messageId: Create API call error $it")
    }

    private suspend fun handleUpdateDraft(
        userId: UserId,
        message: MessageWithBody,
        messageId: MessageId
    ) = draftRemoteDataSource.update(userId, message).onRight {
        draftStateRepository.updateApiMessageIdAndSetSyncedState(
            userId, it.message.messageId, it.message.messageId
        )
        draftUploadTracker.notifyUploadedDraft(messageId, message)
    }.onLeft {
        Timber.w("Sync draft failure $messageId: Update API call error $it")
    }

    /*
     * Matches local attachments with remote ones by keyPackets, name and size and updates their ID.
     * This is needed to refresh the data of "parent attachments", which to support offline mode are copied for
     * this message and only receive their "real" id when contacting API to create the draft.
     */
    private suspend fun updateAttachmentsData(localMessage: MessageWithBody, apiMessage: MessageWithBody) {
        val localAttachments = localMessage.messageBody.attachments
        val remoteAttachments = apiMessage.messageBody.attachments

        remoteAttachments.forEach { attachment ->
            localAttachments.find {
                it.keyPackets == attachment.keyPackets &&
                    it.name == attachment.name && it.mimeType == attachment.mimeType
            }?.let { localAttachment ->
                attachmentRepository.updateMessageAttachment(
                    apiMessage.message.userId,
                    apiMessage.message.messageId,
                    localAttachment.attachmentId,
                    attachment
                )
            } ?: Timber.w("Attachment not found in local message: $attachment")
        }
        if (remoteAttachments.isNotEmpty()) {
            updateParentAttachments(
                userId = apiMessage.message.userId,
                messageId = apiMessage.message.messageId,
                attachmentIds = remoteAttachments.map { it.attachmentId }
            )
        }
    }

    private fun DataError.Remote.shouldLogToSentry() = this != DataError.Remote.CreateDraftRequestNotPerformed

}
