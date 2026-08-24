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

import app.cash.turbine.test
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcontentsearch.domain.model.ContentIndexingActivity
import ch.protonmail.android.mailcontentsearch.domain.model.ContentIndexingState
import ch.protonmail.android.mailsession.data.usecase.ExecuteWithUserSession
import ch.protonmail.android.mailsession.data.wrapper.SyncServiceWrapper
import ch.protonmail.android.mailsession.domain.repository.UserSessionRepository
import ch.protonmail.android.mailsession.domain.wrapper.MailUserSessionWrapper
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import uniffi.mail_uniffi.SyncEvent
import uniffi.mail_uniffi.SyncEventStream
import uniffi.mail_uniffi.SyncOrchestratorEvent
import uniffi.mail_uniffi.SyncOrchestratorEventStream
import uniffi.mail_uniffi.SyncProgress
import uniffi.mail_uniffi.SyncStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

internal class ContentSearchRepositoryImplTest {

    private val userId = UserId("user-1")
    private val dispatcher = UnconfinedTestDispatcher()
    private val wrapper = mockk<MailUserSessionWrapper>()
    private val syncServiceWrapper = mockk<SyncServiceWrapper> {
        // Nothing sized yet by default; individual tests override it when they need a percentage.
        coEvery { userProgress(wrapper) } returns SyncProgress(processed = 0uL, total = 0uL, percentage = 0.0).right()
    }

    private val userSessionRepository = mockk<UserSessionRepository> {
        coEvery { getUserSession(userId) } returns wrapper
        every { observeUserSessionAvailable(userId) } returns flowOf(userId)
    }
    private val executeWithUserSession = ExecuteWithUserSession(userSessionRepository, dispatcher)

    private val repository = ContentSearchRepositoryImpl(
        executeWithUserSession = executeWithUserSession,
        syncService = syncServiceWrapper,
        userSessionRepository = userSessionRepository,
        ioDispatcher = dispatcher,
        appScope = CoroutineScope(dispatcher)
    )

    @Test
    fun `one account failing does not end the orchestrator run`() = runTest(dispatcher) {
        // Given
        val stream = mockk<SyncOrchestratorEventStream> { every { destroy() } returns Unit }
        every { syncServiceWrapper.subscribe() } returns stream.right()
        coEvery { stream.next() } returnsMany listOf(
            SyncOrchestratorEvent.UserFailure(userId.id, "[Driver] boom"),
            SyncOrchestratorEvent.WaitingOnUsers,
            null
        )

        // When
        repository.observeIndexingActivity().test {
            // Then
            assertEquals(ContentIndexingActivity.WaitingOnUsers, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `isFeatureEnabled reflects what the sdk reports for the account`() = runTest(dispatcher) {
        every { wrapper.isContentSearchFFEnabled() } returns true

        assertTrue(repository.isFeatureEnabled(userId))
    }

    @Test
    fun `isFeatureEnabled is false when the account has no session`() = runTest(dispatcher) {
        coEvery { userSessionRepository.getUserSession(userId) } returns null

        assertFalse(repository.isFeatureEnabled(userId))

        verify(exactly = 0) { wrapper.isContentSearchFFEnabled() }
    }

    @Test
    fun `clearLocalData resets the account without stopping the orchestrator`() = runTest(dispatcher) {
        // Given
        coEvery { syncServiceWrapper.reset(wrapper) } returns Unit.right()

        // When
        val result = repository.clearLocalData(userId)

        // Then
        assertEquals(Unit.right(), result)
        coVerify(exactly = 1) { syncServiceWrapper.reset(wrapper) }
        coVerify(exactly = 0) { syncServiceWrapper.stop() }
    }

    @Test
    fun `clearLocalData surfaces a failing reset`() = runTest(dispatcher) {
        // Given
        coEvery { syncServiceWrapper.reset(wrapper) } returns DataError.Local.Unknown.left()

        // When
        val result = repository.clearLocalData(userId)

        // Then
        assertEquals(DataError.Local.Unknown.left(), result)
    }

    @Test
    fun `getIndexingStatus maps the sync service status to the domain state`() = runTest(dispatcher) {
        // Given
        coEvery { syncServiceWrapper.userStatus(wrapper) } returns SyncStatus.COMPLETED.right()

        // When
        val result = repository.getIndexingStatus(userId)

        // Then
        assertEquals(ContentIndexingState.Completed, result)
    }

    @Test
    fun `getIndexingStatus maps ONGOING with no known progress to Initializing`() = runTest(dispatcher) {
        // Given
        coEvery { syncServiceWrapper.userStatus(wrapper) } returns SyncStatus.ONGOING.right()

        // When
        val result = repository.getIndexingStatus(userId)

        // Then
        assertEquals(ContentIndexingState.Initializing, result)
    }

    @Test
    fun `getIndexingStatus reports the per user percentage as soon as rust has sized the backfill`() =
        runTest(dispatcher) {
            // Given
            coEvery { syncServiceWrapper.userStatus(wrapper) } returns SyncStatus.ONGOING.right()
            coEvery { syncServiceWrapper.userProgress(wrapper) } returns
                SyncProgress(processed = 30uL, total = 100uL, percentage = 30.0).right()

            // When
            val result = repository.getIndexingStatus(userId)

            // Then
            assertEquals(ContentIndexingState.Running(30.0), result)
        }

    @Test
    fun `getIndexingStatus falls back to Idle when the session has no status`() = runTest(dispatcher) {
        // Given
        coEvery { syncServiceWrapper.userStatus(wrapper) } returns DataError.Local.Unknown.left()

        // When
        val result = repository.getIndexingStatus(userId)

        // Then
        assertEquals(ContentIndexingState.Idle, result)
    }

    @Test
    fun `observeIndexingActivity opens one orchestrator stream however many collectors there are`() =
        runTest(dispatcher) {
            // Given - the worker and the lifecycle observer both watch, and overlap for as long as the
            // app is on screen. A stream each would pump every event - and log it - twice.
            val stream = mockk<SyncOrchestratorEventStream> { every { destroy() } returns Unit }
            every { syncServiceWrapper.subscribe() } returns stream.right()
            coEvery { stream.next() } coAnswers { awaitCancellation() }

            // When
            repository.observeIndexingActivity().test {
                repository.observeIndexingActivity().test {
                    // Then
                    verify(exactly = 1) { syncServiceWrapper.subscribe() }
                    cancelAndIgnoreRemainingEvents()
                }
                cancelAndIgnoreRemainingEvents()
            }

            // Then - the stream outlives a momentary gap between collectors, which is what the worker
            // handing its foreground service to a replacement leaves behind.
            verify(exactly = 0) { stream.destroy() }

            // ...and is still given back once the last collector has been gone for longer than that.
            advanceUntilIdle()
            verify(exactly = 1) { stream.destroy() }
        }

    @Test
    fun `backs off a stream that ends the moment it opens`() = runTest(dispatcher) {
        // Given - a session with nothing to say hands back a stream that closes at once. At a flat
        // second that is a Rust stream created and destroyed at 1 Hz for as long as anyone collects.
        val stream = mockk<SyncOrchestratorEventStream> { every { destroy() } returns Unit }
        every { syncServiceWrapper.subscribe() } returns stream.right()
        coEvery { stream.next() } returns null

        // When
        repository.observeIndexingActivity().test {
            advanceTimeBy(10.seconds)

            // Then - at 0s, 1s, 3s and 7s, rather than eleven times.
            verify(exactly = 4) { syncServiceWrapper.subscribe() }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `resubscribes at once after a stream that reported, whatever the failures before it cost`() =
        runTest(dispatcher) {
            // Given - three failures push the wait out to eight seconds, then a session finally
            // answers. The stream it hands back must not sit out the interval they left behind.
            val stream = mockk<SyncOrchestratorEventStream> { every { destroy() } returns Unit }
            val failures: List<Either<DataError, SyncOrchestratorEventStream>> =
                List(3) { DataError.Local.Unknown.left() }
            every { syncServiceWrapper.subscribe() } returnsMany failures + stream.right()
            coEvery { stream.next() } returnsMany listOf(SyncOrchestratorEvent.WaitingOnUsers, null)

            // When - the three that failed asked again at 0s, 1s and 3s, and the fourth answered at 7s.
            repository.observeIndexingActivity().test {
                advanceTimeBy(8.seconds)
                verify(exactly = 4) { syncServiceWrapper.subscribe() }

                // Then - a second after the reporting stream ended, not the eight it inherited.
                advanceTimeBy(1.seconds)
                verify(exactly = 5) { syncServiceWrapper.subscribe() }
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `keeps asking for the orchestrator stream however long it has been failing`() = runTest(dispatcher) {
        // Given - a shared stream is not restarted by a collector asking again, so giving up on a run
        // of failures would leave a collector that was already there hearing nothing for good.
        val stream = mockk<SyncOrchestratorEventStream> { every { destroy() } returns Unit }
        val failures: List<Either<DataError, SyncOrchestratorEventStream>> =
            List(6) { DataError.Local.Unknown.left() }
        every { syncServiceWrapper.subscribe() } returnsMany failures + stream.right()
        coEvery { stream.next() } coAnswers { awaitCancellation() }

        // When
        repository.observeIndexingActivity().test {
            advanceUntilIdle()

            // Then - the six that failed, and the one that answered.
            verify(exactly = 7) { syncServiceWrapper.subscribe() }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observeIndexingStatus emits the snapshot then live events then resubscribes on a terminal one`() =
        runTest(dispatcher) {
            // Given
            val liveStream = mockk<SyncEventStream> { every { destroy() } returns Unit }
            val parkedStream = mockk<SyncEventStream> { every { destroy() } returns Unit }
            coEvery { syncServiceWrapper.subscribeUser(wrapper) } returnsMany listOf(
                liveStream.right(),
                parkedStream.right()
            )
            coEvery { syncServiceWrapper.userStatus(wrapper) } returns SyncStatus.ONGOING.right()
            coEvery { liveStream.next() } returnsMany listOf(
                SyncEvent.Progress(SyncProgress(processed = 50uL, total = 100uL, percentage = 50.0)),
                SyncEvent.Completed
            )
            coEvery { parkedStream.next() } coAnswers { awaitCancellation() }

            // When + Then
            repository.observeIndexingStatus(userId).test {
                assertEquals(ContentIndexingState.Initializing, awaitItem())
                assertEquals(ContentIndexingState.Running(50.0), awaitItem())
                assertEquals(ContentIndexingState.Completed, awaitItem())
                assertEquals(ContentIndexingState.Initializing, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `observeIndexingStatus retries when subscribing fails`() = runTest(dispatcher) {
        // Given
        val parkedStream = mockk<SyncEventStream> { every { destroy() } returns Unit }
        coEvery { syncServiceWrapper.subscribeUser(wrapper) } returnsMany listOf(
            DataError.Local.Unknown.left(),
            parkedStream.right()
        )
        coEvery { syncServiceWrapper.userStatus(wrapper) } returns SyncStatus.ONGOING.right()
        coEvery { parkedStream.next() } coAnswers { awaitCancellation() }

        // When + Then
        repository.observeIndexingStatus(userId).test {
            assertEquals(ContentIndexingState.Initializing, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        coVerify(atLeast = 2) { syncServiceWrapper.subscribeUser(wrapper) }
    }

    @Test
    fun `shouldShowMobileBottomSheet returns the sync service value`() = runTest(dispatcher) {
        // Given
        coEvery { syncServiceWrapper.shouldShowMobileSheet(wrapper) } returns true.right()

        // When
        val result = repository.shouldShowMobileBottomSheet(userId)

        // Then
        assertEquals(true, result)
    }

    @Test
    fun `shouldShowMobileBottomSheet falls back to false when the sync service call fails`() = runTest(dispatcher) {
        // Given
        coEvery { syncServiceWrapper.shouldShowMobileSheet(wrapper) } returns DataError.Local.Unknown.left()

        // When
        val result = repository.shouldShowMobileBottomSheet(userId)

        // Then
        assertEquals(false, result)
    }

}
