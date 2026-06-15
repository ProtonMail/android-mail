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

package ch.protonmail.android.mailcontentsearch.data.repository

import arrow.core.Either
import arrow.core.flatten
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.coroutines.IODispatcher
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcontentsearch.data.mapper.toIndexingState
import ch.protonmail.android.mailcontentsearch.domain.model.ContentIndexingState
import ch.protonmail.android.mailcontentsearch.domain.repository.ContentSearchSettingsRepository
import ch.protonmail.android.mailsession.data.usecase.ExecuteWithUserSession
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import uniffi.mail_uniffi.MailUserSessionContentSearchWatchIndexingStreamResult
import uniffi.mail_uniffi.WatchContentSearchIndexingStreamNextAsyncResult
import javax.inject.Inject

class ContentSearchSettingsRepositoryImpl @Inject constructor(
    private val executeWithUserSession: ExecuteWithUserSession,
    @IODispatcher private val ioDispatcher: CoroutineDispatcher
) : ContentSearchSettingsRepository {

    private val enabledChanges = MutableSharedFlow<UserId>(extraBufferCapacity = 8)

    override suspend fun isEnabled(userId: UserId): Either<DataError, Boolean> =
        executeWithUserSession(userId) { wrapper -> wrapper.contentSearchIsEnabled() }.flatten()

    override fun observeIsEnabled(userId: UserId): Flow<Boolean> = enabledChanges
        .filter { it == userId }
        .onStart { emit(userId) }
        .map { isEnabled(userId).getOrNull() ?: false }
        .distinctUntilChanged()
        .flowOn(ioDispatcher)

    override suspend fun setEnabled(userId: UserId, enabled: Boolean): Either<DataError, Unit> =
        executeWithUserSession(userId) { wrapper ->
            wrapper.contentSearchSetEnabled(enabled)
            Unit.right()
        }.flatten().also { result ->
            result.onRight { enabledChanges.tryEmit(userId) }
        }

    override suspend fun clearLocalData(userId: UserId): Either<DataError, Unit> =
        executeWithUserSession(userId) { wrapper ->
            wrapper.contentSearchClearLocalData()
            Unit.right()
        }.flatten()

    override fun observeIndexingStatus(userId: UserId): Flow<ContentIndexingState> =
        observeForUser(userId).flowOn(ioDispatcher)

    override suspend fun getIndexingStatus(userId: UserId): ContentIndexingState =
        readIndexingState(userId) ?: ContentIndexingState.Idle

    private suspend fun readIndexingState(userId: UserId): ContentIndexingState? =
        executeWithUserSession(userId) { wrapper ->
            val status = wrapper.contentSearchGetIndexingStatus().getOrNull() ?: return@executeWithUserSession null
            val progress = wrapper.contentSearchGetIndexingProgress().getOrNull()
            toIndexingState(status, progress)
        }.getOrNull()

    private fun observeForUser(userId: UserId): Flow<ContentIndexingState> = callbackFlow {
        val stream = executeWithUserSession(userId) { wrapper ->
            wrapper.contentSearchWatchIndexingStream()
        }.fold(
            ifLeft = { error ->
                Timber.e("content-search: failed to register indexing watcher: $error")
                null
            },
            ifRight = { result ->
                when (result) {
                    is MailUserSessionContentSearchWatchIndexingStreamResult.Ok -> result.v1
                    is MailUserSessionContentSearchWatchIndexingStreamResult.Error -> {
                        Timber.e("content-search: failed to register indexing watcher: ${result.v1}")
                        null
                    }
                }
            }
        )

        if (stream == null) {
            close()
            return@callbackFlow
        }

        trySend(stream.initialProgress().toIndexingState())

        launch {
            while (isActive) {
                when (val next = stream.nextAsync()) {
                    is WatchContentSearchIndexingStreamNextAsyncResult.Ok ->
                        trySend(next.v1.toIndexingState())

                    is WatchContentSearchIndexingStreamNextAsyncResult.Error -> {
                        Timber.w("content-search: indexing watcher closed: ${next.v1}")
                        close()
                        break
                    }
                }
            }
        }

        awaitClose {
            stream.cancel()
            runCatching { stream.close() }
        }
    }
}
