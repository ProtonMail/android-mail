/*
 * Copyright (c) 2025 Proton Technologies AG
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

package ch.protonmail.android.mailcontentsearch.data.scheduler

import androidx.work.ExistingWorkPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import ch.protonmail.android.mailcommon.domain.AppInBackgroundState
import ch.protonmail.android.mailcontentsearch.data.worker.ContentIndexingWorker
import ch.protonmail.android.mailcontentsearch.domain.model.ContentIndexingState
import ch.protonmail.android.mailcontentsearch.domain.model.EnqueueIndexingResult
import ch.protonmail.android.mailcontentsearch.domain.repository.ContentIndexingScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

class ContentIndexingSchedulerImpl @Inject constructor(
    private val workManager: WorkManager,
    private val appInBackgroundState: AppInBackgroundState
) : ContentIndexingScheduler {

    override suspend fun enqueue(userId: UserId, allowMobileData: Boolean): EnqueueIndexingResult {
        val ongoing = currentOngoingWorkInfo()
        val ongoingUserId = ongoing?.userId()
        if (ongoingUserId != null && ongoingUserId != userId) {
            return EnqueueIndexingResult.BlockedByOtherUser(ongoingUserId)
        }

        val request = ContentIndexingWorker.buildRequest(
            userId = userId,
            runAsForeground = appInBackgroundState.isAppInBackground(),
            allowMobileData = allowMobileData
        )
        workManager.enqueueUniqueWork(
            ContentIndexingWorker.UniqueName,
            ExistingWorkPolicy.REPLACE,
            request
        )
        return EnqueueIndexingResult.Scheduled
    }

    override fun cancel(userId: UserId) {
        workManager.cancelAllWorkByTag(userTag(userId))
    }

    override fun observeState(userId: UserId): Flow<ContentIndexingState> =
        workManager.getWorkInfosForUniqueWorkFlow(ContentIndexingWorker.UniqueName)
            .map { infos ->
                val info = infos.firstOrNull()
                if (info == null || !info.belongsTo(userId)) ContentIndexingState.Idle
                else info.toState()
            }

    override fun observeOngoingUserId(): Flow<UserId?> =
        workManager.getWorkInfosForUniqueWorkFlow(ContentIndexingWorker.UniqueName)
            .map { infos ->
                infos.firstOrNull { !it.state.isFinished }?.userId()
            }

    private suspend fun currentOngoingWorkInfo(): WorkInfo? = workManager
        .getWorkInfosForUniqueWorkFlow(ContentIndexingWorker.UniqueName)
        .first()
        .firstOrNull { !it.state.isFinished }

    private fun WorkInfo.belongsTo(userId: UserId): Boolean {
        val tagged = tags.contains(userTag(userId))
        if (tagged) return true
        // Active workers expose their input via progress data; terminal ones don't, but we treat
        // those as Idle for the observing user anyway.
        return userId() == userId
    }

    private fun WorkInfo.userId(): UserId? {
        val tagged = tags.firstOrNull { it.startsWith(ContentIndexingWorker.TagUserPrefix) }
            ?.removePrefix(ContentIndexingWorker.TagUserPrefix)
        return tagged?.let(::UserId)
    }

    private fun WorkInfo.toState(): ContentIndexingState = when (state) {
        WorkInfo.State.ENQUEUED,
        WorkInfo.State.BLOCKED -> ContentIndexingState.Initializing
        WorkInfo.State.RUNNING -> {
            if (!progress.keyValueMap.containsKey(ContentIndexingWorker.KeyProgress)) ContentIndexingState.Initializing
            else ContentIndexingState.Running(progress.getDouble(ContentIndexingWorker.KeyProgress, 0.0))
        }
        WorkInfo.State.SUCCEEDED -> ContentIndexingState.Completed
        WorkInfo.State.CANCELLED -> ContentIndexingState.Cancelled
        WorkInfo.State.FAILED -> ContentIndexingState.Failed
    }

    private fun userTag(userId: UserId): String = ContentIndexingWorker.userTag(userId.id)
}
