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

package ch.protonmail.android.maildetail.domain.repository

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailcommon.domain.util.requireNotBlank
import ch.protonmail.android.maildetail.domain.usecase.MarkMessageAndConversationReadIfAllMessagesRead
import ch.protonmail.android.mailmessage.domain.model.MessageId
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.toJavaDuration

@HiltWorker
class MarkMessageAndConversationReadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParameters: WorkerParameters,
    private val markMessageAndConversationReadIfAllMessagesRead: MarkMessageAndConversationReadIfAllMessagesRead
) : CoroutineWorker(context, workerParameters) {

    override suspend fun doWork(): Result {
        val userId = UserId(requireNotBlank(inputData.getString(RawUserIdKey), fieldName = "User id"))
        val messageId = MessageId(requireNotBlank(inputData.getString(RawMessageIdKey), fieldName = "Message id"))
        val conversationId = ConversationId(
            requireNotBlank(
                inputData.getString(RawConversationIdKey),
                fieldName = "Conversation id"
            )
        )

        return markMessageAndConversationReadIfAllMessagesRead(userId, messageId, conversationId).fold(
            ifLeft = {
                Timber.e("Failed to mark message and conversation read: $it")
                Result.failure()
            },
            ifRight = {
                Result.success()
            }
        )
    }

    class Enqueuer @Inject constructor(
        private val workManager: WorkManager
    ) {

        fun enqueue(
            userId: UserId,
            params: Map<String, Any>,
            initialDelay: Duration = 0.milliseconds
        ) {
            // Create a new work request
            val data = workDataOf(*params.map { Pair(it.key, it.value) }.toTypedArray())

            val workRequest = OneTimeWorkRequestBuilder<MarkMessageAndConversationReadWorker>().run {
                setInputData(data)
                addTag(userId.id)
                setInitialDelay(initialDelay.toJavaDuration())
                build()
            }

            workManager.enqueue(workRequest)
        }
    }

    companion object {

        internal const val RawUserIdKey = "MarkMessageAndConversationReadWorkerParamUserId"
        internal const val RawMessageIdKey = "MarkMessageAndConversationReadWorkerMessageId"
        internal const val RawConversationIdKey = "MarkMessageAndConversationReadWorkerConversationId"

        fun params(
            userId: UserId,
            messageId: MessageId,
            conversationId: ConversationId
        ) = mapOf(
            RawUserIdKey to userId.id,
            RawMessageIdKey to messageId.id,
            RawConversationIdKey to conversationId.id
        )
    }
}
