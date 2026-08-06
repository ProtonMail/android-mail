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

package ch.protonmail.android.mailcontentsearch.data.wrapper

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.data.mapper.toDataError
import ch.protonmail.android.mailcommon.domain.model.DataError
import uniffi.mail_uniffi.MailUserSession
import uniffi.mail_uniffi.SyncEventStream
import uniffi.mail_uniffi.SyncService
import uniffi.mail_uniffi.SyncServiceIsEnabledResult
import uniffi.mail_uniffi.SyncServiceShouldShowMobileSheetResult
import uniffi.mail_uniffi.SyncServiceStartUserResult
import uniffi.mail_uniffi.SyncServiceSubscribeUserResult
import uniffi.mail_uniffi.SyncServiceUserStatusResult
import uniffi.mail_uniffi.SyncStartOutcome
import uniffi.mail_uniffi.SyncStatus
import uniffi.mail_uniffi.VoidProtonResult

/**
 * As of SDK 0.169.6 the sync service is a session-wide orchestrator: it is constructed from the
 * [uniffi.mail_uniffi.MailSession] and every per-user operation takes the user session as an
 * explicit context. This wrapper binds one instance to a single [ctx] so callers keep working
 * against a per-user API.
 */
class SyncServiceWrapper(private val syncService: SyncService, private val ctx: MailUserSession) {

    suspend fun isEnabled(): Either<DataError, Boolean> {
        return when (val result = syncService.isEnabled(ctx)) {
            is SyncServiceIsEnabledResult.Error -> result.v1.toDataError().left()
            is SyncServiceIsEnabledResult.Ok -> result.v1.right()
        }
    }

    suspend fun setEnabled(enabled: Boolean): Either<DataError, Unit> =
        when (val result = syncService.setEnabled(ctx, enabled)) {
            is VoidProtonResult.Error -> result.v1.toDataError().left()
            VoidProtonResult.Ok -> Unit.right()
        }

    suspend fun start(): Either<DataError, SyncStartOutcome> {
        return when (val result = syncService.startUser(ctx)) {
            is SyncServiceStartUserResult.Error -> result.v1.toDataError().left()
            is SyncServiceStartUserResult.Ok -> result.v1.right()
        }
    }

    suspend fun stop(): Either<DataError, Unit> {
        return when (val result = syncService.stop()) {
            is VoidProtonResult.Error -> result.v1.toDataError().left()
            VoidProtonResult.Ok -> Unit.right()
        }
    }

    suspend fun reset(): Either<DataError, Unit> {
        return when (val result = syncService.reset(ctx)) {
            is VoidProtonResult.Error -> result.v1.toDataError().left()
            VoidProtonResult.Ok -> Unit.right()
        }
    }

    suspend fun status(): Either<DataError, SyncStatus> {
        return when (val result = syncService.userStatus(ctx)) {
            is SyncServiceUserStatusResult.Error -> result.v1.toDataError().left()
            is SyncServiceUserStatusResult.Ok -> result.v1.right()
        }
    }

    fun subscribe(): Either<DataError, SyncEventStream> {
        return when (val result = syncService.subscribeUser(ctx)) {
            is SyncServiceSubscribeUserResult.Error -> result.v1.toDataError().left()
            is SyncServiceSubscribeUserResult.Ok -> result.v1.right()
        }
    }

    suspend fun shouldShowMobileBottomSheet(): Either<DataError, Boolean> {
        return when (val result = syncService.shouldShowMobileSheet(ctx)) {
            is SyncServiceShouldShowMobileSheetResult.Error -> result.v1.toDataError().left()
            is SyncServiceShouldShowMobileSheetResult.Ok -> result.v1.right()
        }
    }
}
