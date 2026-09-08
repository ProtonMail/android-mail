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
import ch.protonmail.android.mailcommon.domain.system.DeviceArchitectureProvider
import ch.protonmail.android.mailcontentsearch.data.background.ContentIndexingWorkScheduler
import ch.protonmail.android.mailcontentsearch.data.util.formatPercentage
import ch.protonmail.android.mailcontentsearch.domain.model.ContentIndexingActivity
import ch.protonmail.android.mailcontentsearch.domain.model.backfillPercentage
import ch.protonmail.android.mailcontentsearch.domain.usecase.ObserveContentIndexingActivity
import ch.protonmail.android.mailcontentsearch.domain.usecase.ObserveContentSearchIndexingStatus
import ch.protonmail.android.mailcontentsearch.domain.usecase.StartContentIndexing
import ch.protonmail.android.mailsession.data.repository.MailSessionRepository
import ch.protonmail.android.mailsession.data.repository.runInRustBackground
import ch.protonmail.android.mailsession.domain.repository.UserSessionRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.produceIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
 * It exits once the orchestrator stops making progress: on [ContentIndexingActivity.Stopped] or
 * [ContentIndexingActivity.Failed], on watchdog silence, or on [ContentIndexingActivity.ForwardMode]
 * / [ContentIndexingActivity.WaitingOnUsers] with no accounts left. Those last two are not exit
 * conditions on their own - they are also published while an account is still to come, see
 * [shouldWaitForAnotherAccount].
 */
@HiltWorker
class ContentIndexingWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParameters: WorkerParameters,
    private val mailSessionRepository: MailSessionRepository,
    private val userSessionRepository: UserSessionRepository,
    private val observeContentIndexingActivity: ObserveContentIndexingActivity,
    private val observeContentSearchIndexingStatus: ObserveContentSearchIndexingStatus,
    private val startContentIndexing: StartContentIndexing,
    private val appInBackgroundState: AppInBackgroundState,
    private val workScheduler: ContentIndexingWorkScheduler,
    private val deviceArchitectureProvider: DeviceArchitectureProvider
) : CoroutineWorker(context, workerParameters) {

    /**
     * Whether this worker *may* be holding the foreground service - [promoteIfWorthIt] sets it
     * optimistically. The one field here that is not single-writer, so nothing reads it outside
     * [notificationMutex].
     */
    @Volatile
    private var isPromoted = false

    /** Completed once the orchestrator has answered whether it has anything to index. */
    private val orchestratorHasWork = CompletableDeferred<Boolean>()

    @Volatile
    private var latestProgress: ContentIndexingActivity.Progress? = null

    // Set while the orchestrator is between accounts, so the loop waits on [HandoverTimeout] rather
    // than the long idle one and the notification is left as it is. Only the loop touches this.
    private var isHandingOver = false

    // Set once the orchestrator loop has decided to end, so backgrounding the app before the
    // visibility collector is cancelled cannot promote a worker on its way out. Written by the loop
    // and read by [promoteIfWorthIt] on another coroutine - hence `@Volatile`, here and below.
    @Volatile
    private var isWindingDown = false

    // Set when this worker asked WorkManager to replace it, so the cancellation that follows is not
    // mistaken for the app or the system stopping indexing.
    @Volatile
    private var isReplacingSelf = false

    // The account being indexed. Rust leaves it null between accounts, so the last one is kept
    // rather than blanked - the notification would otherwise lose its subject during a handover.
    private val activeUserId = MutableStateFlow<UserId?>(null)

    // What the notification shows: the active account and how far its own backfill has got. Held as
    // one value so the two can never be posted out of step.
    @Volatile
    private var notifiedAccount: NotifiedAccount? = null

    private data class NotifiedAccount(val address: String?, val percentage: Double?)

    /** Cached per account, so switching back and forth does not re-read one we have already seen. */
    private val addressCache = mutableMapOf<UserId, String?>()

    private val notificationMutex = Mutex()

    override suspend fun doWork(): Result {
        // Attempt > 1 means WorkManager rescheduled us - worth knowing when the work is found
        // sitting in ENQUEUED with every constraint met.
        Timber.d("content-search: indexing worker running (attempt ${runAttemptCount + 1})")
        // Content search is unavailable on a 32-bit process, so nothing enqueues this there.
        if (!deviceArchitectureProvider.is64Bit()) {
            Timber.d("content-search: indexing worker exiting, unsupported on a 32-bit process")
            return Result.success()
        }
        return runIndexing()
    }

    private suspend fun runIndexing(): Result = try {
        workScheduler.markWorkerRunning()
        mailSessionRepository.runInRustBackground {
            coroutineScope {
                val visibility = launch { observeAppVisibility() }
                val accountProgress = launch { observeActiveAccountProgress() }
                try {
                    awaitIndexingIdle()
                } finally {
                    visibility.cancel()
                    accountProgress.cancel()
                }
            }
        }
        Result.success()
    } catch (cancellation: CancellationException) {
        // No self-restart beyond the visibility swap above: this also catches the Android 15 FGS
        // timeout, and the dataSync budget is app-wide, so backing off and letting the next
        // foreground transition re-enqueue is the only way not to starve the app's other FGS work.
        if (isReplacingSelf) {
            Timber.d("content-search: indexing worker replaced by an unpromoted one")
        } else {
            Timber.d("content-search: indexing worker stopped, ${stopReasonLabel()} ($cancellation)")
        }
        throw cancellation
    }

    // Why the platform took the worker away. The only thing separating an FGS timeout, an exhausted
    // quota, a lost constraint and our own self-replacement - all leave the work in `ENQUEUED`.
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
        // Asked rather than inferred from silence, and idempotent. Also the only thing that starts
        // the orchestrator on a rerun after the process was killed. After subscribing, so nothing
        // the start itself sets off is missed.
        val hasWorkPending = hasWorkOutstanding()
        if (!hasWorkPending) Timber.d("content-search: orchestrator reports nothing to index")
        orchestratorHasWork.complete(hasWorkPending)
        try {
            var isHolding = true
            while (isHolding) isHolding = awaitNextActivity(activities, hasWorkPending)
            // Before unwinding, not after: WorkManager still reports this worker as `RUNNING` until
            // it has finished returning.
            isWindingDown = true
            workScheduler.markWorkerExiting()
        } finally {
            activities.cancel()
        }
    }

    /**
     * Waits for the next activity and answers whether the service is still worth holding: false once
     * the orchestrator has gone idle, or has stayed silent for longer than what it last said allows.
     */
    private suspend fun awaitNextActivity(
        activities: ReceiveChannel<ContentIndexingActivity>,
        hasWorkPending: Boolean
    ): Boolean {
        val timeout = when {
            isHandingOver -> HandoverTimeout
            latestProgress == null -> if (hasWorkPending) StartupTimeout else NoWorkTimeout
            else -> IdleTimeout
        }
        // The activity stream is shared app-wide and never completes, so silence is the only way this
        // worker hears that indexing stopped - hence the watchdog. receiveCatching so the cancel in
        // [awaitIndexingIdle] releases the service instead of failing the run.
        val activity = withTimeoutOrNull(timeout) { activities.receiveCatching().getOrNull() }
        when {
            activity == null ->
                Timber.w("content-search: no indexing progress within $timeout, releasing the service")

            activity is ContentIndexingActivity.Progress -> {
                isHandingOver = false
                refreshNotification(activity)
                return true
            }

            shouldWaitForAnotherAccount(activity) -> {
                // The notification keeps the outgoing account and its last percentage - see
                // [lastDeterminate] - rather than announcing a wait the user need not hear about.
                isHandingOver = true
                Timber.d(
                    "content-search: orchestrator between accounts ($activity), " +
                        "holding for $HandoverTimeout [${lastCounts()}]"
                )
                return true
            }

            else -> Timber.d(
                "content-search: orchestrator went idle ($activity), releasing the service [${lastCounts()}]"
            )
        }
        return false
    }

    /**
     * The counters the session last published, logged beside a hold or a release so a bug report shows
     * what the decision was made against as well as what the orchestrator answered.
     */
    private fun lastCounts(): String = latestProgress?.let {
        "${it.processedMessages}/${it.totalMessages} messages, ${it.completedUsers}/${it.userCount} accounts"
    } ?: "no progress seen"

    /**
     * Whether an activity that is not [ContentIndexingActivity.Progress] leaves an account still to
     * come, rather than ending the session.
     *
     * Neither event says on its own. Rust publishes `ForwardModeEntered` only once every account is
     * terminal, and names the primary account rather than the one that just finished; `WaitingOnUsers`
     * means it found nothing ready to run, which covers both "done" and "an account has signed in but
     * is not prepared yet". A worker that has seen no progress is starting, not waiting.
     *
     * So the orchestrator is asked instead of inferring it from its counters: [StartContentIndexing]
     * reports live stats and is idempotent, a run already under way answering with its stats rather
     * than restarting.
     */
    private suspend fun shouldWaitForAnotherAccount(activity: ContentIndexingActivity): Boolean {
        val isBetweenAccounts = activity is ContentIndexingActivity.ForwardMode ||
            activity is ContentIndexingActivity.WaitingOnUsers
        if (!isBetweenAccounts) return false
        if (latestProgress == null) return false
        return hasWorkOutstanding()
    }

    /**
     * Whether the orchestrator has anything outstanding.
     */
    private suspend fun hasWorkOutstanding(): Boolean = withTimeoutOrNull(OrchestratorAskTimeout) {
        startContentIndexing().fold(
            ifLeft = { error ->
                Timber.w("content-search: could not ask the orchestrator for work: $error")
                true
            },
            ifRight = { summary ->
                Timber.d("content-search: orchestrator reports $summary")
                summary.hasWorkPending
            }
        )
    } ?: run {
        Timber.w("content-search: orchestrator did not answer within $OrchestratorAskTimeout")
        false
    }

    /**
     * Matches the foreground service to where the user is: acquired when the app leaves the screen if
     * there is indexing to protect, given up when it comes back. Giving it up means ending this
     * worker, since WorkManager cannot drop the service on its own - indexing survives, the
     * orchestrator lives in Rust and persists its progress.
     */
    private suspend fun observeAppVisibility() {
        appInBackgroundState.observe()
            .distinctUntilChanged()
            .collectLatest { isInBackground ->
                if (isInBackground) {
                    refreshNotification()
                    return@collectLatest
                }
                // Cancelled by collectLatest if the app leaves again within the window. Asked after
                // the wait, so a worker that took the service during it is not left holding it.
                delay(ForegroundReturnDebounce)
                if (!isServiceHeld()) return@collectLatest
                isReplacingSelf = true
                isWindingDown = true
                workScheduler.markWorkerExiting()
                Timber.d("content-search: app back on screen, dropping the indexing service")
                workScheduler.restart()
            }
    }

    /** Whether the foreground service is held, read under the lock that decides it. */
    private suspend fun isServiceHeld(): Boolean = notificationMutex.withLock { isPromoted }

    private suspend fun refreshNotification(activity: ContentIndexingActivity.Progress) {
        latestProgress = activity
        activity.activeUserId?.let { activeUserId.value = it }
        refreshNotification()
    }

    /**
     * Serialised, because all three of this worker's coroutines get here and they are on the same
     * dispatcher rather than the same thread. Two reading [isPromoted] as false would promote twice;
     * two past that point would race to post, and the older snapshot could win and show progress
     * going backwards. Hence the snapshot being read inside the lock too.
     */
    private suspend fun refreshNotification() = notificationMutex.withLock {
        if (!isPromoted) promoteIfWorthIt()
        // Nothing to show while the app is on screen: the settings screen already reports progress,
        // and a notification for an app the user is looking at is just noise. Same when promotion was
        // refused - there is no notification to update.
        if (!isPromoted) return@withLock
        val account = notifiedAccount
        trySetForeground(account?.address, account?.percentage)
    }

    /**
     * Takes the foreground service, if there is anything to hold one for: the app off screen, because
     * a notification for an app the user is looking at is just noise, and indexing to protect.
     *
     * That second condition is why this exists: a worker is enqueued on every foreground transition
     * whether or not there was anything to do, and one that promoted regardless would flash
     * "Preparing" and drop it again when [NoWorkTimeout] fired.
     *
     * Progress is the other way in, hence this sitting on the notification path and not only the
     * visibility one: the stats are read while Rust is still bringing accounts up, so a worker told
     * there was nothing to do can find work while a background start is still allowed.
     *
     * The answer is awaited rather than read, because a worker that starts with the app already off
     * screen gets here before it has one. [collectLatest] cancels the wait if the app comes back.
     */
    private suspend fun promoteIfWorthIt() {
        if (!appInBackgroundState.isAppInBackground()) return
        // Nothing to protect: this worker is leaving, and the service would have to go straight back.
        if (isWindingDown) return
        if (latestProgress == null && !orchestratorHasWork.await()) {
            Timber.d("content-search: nothing to index, leaving the indexing worker unpromoted")
            return
        }
        Timber.d("content-search: app backgrounded, promoting the indexing worker")
        // With whatever the status stream has published so far, which may be nothing: the job is
        // unprotected until the service is held, and waiting on Rust for a percentage first is all
        // the quota controller needs to stop us. "Preparing" is replaced as soon as it publishes.
        val account = notifiedAccount
        // Set before the attempt, rolled back only on refusal: `setForeground` suspends, and its
        // cancellation says nothing about whether the service started - assigning from the result can
        // leave one held by a worker that thinks it holds none, and never gives it back.
        isPromoted = true
        if (!trySetForeground(account?.address, account?.percentage)) isPromoted = false
    }

    override suspend fun getForegroundInfo(): ForegroundInfo =
        buildForegroundInfo(accountAddress = null, percentage = null)

    // Feeds the notification from the active account's own status - the same stream the settings
    // screen reads, so the two cannot disagree about one account's percentage.
    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun observeActiveAccountProgress() {
        activeUserId
            .filterNotNull()
            .distinctUntilChanged()
            .flatMapLatest { userId ->
                val address = addressFor(userId)
                observeContentSearchIndexingStatus(userId).map { state ->
                    val percentage = state.backfillPercentage
                    // Per-account, as opposed to the orchestrator's session-wide line.
                    Timber.d(
                        "content-search: $userId backfill=${percentage?.let { "${it.formatPercentage()}%" } ?: state}"
                    )
                    NotifiedAccount(address, percentage)
                }
            }
            .distinctUntilChanged()
            .collect { account ->
                notifiedAccount = lastDeterminate(account)
                refreshNotification()
            }
    }

    /**
     * Rust publishes no percentage for a caught-up account - see [backfillPercentage] - which would
     * drop a determinate notification to "Preparing" for the length of a handover. The last real
     * figure is kept until the next account publishes one of its own.
     */
    private fun lastDeterminate(account: NotifiedAccount): NotifiedAccount {
        val previous = notifiedAccount ?: return account
        if (account.percentage != null || previous.address != account.address) return account
        return previous
    }

    private suspend fun addressFor(userId: UserId): String? {
        if (addressCache.containsKey(userId)) return addressCache[userId]
        val address = runCatching { userSessionRepository.getAccount(userId)?.primaryAddress }.getOrNull()
        addressCache[userId] = address
        return address
    }

    /** Whether the service is now held. */
    @Suppress("TooGenericExceptionCaught")
    private suspend fun trySetForeground(accountAddress: String?, percentage: Double?): Boolean {
        return try {
            setForeground(buildForegroundInfo(accountAddress, percentage))
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Denied if the app is already in the background on Android 12+, if POST_NOTIFICATIONS
            // was refused, or if the Android 15+ dataSync budget is spent. Rust keeps indexing
            // either way until the process terminates; only the notification is lost.
            Timber.w(e, "content-search: setForeground denied, continuing without a notification")
            false
        }
    }

    private fun buildForegroundInfo(accountAddress: String?, percentage: Double?): ForegroundInfo {
        val notification = ContentIndexingNotification.build(context, accountAddress, percentage)
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
         * How long the orchestrator has to pick up the next account when its stats say there are
         * accounts left. Far shorter than [IdleTimeout] because it bounds a notification already on
         * screen, but long enough to fetch an account's first batch.
         */
        val HandoverTimeout: Duration = 30.seconds

        /**
         * How long a worker the orchestrator said it had work for waits for its first progress.
         *
         * The orchestrator answers from its stats and can then publish nothing at all. Losing the
         * network does that: it stops mid-account, and there is no event for it. The notification
         * has nothing to report until the first progress, so this is how long "Preparing" can sit
         * on screen with nothing behind it. [IdleTimeout] would make that five minutes.
         */
        val StartupTimeout: Duration = 1.minutes

        /**
         * How long a worker the orchestrator said it had no work for waits before giving up. It holds
         * no service, but it does hold a job, and an idle job is charged against standby quota like
         * any other.
         */
        val NoWorkTimeout: Duration = 10.seconds

        /**
         * How long the orchestrator has to say whether it has work. Generous, because a start loads
         * every account's session before it answers; the point is only that one that has stopped
         * answering cannot park the loop for ever.
         */
        val OrchestratorAskTimeout: Duration = 30.seconds

        /** How long the app has to stay on screen before the service is handed back. */
        val ForegroundReturnDebounce: Duration = 2.seconds
    }
}
