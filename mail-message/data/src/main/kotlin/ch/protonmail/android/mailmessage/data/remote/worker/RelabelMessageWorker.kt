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
import ch.protonmail.android.mailcommon.domain.util.requireNotBlank
import ch.protonmail.android.mailmessage.data.local.MessageLocalDataSource
import ch.protonmail.android.mailmessage.data.remote.MessageApi
import ch.protonmail.android.mailmessage.data.remote.resource.PostRelabelBody
import ch.protonmail.android.mailmessage.domain.entity.MessageId
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId
import me.proton.core.network.data.ApiProvider
import me.proton.core.network.domain.ApiResult
import me.proton.core.network.domain.isRetryable
import javax.inject.Inject

@HiltWorker
class RelabelMessageWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParameters: WorkerParameters,
    private val apiProvider: ApiProvider,
    private val messageLocalDataSource: MessageLocalDataSource
) : CoroutineWorker(context, workerParameters) {

    override suspend fun doWork(): Result {
        val userId = requireNotBlank(inputData.getString(RawUserIdKey), fieldName = "User id")
        val messageId = requireNotBlank(inputData.getString(RawMessageIdKey), fieldName = "Message id")
        val labelIds = inputData.getStringArray(RawLabelListKey)

        if (labelIds.isNullOrEmpty()) {
            return Result.failure()
        }

        val result = apiProvider.get<MessageApi>(UserId(userId)).invoke {
            relabel(
                messageId = messageId,
                putLabelBody = PostRelabelBody(labelIds = labelIds.toList())
            )
        }

        return when (result) {
            is ApiResult.Success -> Result.success()
            is ApiResult.Error -> {
                if (result.isRetryable()) return Result.retry()
                else {
                    messageLocalDataSource.relabel(UserId(userId), MessageId(messageId), labelIds.map { LabelId(it) })
                    Result.failure()
                }
            }
        }
    }

    companion object {


        const val RawUserIdKey: String = "relabelMessageWorkParamUserId"
        const val RawMessageIdKey: String = "relabelMessageWorkParamMessageId"
        const val RawLabelListKey: String = "relabelMessageWorkParamLabelList"
    }

    class Enqueuer @Inject constructor(private val workManager: WorkManager) {

        fun enqueue(
            userId: UserId,
            message: MessageId,
            labelList: List<LabelId>
        ) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val data = workDataOf(
                RawUserIdKey to userId.id,
                RawMessageIdKey to message.id,
                RawLabelListKey to labelList.map { it.id }.toTypedArray()
            )

            val request = OneTimeWorkRequestBuilder<RelabelMessageWorker>()
                .setConstraints(constraints)
                .setInputData(data)
                .build()

            workManager.enqueue(request)
        }

    }
}
