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
import ch.protonmail.android.mailcommon.domain.util.requireNotBlank
import ch.protonmail.android.mailconversation.data.remote.ConversationApi
import ch.protonmail.android.mailconversation.data.remote.resource.MarkConversationAsUnreadBody
import ch.protonmail.android.mailconversation.domain.repository.ConversationLocalDataSource
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import me.proton.core.domain.entity.UserId
import me.proton.core.network.data.ApiProvider
import me.proton.core.network.domain.ApiResult
import me.proton.core.network.domain.isRetryable
import javax.inject.Inject

@HiltWorker
class MarkConversationAsUnreadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParameters: WorkerParameters,
    private val apiProvider: ApiProvider,
    private val conversationLocalDataSource: ConversationLocalDataSource
) : CoroutineWorker(context, workerParameters) {

    override suspend fun doWork(): Result {
        val userId = requireNotBlank(inputData.getString(RawUserIdKey), fieldName = "User id")
            .let(::UserId)
        val conversationId = requireNotBlank(inputData.getString(RawConversationIdKey), fieldName = "Conversation id")
            .let(::ConversationId)

        val api = apiProvider.get<ConversationApi>(userId)
        val requestBody = MarkConversationAsUnreadBody(
            conversationIds = listOf(conversationId.id)
        )
        val result = api {
            markAsUnread(requestBody)
        }

        return when (result) {
            is ApiResult.Success -> Result.success()
            is ApiResult.Error -> {
                if (result.isRetryable()) {
                    Result.retry()
                } else {
                    conversationLocalDataSource.markRead(userId, conversationId)
                    Result.failure()
                }
            }
        }
    }

    companion object {

        const val RawUserIdKey = "markUnreadWorkParamUserId"
        const val RawConversationIdKey = "markUnreadWorkParamConversationId"
    }

    class Enqueuer @Inject constructor(private val workManager: WorkManager) {

        fun enqueue(userId: UserId, conversationId: ConversationId) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val data = workDataOf(
                RawUserIdKey to userId.id,
                RawConversationIdKey to conversationId.id
            )

            val workRequest = OneTimeWorkRequestBuilder<MarkConversationAsUnreadWorker>()
                .setConstraints(constraints)
                .setInputData(data)
                .build()

            workManager.enqueue(workRequest)
        }
    }
}
