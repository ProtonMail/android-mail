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
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import ch.protonmail.android.mailcommon.domain.AppInBackgroundState
import ch.protonmail.android.mailcontentsearch.domain.repository.ContentSearchIndexer
import ch.protonmail.android.mailsession.data.repository.MailSessionRepository
import ch.protonmail.android.mailsession.data.repository.runInRustBackground
import ch.protonmail.android.mailsession.domain.repository.UserSessionRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
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
    private val appInBackgroundState: AppInBackgroundState
) : CoroutineWorker(context, workerParameters) {

    private val isSelfRestarting = AtomicBoolean(false)

    override suspend fun doWork(): Result {
        val userId = userIdOrNull()
        if (userId == null) {
            Timber.e("ContentIndexingWorker: missing userId in input data")
            return Result.failure()
        }
        val runAsForeground = inputData.getBoolean(KeyRunAsForeground, true)
        val allowMobileData = inputData.getBoolean(KeyAllowMobileData, false)

        Timber.d("ContentIndexingWorker: starting for $userId (foreground=$runAsForeground)")

        val accountLabel = accountLabelFor(userId)
        if (runAsForeground) trySetForeground(userId, accountLabel, progress = null)

        return mailSessionRepository.runInRustBackground {
            coroutineScope {
                val swapObserver = launch { observeModeSwap(userId, runAsForeground, allowMobileData) }
                try {
                    indexer.index(userId) { percent ->
                        setProgress(workDataOf(KeyProgress to percent))
                        if (runAsForeground) trySetForeground(userId, accountLabel, percent)
                    }.fold(
                        ifLeft = { error ->
                            Timber.e("ContentIndexingWorker: failed with $error")
                            indexer.cancel(userId)
                            Result.failure()
                        },
                        ifRight = { Result.success() }
                    )
                } catch (cancellation: CancellationException) {
                    withContext(NonCancellable) { handleCancellation(userId, allowMobileData) }
                    throw cancellation
                } finally {
                    swapObserver.cancel()
                }
            }
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
        userId: UserId,
        runAsForeground: Boolean,
        allowMobileData: Boolean
    ) {
        appInBackgroundState.observe()
            .debounce(ModeSwapDebounceMillis.milliseconds)
            .distinctUntilChanged()
            .collect { isBackground ->
                if (isBackground != runAsForeground) {
                    Timber.d("ContentIndexingWorker: swapping mode (foreground=$isBackground) for $userId")
                    enqueueSelf(userId, runAsForeground = isBackground, allowMobileData = allowMobileData)
                }
            }
    }

    private suspend fun handleCancellation(userId: UserId, allowMobileData: Boolean) {
        val reason = currentStopReason()
        val selfRestarting = isSelfRestarting.get()
        when (decideCancellationAction(reason, selfRestarting)) {
            CancellationAction.RestartInBackgroundMode -> {
                Timber.w("ContentIndexingWorker: FGS timeout, restarting in background mode")
                enqueueSelf(userId, runAsForeground = false, allowMobileData = allowMobileData)
            }

            CancellationAction.PreserveIndexerSession -> {
                Timber.d(
                    "ContentIndexingWorker: stopped (selfRestart=$selfRestarting, reason=$reason), indexer preserved"
                )
            }

            CancellationAction.ReleaseIndexerSession -> {
                Timber.d("ContentIndexingWorker: cancelled by app, releasing indexer session")
                indexer.cancel(userId)
            }
        }
    }

    private fun currentStopReason(): Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        stopReason
    } else {
        WorkInfo.STOP_REASON_NOT_STOPPED
    }

    private fun enqueueSelf(
        userId: UserId,
        runAsForeground: Boolean,
        allowMobileData: Boolean
    ) {
        isSelfRestarting.set(true)
        WorkManager.getInstance(context).enqueueUniqueWork(
            UniqueName,
            ExistingWorkPolicy.REPLACE,
            buildRequest(userId, runAsForeground, allowMobileData)
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
        const val KeyRunAsForeground = "ContentIndexingWorker.RunAsForeground"
        const val KeyAllowMobileData = "ContentIndexingWorker.AllowMobileData"
        const val TagUserPrefix = "content_indexing_user:"

        private const val ModeSwapDebounceMillis = 2_000L

        fun userTag(userId: String): String = "$TagUserPrefix$userId"

        internal fun decideCancellationAction(stopReason: Int, isSelfRestarting: Boolean): CancellationAction = when {
            stopReason == WorkInfo.STOP_REASON_FOREGROUND_SERVICE_TIMEOUT ||
                stopReason == WorkInfo.STOP_REASON_TIMEOUT ->
                CancellationAction.RestartInBackgroundMode

            isSelfRestarting -> CancellationAction.PreserveIndexerSession
            stopReason == WorkInfo.STOP_REASON_CANCELLED_BY_APP -> CancellationAction.ReleaseIndexerSession
            else -> CancellationAction.PreserveIndexerSession
        }

        fun buildRequest(
            userId: UserId,
            runAsForeground: Boolean,
            allowMobileData: Boolean
        ): OneTimeWorkRequest {
            val networkType = if (allowMobileData) NetworkType.CONNECTED else NetworkType.UNMETERED
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(networkType)
                .setRequiresBatteryNotLow(true)
                .build()
            return OneTimeWorkRequestBuilder<ContentIndexingWorker>()
                .setConstraints(constraints)
                .setInputData(
                    workDataOf(
                        KeyUserId to userId.id,
                        KeyRunAsForeground to runAsForeground,
                        KeyAllowMobileData to allowMobileData
                    )
                )
                .addTag(userId.id)
                .addTag(userTag(userId.id))
                .build()
        }
    }
}

internal enum class CancellationAction {
    RestartInBackgroundMode,
    PreserveIndexerSession,
    ReleaseIndexerSession
}
