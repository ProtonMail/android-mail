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
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcontentsearch.domain.model.ContentIndexingState
import ch.protonmail.android.mailsession.data.repository.MailSessionRepository
import ch.protonmail.android.mailsession.data.usecase.ExecuteWithUserSession
import ch.protonmail.android.mailsession.data.wrapper.MailSessionWrapper
import ch.protonmail.android.mailsession.data.wrapper.SyncServiceWrapper
import ch.protonmail.android.mailsession.domain.repository.UserSessionRepository
import ch.protonmail.android.mailsession.domain.wrapper.MailUserSessionWrapper
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import uniffi.mail_uniffi.SyncEvent
import uniffi.mail_uniffi.SyncEventStream
import uniffi.mail_uniffi.SyncProgress
import uniffi.mail_uniffi.SyncStatus
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertEquals

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

    private val mailSession = mockk<MailSessionWrapper>()
    private val mailSessionRepository = mockk<MailSessionRepository> {
        every { isMailSessionInitialised() } returns true
        every { getMailSession() } returns mailSession
    }

    private val repository = ContentSearchRepositoryImpl(
        executeWithUserSession = executeWithUserSession,
        mailSessionRepository = mailSessionRepository,
        syncService = syncServiceWrapper,
        userSessionRepository = userSessionRepository,
        ioDispatcher = dispatcher
    )

    @Test
    fun `isFeatureEnabled reflects what the sdk reports`() = runTest(dispatcher) {
        every { mailSession.isContentSearchFFEnabled() } returns true

        assertTrue(repository.isFeatureEnabled())
    }

    @Test
    fun `isFeatureEnabled is false before the mail session exists`() = runTest(dispatcher) {
        every { mailSessionRepository.isMailSessionInitialised() } returns false

        assertFalse(repository.isFeatureEnabled())

        verify(exactly = 0) { mailSessionRepository.getMailSession() }
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

    @Test
    fun `isMeteredConnectionAllowed returns the sync service value`() = runTest(dispatcher) {
        // Given
        coEvery { syncServiceWrapper.isMeteredConnectionAllowed(wrapper) } returns true.right()

        // When
        val result = repository.isMeteredConnectionAllowed(userId)

        // Then
        assertEquals(true.right(), result)
    }

    @Test
    fun `isMeteredConnectionAllowed propagates the sync service failure`() = runTest(dispatcher) {
        // Given
        coEvery { syncServiceWrapper.isMeteredConnectionAllowed(wrapper) } returns DataError.Local.Unknown.left()

        // When
        val result = repository.isMeteredConnectionAllowed(userId)

        // Then
        assertEquals(DataError.Local.Unknown.left(), result)
    }

    @Test
    fun `setMeteredConnectionAllowed forwards the value to the sync service`() = runTest(dispatcher) {
        // Given
        coEvery { syncServiceWrapper.setAllowMeteredConnection(wrapper, false) } returns Unit.right()

        // When
        val result = repository.setMeteredConnectionAllowed(userId, false)

        // Then
        assertEquals(Unit.right(), result)
        coVerify(exactly = 1) { syncServiceWrapper.setAllowMeteredConnection(wrapper, false) }
    }

    @Test
    fun `setMeteredConnectionAllowed propagates the sync service failure`() = runTest(dispatcher) {
        // Given
        coEvery {
            syncServiceWrapper.setAllowMeteredConnection(wrapper, true)
        } returns DataError.Local.Unknown.left()

        // When
        val result = repository.setMeteredConnectionAllowed(userId, true)

        // Then
        assertEquals(DataError.Local.Unknown.left(), result)
    }
}
