/*
 * Copyright (c) 2026 Proton Technologies AG
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

package ch.protonmail.android.mailsession.data.background

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import ch.protonmail.android.mailsession.data.usecase.StartBackgroundExecution
import ch.protonmail.android.mailsession.domain.background.PendingSendTracker
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.milliseconds

/**
 * Expedited worker scheduled when the app is backgrounded with a send still in flight. It resumes the
 * paused send queue. It the foreground notification until the queued sends actually
 * complete.
 */
@HiltWorker
internal class SendMessageCompletionWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val startBackgroundExecution: StartBackgroundExecution,
    private val pendingSendTracker: PendingSendTracker
) : CoroutineWorker(context, params) {

    // Only called on Android 11 and lower, where WorkManager runs expedited work as a foreground
    // service and asks for its notification.
    override suspend fun getForegroundInfo(): ForegroundInfo =
        ForegroundInfo(SendingNotification.NotificationId, SendingNotification.build(context).build())

    override suspend fun doWork(): Result = try {
        drainPendingSends()
    } catch (e: CancellationException) {
        Timber.d("Send completion: work stopped; skipping - $e")
        Result.success()
    }

    private suspend fun drainPendingSends(): Result = coroutineScope {
        Timber.d("Send completion: Draining pending sends...")
        val execution = launch { startBackgroundExecution().first() }
        withTimeoutOrNull(MaxDrainDurationMs.milliseconds) {
            while (pendingSendTracker.hasPendingSends() && execution.isActive) {
                delay(PollIntervalMs.milliseconds)
            }
        }
        execution.cancel()
        Timber.d("Send completion: Pending sends drained, stopping foreground work.")
        Result.success()
    }

    private companion object {

        private const val PollIntervalMs = 500L
        private const val MaxDrainDurationMs = 30_000L
    }
}
