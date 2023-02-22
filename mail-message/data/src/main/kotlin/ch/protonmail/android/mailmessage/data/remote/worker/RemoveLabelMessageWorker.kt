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
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import ch.protonmail.android.mailcommon.domain.util.requireNotBlank
import ch.protonmail.android.mailmessage.data.local.MessageLocalDataSource
import ch.protonmail.android.mailmessage.data.remote.MessageApi
import ch.protonmail.android.mailmessage.data.remote.resource.PutLabelBody
import ch.protonmail.android.mailmessage.domain.entity.MessageId
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId
import me.proton.core.network.data.ApiProvider
import me.proton.core.network.domain.ApiResult
import me.proton.core.network.domain.isRetryable

@HiltWorker
class RemoveLabelMessageWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParameters: WorkerParameters,
    private val apiProvider: ApiProvider,
    private val messageLocalDataSource: MessageLocalDataSource
) : CoroutineWorker(context, workerParameters) {

    override suspend fun doWork(): Result {
        val userId = requireNotBlank(inputData.getString(RawUserIdKey), fieldName = "User id")
        val messageId = requireNotBlank(inputData.getString(RawMessageIdKey), fieldName = "Message id")
        val labelId = requireNotBlank(inputData.getString(RawLabelIdKey), fieldName = "Label id")

        val result = apiProvider.get<MessageApi>(UserId(userId)).invoke {
            removeLabel(
                PutLabelBody(
                    labelId = labelId,
                    messageIds = listOf(messageId)
                )
            )
        }

        return when (result) {
            is ApiResult.Success -> Result.success()
            is ApiResult.Error -> {
                if (result.isRetryable()) return Result.retry()

                messageLocalDataSource.addLabel(UserId(userId), MessageId(messageId), LabelId(labelId))
                Result.failure()
            }
        }
    }

    companion object {

        internal const val RawUserIdKey = "removeLabelWorkParamUserId"
        internal const val RawMessageIdKey = "removeLabelWorkParamMessageId"
        internal const val RawLabelIdKey = "removeLabelWorkParamLabelId"

        fun params(
            userId: UserId,
            messageId: MessageId,
            labelId: LabelId
        ) = mapOf(
            RawUserIdKey to userId.id,
            RawMessageIdKey to messageId.id,
            RawLabelIdKey to labelId.id
        )
    }
}
