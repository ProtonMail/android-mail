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

package ch.protonmail.android.mailcontentsearch.data.worker

import android.app.Notification
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import ch.protonmail.android.mailcontentsearch.data.worker.ContentIndexingNotification.AccountProgress
import ch.protonmail.android.mailcontentsearch.domain.model.ContentIndexingActivity
import ch.protonmail.android.mailcontentsearch.domain.usecase.ObserveContentIndexingActivity
import ch.protonmail.android.mailsession.data.repository.MailSessionRepository
import ch.protonmail.android.mailsession.data.repository.runInRustBackground
import ch.protonmail.android.mailsession.domain.repository.UserSessionRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.produceIn
import kotlinx.coroutines.withTimeoutOrNull
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * Holds a foreground service open while the Rust orchestrator indexes, and shows the progress
 * notification. It drives nothing itself - the orchestrator picks its own accounts and their order -
 * and exists only so the process survives being backgrounded.
 *
 * It therefore exits as soon as the orchestrator stops making progress: on
 * [ContentIndexingActivity.ForwardMode], [ContentIndexingActivity.WaitingOnUsers],
 * [ContentIndexingActivity.Stopped] or [ContentIndexingActivity.Failed], or when the watchdog fires
 * because nothing arrived at all. Anything else would keep an indexing notification on screen for an
 * orchestrator that has already gone quiet.
 */
@HiltWorker
class ContentIndexingWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParameters: WorkerParameters,
    private val mailSessionRepository: MailSessionRepository,
    private val userSessionRepository: UserSessionRepository,
    private val observeContentIndexingActivity: ObserveContentIndexingActivity
) : CoroutineWorker(context, workerParameters) {

    override suspend fun doWork(): Result = try {
        mailSessionRepository.runInRustBackground {
            trySetForeground(accountLabel = null, progress = null)
            awaitIndexingIdle()
        }
        Result.success()
    } catch (cancellation: CancellationException) {
        // Includes the Android 15 FGS timeout. Deliberately no self-restart: the dataSync 6h/24h
        // budget is app-wide and SystemForegroundService is shared with other WorkManager foreground
        // work, so backing off and letting the next foreground transition re-enqueue is the only way
        // not to starve the rest of the app.
        Timber.d("content-search: indexing worker stopped ($cancellation)")
        throw cancellation
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun awaitIndexingIdle() = coroutineScope {
        // Consumed through a channel rather than collected, so the watchdog can bound the wait for
        // each individual emission instead of the run as a whole.
        val activities = observeContentIndexingActivity().produceIn(this)
        try {
            while (true) {
                // receiveCatching, so an upstream that completes releases the service the same way a
                // silent one does instead of failing the run.
                val activity = withTimeoutOrNull(IdleTimeout) { activities.receiveCatching().getOrNull() }
                if (activity == null) {
                    Timber.w("content-search: no more indexing activity within $IdleTimeout, releasing the service")
                    break
                }
                if (activity !is ContentIndexingActivity.Progress) {
                    Timber.d("content-search: orchestrator went idle ($activity), releasing the service")
                    break
                }
                refreshNotification(activity)
            }
        } finally {
            activities.cancel()
        }
    }

    private suspend fun refreshNotification(activity: ContentIndexingActivity.Progress) {
        val label = activity.activeUserId?.let { accountLabelFor(it) }
        trySetForeground(label, activity.toAccountProgress())
    }

    /**
     * Account-level progress only. `SyncOrchestratorProgress.percentage` is summed across every
     * account's totals, so it lurches whenever an account is added or removed.
     */
    private fun ContentIndexingActivity.Progress.toAccountProgress(): AccountProgress? =
        if (userCount > 0) AccountProgress(completed = completedUsers.toInt(), total = userCount.toInt()) else null

    override suspend fun getForegroundInfo(): ForegroundInfo = buildForegroundInfo(accountLabel = null, progress = null)

    private suspend fun accountLabelFor(userId: UserId): String? = runCatching {
        userSessionRepository.getAccount(userId)?.primaryAddress
    }.getOrNull()

    @Suppress("TooGenericExceptionCaught")
    private suspend fun trySetForeground(accountLabel: String?, progress: AccountProgress?) {
        try {
            setForeground(buildForegroundInfo(accountLabel, progress))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // FGS promotion can be denied if the app is in the background on Android 12+, if
            // POST_NOTIFICATIONS was refused, or if the dataSync budget is exhausted on Android 15+.
            // Rust keeps indexing either way; only the notification is lost.
            Timber.w(e, "content-search: setForeground denied, continuing without a notification")
        }
    }

    private fun buildForegroundInfo(accountLabel: String?, progress: AccountProgress?): ForegroundInfo {
        val notification = ContentIndexingNotification.build(context, accountLabel, progress)
            .build()
            .apply { flags = flags or Notification.FLAG_NO_CLEAR or Notification.FLAG_ONGOING_EVENT }
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ForegroundInfo(
                ContentIndexingNotification.NotificationId,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(ContentIndexingNotification.NotificationId, notification)
        }
    }

    internal companion object {

        /**
         * An orchestrator that never publishes must not hold a foreground service forever. Generous,
         * because a large first backfill can stay quiet for a while before the first progress event.
         */
        val IdleTimeout: Duration = 5.minutes
    }
}
