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

package ch.protonmail.android.mailsession.data.wrapper

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.data.mapper.toDataError
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailsession.data.repository.MailSessionRepository
import ch.protonmail.android.mailsession.data.repository.runInRustBackground
import ch.protonmail.android.mailsession.domain.wrapper.MailUserSessionWrapper
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import timber.log.Timber
import uniffi.mail_uniffi.SyncEventStream
import uniffi.mail_uniffi.SyncOrchestartorStartOutcome
import uniffi.mail_uniffi.SyncOrchestartorStartStats
import uniffi.mail_uniffi.SyncOrchestratorEventStream
import uniffi.mail_uniffi.SyncProgress
import uniffi.mail_uniffi.SyncService
import uniffi.mail_uniffi.SyncServiceIsEnabledResult
import uniffi.mail_uniffi.SyncServiceShouldShowMobileSheetResult
import uniffi.mail_uniffi.SyncServiceStartResult
import uniffi.mail_uniffi.SyncServiceStartUserResult
import uniffi.mail_uniffi.SyncServiceSubscribeResult
import uniffi.mail_uniffi.SyncServiceSubscribeUserResult
import uniffi.mail_uniffi.SyncServiceUserProgressResult
import uniffi.mail_uniffi.SyncServiceUserStatusResult
import uniffi.mail_uniffi.SyncStartOutcome
import uniffi.mail_uniffi.SyncStatus
import uniffi.mail_uniffi.VoidProtonResult
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Wraps the session-wide Rust sync orchestrator.
 *
 * Every method other than [subscribe] and [subscribeUser] is an actor round-trip. Once the app
 * leaves the foreground the Rust task service is suspended, so those round-trips would hang
 * indefinitely unless a background execution scope is open - hence every call goes through
 * [actorCall], which opens one and bounds the wait.
 */
@Suppress("TooManyFunctions")
class SyncServiceWrapper(
    private val syncService: SyncService,
    private val mailSessionRepository: MailSessionRepository
) {

    /**
     * [start] and [stop] act on the whole orchestrator, so they are serialised against each other
     * to keep a stop from landing in the middle of a start (and vice versa).
     */
    private val lifecycleMutex = Mutex()

    suspend fun start(): Either<DataError, SyncOrchestartorStartStats> = lifecycleMutex.withLock {
        actorCall { service ->
            when (val result = service.start()) {
                is SyncServiceStartResult.Error -> result.v1.toDataError().left()
                is SyncServiceStartResult.Ok -> result.v1.stats().right()
            }
        }
    }

    suspend fun stop(): Either<DataError, Unit> = lifecycleMutex.withLock {
        actorCall { service ->
            when (val result = service.stop()) {
                is VoidProtonResult.Error -> result.v1.toDataError().left()
                VoidProtonResult.Ok -> Unit.right()
            }
        }
    }

    fun subscribe(): Either<DataError, SyncOrchestratorEventStream> = when (val result = syncService.subscribe()) {
        is SyncServiceSubscribeResult.Error -> result.v1.toDataError().left()
        is SyncServiceSubscribeResult.Ok -> result.v1.right()
    }

    fun subscribeUser(userSession: MailUserSessionWrapper): Either<DataError, SyncEventStream> =
        when (val result = syncService.subscribeUser(userSession.getRustUserSession())) {
            is SyncServiceSubscribeUserResult.Error -> result.v1.toDataError().left()
            is SyncServiceSubscribeUserResult.Ok -> result.v1.right()
        }

    suspend fun startUser(userSession: MailUserSessionWrapper): Either<DataError, SyncStartOutcome> =
        actorCall { service ->
            when (val result = service.startUser(userSession.getRustUserSession())) {
                is SyncServiceStartUserResult.Error -> result.v1.toDataError().left()
                is SyncServiceStartUserResult.Ok -> result.v1.right()
            }
        }

    suspend fun isEnabled(userSession: MailUserSessionWrapper): Either<DataError, Boolean> = actorCall { service ->
        when (val result = service.isEnabled(userSession.getRustUserSession())) {
            is SyncServiceIsEnabledResult.Error -> result.v1.toDataError().left()
            is SyncServiceIsEnabledResult.Ok -> result.v1.right()
        }
    }

    suspend fun setEnabled(userSession: MailUserSessionWrapper, enabled: Boolean): Either<DataError, Unit> =
        actorCall { service ->
            when (val result = service.setEnabled(userSession.getRustUserSession(), enabled)) {
                is VoidProtonResult.Error -> result.v1.toDataError().left()
                VoidProtonResult.Ok -> Unit.right()
            }
        }

    suspend fun userStatus(userSession: MailUserSessionWrapper): Either<DataError, SyncStatus> = actorCall { service ->
        when (val result = service.userStatus(userSession.getRustUserSession())) {
            is SyncServiceUserStatusResult.Error -> result.v1.toDataError().left()
            is SyncServiceUserStatusResult.Ok -> result.v1.right()
        }
    }

    suspend fun userProgress(userSession: MailUserSessionWrapper): Either<DataError, SyncProgress> =
        actorCall { service ->
            when (val result = service.userProgress(userSession.getRustUserSession())) {
                is SyncServiceUserProgressResult.Error -> result.v1.toDataError().left()
                is SyncServiceUserProgressResult.Ok -> result.v1.right()
            }
        }

    suspend fun reset(userSession: MailUserSessionWrapper): Either<DataError, Unit> =
        actorCall(timeout = ResetTimeout) { service ->
            when (val result = service.reset(userSession.getRustUserSession())) {
                is VoidProtonResult.Error -> result.v1.toDataError().left()
                VoidProtonResult.Ok -> Unit.right()
            }
        }

    suspend fun shouldShowMobileSheet(userSession: MailUserSessionWrapper): Either<DataError, Boolean> =
        actorCall { service ->
            when (val result = service.shouldShowMobileSheet(userSession.getRustUserSession())) {
                is SyncServiceShouldShowMobileSheetResult.Error -> result.v1.toDataError().left()
                is SyncServiceShouldShowMobileSheetResult.Ok -> result.v1.right()
            }
        }

    private suspend fun <T> actorCall(
        timeout: Duration = DefaultTimeout,
        block: suspend (SyncService) -> Either<DataError, T>
    ): Either<DataError, T> = try {
        mailSessionRepository.runInRustBackground {
            withTimeout(timeout) { block(syncService) }
        }
    } catch (timedOut: TimeoutCancellationException) {
        Timber.w(timedOut, "content-search: sync service call did not answer within $timeout")
        DataError.Local.Unknown.left()
    }

    private fun SyncOrchestartorStartOutcome.stats(): SyncOrchestartorStartStats = when (this) {
        is SyncOrchestartorStartOutcome.Started -> v1
        is SyncOrchestartorStartOutcome.Ongoing -> v1
    }

    private companion object {

        val DefaultTimeout: Duration = 30.seconds

        // reset() clears every indexed message for the account before answering.
        val ResetTimeout: Duration = 120.seconds
    }
}
