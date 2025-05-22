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

import arrow.core.getOrElse
import ch.protonmail.android.mailcommon.domain.coroutines.DefaultDispatcher
import ch.protonmail.android.mailcomposer.domain.model.DraftBody
import ch.protonmail.android.mailcomposer.domain.model.DraftFields
import ch.protonmail.android.mailcomposer.domain.model.MessageWithDecryptedBody
import ch.protonmail.android.mailcomposer.domain.model.SenderEmail
import ch.protonmail.android.mailcomposer.domain.usecase.DraftUploader
import ch.protonmail.android.mailcomposer.domain.usecase.GetDecryptedDraftFields
import ch.protonmail.android.mailcomposer.domain.usecase.GetLocalMessageDecrypted
import ch.protonmail.android.mailcomposer.domain.usecase.ProvideNewDraftId
import ch.protonmail.android.mailcomposer.domain.usecase.StoreDraftWithAllFields
import ch.protonmail.android.mailcomposer.domain.usecase.StoreDraftWithParentAttachments
import ch.protonmail.android.mailcomposer.presentation.usecase.InjectAddressSignature
import ch.protonmail.android.mailcomposer.presentation.usecase.ParentMessageToDraftFields
import ch.protonmail.android.mailmessage.domain.model.DraftAction
import ch.protonmail.android.mailmessage.domain.model.MessageId
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withContext
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import javax.inject.Inject

class DraftFacade @Inject constructor(
    private val provideNewDraftId: ProvideNewDraftId,
    private val getDecryptedDraftFields: GetDecryptedDraftFields,
    private val getLocalMessageDecrypted: GetLocalMessageDecrypted,
    private val parentMessageToDraftFields: ParentMessageToDraftFields,
    private val storeDraftWithAllFields: StoreDraftWithAllFields,
    private val storeDraftWithParentAttachments: StoreDraftWithParentAttachments,
    private val injectAddressSignature: InjectAddressSignature,
    private val draftUploader: DraftUploader,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) {

    fun provideNewDraftId() = provideNewDraftId.invoke()

    suspend fun getDecryptedDraftFields(userId: UserId, messageId: MessageId) =
        getDecryptedDraftFields.invoke(userId, messageId)

    suspend fun parentMessageToDraftFields(
        userId: UserId,
        messageId: MessageId,
        action: DraftAction
    ): Pair<MessageWithDecryptedBody, DraftFields>? = withContext(defaultDispatcher) {
        val parentMessage = getLocalMessageDecrypted.invoke(userId, messageId).getOrElse {
            Timber.d("Failed to get local message decrypted.")
            return@withContext null
        }

        val fields = parentMessageToDraftFields.invoke(userId, parentMessage, action).getOrElse {
            Timber.d("Failed to draft fields from parent message.")
            return@withContext null
        }

        parentMessage to fields
    }

    suspend fun storeDraft(
        userId: UserId,
        draftMessageId: MessageId,
        fields: DraftFields,
        action: DraftAction
    ) = storeDraftWithAllFields(
        userId,
        draftMessageId,
        fields,
        action
    )

    suspend fun injectAddressSignature(
        userId: UserId,
        draftBody: DraftBody,
        senderEmail: SenderEmail,
        previousSenderEmail: SenderEmail?
    ) = injectAddressSignature.invoke(userId, draftBody, senderEmail, previousSenderEmail)

    suspend fun storeDraftWithParentAttachments(
        userId: UserId,
        messageId: MessageId,
        parentMessage: MessageWithDecryptedBody,
        senderEmail: SenderEmail,
        draftAction: DraftAction
    ) = storeDraftWithParentAttachments.invoke(
        userId,
        messageId,
        parentMessage,
        senderEmail,
        draftAction
    )

    fun startContinuousUpload(
        userId: UserId,
        messageId: MessageId,
        action: DraftAction,
        scope: CoroutineScope
    ) = draftUploader.startContinuousUpload(
        userId = userId,
        messageId = messageId,
        action = action,
        scope = scope
    )

    fun stopContinuousUpload() = draftUploader.stopContinuousUpload()

    suspend fun forceUpload(userId: UserId, messageId: MessageId) = draftUploader.upload(userId, messageId)
}
