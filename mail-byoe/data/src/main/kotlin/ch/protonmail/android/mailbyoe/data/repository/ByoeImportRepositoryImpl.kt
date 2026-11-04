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

package ch.protonmail.android.mailbyoe.data.repository

import ch.protonmail.android.mailbyoe.domain.model.ByoeImportError
import ch.protonmail.android.mailbyoe.domain.model.ByoeImportState
import ch.protonmail.android.mailbyoe.domain.model.ByoeImportStatus
import ch.protonmail.android.mailbyoe.domain.repository.ByoeImportRepository
import ch.protonmail.android.mailsession.domain.repository.UserSessionRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import uniffi.mail_uniffi.ByoeImportsImportStatusesResult
import uniffi.mail_uniffi.ByoeImportsWatchImportsResult
import uniffi.mail_uniffi.LiveQueryCallback
import javax.inject.Inject
import uniffi.mail_uniffi.ByoeImportError as RustByoeImportError
import uniffi.mail_uniffi.ByoeImportState as RustByoeImportState
import uniffi.mail_uniffi.ByoeImportStatus as RustByoeImportStatus

/**
 * Reads the imports from `MailUserSession.byoeImports()` and reads them again whenever the SDK reports a change.
 */
class ByoeImportRepositoryImpl @Inject constructor(
    private val userSessionRepository: UserSessionRepository
) : ByoeImportRepository {

    override fun observeImportStatuses(userId: UserId): Flow<List<ByoeImportStatus>> = callbackFlow {
        val session = userSessionRepository.getUserSession(userId)
        if (session == null) {
            Timber.e("byoe: no user session to observe the Gmail imports")
            close()
            return@callbackFlow
        }

        val imports = session.getRustUserSession().byoeImports()
        val refreshes = Channel<Unit>(Channel.CONFLATED)
        val callback = object : LiveQueryCallback {
            override fun onUpdate() {
                refreshes.trySend(Unit)
            }
        }

        // Watch before the first read, so a change between the two is not missed.
        val watch = imports.watchImports(callback)
        if (watch is ByoeImportsWatchImportsResult.Error) {
            Timber.e("byoe: unable to watch the Gmail imports: ${watch.v1}")
        }
        refreshes.trySend(Unit)

        launch {
            for (refresh in refreshes) {
                when (val result = imports.importStatuses()) {
                    is ByoeImportsImportStatusesResult.Ok -> send(result.v1.map { it.toImportStatus() })
                    is ByoeImportsImportStatusesResult.Error ->
                        Timber.e("byoe: unable to read the Gmail imports: ${result.v1}")
                }
            }
        }

        awaitClose {
            (watch as? ByoeImportsWatchImportsResult.Ok)?.v1?.disconnect()
            watch.destroy()
            imports.destroy()
        }
    }
}

private fun RustByoeImportStatus.toImportStatus() = ByoeImportStatus(
    addressId = addressId.value.toString(),
    state = state.toImportState(),
    error = error?.toImportError(),
    processed = processed.toLong(),
    total = total?.toLong()
)

private fun RustByoeImportState.toImportState() = when (this) {
    RustByoeImportState.QUEUED -> ByoeImportState.Queued
    RustByoeImportState.RUNNING -> ByoeImportState.Running
    RustByoeImportState.DONE -> ByoeImportState.Done
    RustByoeImportState.FAILED -> ByoeImportState.Failed
    RustByoeImportState.PAUSED -> ByoeImportState.Paused
    RustByoeImportState.CANCELED -> ByoeImportState.Canceled
    RustByoeImportState.DELAYED -> ByoeImportState.Delayed
    RustByoeImportState.UNKNOWN -> ByoeImportState.Unknown
}

private fun RustByoeImportError.toImportError() = when (this) {
    RustByoeImportError.CONNECTION -> ByoeImportError.Connection
    RustByoeImportError.STORAGE_LIMIT -> ByoeImportError.StorageLimit
    RustByoeImportError.UNKNOWN -> ByoeImportError.Unknown
}
