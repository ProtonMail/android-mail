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

package ch.protonmail.android.mailcontentsearch.data.indexer

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcontentsearch.data.mapper.isTerminal
import ch.protonmail.android.mailcontentsearch.data.usecase.CreateRustSyncService
import ch.protonmail.android.mailcontentsearch.domain.model.ContentIndexingError
import ch.protonmail.android.mailcontentsearch.domain.repository.ContentSearchIndexer
import ch.protonmail.android.mailsession.data.usecase.ExecuteWithUserSession
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import uniffi.mail_uniffi.SyncDriverEvent
import uniffi.mail_uniffi.SyncEvent
import uniffi.mail_uniffi.SyncEventStream
import uniffi.mail_uniffi.SyncStartOutcome
import javax.inject.Inject

class RustContentSearchIndexer @Inject constructor(
    private val executeWithUserSession: ExecuteWithUserSession,
    private val createRustSyncService: CreateRustSyncService
) : ContentSearchIndexer {

    override suspend fun index(
        userId: UserId,
        onProgress: suspend (Double) -> Unit
    ): Either<ContentIndexingError, Unit> = executeWithUserSession(userId) { wrapper ->
        val syncService = createRustSyncService(wrapper)

        val stream = when (val result = syncService.subscribe()) {
            is Either.Left -> {
                Timber.e("content-search: failed to subscribe to sync events: ${result.value}")
                return@executeWithUserSession ContentIndexingError.Unknown(result.value.toString()).left()
            }

            is Either.Right -> result.value
        }

        try {
            val startOutcome = when (val result = syncService.start()) {
                is Either.Left -> {
                    Timber.e("content-search: failed to start sync: ${result.value}")
                    return@executeWithUserSession ContentIndexingError.Unknown(result.value.toString()).left()
                }

                is Either.Right -> result.value
            }

            when (startOutcome) {
                SyncStartOutcome.COMPLETED -> return@executeWithUserSession Unit.right()
                SyncStartOutcome.DISABLED -> return@executeWithUserSession ContentIndexingError.Cancelled.left()
                SyncStartOutcome.STARTED, SyncStartOutcome.ONGOING -> Unit
            }

            consumeEvents(stream, onProgress)
        } finally {
            runCatching { stream.destroy() }
        }
    }.fold(
        ifLeft = {
            Timber.e("content-search: failed to start indexing session: $it")
            ContentIndexingError.Unknown(it.toString()).left()
        },
        ifRight = { it }
    )

    override suspend fun cancel(userId: UserId) {
        executeWithUserSession(userId) { wrapper ->
            createRustSyncService(wrapper).stop()
        }
    }

    private suspend fun consumeEvents(
        stream: SyncEventStream,
        onProgress: suspend (Double) -> Unit
    ): Either<ContentIndexingError, Unit> {
        while (true) {
            val event = stream.next() ?: run {
                Timber.w("content-search: sync event stream closed unexpectedly")
                return ContentIndexingError.Cancelled.left()
            }

            if (event is SyncEvent.Progress) onProgress(event.v1)

            if (event.isTerminal()) return event.toResult()
        }
    }

    private fun SyncEvent.toResult(): Either<ContentIndexingError, Unit> = when (this) {
        is SyncEvent.Completed -> Unit.right()
        is SyncEvent.Stopped -> ContentIndexingError.Cancelled.left()
        is SyncEvent.Driver -> when (val driverEvent = this.v1) {
            is SyncDriverEvent.Failure -> ContentIndexingError.Unknown(driverEvent.v1).left()
            is SyncDriverEvent.Completed -> Unit.right() // unreachable; guarded by isTerminal()
        }

        is SyncEvent.Started,
        is SyncEvent.Progress,
        is SyncEvent.Worker -> Unit.right() // unreachable; guarded by isTerminal()
    }
}
