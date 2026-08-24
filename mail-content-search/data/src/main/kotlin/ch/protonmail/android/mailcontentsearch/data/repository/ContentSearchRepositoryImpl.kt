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
import ch.protonmail.android.mailcommon.domain.coroutines.AppScope
import ch.protonmail.android.mailcommon.domain.coroutines.IODispatcher
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcontentsearch.data.mapper.isTerminal
import ch.protonmail.android.mailcontentsearch.data.mapper.log
import ch.protonmail.android.mailcontentsearch.data.mapper.toIndexingActivity
import ch.protonmail.android.mailcontentsearch.data.mapper.toIndexingState
import ch.protonmail.android.mailcontentsearch.data.mapper.toStartSummary
import ch.protonmail.android.mailcontentsearch.domain.model.ContentIndexingActivity
import ch.protonmail.android.mailcontentsearch.domain.model.ContentIndexingStartSummary
import ch.protonmail.android.mailcontentsearch.domain.model.ContentIndexingState
import ch.protonmail.android.mailcontentsearch.domain.repository.ContentSearchRepository
import ch.protonmail.android.mailsession.data.usecase.ExecuteWithUserSession
import ch.protonmail.android.mailsession.data.wrapper.SyncServiceWrapper
import ch.protonmail.android.mailsession.domain.repository.UserSessionRepository
import ch.protonmail.android.mailsession.domain.wrapper.MailUserSessionWrapper
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import uniffi.mail_uniffi.SyncOrchestratorEventStream
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class ContentSearchRepositoryImpl @Inject constructor(
    private val executeWithUserSession: ExecuteWithUserSession,
    private val syncService: SyncServiceWrapper,
    private val userSessionRepository: UserSessionRepository,
    @IODispatcher private val ioDispatcher: CoroutineDispatcher,
    @AppScope private val appScope: CoroutineScope
) : ContentSearchRepository {

    /**
     * Backs [observeIndexingActivity].
     *
     * The orchestrator stream has no replay and ends when the session is torn down, so a caller would
     * otherwise stop hearing about indexing after the first restart. Resubscribing keeps a long-lived
     * collector (the worker) alive across that.
     *
     * A round that reports nothing backs off instead of being given up on after a few tries: giving up
     * was affordable while each caller had a stream of its own, but [SharingStarted.WhileSubscribed]
     * restarts a shared upstream only on the first subscriber, so a collector that was already there
     * would hear nothing for the rest of its life. Reports nothing covers both a subscribe that failed
     * and a stream that opened and closed without a word - the same session saying it has nothing for
     * us, the second at the price of a Rust stream created and destroyed on every pass. The backoff
     * resets as soon as a stream does report, so resubscribing across a session teardown stays quick.
     *
     * Shared, because there is more than one collector - the worker and the lifecycle observer, which
     * overlap for as long as the app is on screen - and a Rust stream each means two event pumps
     * reporting the same run, with every trace line logged twice.
     *
     * The stop timeout outlives the worker handing its foreground service to a replacement, which
     * leaves nobody subscribed for a moment. Without it that gap would destroy the Rust stream and
     * subscribe again, for a collector that is about to come back.
     *
     * Being a [SharedFlow] it never completes, so a collector cannot tell "the orchestrator is done"
     * from "we are between subscriptions" - the worker bounds its own wait with a watchdog rather than
     * waiting for the stream to end.
     */
    private val indexingActivity: SharedFlow<ContentIndexingActivity> = flow {
        var backoff = ResubscribeBackoff
        while (currentCoroutineContext().isActive) {
            val stream = syncService.subscribe().getOrElse { error ->
                Timber.e("content-search: failed to watch the indexing orchestrator: $error")
                null
            }

            var reportedActivity = false
            if (stream != null) emitAll(observeOrchestrator(stream).onEach { reportedActivity = true })

            // Decided by this round rather than carried from the last, so a stream that reported does
            // not first sit out the interval a run of failures before it left behind.
            val resubscribeIn = if (reportedActivity) ResubscribeBackoff else backoff
            delay(resubscribeIn)
            backoff = (resubscribeIn * 2).coerceAtMost(MaxResubscribeBackoff)
        }
    }.flowOn(ioDispatcher)
        .shareIn(appScope, SharingStarted.WhileSubscribed(stopTimeoutMillis = SharingStopTimeoutMillis))

    // Answered by the user session, so an account without one - signed out, or still unlocking -
    // reads as unavailable.
    override suspend fun isFeatureEnabled(userId: UserId): Boolean =
        executeWithUserSession(userId) { wrapper -> wrapper.isContentSearchFFEnabled() }.getOrElse { error ->
            Timber.d("content-search: availability could not be resolved for the account: $error")
            false
        }

    // Only reset: stop() is global as of the sync orchestrator, so it would halt indexing for
    // every other account too. reset() already re-prepares this user and hands the orchestrator
    // its next candidate.
    override suspend fun clearLocalData(userId: UserId): Either<DataError, Unit> =
        executeWithUserSession(userId) { wrapper -> syncService.reset(wrapper) }.flatten()

    override suspend fun startIndexing(): Either<DataError, ContentIndexingStartSummary> =
        withContext(ioDispatcher) { syncService.start().map { it.toStartSummary() } }

    // Hands the account back to the orchestrator, which clears the failure Rust has on record. The
    // session-wide `startIndexing` does not: it skips accounts on record as failed rather than
    // retrying them.
    override suspend fun startIndexingForUser(userId: UserId): Either<DataError, Unit> =
        executeWithUserSession(userId) { wrapper -> syncService.startUser(wrapper).map { } }.flatten()

    override suspend fun stopIndexing(): Either<DataError, Unit> = withContext(ioDispatcher) { syncService.stop() }

    override fun observeIndexingActivity(): Flow<ContentIndexingActivity> = indexingActivity

    private fun observeOrchestrator(stream: SyncOrchestratorEventStream): Flow<ContentIndexingActivity> = callbackFlow {
        launch {
            while (isActive) {
                val event = stream.next()
                if (event == null) {
                    Timber.w("content-search: indexing orchestrator watcher closed")
                    close()
                    break
                }
                // Logged before mapping: the events the mapper drops (a single account failing,
                // above all) are the ones worth having in a bug report.
                event.log()
                event.toIndexingActivity()?.let {
                    // Buffered without bound below: dropping an event here would mean dropping a
                    // terminal one, and the worker holds the foreground service until it sees that.
                    if (trySend(it).isFailure) Timber.w("content-search: dropped an indexing event ($it)")
                }
            }
        }

        awaitClose {
            runCatching { stream.destroy() }
        }
    }.buffer(Channel.UNLIMITED)

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
            delay(ResubscribeBackoff)
            close()
            return@callbackFlow
        }

        launch {
            while (isActive) {
                val event = stream.next()
                if (event == null) {
                    Timber.w("content-search: indexing watcher closed")
                    // Throttle the resubscribe in observeIndexingStatus so quick transitions don't make the UI flash.
                    delay(ResubscribeBackoff)
                    close()
                    break
                }

                event.toIndexingState()?.let { trySend(it) }
                if (event.isTerminal()) {
                    // Throttle the resubscribe in observeIndexingStatus so a session that fails
                    // immediately on every subscribe can't spin the loop with no backoff.
                    delay(ResubscribeBackoff)
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

        val ResubscribeBackoff: Duration = 1.seconds

        val MaxResubscribeBackoff: Duration = 1.minutes

        const val SharingStopTimeoutMillis = 5_000L
    }
}
