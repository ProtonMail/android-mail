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

package ch.protonmail.android.mailmessage.data.remote.worker

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
import ch.protonmail.android.mailmessage.data.remote.MessageApi
import ch.protonmail.android.mailmessage.data.remote.resource.ConversationLabelBody
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId
import me.proton.core.network.data.ApiProvider
import me.proton.core.network.domain.ApiResult
import me.proton.core.network.domain.isRetryable
import me.proton.core.util.kotlin.takeIfNotBlank
import javax.inject.Inject

internal const val KEY_REMOVE_LABEL_CONV_WORK_RAW_USER_ID = "removeLabelConversationWorkParamUserId"
internal const val KEY_REMOVE_LABEL_CONV_WORK_RAW_CONV_ID = "removeLabelConversationWorkParamMessageId"
internal const val KEY_REMOVE_LABEL_CONV_WORK_RAW_LABEL_ID = "removeLabelConversationWorkParamLabelId"

@HiltWorker
class RemoveLabelConversationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParameters: WorkerParameters,
    private val apiProvider: ApiProvider
) : CoroutineWorker(context, workerParameters) {

    override suspend fun doWork(): Result {
        val userId = inputData.getString(KEY_REMOVE_LABEL_CONV_WORK_RAW_USER_ID)?.takeIfNotBlank()
        val conversationId =
            inputData.getString(KEY_REMOVE_LABEL_CONV_WORK_RAW_CONV_ID)?.takeIfNotBlank()
        val labelId = inputData.getString(KEY_REMOVE_LABEL_CONV_WORK_RAW_LABEL_ID)?.takeIfNotBlank()

        if (userId == null || conversationId == null || labelId == null) {
            return Result.failure()
        }

        val result = apiProvider.get<MessageApi>(UserId(userId)).invoke {
            removeLabel(
                ConversationLabelBody(
                    labelId = labelId,
                    conversationIds = listOf(conversationId)
                )
            )
        }

        return when (result) {
            is ApiResult.Success -> Result.success()
            is ApiResult.Error -> {
                if (result.isRetryable()) return Result.retry()
                Result.failure()
            }
        }
    }

    class Enqueuer @Inject constructor(private val workManager: WorkManager) {

        fun enqueue(
            userId: UserId,
            conversationId: ConversationId,
            labelId: LabelId
        ) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val data = workDataOf(
                KEY_REMOVE_LABEL_CONV_WORK_RAW_USER_ID to userId.id,
                KEY_REMOVE_LABEL_CONV_WORK_RAW_CONV_ID to conversationId.id,
                KEY_REMOVE_LABEL_CONV_WORK_RAW_LABEL_ID to labelId.id
            )

            val request = OneTimeWorkRequestBuilder<RemoveLabelConversationWorker>()
                .setConstraints(constraints)
                .setInputData(data)
                .build()

            workManager.enqueue(request)
        }
    }
}
