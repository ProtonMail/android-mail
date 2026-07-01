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
import kotlinx.coroutines.flow.map
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

class ContentIndexingSchedulerImpl @Inject constructor(
    private val workManager: WorkManager,
    private val appInBackgroundState: AppInBackgroundState
) : ContentIndexingScheduler {

    override suspend fun enqueueSweep(allowMobileData: Boolean, replaceExisting: Boolean): EnqueueIndexingResult {
        val request = ContentIndexingWorker.buildRequest(
            runAsForeground = appInBackgroundState.isAppInBackground(),
            allowMobileData = allowMobileData
        )
        val policy = if (replaceExisting) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP
        workManager.enqueueUniqueWork(ContentIndexingWorker.UniqueName, policy, request)
        return EnqueueIndexingResult.Scheduled
    }

    override fun observeState(userId: UserId): Flow<ContentIndexingState> =
        workManager.getWorkInfosForUniqueWorkFlow(ContentIndexingWorker.UniqueName)
            .map { infos ->
                val info = infos.firstOrNull()
                when {
                    info == null -> ContentIndexingState.Idle
                    // The sweep publishes the account it is currently indexing via progress data;
                    // report its live state for that account.
                    info.currentUserId() == userId -> info.toState()
                    // A sweep that is enqueued/starting has not announced an account yet; surface the
                    // transient "preparing" state so a just-enabled account does not look idle.
                    info.currentUserId() == null && !info.state.isFinished -> ContentIndexingState.Initializing
                    else -> ContentIndexingState.Idle
                }
            }

    private fun WorkInfo.currentUserId(): UserId? =
        progress.getString(ContentIndexingWorker.KeyCurrentUserId)?.takeIf { it.isNotBlank() }?.let(::UserId)

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
}
