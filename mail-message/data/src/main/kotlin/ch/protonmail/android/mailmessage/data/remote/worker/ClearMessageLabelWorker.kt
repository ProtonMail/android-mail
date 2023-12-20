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
import ch.protonmail.android.mailmessage.data.local.MessageLocalDataSource
import ch.protonmail.android.mailmessage.data.remote.MessageApi
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId
import me.proton.core.network.data.ApiProvider
import me.proton.core.network.domain.ApiResult
import me.proton.core.network.domain.isRetryable
import me.proton.core.util.kotlin.takeIfNotBlank

@HiltWorker
class ClearMessageLabelWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParameters: WorkerParameters,
    private val apiProvider: ApiProvider,
    private val messageLocalDataSource: MessageLocalDataSource
) : CoroutineWorker(context, workerParameters) {

    override suspend fun doWork(): Result {
        val userId = inputData.getString(RawUserIdKey)?.takeIfNotBlank()
        val labelId = inputData.getString(RawLabelIdKey)?.takeIfNotBlank()

        if (userId == null || labelId == null) {
            return Result.failure()
        }

        val result = apiProvider.get<MessageApi>(UserId(userId)).invoke { emptyLabel(labelId) }

        return when (result) {
            is ApiResult.Success -> {
                messageLocalDataSource.deleteMessagesWithLabel(userId = UserId(userId), labelId = LabelId(labelId))
                Result.success()
            }
            is ApiResult.Error -> {
                if (result.isRetryable()) Result.retry()
                else Result.failure()
            }
        }
    }

    companion object {

        internal const val RawUserIdKey = "userId"
        internal const val RawLabelIdKey = "labelId"

        fun params(userId: UserId, labelId: LabelId) = mapOf(
            RawUserIdKey to userId.id,
            RawLabelIdKey to labelId.id
        )

        fun id(userId: UserId, labelId: LabelId): String = "ClearLabelWorker-${userId.id}-${labelId.id}"
    }
}
