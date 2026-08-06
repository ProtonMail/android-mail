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
import arrow.core.getOrElse
import ch.protonmail.android.mailcommon.domain.coroutines.IODispatcher
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcontentsearch.data.mapper.isTerminal
import ch.protonmail.android.mailcontentsearch.data.mapper.toIndexingState
import ch.protonmail.android.mailcontentsearch.domain.model.ContentIndexingState
import ch.protonmail.android.mailcontentsearch.domain.repository.ContentSearchRepository
import ch.protonmail.android.mailsession.data.repository.MailSessionRepository
import ch.protonmail.android.mailsession.data.usecase.ExecuteWithUserSession
import ch.protonmail.android.mailsession.data.wrapper.SyncServiceWrapper
import ch.protonmail.android.mailsession.domain.repository.UserSessionRepository
import ch.protonmail.android.mailsession.domain.wrapper.MailUserSessionWrapper
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

class ContentSearchRepositoryImpl @Inject constructor(
    private val executeWithUserSession: ExecuteWithUserSession,
    private val mailSessionRepository: MailSessionRepository,
    private val syncService: SyncServiceWrapper,
    private val userSessionRepository: UserSessionRepository,
    @IODispatcher private val ioDispatcher: CoroutineDispatcher
) : ContentSearchRepository {

    // Resolved from the app session rather than a user session, so it answers before login too.
    override suspend fun isFeatureEnabled(): Boolean = withContext(ioDispatcher) {
        if (!mailSessionRepository.isMailSessionInitialised()) {
            Timber.d("content-search: availability requested before the mail session was created")
            return@withContext false
        }
        mailSessionRepository.getMailSession().isContentSearchFFEnabled()
    }

    // Only reset: stop() is global as of the sync orchestrator, so it would halt indexing for
    // every other account too. reset() already re-prepares this user and hands the orchestrator
    // its next candidate.
    override suspend fun clearLocalData(userId: UserId): Either<DataError, Unit> =
        executeWithUserSession(userId) { wrapper -> syncService.reset(wrapper) }.flatten()

    // observeForUser ends on every terminal event, so resubscribe to stay durable across worker
    // reschedules (which stop and restart the underlying session). Gated on the user session so
    // the loop terminates on logout instead of spinning on NoUserSession at the backoff rate.
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeIndexingStatus(userId: UserId): Flow<ContentIndexingState> =
        userSessionRepository.observeUserSessionAvailable(userId)
            .flatMapLatest { availableUserId ->
                if (availableUserId == null) {
                    emptyFlow()
                } else {
                    flow {
                        while (currentCoroutineContext().isActive) {
                            emitAll(observeForUser(userId))
                        }
                    }
                }
            }
            .flowOn(ioDispatcher)

    override suspend fun getIndexingStatus(userId: UserId): ContentIndexingState =
        readIndexingState(userId) ?: ContentIndexingState.Idle

    override suspend fun shouldShowMobileBottomSheet(userId: UserId): Boolean =
        executeWithUserSession(userId) { wrapper ->
            syncService.shouldShowMobileSheet(wrapper)
        }.flatten().getOrElse { false }

    private suspend fun readIndexingState(userId: UserId): ContentIndexingState? =
        executeWithUserSession(userId) { wrapper -> currentIndexingState(wrapper) }.getOrNull()

    // Both reads answer while the orchestrator is stopped, so the UI gets a determinate
    // percentage from the very first frame instead of an indefinite "preparing".
    private suspend fun currentIndexingState(wrapper: MailUserSessionWrapper): ContentIndexingState? {
        val status = syncService.userStatus(wrapper).getOrNull() ?: return null
        // No totals yet means Rust has not sized the backfill: report "preparing" rather than 0%.
        val progress = syncService.userProgress(wrapper).getOrNull()?.takeIf { it.total > 0uL }?.percentage
        return status.toIndexingState(progress)
    }

    private fun observeForUser(userId: UserId): Flow<ContentIndexingState> = callbackFlow {
        // Subscribe before reading the snapshot: the stream has no replay, so anything
        // published between the snapshot read and subscribe() would otherwise be lost
        // (e.g. a terminal event that fires in that window would never reach this collector).
        val stream = executeWithUserSession(userId) { wrapper ->
            val subscribeResult = syncService.subscribeUser(wrapper)

            subscribeResult.onRight { currentIndexingState(wrapper)?.let { trySend(it) } }

            subscribeResult
        }.flatten().fold(
            ifLeft = { error ->
                Timber.e("content-search: failed to register indexing watcher: $error")
                null
            },
            ifRight = { it }
        )

        if (stream == null) {
            // Throttle the resubscribe in observeIndexingStatus so quick transitions don't make the UI flash.
            delay(ResubscribeBackoffMillis.milliseconds)
            close()
            return@callbackFlow
        }

        launch {
            while (isActive) {
                val event = stream.next()
                if (event == null) {
                    Timber.w("content-search: indexing watcher closed")
                    // Throttle the resubscribe in observeIndexingStatus so quick transitions don't make the UI flash.
                    delay(ResubscribeBackoffMillis.milliseconds)
                    close()
                    break
                }

                event.toIndexingState()?.let { trySend(it) }
                if (event.isTerminal()) {
                    // Throttle the resubscribe in observeIndexingStatus so a session that fails
                    // immediately on every subscribe can't spin the loop with no backoff.
                    delay(ResubscribeBackoffMillis.milliseconds)
                    close()
                    break
                }
            }
        }

        awaitClose {
            runCatching { stream.destroy() }
        }
    }

    private companion object {

        const val ResubscribeBackoffMillis = 1_000L
    }
}
