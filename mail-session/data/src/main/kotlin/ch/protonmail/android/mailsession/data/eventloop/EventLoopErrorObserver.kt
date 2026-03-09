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

package ch.protonmail.android.mailsession.data.eventloop

import ch.protonmail.android.mailcommon.domain.coroutines.DefaultDispatcher
import ch.protonmail.android.mailsession.domain.eventloop.EventLoopErrorSignal
import ch.protonmail.android.mailsession.domain.repository.UserSessionRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import timber.log.Timber
import uniffi.mail_uniffi.EventError
import uniffi.mail_uniffi.EventLoopErrorObserver
import uniffi.mail_uniffi.EventLoopErrorObserverHandle
import uniffi.mail_uniffi.MailUserSessionObserveEventLoopErrorsResult
import javax.inject.Inject

class RustEventLoopErrorObserver @Inject constructor(
    private val userSessionRepository: UserSessionRepository,
    private val eventLoopErrorFlow: EventLoopErrorSignal,
    @DefaultDispatcher private val dispatcher: CoroutineDispatcher
) {

    private var handle: EventLoopErrorObserverHandle? = null
    private var observerJob: Job? = null
    private val coroutineScope = CoroutineScope(SupervisorJob() + dispatcher)

    fun start() {
        observerJob?.cancel()
        observerJob = coroutineScope.launch {
            userSessionRepository.observePrimaryUserId()
                .filterNotNull()
                .collectLatest { userId ->
                    cleanupHandle()

                    val userSession = userSessionRepository.getUserSession(userId)

                    if (userSession == null) {
                        Timber.d("event-loop-observer: unable to retrieve user session for userId '$userId'")
                        return@collectLatest
                    }

                    val callback = object : EventLoopErrorObserver {
                        override suspend fun onEventLoopError(error: EventError) {
                            eventLoopErrorFlow.submit(error)
                            Timber.d("event-loop-observer: error $error")
                        }
                    }

                    when (val result = userSession.observeEventLoopErrors(callback)) {
                        is MailUserSessionObserveEventLoopErrorsResult.Error ->
                            Timber.e("event-loop-observer: failed to init observer ${result.v1}")

                        is MailUserSessionObserveEventLoopErrorsResult.Ok -> {
                            Timber.d("event-loop-observer: registered for $userId")
                            handle = result.v1
                        }
                    }
                }
        }
    }

    fun disconnect() {
        coroutineScope.cancel()
        cleanupHandle()
    }

    private fun cleanupHandle() {
        handle?.disconnect()
        handle = null
    }
}
