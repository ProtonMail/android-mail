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
import ch.protonmail.android.mailconversation.data.remote.ConversationApi
import ch.protonmail.android.mailconversation.data.remote.resource.ConversationActionBody
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId
import me.proton.core.network.data.ApiProvider
import me.proton.core.network.domain.ApiResult
import me.proton.core.network.domain.isRetryable
import me.proton.core.util.kotlin.takeIfNotBlank

@HiltWorker
class RemoveLabelConversationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParameters: WorkerParameters,
    private val apiProvider: ApiProvider
) : CoroutineWorker(context, workerParameters) {

    override suspend fun doWork(): Result {
        val userId = inputData.getString(RawUserIdKey)?.takeIfNotBlank()
        val conversationIds = inputData.getStringArray(RawConversationIdsKey)?.toList()
        val labelId = inputData.getString(RawLabelIdKey)?.takeIfNotBlank()

        if (userId == null || conversationIds == null || labelId == null) {
            return Result.failure()
        }

        val result = apiProvider.get<ConversationApi>(UserId(userId)).invoke {
            removeLabel(
                ConversationActionBody(
                    labelId = labelId,
                    conversationIds = conversationIds
                )
            )
        }

        return when (result) {
            is ApiResult.Success -> Result.success()
            is ApiResult.Error -> {
                if (result.isRetryable()) return Result.retry()
                else Result.failure()
            }
        }
    }

    companion object {

        internal const val RawUserIdKey = "removeLabelConversationWorkParamUserId"
        internal const val RawConversationIdsKey = "removeLabelConversationWorkParamConversationIds"
        internal const val RawLabelIdKey = "removeLabelConversationWorkParamLabelId"

        fun params(
            userId: UserId,
            conversationIds: List<ConversationId>,
            labelId: LabelId
        ) = mapOf(
            RawUserIdKey to userId.id,
            RawConversationIdsKey to conversationIds.map { it.id }.toTypedArray(),
            RawLabelIdKey to labelId.id
        )
    }
}
