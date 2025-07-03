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

package ch.protonmail.android.composer.data.remote

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import ch.protonmail.android.composer.data.annotations.ApiSendingErrorsEnabled
import ch.protonmail.android.composer.data.usecase.SendMessage
import ch.protonmail.android.mailcommon.domain.util.requireNotBlank
import ch.protonmail.android.mailcomposer.domain.usecase.UpdateDraftStateForError
import ch.protonmail.android.mailmessage.domain.model.DraftSyncState
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.repository.DraftStateRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import me.proton.core.domain.entity.UserId
import timber.log.Timber

@HiltWorker
internal class SendMessageWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParameters: WorkerParameters,
    private val sendMessage: SendMessage,
    private val draftStateRepository: DraftStateRepository,
    private val updateDraftStateForError: UpdateDraftStateForError,
    @ApiSendingErrorsEnabled private val isApiSendingErrorsEnabled: Boolean
) : CoroutineWorker(context, workerParameters) {

    override suspend fun doWork(): Result {
        val userId = UserId(requireNotBlank(inputData.getString(RawUserIdKey), fieldName = "User id"))
        val messageId = MessageId(requireNotBlank(inputData.getString(RawMessageIdKey), fieldName = "Message id"))

        return sendMessage(userId, messageId).fold(
            ifLeft = {
                Timber
                    .tag("SendMessageWorker")
                    .e("API error sending message - error: %s - messageId: %s", it, messageId)

                updateDraftStateForError(
                    userId, messageId, DraftSyncState.ErrorSending,
                    sendingError = it.toSendingError(isApiSendingErrorsEnabled)
                )
                Result.failure()
            },
            ifRight = {
                draftStateRepository.updateDraftSyncState(userId, messageId, DraftSyncState.Sent, null)
                Result.success()
            }
        )
    }

    companion object {

        internal const val RawUserIdKey = "sendMessageWorkParamUserId"
        internal const val RawMessageIdKey = "sendMessageWorkParamMessageId"

        fun params(userId: UserId, messageId: MessageId) = mapOf(
            RawUserIdKey to userId.id,
            RawMessageIdKey to messageId.id
        )

        fun id(messageId: MessageId): String = "SendMessageWorker-${messageId.id}"
    }
}
