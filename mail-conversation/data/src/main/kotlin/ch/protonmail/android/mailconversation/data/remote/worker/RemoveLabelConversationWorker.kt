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

package ch.protonmail.android.mailconversation.data.remote.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailconversation.data.remote.ConversationApi
import ch.protonmail.android.mailconversation.data.remote.resource.PutConversationLabelBody
import ch.protonmail.android.mailconversation.domain.repository.ConversationLocalDataSource
import ch.protonmail.android.mailmessage.data.local.MessageLocalDataSource
import ch.protonmail.android.mailmessage.domain.entity.MessageId
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId
import me.proton.core.network.data.ApiProvider
import me.proton.core.network.domain.ApiResult
import me.proton.core.network.domain.isRetryable
import me.proton.core.util.kotlin.takeIfNotBlank
import javax.inject.Inject

@HiltWorker
class RemoveLabelConversationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParameters: WorkerParameters,
    private val apiProvider: ApiProvider,
    private val conversationLocalDataSource: ConversationLocalDataSource,
    private val messageLocalDataSource: MessageLocalDataSource
) : CoroutineWorker(context, workerParameters) {

    override suspend fun doWork(): Result {
        val userId = inputData.getString(RawUserIdKey)?.takeIfNotBlank()
        val conversationId =
            inputData.getString(RawConversationIdKey)?.takeIfNotBlank()
        val labelId = inputData.getString(RawLabelIdKey)?.takeIfNotBlank()
        val messageIds = inputData.getStringArray(RawAffectedMessageIds)?.toList()

        if (userId == null || conversationId == null || labelId == null) {
            return Result.failure()
        }

        val result = apiProvider.get<ConversationApi>(UserId(userId)).invoke {
            removeLabel(
                PutConversationLabelBody(
                    labelId = labelId,
                    conversationIds = listOf(conversationId)
                )
            )
        }

        return when (result) {
            is ApiResult.Success -> Result.success()
            is ApiResult.Error -> {
                if (result.isRetryable()) return Result.retry()
                else {
                    conversationLocalDataSource.addLabel(
                        UserId(userId),
                        ConversationId(conversationId),
                        LabelId(labelId)
                    )
                    messageIds?.map { MessageId(it) }?.forEach {
                        messageLocalDataSource.addLabel(UserId(userId), it, LabelId(labelId))
                    }
                    Result.failure()
                }
            }
        }
    }

    companion object {

        const val RawUserIdKey = "removeLabelConversationWorkParamUserId"
        const val RawConversationIdKey = "removeLabelConversationWorkParamMessageId"
        const val RawLabelIdKey = "removeLabelConversationWorkParamLabelId"
        const val RawAffectedMessageIds = "removeLabelConversationWorkParamMsgIds"

    }

    class Enqueuer @Inject constructor(private val workManager: WorkManager) {

        fun enqueue(
            userId: UserId,
            conversationId: ConversationId,
            labelId: LabelId,
            affectedMessageIds: List<MessageId>
        ) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val data = workDataOf(
                RawUserIdKey to userId.id,
                RawConversationIdKey to conversationId.id,
                RawLabelIdKey to labelId.id,
                RawAffectedMessageIds to affectedMessageIds.map { it.id }.toTypedArray()
            )

            val request = OneTimeWorkRequestBuilder<RemoveLabelConversationWorker>()
                .setConstraints(constraints)
                .setInputData(data)
                .build()

            workManager.enqueue(request)
        }
    }
}
