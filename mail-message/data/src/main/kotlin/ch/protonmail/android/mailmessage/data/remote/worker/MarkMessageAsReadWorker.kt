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
import ch.protonmail.android.mailmessage.data.remote.resource.MarkMessageAsReadBody
import ch.protonmail.android.mailmessage.domain.entity.MessageId
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import me.proton.core.domain.entity.UserId
import me.proton.core.network.data.ApiProvider
import me.proton.core.network.domain.ApiResult
import me.proton.core.network.domain.isRetryable
import me.proton.core.util.kotlin.deserializeList
import me.proton.core.util.kotlin.serialize

@HiltWorker
class MarkMessageAsReadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParameters: WorkerParameters,
    private val apiProvider: ApiProvider,
    private val messageLocalDataSource: MessageLocalDataSource
) : CoroutineWorker(context, workerParameters) {

    override suspend fun doWork(): Result {
        val userId = requireNotBlank(inputData.getString(RawUserIdKey), fieldName = "User id")
            .let(::UserId)
        val messageIds = requireNotBlank(inputData.getString(RawMessageIdsKey), fieldName = "Message id")
            .deserializeList<String>()

        val api = apiProvider.get<MessageApi>(userId)
        val requestBody = MarkMessageAsReadBody(
            messageIds = messageIds
        )
        val result = api {
            markAsRead(requestBody)
        }

        return when (result) {
            is ApiResult.Success -> Result.success()
            is ApiResult.Error -> {
                if (result.isRetryable()) {
                    Result.retry()
                } else {
                    messageLocalDataSource.markUnread(userId, messageIds.map { MessageId(it) })
                    Result.failure()
                }
            }
        }
    }

    companion object {

        internal const val RawUserIdKey = "markReadWorkParamUserId"
        internal const val RawMessageIdsKey = "markReadWorkParamMessageId"

        fun params(userId: UserId, messageIds: List<MessageId>) = mapOf(
            RawUserIdKey to userId.id,
            RawMessageIdsKey to messageIds.map { it.id }.serialize()
        )
    }
}
