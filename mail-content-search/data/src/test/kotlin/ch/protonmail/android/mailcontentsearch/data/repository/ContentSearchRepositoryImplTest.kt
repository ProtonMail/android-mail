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
import ch.protonmail.android.mailcontentsearch.data.usecase.CreateRustSyncService
import ch.protonmail.android.mailcontentsearch.data.wrapper.SyncServiceWrapper
import ch.protonmail.android.mailcontentsearch.domain.model.ContentIndexingState
import ch.protonmail.android.mailsession.data.usecase.ExecuteWithUserSession
import ch.protonmail.android.mailsession.domain.repository.UserSessionRepository
import ch.protonmail.android.mailsession.domain.wrapper.MailUserSessionWrapper
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import uniffi.mail_uniffi.SyncEvent
import uniffi.mail_uniffi.SyncEventStream
import uniffi.mail_uniffi.SyncProgress
import uniffi.mail_uniffi.SyncStatus
import kotlin.test.Test
import kotlin.test.assertEquals

internal class ContentSearchRepositoryImplTest {

    private val userId = UserId("user-1")
    private val dispatcher = UnconfinedTestDispatcher()
    private val wrapper = mockk<MailUserSessionWrapper>()
    private val syncServiceWrapper = mockk<SyncServiceWrapper>()

    private val userSessionRepository = mockk<UserSessionRepository> {
        coEvery { getUserSession(userId) } returns wrapper
    }
    private val executeWithUserSession = ExecuteWithUserSession(userSessionRepository, dispatcher)
    private val createRustSyncService = mockk<CreateRustSyncService> {
        every { this@mockk(wrapper) } returns syncServiceWrapper
    }

    private val repository = ContentSearchRepositoryImpl(
        executeWithUserSession = executeWithUserSession,
        createRustSyncService = createRustSyncService,
        ioDispatcher = dispatcher
    )

    @Test
    fun `clearLocalData resets the sync service`() = runTest(dispatcher) {
        // Given
        coEvery { syncServiceWrapper.reset() } returns Unit.right()

        // When
        val result = repository.clearLocalData(userId)

        // Then
        assertEquals(Unit.right(), result)
        coVerify { syncServiceWrapper.reset() }
    }

    @Test
    fun `getIndexingStatus maps the sync service status to the domain state`() = runTest(dispatcher) {
        // Given
        coEvery { syncServiceWrapper.status() } returns SyncStatus.COMPLETED.right()

        // When
        val result = repository.getIndexingStatus(userId)

        // Then
        assertEquals(ContentIndexingState.Completed, result)
    }

    @Test
    fun `getIndexingStatus maps ONGOING with no known progress to Initializing`() = runTest(dispatcher) {
        // Given
        coEvery { syncServiceWrapper.status() } returns SyncStatus.ONGOING.right()

        // When
        val result = repository.getIndexingStatus(userId)

        // Then
        assertEquals(ContentIndexingState.Initializing, result)
    }

    @Test
    fun `getIndexingStatus falls back to Idle when the session has no status`() = runTest(dispatcher) {
        // Given
        coEvery { syncServiceWrapper.status() } returns DataError.Local.Unknown.left()

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
            coEvery { syncServiceWrapper.subscribe() } returnsMany listOf(liveStream.right(), parkedStream.right())
            coEvery { syncServiceWrapper.status() } returns SyncStatus.ONGOING.right()
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
        coEvery { syncServiceWrapper.subscribe() } returnsMany listOf(
            DataError.Local.Unknown.left(),
            parkedStream.right()
        )
        coEvery { syncServiceWrapper.status() } returns SyncStatus.ONGOING.right()
        coEvery { parkedStream.next() } coAnswers { awaitCancellation() }

        // When + Then
        repository.observeIndexingStatus(userId).test {
            assertEquals(ContentIndexingState.Initializing, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        coVerify(atLeast = 2) { syncServiceWrapper.subscribe() }
    }

    @Test
    fun `shouldShowMobileBottomSheet returns the sync service value`() = runTest(dispatcher) {
        // Given
        coEvery { syncServiceWrapper.shouldShowMobileBottomSheet() } returns true.right()

        // When
        val result = repository.shouldShowMobileBottomSheet(userId)

        // Then
        assertEquals(true, result)
    }

    @Test
    fun `shouldShowMobileBottomSheet falls back to false when the sync service call fails`() = runTest(dispatcher) {
        // Given
        coEvery { syncServiceWrapper.shouldShowMobileBottomSheet() } returns DataError.Local.Unknown.left()

        // When
        val result = repository.shouldShowMobileBottomSheet(userId)

        // Then
        assertEquals(false, result)
    }
}
