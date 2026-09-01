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

package ch.protonmail.android.mailtelemetry.debug.data

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailsession.data.usecase.ExecuteWithUserSession
import ch.protonmail.android.mailsession.domain.repository.UserSessionRepository
import ch.protonmail.android.mailtelemetry.debug.domain.TelemetryDebugError
import ch.protonmail.android.mailtelemetry.debug.domain.TelemetryDebugEvent
import ch.protonmail.android.mailtelemetry.debug.domain.TelemetryDebugRepository
import ch.protonmail.android.mailtelemetry.debug.domain.TelemetryDebugSwitches
import kotlinx.coroutines.flow.firstOrNull
import uniffi.mail_uniffi.MailUserSession
import uniffi.mail_uniffi.MailUserSessionClearTelemetryDebugLogResult
import uniffi.mail_uniffi.MailUserSessionDiscardTelemetryPendingQueueResult
import uniffi.mail_uniffi.MailUserSessionHaltTelemetryDebugSendingResult
import uniffi.mail_uniffi.MailUserSessionIsCapturingTelemetryDebugEventsResult
import uniffi.mail_uniffi.MailUserSessionIsTelemetryDebugSendingHaltedResult
import uniffi.mail_uniffi.MailUserSessionResumeTelemetryDebugSendingResult
import uniffi.mail_uniffi.MailUserSessionStartCapturingTelemetryDebugEventsResult
import uniffi.mail_uniffi.MailUserSessionStopCapturingTelemetryDebugEventsResult
import uniffi.mail_uniffi.MailUserSessionTelemetryDebugEventsResult
import javax.inject.Inject
import javax.inject.Singleton
import uniffi.mail_uniffi.TelemetryDebugEvent as RustTelemetryDebugEvent

/**
 * Reads and drives the SDK's telemetry debug log for the primary logged-in user.
 */
@Singleton
class RustTelemetryDebugRepository @Inject constructor(
    private val userSessionRepository: UserSessionRepository,
    private val executeWithUserSession: ExecuteWithUserSession
) : TelemetryDebugRepository {

    override suspend fun getEvents(): Either<TelemetryDebugError, List<TelemetryDebugEvent>> = onPrimarySession {
        when (val result = it.telemetryDebugEvents()) {
            // The SDK returns the log oldest first; the screen shows the latest activity on top.
            is MailUserSessionTelemetryDebugEventsResult.Ok -> result.v1.map(::toDomain).asReversed().right()
            is MailUserSessionTelemetryDebugEventsResult.Error -> result.v1.toError()
        }
    }

    override suspend fun getSwitches(): Either<TelemetryDebugError, TelemetryDebugSwitches> = onPrimarySession {
        val capturing = when (val result = it.isCapturingTelemetryDebugEvents()) {
            is MailUserSessionIsCapturingTelemetryDebugEventsResult.Ok -> result.v1
            is MailUserSessionIsCapturingTelemetryDebugEventsResult.Error ->
                return@onPrimarySession result.v1.toError()
        }
        val halted = when (val result = it.isTelemetryDebugSendingHalted()) {
            is MailUserSessionIsTelemetryDebugSendingHaltedResult.Ok -> result.v1
            is MailUserSessionIsTelemetryDebugSendingHaltedResult.Error ->
                return@onPrimarySession result.v1.toError()
        }
        TelemetryDebugSwitches(capturing = capturing, sendingHalted = halted).right()
    }

    override suspend fun setCapturing(enabled: Boolean): Either<TelemetryDebugError, Unit> = onPrimarySession {
        if (enabled) {
            when (val result = it.startCapturingTelemetryDebugEvents()) {
                is MailUserSessionStartCapturingTelemetryDebugEventsResult.Ok -> Unit.right()
                is MailUserSessionStartCapturingTelemetryDebugEventsResult.Error -> result.v1.toError()
            }
        } else {
            when (val result = it.stopCapturingTelemetryDebugEvents()) {
                is MailUserSessionStopCapturingTelemetryDebugEventsResult.Ok -> Unit.right()
                is MailUserSessionStopCapturingTelemetryDebugEventsResult.Error -> result.v1.toError()
            }
        }
    }

    override suspend fun setSendingHalted(halted: Boolean): Either<TelemetryDebugError, Unit> = onPrimarySession {
        if (halted) {
            when (val result = it.haltTelemetryDebugSending()) {
                is MailUserSessionHaltTelemetryDebugSendingResult.Ok -> Unit.right()
                is MailUserSessionHaltTelemetryDebugSendingResult.Error -> result.v1.toError()
            }
        } else {
            when (val result = it.resumeTelemetryDebugSending()) {
                is MailUserSessionResumeTelemetryDebugSendingResult.Ok -> Unit.right()
                is MailUserSessionResumeTelemetryDebugSendingResult.Error -> result.v1.toError()
            }
        }
    }

    override suspend fun clearCapturedEvents(): Either<TelemetryDebugError, Unit> = onPrimarySession {
        when (val result = it.clearTelemetryDebugLog()) {
            is MailUserSessionClearTelemetryDebugLogResult.Ok -> Unit.right()
            is MailUserSessionClearTelemetryDebugLogResult.Error -> result.v1.toError()
        }
    }

    override suspend fun discardPendingQueue(): Either<TelemetryDebugError, Unit> = onPrimarySession {
        when (val result = it.discardTelemetryPendingQueue()) {
            is MailUserSessionDiscardTelemetryPendingQueueResult.Ok -> Unit.right()
            is MailUserSessionDiscardTelemetryPendingQueueResult.Error -> result.v1.toError()
        }
    }

    private suspend fun <T> onPrimarySession(
        block: suspend (MailUserSession) -> Either<TelemetryDebugError, T>
    ): Either<TelemetryDebugError, T> {
        val userId = userSessionRepository.observePrimaryUserId().firstOrNull()
            ?: return NoUserSessionError.left()

        return executeWithUserSession(userId) { block(it.getRustUserSession()) }
            .mapLeft { TelemetryDebugError(it.toString()) }
            .flatMap { it }
    }

    private fun toDomain(event: RustTelemetryDebugEvent) = TelemetryDebugEvent(
        id = event.id,
        measurementGroup = event.measurementGroup,
        name = event.event,
        values = event.values,
        dimensions = event.dimensions
    )

    private fun Any?.toError() = TelemetryDebugError(this.toString()).left()

    private companion object {

        val NoUserSessionError = TelemetryDebugError("No logged-in user session.")
    }
}
