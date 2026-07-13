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
import ch.protonmail.android.mailcommon.domain.coroutines.IODispatcher
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcontentsearch.data.mapper.isTerminal
import ch.protonmail.android.mailcontentsearch.data.mapper.toIndexingState
import ch.protonmail.android.mailcontentsearch.data.usecase.CreateRustSyncService
import ch.protonmail.android.mailcontentsearch.data.wrapper.SyncServiceWrapper
import ch.protonmail.android.mailcontentsearch.domain.model.ContentIndexingState
import ch.protonmail.android.mailcontentsearch.domain.repository.ContentSearchRepository
import ch.protonmail.android.mailsession.data.usecase.ExecuteWithUserSession
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import javax.inject.Inject

class ContentSearchRepositoryImpl @Inject constructor(
    private val executeWithUserSession: ExecuteWithUserSession,
    private val createRustSyncService: CreateRustSyncService,
    @IODispatcher private val ioDispatcher: CoroutineDispatcher
) : ContentSearchRepository {

    override suspend fun clearLocalData(userId: UserId): Either<DataError, Unit> =
        executeWithUserSession(userId) { wrapper ->
            createRustSyncService(wrapper).reset()
        }.flatten()

    override fun observeIndexingStatus(userId: UserId): Flow<ContentIndexingState> =
        observeForUser(userId).flowOn(ioDispatcher)

    override suspend fun getIndexingStatus(userId: UserId): ContentIndexingState =
        readIndexingState(userId) ?: ContentIndexingState.Idle

    private suspend fun readIndexingState(userId: UserId): ContentIndexingState? =
        executeWithUserSession(userId) { wrapper ->
            currentIndexingState(createRustSyncService(wrapper))
        }.getOrNull()

    private suspend fun currentIndexingState(syncService: SyncServiceWrapper): ContentIndexingState? =
        syncService.status().getOrNull()?.toIndexingState(progress = null)

    private fun observeForUser(userId: UserId): Flow<ContentIndexingState> = callbackFlow {
        // Subscribe before reading the snapshot: the stream has no replay, so anything
        // published between the snapshot read and subscribe() would otherwise be lost
        // (e.g. a terminal event that fires in that window would never reach this collector).
        val stream = executeWithUserSession(userId) { wrapper ->
            val syncService = createRustSyncService(wrapper)
            val subscribeResult = syncService.subscribe()

            subscribeResult.onRight { currentIndexingState(syncService)?.let { trySend(it) } }

            subscribeResult
        }.flatten().fold(
            ifLeft = { error ->
                Timber.e("content-search: failed to register indexing watcher: $error")
                null
            },
            ifRight = { it }
        )

        if (stream == null) {
            close()
            return@callbackFlow
        }

        launch {
            while (isActive) {
                val event = stream.next()
                if (event == null) {
                    Timber.w("content-search: indexing watcher closed")
                    close()
                    break
                }

                event.toIndexingState()?.let { trySend(it) }
                if (event.isTerminal()) {
                    close()
                    break
                }
            }
        }

        awaitClose {
            runCatching { stream.destroy() }
        }
    }
}
