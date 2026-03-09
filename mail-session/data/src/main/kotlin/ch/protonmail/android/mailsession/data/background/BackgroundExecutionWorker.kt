/*
 * Copyright (c) 2022 Proton Technologies AG
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
import androidx.work.WorkerParameters
import ch.protonmail.android.mailsession.data.usecase.StartBackgroundExecution
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import timber.log.Timber
import uniffi.mail_uniffi.BackgroundExecutionStatus

@HiltWorker
internal class BackgroundExecutionWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted private val params: WorkerParameters,
    private val startBackgroundExecution: StartBackgroundExecution
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = triggerBackgroundExecution()

    private suspend fun triggerBackgroundExecution(): Result {
        Timber.tag("BackgroundExecutionWorker").d("Triggering background execution...")

        val result = startBackgroundExecution().first()

        return when (val status = result.status) {
            is BackgroundExecutionStatus.Failed -> {
                logger.d("Failed with error: ${status.v1}")
                Result.failure()
            }

            // Consider as a success if aborted in foreground/no logged in accounts.
            BackgroundExecutionStatus.AbortedInForeground,
            BackgroundExecutionStatus.SkippedNoActiveContexts,
            BackgroundExecutionStatus.AbortedInBackground,
            BackgroundExecutionStatus.Executed,
            BackgroundExecutionStatus.TimedOut -> {
                logger.d(
                    "Execution completed - unsentMessages: %s, pendingActions: %s",
                    result.hasUnsentMessages, result.hasPendingActions
                )
                Result.success()
            }
        }
    }

    private companion object {

        private val logger = Timber.tag("BackgroundExecutionWorker")
    }
}
