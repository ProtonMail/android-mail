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
import ch.protonmail.android.mailcontentsearch.domain.usecase.ObserveContentSearchEnabled
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
import kotlinx.coroutines.flow.combine
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
    private val observeContentSearchEnabled: ObserveContentSearchEnabled,
    private val appInBackgroundState: AppInBackgroundState
) : CoroutineWorker(context, workerParameters) {

    private val isSelfRestarting = AtomicBoolean(false)

    // The account currently being indexed. Tracked so cancellation can release the right Rust
    // session and so the account being indexed can be published via progress data.
    @Volatile
    private var currentUserId: UserId? = null

    override suspend fun doWork(): Result {
        val runAsForeground = inputData.getBoolean(KeyRunAsForeground, true)
        val allowMobileData = inputData.getBoolean(KeyAllowMobileData, false)

        Timber.d("ContentIndexingWorker: starting sweep (foreground=$runAsForeground)")

        return mailSessionRepository.runInRustBackground {
            coroutineScope {
                val swapObserver = launch { observeModeSwap(runAsForeground, allowMobileData) }
                try {
                    runSweep(runAsForeground)
                } catch (cancellation: CancellationException) {
                    withContext(NonCancellable) { handleCancellation(allowMobileData) }
                    throw cancellation
                } finally {
                    swapObserver.cancel()
                }
            }
        }
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
        val failedAccounts = mutableSetOf<UserId>()
        while (true) {
            val nextAccount = findFirstEligibleAccountToIndex(skip = failedAccounts) ?: break
            when (indexAccountWithInterruption(nextAccount, failedAccounts = failedAccounts, runAsForeground)) {
                IndexOutcome.Completed -> Timber.d("ContentIndexingWorker: sweep completed $nextAccount, advancing")
                IndexOutcome.Interrupted -> {
                    Timber.d("ContentIndexingWorker: $nextAccount interrupted, pausing and re-evaluating")
                    // preserve partial index; the account is revisited if still eligible
                    indexer.cancel(nextAccount)
                }
                IndexOutcome.Failed -> {
                    Timber.e("ContentIndexingWorker: sweep failed for $nextAccount, advancing")
                    indexer.cancel(nextAccount)
                    failedAccounts += nextAccount
                }
            }
        }
        Timber.d("ContentIndexingWorker: sweep finished")
        return Result.success()
    }

    /**
     * Indexes [userId], racing it against changes that mean it should no longer be the account being
     * indexed — the active user switching to a higher-priority account, or this account being
     * disabled. Returns [IndexOutcome.Interrupted] in that case so the caller can pause and re-evaluate.
     */
    private suspend fun indexAccountWithInterruption(
        userId: UserId,
        failedAccounts: Set<UserId>,
        runAsForeground: Boolean
    ): IndexOutcome {
        return coroutineScope {
            val indexing = async { indexAccount(userId, runAsForeground) }
            val interruption = launch {
                awaitInterruption(currentlyIndexing = userId, failedAccounts = failedAccounts) {
                    indexing.cancel(InterruptionSignal())
                }
            }
            try {
                indexing.await().fold(
                    ifLeft = { IndexOutcome.Failed },
                    ifRight = { IndexOutcome.Completed }
                )
            } catch (signal: InterruptionSignal) {
                Timber.d("ContentIndexingWorker: $userId interrupted (${signal.message})")
                // Let the indexing coroutine finish tearing down its watch stream before the caller
                // pauses Rust indexing for this account.
                indexing.join()
                IndexOutcome.Interrupted
            } finally {
                interruption.cancel()
            }
        }
    }

    @OptIn(FlowPreview::class)
    private suspend fun awaitInterruption(
        currentlyIndexing: UserId,
        failedAccounts: Set<UserId>,
        onInterrupt: () -> Unit
    ) {
        // React to the active user changing or this account's content-search setting changing, then
        // re-evaluate. If the account that should run next is no longer the one we are indexing
        // (a higher-priority active account, or this account became ineligible), interrupt it.
        // Honour the sweep's failed-accounts set so an account that already failed this sweep cannot
        // repeatedly interrupt the account currently making progress.
        combine(
            userSessionRepository.observePrimaryUserId().filterNotNull().distinctUntilChanged(),
            observeContentSearchEnabled(currentlyIndexing).distinctUntilChanged()
        ) { _, _ -> }
            .debounce(InterruptionDebounceMillis.milliseconds)
            .collect {
                val topPriority = findFirstEligibleAccountToIndex(skip = failedAccounts)
                if (topPriority != currentlyIndexing) {
                    Timber.d("ContentIndexingWorker: $topPriority should run instead of $currentlyIndexing")
                    onInterrupt()
                }
            }
    }

    private suspend fun indexAccount(userId: UserId, runAsForeground: Boolean): Either<ContentIndexingError, Unit> {
        currentUserId = userId
        val accountLabel = accountLabelFor(userId)
        Timber.d("content-search: $userId starting (progress=0.0)")
        setProgress(workDataOf(KeyCurrentUserId to userId.id, KeyProgress to 0.0))
        if (runAsForeground) trySetForeground(accountLabel, progress = 0.0)
        return indexer.index(userId) { percent ->
            Timber.d("content-search: $userId progress=$percent")
            setProgress(workDataOf(KeyCurrentUserId to userId.id, KeyProgress to percent))
            if (runAsForeground) trySetForeground(accountLabel, percent)
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo = buildForegroundInfo(accountLabel = null, progress = 0.0)

    private suspend fun accountLabelFor(userId: UserId): String? = runCatching {
        userSessionRepository.getAccount(userId)?.primaryAddress
    }.getOrNull()

    @Suppress("TooGenericExceptionCaught")
    private suspend fun trySetForeground(accountLabel: String?, progress: Double) {
        try {
            setForeground(buildForegroundInfo(accountLabel, progress))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // FGS promotion can be denied if the app is in the background on Android 12+, or if the
            // dataSync 6h/24h budget is exhausted on Android 15+. Indexing continues in background.
            Timber.w(e, "ContentIndexingWorker: setForeground denied, continuing in background")
        }
    }

    @OptIn(FlowPreview::class)
    private suspend fun observeModeSwap(runAsForeground: Boolean, allowMobileData: Boolean) {
        appInBackgroundState.observe()
            .debounce(ModeSwapDebounceMillis.milliseconds)
            .distinctUntilChanged()
            .collect { isBackground ->
                if (isBackground != runAsForeground) {
                    Timber.d("ContentIndexingWorker: swapping mode (foreground=$isBackground)")
                    enqueueSelf(runAsForeground = isBackground, allowMobileData)
                }
            }
    }

    private suspend fun handleCancellation(allowMobileData: Boolean) {
        val reason = currentStopReason()
        val selfRestarting = isSelfRestarting.get()
        when (decideCancellationAction(reason, selfRestarting)) {
            CancellationAction.RestartInBackgroundMode -> {
                Timber.w("ContentIndexingWorker: FGS timeout, restarting in background mode")
                enqueueSelf(runAsForeground = false, allowMobileData)
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

    private fun enqueueSelf(runAsForeground: Boolean, allowMobileData: Boolean) {
        isSelfRestarting.set(true)
        WorkManager.getInstance(context).enqueueUniqueWork(
            UniqueName,
            ExistingWorkPolicy.REPLACE,
            buildRequest(runAsForeground, allowMobileData)
        )
    }

    private fun buildForegroundInfo(accountLabel: String?, progress: Double): ForegroundInfo {
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

    companion object {

        const val UniqueName = "content_indexing_worker"
        const val KeyProgress = "ContentIndexingWorker.Progress"
        const val KeyCurrentUserId = "ContentIndexingWorker.CurrentUserId"
        const val KeyRunAsForeground = "ContentIndexingWorker.RunAsForeground"
        const val KeyAllowMobileData = "ContentIndexingWorker.AllowMobileData"

        private const val ModeSwapDebounceMillis = 2_000L
        private const val InterruptionDebounceMillis = 500L

        internal fun decideCancellationAction(stopReason: Int, isSelfRestarting: Boolean): CancellationAction = when {
            stopReason == WorkInfo.STOP_REASON_FOREGROUND_SERVICE_TIMEOUT ||
                stopReason == WorkInfo.STOP_REASON_TIMEOUT ->
                CancellationAction.RestartInBackgroundMode

            isSelfRestarting -> CancellationAction.PreserveIndexerSession
            stopReason == WorkInfo.STOP_REASON_CANCELLED_BY_APP -> CancellationAction.ReleaseIndexerSession
            else -> CancellationAction.PreserveIndexerSession
        }

        /**
         * Builds a sweep request. The worker discovers its accounts at runtime and publishes the
         * account it is currently indexing via progress data.
         */
        fun buildRequest(runAsForeground: Boolean, allowMobileData: Boolean): OneTimeWorkRequest {
            val networkType = if (allowMobileData) NetworkType.CONNECTED else NetworkType.UNMETERED
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(networkType)
                .setRequiresBatteryNotLow(true)
                .build()
            val data = Data.Builder()
                .putBoolean(KeyRunAsForeground, runAsForeground)
                .putBoolean(KeyAllowMobileData, allowMobileData)
                .build()
            return OneTimeWorkRequestBuilder<ContentIndexingWorker>()
                .setConstraints(constraints)
                .setInputData(data)
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
    Interrupted,
    Failed
}

/** Cancels the in-flight per-account index when that account should no longer be the one indexing. */
private class InterruptionSignal : CancellationException("interrupted: another account should run")
