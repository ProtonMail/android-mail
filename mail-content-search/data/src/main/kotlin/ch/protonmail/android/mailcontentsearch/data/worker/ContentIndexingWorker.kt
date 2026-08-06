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
import ch.protonmail.android.mailcommon.domain.AppInBackgroundState
import ch.protonmail.android.mailcontentsearch.data.background.ContentIndexingWorkScheduler
import ch.protonmail.android.mailcontentsearch.data.worker.ContentIndexingNotification.IndexingProgress
import ch.protonmail.android.mailcontentsearch.domain.model.ContentIndexingActivity
import ch.protonmail.android.mailcontentsearch.domain.usecase.ObserveContentIndexingActivity
import ch.protonmail.android.mailsession.data.repository.MailSessionRepository
import ch.protonmail.android.mailsession.data.repository.runInRustBackground
import ch.protonmail.android.mailsession.domain.repository.UserSessionRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.produceIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

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
    private val observeContentIndexingActivity: ObserveContentIndexingActivity,
    private val appInBackgroundState: AppInBackgroundState,
    private val workScheduler: ContentIndexingWorkScheduler
) : CoroutineWorker(context, workerParameters) {

    // The worker has to be enqueued while the app is foregrounded - WorkManager will not promote one
    // started from the background - but the service is only worth anything once the app is off
    // screen, so promotion is deferred until then rather than done at start.
    //
    // Set from the result of the attempt, not before it: promotion can be refused (see
    // [trySetForeground]), and a worker that never took a service has neither a notification to
    // update nor anything to give up when the app comes back.
    @Volatile
    private var isPromoted = false

    @Volatile
    private var latestProgress: ContentIndexingActivity.Progress? = null

    // Set when this worker asked WorkManager to replace it, so the cancellation that follows is not
    // mistaken for the app or the system stopping indexing.
    @Volatile
    private var isReplacingSelf = false

    override suspend fun doWork(): Result = try {
        mailSessionRepository.runInRustBackground {
            coroutineScope {
                val visibility = launch { observeAppVisibility() }
                try {
                    awaitIndexingIdle()
                } finally {
                    visibility.cancel()
                }
            }
        }
        Result.success()
    } catch (cancellation: CancellationException) {
        // Deliberately no self-restart here beyond the visibility swap above. This also catches the
        // Android 15 FGS timeout, and the dataSync 6h/24h budget is app-wide - SystemForegroundService
        // is shared with the rest of the app's foreground work - so backing off and letting the next
        // foreground transition re-enqueue is the only way not to starve everything else.
        if (isReplacingSelf) {
            Timber.d("content-search: indexing worker replaced by an unpromoted one")
        } else {
            Timber.d("content-search: indexing worker stopped ($cancellation)")
        }
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

    /**
     * Matches the foreground service to where the user actually is: acquired when the app leaves the
     * screen, given up when it comes back.
     *
     * Giving it up means ending this worker, because WorkManager offers no way to drop the service
     * on its own - so the release path replaces the worker instead of just returning, and the
     * replacement runs without a notification. Indexing does not restart with it: the orchestrator
     * lives in Rust and persists its progress, so an interrupted account resumes where it stopped.
     *
     * Debounced, so an app that is briefly backgrounded (a permission dialog, a share sheet) does
     * not pay for a service it will hand back a moment later.
     */
    @OptIn(FlowPreview::class)
    private suspend fun observeAppVisibility() {
        appInBackgroundState.observe()
            .debounce(VisibilityDebounce)
            .distinctUntilChanged()
            .collect { isInBackground ->
                when {
                    isInBackground && !isPromoted -> promote()

                    !isInBackground && isPromoted -> {
                        isReplacingSelf = true
                        Timber.d("content-search: app back on screen, dropping the indexing service")
                        workScheduler.restart()
                    }
                }
            }
    }

    /**
     * Takes the foreground service, and records whether it was actually granted.
     *
     * A refusal is not retried here: this worker is short-lived and the next foreground transition
     * enqueues a fresh one, so the only thing re-asking would add is a repeat of the warning. It does
     * mean the worker carries on unpromoted for the rest of its run, which is the honest state - the
     * release path below then has nothing to give up, and does nothing.
     */
    private suspend fun promote() {
        Timber.d("content-search: app backgrounded, promoting the indexing worker")
        val progress = latestProgress
        val label = progress?.activeUserId?.let { accountLabelFor(it) }
        isPromoted = trySetForeground(label, progress?.toNotificationProgress())
    }

    private suspend fun refreshNotification(activity: ContentIndexingActivity.Progress) {
        latestProgress = activity
        // Nothing to show while the app is on screen: the settings screen already reports progress,
        // and a notification for an app the user is looking at is just noise. Same when promotion was
        // refused - there is no notification to update.
        if (!isPromoted) return
        val label = activity.activeUserId?.let { accountLabelFor(it) }
        trySetForeground(label, activity.toNotificationProgress())
    }

    private fun ContentIndexingActivity.Progress.toNotificationProgress() = IndexingProgress(
        percentage = percentage,
        processedMessages = processedMessages,
        totalMessages = totalMessages,
        completedAccounts = completedUsers.toInt(),
        totalAccounts = userCount.toInt()
    )

    override suspend fun getForegroundInfo(): ForegroundInfo = buildForegroundInfo(accountLabel = null, progress = null)

    private suspend fun accountLabelFor(userId: UserId): String? = runCatching {
        userSessionRepository.getAccount(userId)?.primaryAddress
    }.getOrNull()

    /** Whether the service is now held. */
    @Suppress("TooGenericExceptionCaught")
    private suspend fun trySetForeground(accountLabel: String?, progress: IndexingProgress?): Boolean {
        return try {
            setForeground(buildForegroundInfo(accountLabel, progress))
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // FGS promotion can be denied if the app is in the background on Android 12+, if
            // POST_NOTIFICATIONS was refused, or if the dataSync budget is exhausted on Android 15+.
            // Rust keeps indexing either way; only the notification is lost.
            Timber.w(e, "content-search: setForeground denied, continuing without a notification")
            false
        }
    }

    private fun buildForegroundInfo(accountLabel: String?, progress: IndexingProgress?): ForegroundInfo {
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

        /**
         * Long enough to ride out a permission dialog or a share sheet, short enough that a user who
         * really has left the app is covered well before the process is a candidate for death.
         */
        val VisibilityDebounce: Duration = 2.seconds
    }
}
