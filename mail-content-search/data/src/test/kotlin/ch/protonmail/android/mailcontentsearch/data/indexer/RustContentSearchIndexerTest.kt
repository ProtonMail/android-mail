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

import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcontentsearch.data.usecase.CreateRustSyncService
import ch.protonmail.android.mailcontentsearch.data.wrapper.SyncServiceWrapper
import ch.protonmail.android.mailcontentsearch.domain.model.ContentIndexingError
import ch.protonmail.android.mailsession.data.usecase.ExecuteWithUserSession
import ch.protonmail.android.mailsession.domain.repository.UserSessionRepository
import ch.protonmail.android.mailsession.domain.wrapper.MailUserSessionWrapper
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import uniffi.mail_uniffi.SyncDriverEvent
import uniffi.mail_uniffi.SyncEvent
import uniffi.mail_uniffi.SyncEventStream
import uniffi.mail_uniffi.SyncStartOutcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class RustContentSearchIndexerTest {

    private val userId = UserId("user-1")
    private val dispatcher = UnconfinedTestDispatcher()
    private val wrapper = mockk<MailUserSessionWrapper>()
    private val syncServiceWrapper = mockk<SyncServiceWrapper>()
    private val stream = mockk<SyncEventStream> {
        every { destroy() } returns Unit
    }

    private val userSessionRepository = mockk<UserSessionRepository> {
        coEvery { getUserSession(userId) } returns wrapper
    }
    private val executeWithUserSession = ExecuteWithUserSession(userSessionRepository, dispatcher)
    private val createRustSyncService = mockk<CreateRustSyncService> {
        every { this@mockk(wrapper) } returns syncServiceWrapper
    }

    private val indexer = RustContentSearchIndexer(executeWithUserSession, createRustSyncService)

    @Test
    fun `index short-circuits to Unit when start reports COMPLETED`() = runTest {
        // Given
        coEvery { syncServiceWrapper.subscribe() } returns stream.right()
        coEvery { syncServiceWrapper.start() } returns SyncStartOutcome.COMPLETED.right()

        // When
        val result = indexer.index(userId) { }

        // Then
        assertEquals(Unit.right(), result)
        verify { stream.destroy() }
    }

    @Test
    fun `index returns Cancelled when start reports DISABLED`() = runTest {
        // Given
        coEvery { syncServiceWrapper.subscribe() } returns stream.right()
        coEvery { syncServiceWrapper.start() } returns SyncStartOutcome.DISABLED.right()

        // When
        val result = indexer.index(userId) { }

        // Then
        assertEquals(ContentIndexingError.Cancelled.left(), result)
    }

    @Test
    fun `index reports progress and completes on the Completed event`() = runTest {
        // Given
        val progressValues = mutableListOf<Double>()
        coEvery { syncServiceWrapper.subscribe() } returns stream.right()
        coEvery { syncServiceWrapper.start() } returns SyncStartOutcome.STARTED.right()
        coEvery { stream.next() } returnsMany listOf(SyncEvent.Progress(30.0), SyncEvent.Completed)

        // When
        val result = indexer.index(userId) { progressValues.add(it) }

        // Then
        assertEquals(Unit.right(), result)
        assertEquals(listOf(30.0), progressValues)
        verify { stream.destroy() }
    }

    @Test
    fun `index returns Cancelled when the sync is stopped`() = runTest {
        // Given
        coEvery { syncServiceWrapper.subscribe() } returns stream.right()
        coEvery { syncServiceWrapper.start() } returns SyncStartOutcome.ONGOING.right()
        coEvery { stream.next() } returns SyncEvent.Stopped

        // When
        val result = indexer.index(userId) { }

        // Then
        assertEquals(ContentIndexingError.Cancelled.left(), result)
    }

    @Test
    fun `index returns Cancelled when the stream closes unexpectedly`() = runTest {
        // Given
        coEvery { syncServiceWrapper.subscribe() } returns stream.right()
        coEvery { syncServiceWrapper.start() } returns SyncStartOutcome.STARTED.right()
        coEvery { stream.next() } returns null

        // When
        val result = indexer.index(userId) { }

        // Then
        assertEquals(ContentIndexingError.Cancelled.left(), result)
    }

    @Test
    fun `index returns Unknown when the driver reports a failure`() = runTest {
        // Given
        coEvery { syncServiceWrapper.subscribe() } returns stream.right()
        coEvery { syncServiceWrapper.start() } returns SyncStartOutcome.STARTED.right()
        coEvery { stream.next() } returns SyncEvent.Driver(SyncDriverEvent.Failure("boom"))

        // When
        val result = indexer.index(userId) { }

        // Then
        assertTrue(result.isLeft())
    }

    @Test
    fun `index destroys the stream even when subscribe succeeds but start fails`() = runTest {
        // Given
        coEvery { syncServiceWrapper.subscribe() } returns stream.right()
        coEvery { syncServiceWrapper.start() } returns DataError.Local.Unknown.left()

        // When
        val result = indexer.index(userId) { }

        // Then
        assertTrue(result.isLeft())
        verify { stream.destroy() }
    }

    @Test
    fun `index returns Unknown when subscribing fails`() = runTest {
        // Given
        coEvery { syncServiceWrapper.subscribe() } returns DataError.Local.Unknown.left()

        // When
        val result = indexer.index(userId) { }

        // Then
        assertTrue(result.isLeft())
    }

    @Test
    fun `cancel stops the sync service`() = runTest {
        // Given
        coEvery { syncServiceWrapper.stop() } returns Unit.right()

        // When
        indexer.cancel(userId)

        // Then
        coVerify { syncServiceWrapper.stop() }
    }
}
