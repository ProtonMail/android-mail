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

package ch.protonmail.android.composer.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import arrow.core.Either
import ch.protonmail.android.mailcommon.domain.AppInBackgroundState
import ch.protonmail.android.mailcommon.domain.annotation.MissingRustApi
import ch.protonmail.android.mailcomposer.domain.model.MessageSendingStatus
import ch.protonmail.android.mailcomposer.domain.usecase.MarkMessageSendingStatusesAsSeen
import ch.protonmail.android.mailcomposer.domain.usecase.QueryUnseenMessageSendingStatuses
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailsession.domain.background.PendingSendTracker
import ch.protonmail.android.mailsession.domain.repository.EventLoopRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import me.proton.core.domain.entity.UserId
import me.proton.core.util.kotlin.takeIfNotBlank
import timber.log.Timber

@MissingRustApi
// Rust will provide new API to check if the message unsent and in the queue
// Then we need to update this implementation

@HiltWorker
class SendingStatusWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParameters: WorkerParameters,
    private val queryUnseenDraftSendResults: QueryUnseenMessageSendingStatuses,
    private val markMessageSendingStatusesAsSeen: MarkMessageSendingStatusesAsSeen,
    private val eventLoopRepository: EventLoopRepository,
    private val appInBackgroundState: AppInBackgroundState,
    private val pendingSendTracker: PendingSendTracker
) : CoroutineWorker(context, workerParameters) {

    override suspend fun doWork(): Result {
        Timber.d("SendingStatusWorker: Checking draft send status")

        val userId = inputData.getString(RawUserIdKey)?.takeIfNotBlank()?.let { UserId(it) }
        val messageId = inputData.getString(RawMessageIdKey)?.takeIfNotBlank()?.let { MessageId(it) }

        if (userId == null || messageId == null) {
            Timber.e("SendingStatusWorker: Missing userId or messageId")
            return Result.failure()
        }

        return monitorMessageSending(userId, messageId)
    }

    private suspend fun monitorMessageSending(userId: UserId, messageId: MessageId): Result {
        return when (val result = queryUnseenDraftSendResults(userId)) {
            is Either.Right -> {
                val messageStatus = result.value.find { it.messageId == messageId }

                when {
                    messageStatus != null -> {
                        Timber.d("Message status found: $messageStatus")
                        confirmMessageStatus(userId, messageStatus)
                    }
                    runAttemptCount >= MaxRetries -> {
                        Timber.d("Max retries reached, stopping worker.")
                        Result.failure()
                    }
                    else -> {
                        Timber.d("No status found for message, we will retry. Run attempt: $runAttemptCount")
                        eventLoopRepository.trigger(userId)
                        Result.retry()
                    }
                }
            }

            is Either.Left -> {
                Timber.e("Error fetching draft send results - ${result.value}")
                Result.retry()
            }
        }
    }

    private suspend fun confirmMessageStatus(userId: UserId, messageStatus: MessageSendingStatus): Result {
        // The send reached a terminal status, so it is no longer queued.
        pendingSendTracker.onSendCompleted(messageStatus.messageId.id)
        if (appInBackgroundState.isAppInBackground()) {
            Timber.d("App is in background, marking message as seen.")
            markMessageSendingStatusesAsSeen(userId, listOf(messageStatus.messageId))
        }
        Timber.d("Stopping worker.")
        return Result.success()
    }

    companion object {
        private const val MaxRetries = 9

        const val RawUserIdKey = "SendingStatusWorkerUserId"
        const val RawMessageIdKey = "SendingStatusWorkerMessageId"

        fun params(userId: UserId, messageId: MessageId) = mapOf(
            RawUserIdKey to userId.id,
            RawMessageIdKey to messageId.id
        )

        fun id(userId: UserId, messageId: MessageId): String = "SendingStatusWorker-${userId.id}-${messageId.id}"
    }
}
