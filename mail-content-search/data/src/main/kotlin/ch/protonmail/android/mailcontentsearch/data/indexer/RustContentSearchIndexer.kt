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
import ch.protonmail.android.mailcontentsearch.domain.model.ContentIndexingError
import ch.protonmail.android.mailcontentsearch.domain.repository.ContentSearchIndexer
import ch.protonmail.android.mailsession.data.usecase.ExecuteWithUserSession
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import uniffi.mail_uniffi.ContentSearchIndexingProgress
import uniffi.mail_uniffi.ContentSearchIndexingStatus
import uniffi.mail_uniffi.MailUserSessionContentSearchWatchIndexingStreamResult
import uniffi.mail_uniffi.WatchContentSearchIndexingStream
import uniffi.mail_uniffi.WatchContentSearchIndexingStreamNextAsyncResult
import javax.inject.Inject

class RustContentSearchIndexer @Inject constructor(
    private val executeWithUserSession: ExecuteWithUserSession
) : ContentSearchIndexer {

    override suspend fun index(
        userId: UserId,
        onProgress: suspend (Double) -> Unit
    ): Either<ContentIndexingError, Unit> = executeWithUserSession(userId) { wrapper ->
        wrapper.contentSearchStartIndexing()

        when (val result = wrapper.contentSearchWatchIndexingStream()) {
            is MailUserSessionContentSearchWatchIndexingStreamResult.Error -> {
                Timber.e("content-search: failed to start indexing watcher: ${result.v1}")
                ContentIndexingError.Unknown(result.v1.toString()).left()
            }

            is MailUserSessionContentSearchWatchIndexingStreamResult.Ok -> {
                val stream = result.v1
                try {
                    consumeIndexing(stream, onProgress)
                } finally {
                    stream.cancel()
                    runCatching { stream.close() }
                }
            }
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
            wrapper.contentSearchCancelIndexing(clearData = false)
        }
    }

    private suspend fun consumeIndexing(
        stream: WatchContentSearchIndexingStream,
        onProgress: suspend (Double) -> Unit
    ): Either<ContentIndexingError, Unit> {
        // Emit an initial progress tick so the worker shows a non-zero notification quickly.
        stream.initialProgress().let { progress ->
            progress.emitProgress(onProgress)
            if (progress.status.isTerminal()) return progress.status.toResult()
        }

        while (true) {
            when (val next = stream.nextAsync()) {
                is WatchContentSearchIndexingStreamNextAsyncResult.Error -> {
                    Timber.w("content-search: indexing watcher closed: ${next.v1}")
                    return ContentIndexingError.Cancelled.left()
                }

                is WatchContentSearchIndexingStreamNextAsyncResult.Ok -> {
                    val progress = next.v1
                    progress.emitProgress(onProgress)
                    if (progress.status.isTerminal()) return progress.status.toResult()
                }
            }
        }
    }

    private suspend fun ContentSearchIndexingProgress.emitProgress(onProgress: suspend (Double) -> Unit) {
        @Suppress("MagicNumber")
        estimatedFraction?.let { onProgress(it * 100) }
        Timber.d("content-search: progress = $this")
    }

    private fun ContentSearchIndexingStatus.toResult(): Either<ContentIndexingError, Unit> = when (this) {
        ContentSearchIndexingStatus.COMPLETED -> Unit.right()
        ContentSearchIndexingStatus.INTERRUPTED,
        ContentSearchIndexingStatus.NONE -> ContentIndexingError.Cancelled.left()

        ContentSearchIndexingStatus.ONGOING -> Unit.right() // unreachable; guarded by isTerminal()
    }
}
