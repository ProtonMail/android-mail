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

import java.util.concurrent.atomic.AtomicBoolean
import android.app.Notification
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import arrow.core.Either
import ch.protonmail.android.mailcommon.domain.AppInBackgroundState
import ch.protonmail.android.mailcontentsearch.domain.model.ContentIndexingError
import ch.protonmail.android.mailcontentsearch.domain.repository.ContentSearchIndexer
import ch.protonmail.android.mailcontentsearch.domain.usecase.FindFirstEligibleAccountToIndex
import ch.protonmail.android.mailsession.data.repository.MailSessionRepository
import ch.protonmail.android.mailsession.data.repository.runInRustBackground
import ch.protonmail.android.mailsession.domain.repository.UserSessionRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import kotlin.time.Duration.Companion.milliseconds

@HiltWorker
class ContentIndexingWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParameters: WorkerParameters,
    private val indexer: ContentSearchIndexer,
    private val mailSessionRepository: MailSessionRepository,
    private val userSessionRepository: UserSessionRepository,
    private val findFirstEligibleAccountToIndex: FindFirstEligibleAccountToIndex,
    private val appInBackgroundState: AppInBackgroundState
) : CoroutineWorker(context, workerParameters) {

    private val isSelfRestarting = AtomicBoolean(false)

    // The account currently being indexed. Tracked so cancellation can release the right Rust
    // session and so a self-restart can resume the same account (single mode) or sweep.
    @Volatile
    private var currentUserId: UserId? = null

    override suspend fun doWork(): Result {
        val runAsForeground = inputData.getBoolean(KeyRunAsForeground, true)
        val allowMobileData = inputData.getBoolean(KeyAllowMobileData, false)
        val autoAdvance = inputData.getBoolean(KeyAutoAdvance, false)

        Timber.d("ContentIndexingWorker: starting (foreground=$runAsForeground, sweep=$autoAdvance)")

        return mailSessionRepository.runInRustBackground {
            coroutineScope {
                val swapObserver = launch { observeModeSwap(runAsForeground, allowMobileData, autoAdvance) }
                try {
                    if (autoAdvance) runSweep(runAsForeground) else runSingleAccount(runAsForeground)
                } catch (cancellation: CancellationException) {
                    withContext(NonCancellable) { handleCancellation(allowMobileData, autoAdvance) }
                    throw cancellation
                } finally {
                    swapObserver.cancel()
                }
            }
        }
    }

    /** Index a single account taken from the worker input (manual settings toggle, no advance). */
    private suspend fun runSingleAccount(runAsForeground: Boolean): Result {
        val userId = userIdOrNull()
        if (userId == null) {
            Timber.e("ContentIndexingWorker: missing userId in input data")
            return Result.failure()
        }
        return indexAccount(userId, runAsForeground).fold(
            ifLeft = { error ->
                Timber.e("ContentIndexingWorker: failed for $userId with $error")
                indexer.cancel(userId)
                Result.failure()
            },
            ifRight = { Result.success() }
        )
    }

    /**
     * Index every eligible account in turn, advancing as each completes. A single worker (and a
     * single foreground service) drives the whole sweep. An account that errors out is skipped for
     * the remainder of this sweep so it cannot wedge the loop; it is retried on the next sweep.
     *
     * The sweep also reacts to the active (primary) user changing: when the user switches to an
     * account that should be indexed first, the in-flight account is paused (its partial index is
     * preserved) and the loop re-evaluates primary-first, so the account now in use is prioritised.
     */
    private suspend fun runSweep(runAsForeground: Boolean): Result {
        val failed = mutableSetOf<UserId>()
        while (true) {
            val next = findFirstEligibleAccountToIndex(skip = failed) ?: break
            when (indexAccountWithPreemption(next, runAsForeground)) {
                IndexOutcome.Completed -> Timber.d("ContentIndexingWorker: sweep completed $next, advancing")
                IndexOutcome.Preempted -> {
                    Timber.d("ContentIndexingWorker: $next preempted by active-user change, pausing")
                    indexer.cancel(next) // preserve partial index; the account is revisited later
                }
                IndexOutcome.Failed -> {
                    Timber.e("ContentIndexingWorker: sweep failed for $next, advancing")
                    indexer.cancel(next)
                    failed += next
                }
            }
        }
        Timber.d("ContentIndexingWorker: sweep finished")
        return Result.success()
    }

    /**
     * Indexes [userId], racing it against active-user changes. Returns [IndexOutcome.Preempted] when
     * the user switches to an account that takes priority, so the caller can pause and re-evaluate.
     */
    private suspend fun indexAccountWithPreemption(userId: UserId, runAsForeground: Boolean): IndexOutcome {
        return coroutineScope {
            val indexing = async { indexAccount(userId, runAsForeground) }
            val preemption = launch {
                awaitPreemption(currentlyIndexing = userId) { indexing.cancel(PreemptionSignal()) }
            }
            try {
                indexing.await().fold(
                    ifLeft = { IndexOutcome.Failed },
                    ifRight = { IndexOutcome.Completed }
                )
            } catch (signal: PreemptionSignal) {
                Timber.d("ContentIndexingWorker: $userId preempted (${signal.message})")
                // Let the indexing coroutine finish tearing down its watch stream before the caller
                // pauses Rust indexing for this account.
                indexing.join()
                IndexOutcome.Preempted
            } finally {
                preemption.cancel()
            }
        }
    }

    @OptIn(FlowPreview::class)
    private suspend fun awaitPreemption(currentlyIndexing: UserId, onPreempt: () -> Unit) {
        userSessionRepository.observePrimaryUserId()
            .filterNotNull()
            .distinctUntilChanged()
            .debounce(PreemptionDebounceMillis.milliseconds)
            .collect {
                // The account now in use takes priority only if it is itself eligible to index.
                val topPriority = findFirstEligibleAccountToIndex()
                if (topPriority != null && topPriority != currentlyIndexing) {
                    Timber.d("ContentIndexingWorker: active user prioritises $topPriority over $currentlyIndexing")
                    onPreempt()
                }
            }
    }

    private suspend fun indexAccount(userId: UserId, runAsForeground: Boolean): Either<ContentIndexingError, Unit> {
        currentUserId = userId
        val accountLabel = accountLabelFor(userId)
        setProgress(workDataOf(KeyCurrentUserId to userId.id, KeyProgress to 0.0))
        if (runAsForeground) trySetForeground(userId, accountLabel, progress = 0.0)
        return indexer.index(userId) { percent ->
            setProgress(workDataOf(KeyCurrentUserId to userId.id, KeyProgress to percent))
            if (runAsForeground) trySetForeground(userId, accountLabel, percent)
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val userId = userIdOrNull()
        val label = userId?.let { accountLabelFor(it) }
        return buildForegroundInfo(userId, label, progress = null)
    }

    private fun userIdOrNull(): UserId? = inputData.getString(KeyUserId)?.takeIf { it.isNotBlank() }?.let(::UserId)

    private suspend fun accountLabelFor(userId: UserId): String? = runCatching {
        userSessionRepository.getAccount(userId)?.primaryAddress
    }.getOrNull()

    @Suppress("TooGenericExceptionCaught")
    private suspend fun trySetForeground(
        userId: UserId,
        accountLabel: String?,
        progress: Double?
    ) {
        try {
            setForeground(buildForegroundInfo(userId, accountLabel, progress))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // FGS promotion can be denied if the app is in the background on Android 12+, or if the
            // dataSync 6h/24h budget is exhausted on Android 15+. Indexing continues in background.
            Timber.w(e, "ContentIndexingWorker: setForeground denied, continuing in background")
        }
    }

    @OptIn(FlowPreview::class)
    private suspend fun observeModeSwap(
        runAsForeground: Boolean,
        allowMobileData: Boolean,
        autoAdvance: Boolean
    ) {
        appInBackgroundState.observe()
            .debounce(ModeSwapDebounceMillis.milliseconds)
            .distinctUntilChanged()
            .collect { isBackground ->
                if (isBackground != runAsForeground) {
                    Timber.d("ContentIndexingWorker: swapping mode (foreground=$isBackground)")
                    enqueueSelf(
                        restartUserId(autoAdvance),
                        runAsForeground = isBackground,
                        allowMobileData,
                        autoAdvance
                    )
                }
            }
    }

    private suspend fun handleCancellation(allowMobileData: Boolean, autoAdvance: Boolean) {
        val reason = currentStopReason()
        val selfRestarting = isSelfRestarting.get()
        when (decideCancellationAction(reason, selfRestarting)) {
            CancellationAction.RestartInBackgroundMode -> {
                Timber.w("ContentIndexingWorker: FGS timeout, restarting in background mode")
                enqueueSelf(restartUserId(autoAdvance), runAsForeground = false, allowMobileData, autoAdvance)
            }

            CancellationAction.PreserveIndexerSession -> {
                Timber.d(
                    "ContentIndexingWorker: stopped (selfRestart=$selfRestarting, reason=$reason), indexer preserved"
                )
            }

            CancellationAction.ReleaseIndexerSession -> {
                Timber.d("ContentIndexingWorker: cancelled by app, releasing indexer session")
                currentUserId?.let { indexer.cancel(it) }
            }
        }
    }

    private fun currentStopReason(): Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        stopReason
    } else {
        WorkInfo.STOP_REASON_NOT_STOPPED
    }

    // A sweep restart must stay untagged (see buildRequest) so it keeps observing/cancelling as a
    // whole sweep instead of getting pinned to whichever account happened to be in flight.
    private fun restartUserId(autoAdvance: Boolean): UserId? = currentUserId.takeUnless { autoAdvance }

    private fun enqueueSelf(
        userId: UserId?,
        runAsForeground: Boolean,
        allowMobileData: Boolean,
        autoAdvance: Boolean
    ) {
        isSelfRestarting.set(true)
        WorkManager.getInstance(context).enqueueUniqueWork(
            UniqueName,
            ExistingWorkPolicy.REPLACE,
            buildRequest(userId, runAsForeground, allowMobileData, autoAdvance)
        )
    }

    private fun buildForegroundInfo(
        userId: UserId?,
        accountLabel: String?,
        progress: Double?
    ): ForegroundInfo {
        val notification = ContentIndexingNotification.build(context, userId, accountLabel, progress).build().apply {
            flags = flags or Notification.FLAG_NO_CLEAR or Notification.FLAG_ONGOING_EVENT
        }
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

    companion object {

        const val UniqueName = "content_indexing_worker"
        const val KeyProgress = "ContentIndexingWorker.Progress"
        const val KeyUserId = "ContentIndexingWorker.UserId"
        const val KeyCurrentUserId = "ContentIndexingWorker.CurrentUserId"
        const val KeyRunAsForeground = "ContentIndexingWorker.RunAsForeground"
        const val KeyAllowMobileData = "ContentIndexingWorker.AllowMobileData"
        const val KeyAutoAdvance = "ContentIndexingWorker.AutoAdvance"
        const val TagUserPrefix = "content_indexing_user:"

        private const val ModeSwapDebounceMillis = 2_000L
        private const val PreemptionDebounceMillis = 500L

        fun userTag(userId: String): String = "$TagUserPrefix$userId"

        internal fun decideCancellationAction(stopReason: Int, isSelfRestarting: Boolean): CancellationAction = when {
            stopReason == WorkInfo.STOP_REASON_FOREGROUND_SERVICE_TIMEOUT ||
                stopReason == WorkInfo.STOP_REASON_TIMEOUT ->
                CancellationAction.RestartInBackgroundMode

            isSelfRestarting -> CancellationAction.PreserveIndexerSession
            stopReason == WorkInfo.STOP_REASON_CANCELLED_BY_APP -> CancellationAction.ReleaseIndexerSession
            else -> CancellationAction.PreserveIndexerSession
        }

        /**
         * Builds an indexing request. A non-null [userId] indexes a single account (tagged for
         * per-account cancellation); a null [userId] with [autoAdvance] set drives a full sweep,
         * which discovers its accounts at runtime and publishes the active one via progress data.
         */
        fun buildRequest(
            userId: UserId?,
            runAsForeground: Boolean,
            allowMobileData: Boolean,
            autoAdvance: Boolean
        ): OneTimeWorkRequest {
            val networkType = if (allowMobileData) NetworkType.CONNECTED else NetworkType.UNMETERED
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(networkType)
                .setRequiresBatteryNotLow(true)
                .build()
            val data = Data.Builder()
                .putBoolean(KeyRunAsForeground, runAsForeground)
                .putBoolean(KeyAllowMobileData, allowMobileData)
                .putBoolean(KeyAutoAdvance, autoAdvance)
                .apply { if (userId != null) putString(KeyUserId, userId.id) }
                .build()
            return OneTimeWorkRequestBuilder<ContentIndexingWorker>()
                .setConstraints(constraints)
                .setInputData(data)
                .apply {
                    if (userId != null) {
                        addTag(userId.id)
                        addTag(userTag(userId.id))
                    }
                }
                .build()
        }
    }
}

internal enum class CancellationAction {
    RestartInBackgroundMode,
    PreserveIndexerSession,
    ReleaseIndexerSession
}

internal enum class IndexOutcome {
    Completed,
    Preempted,
    Failed
}

/** Cancels the in-flight per-account index when the active user switches to a higher-priority account. */
private class PreemptionSignal : CancellationException("preempted by active-user change")
