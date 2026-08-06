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
import ch.protonmail.android.mailcontentsearch.domain.usecase.StartContentIndexing
import ch.protonmail.android.mailsession.data.repository.MailSessionRepository
import ch.protonmail.android.mailsession.data.repository.runInRustBackground
import ch.protonmail.android.mailsession.domain.repository.UserSessionRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
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
    private val startContentIndexing: StartContentIndexing,
    private val appInBackgroundState: AppInBackgroundState,
    private val workScheduler: ContentIndexingWorkScheduler
) : CoroutineWorker(context, workerParameters) {

    // The worker has to be enqueued while the app is foregrounded - WorkManager will not promote one
    // started from the background - but the service is only worth anything once the app is off screen
    // and there is indexing to protect, so promotion waits for both. See [promoteIfWorthIt].
    //
    // Set from the result of the attempt, not before it: promotion can be refused (see
    // [trySetForeground]), and a worker that never took a service has neither a notification to
    // update nor anything to give up when the app comes back.
    @Volatile
    private var isPromoted = false

    /** Completed once the orchestrator has answered whether it has anything to index. */
    private val orchestratorHasWork = CompletableDeferred<Boolean>()

    @Volatile
    private var latestProgress: ContentIndexingActivity.Progress? = null

    // Set when this worker asked WorkManager to replace it, so the cancellation that follows is not
    // mistaken for the app or the system stopping indexing.
    @Volatile
    private var isReplacingSelf = false

    @Volatile
    private var cachedAccountLabel: Pair<UserId, String?>? = null

    override suspend fun doWork(): Result = try {
        // A run attempt above the first means WorkManager rescheduled us, which is worth knowing
        // when the worker is found sitting in ENQUEUED with every constraint met.
        Timber.d("content-search: indexing worker running (attempt ${runAttemptCount + 1})")
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
            Timber.d("content-search: indexing worker stopped, ${stopReasonLabel()} ($cancellation)")
        }
        throw cancellation
    }

    /**
     * Why the platform took the worker away, if it did.
     *
     * The one thing that separates cases which look identical from the outside: a timed-out
     * foreground service, an exhausted job quota, a lost constraint and our own self-replacement
     * all leave the work sitting in `ENQUEUED` afterwards.
     */
    private fun stopReasonLabel(): String = when {
        !isStopped -> "not stopped by WorkManager"
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> "stopReason=$stopReason"
        else -> "stopReason unavailable below API 31"
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun awaitIndexingIdle() = coroutineScope {
        // Consumed through a channel rather than collected, so the watchdog can bound the wait for
        // each individual emission instead of the run as a whole.
        val activities = observeContentIndexingActivity().produceIn(this)
        // Asked rather than inferred from silence, and idempotent - an orchestrator that is already
        // running answers with its current stats. Also the only thing that starts it when
        // WorkManager reruns this worker after the process was killed. Asked after subscribing, so
        // nothing the start itself sets off is missed.
        val hasWorkPending = startContentIndexing().fold(
            ifLeft = { error ->
                Timber.w("content-search: could not ask the orchestrator for work: $error")
                true
            },
            ifRight = { it.hasWorkPending }
        )
        if (!hasWorkPending) Timber.d("content-search: orchestrator reports nothing to index")
        orchestratorHasWork.complete(hasWorkPending)
        try {
            while (true) {
                val timeout = if (latestProgress == null && !hasWorkPending) NoWorkTimeout else IdleTimeout
                // receiveCatching, so an upstream that completes releases the service the same way a
                // silent one does instead of failing the run.
                val activity = withTimeoutOrNull(timeout) { activities.receiveCatching().getOrNull() }
                if (activity == null) {
                    Timber.w("content-search: no indexing progress within $timeout, releasing the service")
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
     * screen if there is indexing to protect, given up when it comes back.
     *
     * Giving it up means ending this worker, because WorkManager offers no way to drop the service
     * on its own - so the release path replaces the worker instead of just returning, and the
     * replacement runs without a notification. Indexing does not restart with it: the orchestrator
     * lives in Rust and persists its progress, so an interrupted account resumes where it stopped.
     *
     * Acquiring is not debounced. A job is only exempt from JobScheduler's standby quota while the
     * app holds a foreground service, and quota starts being charged the moment the app stops being
     * the top activity - waiting even a couple of seconds is long enough to be stopped with
     * STOP_REASON_QUOTA, after which the replacement will not be started until quota recovers. The
     * same haste keeps the promotion inside the grace period for starting a service from the
     * background on Android 12+. It does wait for a reason to hold one at all - see [promoteIfWorthIt].
     *
     * Releasing is debounced, so an app that is briefly backgrounded - a permission dialog, a share
     * sheet - does not churn a worker it is about to want back.
     */
    private suspend fun observeAppVisibility() {
        appInBackgroundState.observe()
            .distinctUntilChanged()
            .collectLatest { isInBackground ->
                when {
                    isInBackground && !isPromoted -> refreshNotification()

                    !isInBackground && isPromoted -> {
                        // Cancelled by collectLatest if the app leaves again within the window.
                        delay(ForegroundReturnDebounce)
                        isReplacingSelf = true
                        Timber.d("content-search: app back on screen, dropping the indexing service")
                        workScheduler.restart()
                    }
                }
            }
    }

    private suspend fun refreshNotification(activity: ContentIndexingActivity.Progress) {
        latestProgress = activity
        refreshNotification()
    }

    private suspend fun refreshNotification() {
        if (!isPromoted) promoteIfWorthIt()
        // Nothing to show while the app is on screen: the settings screen already reports progress,
        // and a notification for an app the user is looking at is just noise. Same when promotion was
        // refused - there is no notification to update.
        if (!isPromoted) return
        val progress = latestProgress
        val label = progress?.activeUserId?.let { accountLabelFor(it) }
        trySetForeground(label, progress?.toNotificationProgress())
    }

    /**
     * Takes the foreground service, if there is anything to hold one for: the app off screen, because
     * a notification for an app the user is looking at is just noise, and indexing to protect.
     *
     * That second condition is why this exists. A worker is enqueued on every foreground transition
     * whether or not the orchestrator had anything to do, and one that promoted regardless would show
     * "Preparing" only to drop it again when [NoWorkTimeout] fired - a flash for every trip to the
     * background, protecting nothing. Holding off costs nothing: that worker exits either way.
     *
     * Progress is the other way in, hence this sitting on the notification path and not only on the
     * visibility one: the stats are read while Rust is still bringing accounts up, so a worker told
     * there was nothing to do can find work while still inside the window where a service may be
     * started from the background.
     *
     * The answer is awaited rather than read, because a worker that starts with the app already off
     * screen - a rerun after the process was killed - gets here before it has one. [collectLatest]
     * cancels the wait if the app comes back first.
     *
     * A refusal is not retried here: this worker is short-lived and the next foreground transition
     * enqueues a fresh one, so the only thing re-asking would add is a repeat of the warning. It does
     * mean the worker carries on unpromoted for the rest of its run, which is the honest state - the
     * release path then has nothing to give up, and does nothing.
     */
    private suspend fun promoteIfWorthIt() {
        if (!appInBackgroundState.isAppInBackground()) return
        if (latestProgress == null && !orchestratorHasWork.await()) {
            Timber.d("content-search: nothing to index, leaving the indexing worker unpromoted")
            return
        }
        Timber.d("content-search: app backgrounded, promoting the indexing worker")
        // Bare notification first. Resolving the account label is a session round-trip, and the job
        // is unprotected for as long as it takes - which is all the quota controller needs to stop
        // us. The label follows a moment later, by which point the service is already held.
        isPromoted = trySetForeground(
            accountLabel = null,
            progress = latestProgress?.toNotificationProgress()
        )
    }

    private fun ContentIndexingActivity.Progress.toNotificationProgress() = IndexingProgress(
        percentage = percentage,
        processedMessages = processedMessages,
        totalMessages = totalMessages,
        completedAccounts = completedUsers.toInt(),
        totalAccounts = userCount.toInt()
    )

    override suspend fun getForegroundInfo(): ForegroundInfo = buildForegroundInfo(accountLabel = null, progress = null)

    /**
     * Cached for one account, because that is how many the orchestrator indexes at a time. Progress
     * arrives every batch, and re-reading the account for each of them would put a session
     * round-trip between the orchestrator and every notification update.
     */
    private suspend fun accountLabelFor(userId: UserId): String? {
        cachedAccountLabel?.takeIf { it.first == userId }?.let { return it.second }
        val label = runCatching { userSessionRepository.getAccount(userId)?.primaryAddress }.getOrNull()
        cachedAccountLabel = userId to label
        return label
    }

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

        /** An orchestrator that stops publishing must not hold a foreground service forever. */
        val IdleTimeout: Duration = 5.minutes

        /**
         * How long a worker the orchestrator said it had no work for waits before giving up. Such a
         * worker takes no service, so there is no notification to bound, but it does hold a job -
         * and a job that does nothing is charged against the app's standby quota like any other.
         *
         * Not zero, because the stats are read while Rust is still bringing accounts up, so a fresh
         * login can be reported as nothing to index for a moment; progress arriving inside the window
         * promotes the worker after all.
         */
        val NoWorkTimeout: Duration = 10.seconds

        /**
         * How long the app has to stay on screen before the service is handed back. Long enough to
         * ride out a permission dialog or a share sheet without churning the worker.
         */
        val ForegroundReturnDebounce: Duration = 2.seconds
    }
}
