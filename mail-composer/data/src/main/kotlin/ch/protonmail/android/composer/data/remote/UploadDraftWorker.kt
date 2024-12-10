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
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.await
import ch.protonmail.android.composer.data.extension.awaitCompletion
import ch.protonmail.android.composer.data.usecase.UploadDraft
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.model.isMessageAlreadySentDraftError
import ch.protonmail.android.mailcommon.domain.util.requireNotBlank
import ch.protonmail.android.mailcomposer.domain.usecase.UpdateDraftStateForError
import ch.protonmail.android.mailmessage.domain.model.DraftSyncState
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.model.SendingError
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import me.proton.core.domain.entity.UserId

@HiltWorker
internal class UploadDraftWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted private val workerParameters: WorkerParameters,
    private val workManager: WorkManager,
    private val uploadDraft: UploadDraft,
    private val updateDraftStateForError: UpdateDraftStateForError
) : CoroutineWorker(context, workerParameters) {

    override suspend fun doWork(): Result {
        val userId = UserId(requireNotBlank(inputData.getString(RawUserIdKey), fieldName = "User id"))
        val messageId = MessageId(requireNotBlank(inputData.getString(RawMessageIdKey), fieldName = "Message ids"))

        if (shouldAwaitPreviousDraftUploadExecution(messageId)) return Result.retry()

        return uploadDraft(userId, messageId).fold(
            ifLeft = {
                updateDraftStateForError(userId, messageId, DraftSyncState.ErrorUploadDraft, it.toSendingError())
                return when (it) {
                    is DataError.Remote.Http -> if (it.isRetryable) Result.retry() else Result.failure()
                    else -> Result.failure()
                }
            },
            ifRight = {
                Result.success()
            }
        )
    }

    // Handles the case where the Draft upload is called as part of the Sending flow, and an existing UploadDraftWork
    // job triggered from the standard Upload flow is already enqueued, blocked or running.
    // In case it's enqueued or blocked, the job gets cancelled and the current job re-scheduled (via Retry).
    // If it's running we wait for the run to complete without cancellation. In any other case the worker runs directly.
    private suspend fun shouldAwaitPreviousDraftUploadExecution(messageId: MessageId): Boolean {
        if (!workerParameters.tags.contains(sendId(messageId))) return false

        val uploadDraftUniqueWorkName = id(messageId)
        val uniqueWorkInfo = workManager.getWorkInfosForUniqueWork(uploadDraftUniqueWorkName)
            .awaitCompletion()
            .firstOrNull()
            ?: return false

        return when (uniqueWorkInfo.state) {
            WorkInfo.State.ENQUEUED,
            WorkInfo.State.BLOCKED -> {
                workManager.cancelUniqueWork(uploadDraftUniqueWorkName).await()
                return true
            }

            WorkInfo.State.RUNNING -> true
            WorkInfo.State.SUCCEEDED,
            WorkInfo.State.FAILED,
            WorkInfo.State.CANCELLED -> false
        }
    }

    private fun DataError.toSendingError() = when {
        this.isMessageAlreadySentDraftError() -> SendingError.MessageAlreadySent
        else -> null
    }

    companion object {

        internal const val RawUserIdKey = "syncDraftWorkParamUserId"
        internal const val RawMessageIdKey = "syncDraftWorkParamMessageId"

        fun params(userId: UserId, messageId: MessageId) = mapOf(
            RawUserIdKey to userId.id,
            RawMessageIdKey to messageId.id
        )

        fun id(messageId: MessageId): String = "SyncDraftWorker-${messageId.id}"
        fun sendId(messageId: MessageId): String = "SendMessageWorker-${messageId.id}"
    }
}
