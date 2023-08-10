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
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailcommon.domain.util.requireNotBlank
import ch.protonmail.android.mailconversation.data.remote.ConversationApi
import ch.protonmail.android.mailconversation.data.remote.resource.MarkConversationAsUnreadBody
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId
import me.proton.core.network.data.ApiProvider
import me.proton.core.network.domain.ApiResult
import me.proton.core.network.domain.isRetryable

@HiltWorker
class MarkConversationAsUnreadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParameters: WorkerParameters,
    private val apiProvider: ApiProvider
) : CoroutineWorker(context, workerParameters) {

    override suspend fun doWork(): Result {
        val userId = requireNotBlank(inputData.getString(RawUserIdKey), fieldName = "User id")
            .let(::UserId)
        val conversationIds = requireNotNull(inputData.getStringArray(RawConversationIdsKey)).toList()
        val contextLabelId = requireNotBlank(inputData.getString(RawContextLabelId), fieldName = "Context Label id")
            .let(::LabelId)

        val api = apiProvider.get<ConversationApi>(userId)
        val requestBody = MarkConversationAsUnreadBody(
            conversationIds = conversationIds,
            labelId = contextLabelId.id
        )
        val result = api {
            markAsUnread(requestBody)
        }

        return when (result) {
            is ApiResult.Success -> Result.success()
            is ApiResult.Error -> {
                if (result.isRetryable()) Result.retry()
                else Result.failure()
            }
        }
    }

    companion object {

        internal const val RawUserIdKey = "markUnreadWorkParamUserId"
        internal const val RawConversationIdsKey = "markUnreadWorkParamConversationIds"
        internal const val RawContextLabelId = "markUnreadWorkParamContextLabelId"

        fun params(
            userId: UserId,
            conversationIds: List<ConversationId>,
            contextLabelId: LabelId
        ) = mapOf(
            RawUserIdKey to userId.id,
            RawConversationIdsKey to conversationIds.map { it.id }.toTypedArray(),
            RawContextLabelId to contextLabelId.id
        )
    }
}
