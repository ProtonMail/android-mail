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

package ch.protonmail.android.mailcontentsearch.data.mapper

import ch.protonmail.android.mailcontentsearch.domain.model.ContentIndexingState
import uniffi.mail_uniffi.SyncDriverEvent
import uniffi.mail_uniffi.SyncEvent
import uniffi.mail_uniffi.SyncStatus
import uniffi.mail_uniffi.SyncWorkerEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class ContentIndexingStatusMapperTest {

    @Test
    fun `maps PENDING status to Idle`() {
        assertEquals(ContentIndexingState.Idle, SyncStatus.PENDING.toIndexingState(null))
    }

    @Test
    fun `maps COMPLETED status to Completed`() {
        assertEquals(ContentIndexingState.Completed, SyncStatus.COMPLETED.toIndexingState(null))
    }

    @Test
    fun `maps ONGOING status to Running with the given progress`() {
        assertEquals(ContentIndexingState.Running(42.0), SyncStatus.ONGOING.toIndexingState(42.0))
    }

    @Test
    fun `maps ONGOING status with no progress to Initializing`() {
        assertEquals(ContentIndexingState.Initializing, SyncStatus.ONGOING.toIndexingState(null))
    }

    @Test
    fun `maps Started event to Initializing`() {
        assertEquals(ContentIndexingState.Initializing, SyncEvent.Started.toIndexingState())
    }

    @Test
    fun `maps Progress event to Running`() {
        assertEquals(ContentIndexingState.Running(64.0), SyncEvent.Progress(64.0).toIndexingState())
    }

    @Test
    fun `maps Completed event to Completed`() {
        assertEquals(ContentIndexingState.Completed, SyncEvent.Completed.toIndexingState())
    }

    @Test
    fun `maps Stopped event to Cancelled`() {
        assertEquals(ContentIndexingState.Cancelled, SyncEvent.Stopped.toIndexingState())
    }

    @Test
    fun `maps Driver Failure event to Failed`() {
        assertEquals(
            ContentIndexingState.Failed,
            SyncEvent.Driver(SyncDriverEvent.Failure("boom")).toIndexingState()
        )
    }

    @Test
    fun `Driver Completed event maps to null`() {
        assertNull(SyncEvent.Driver(SyncDriverEvent.Completed).toIndexingState())
    }

    @Test
    fun `Worker event maps to null`() {
        assertNull(SyncEvent.Worker(SyncWorkerEvent.Processed("name", 1u)).toIndexingState())
    }

    @Test
    fun `terminal events are reported as terminal`() {
        assertTrue(SyncEvent.Completed.isTerminal())
        assertTrue(SyncEvent.Stopped.isTerminal())
        assertTrue(SyncEvent.Driver(SyncDriverEvent.Failure("boom")).isTerminal())
    }

    @Test
    fun `non-terminal events are reported as non-terminal`() {
        assertFalse(SyncEvent.Started.isTerminal())
        assertFalse(SyncEvent.Progress(10.0).isTerminal())
        assertFalse(SyncEvent.Driver(SyncDriverEvent.Completed).isTerminal())
        assertFalse(SyncEvent.Worker(SyncWorkerEvent.Processed("name", 1u)).isTerminal())
    }
}
