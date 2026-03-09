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

package ch.protonmail.android.mailsession.data.usecase

import ch.protonmail.android.mailsession.data.repository.MailSessionRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.channels.onSuccess
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import timber.log.Timber
import uniffi.mail_uniffi.BackgroundExecutionCallback
import uniffi.mail_uniffi.BackgroundExecutionResult
import uniffi.mail_uniffi.MailSessionStartBackgroundExecutionResult
import javax.inject.Inject

class StartBackgroundExecution @Inject constructor(
    private val mailSessionRepository: MailSessionRepository
) {

    operator fun invoke(): Flow<BackgroundExecutionResult> = callbackFlow {
        val callback = object : BackgroundExecutionCallback {
            override suspend fun onExecutionCompleted(result: BackgroundExecutionResult) {
                Timber.tag("BackgroundExecution").d("Background execution result received: $result")

                trySend(result)
                    .onSuccess { Timber.tag("BackgroundExecution").d("Result sent to flow successfully.") }
                    .onFailure { Timber.tag("BackgroundExecution").d("Failed to send result to flow.") }

                Timber.tag("BackgroundExecution").d("Flow closed after sending result.")
            }

        }

        var activeHandle: MailSessionStartBackgroundExecutionResult? = null
        val backgroundExecutionResult = mailSessionRepository.getMailSession().startBackgroundTask(callback)

        when (backgroundExecutionResult) {
            is MailSessionStartBackgroundExecutionResult.Ok -> {
                activeHandle = backgroundExecutionResult
                Timber.tag("BackgroundExecution").d("Started successfully.")
            }

            is MailSessionStartBackgroundExecutionResult.Error -> {
                Timber.tag("BackgroundExecution").e("Error starting background task: ${backgroundExecutionResult.v1}")
                close(RuntimeException("Failed to start background task: ${backgroundExecutionResult.v1}"))
            }
        }

        awaitClose {
            activeHandle?.destroy()
            activeHandle = null
            Timber.tag("BackgroundExecution").d("callbackFlow handle cleaned up.")
        }
    }
}
