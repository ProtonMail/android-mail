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

import ch.protonmail.android.mailcontentsearch.domain.model.ContentIndexingActivity
import me.proton.core.domain.entity.UserId
import uniffi.mail_uniffi.SyncOrchestartorStartStats
import uniffi.mail_uniffi.SyncOrchestratorEvent
import uniffi.mail_uniffi.SyncOrchestratorProgress
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class ContentIndexingActivityMapperTest {

    @Test
    fun `start stats map to the domain summary`() {
        // Given
        val stats = SyncOrchestartorStartStats(
            completed = 1uL,
            pending = 2uL,
            disabled = 3uL,
            failed = 4uL,
            ongoing = 5uL,
            total = 15uL
        )

        // When
        val summary = stats.toStartSummary()

        // Then
        assertEquals(2, summary.pending)
        assertEquals(5, summary.ongoing)
        assertEquals(1, summary.completed)
        assertEquals(4, summary.failed)
        assertEquals(3, summary.disabled)
        assertEquals(15, summary.total)
    }

    @Test
    fun `there is work pending when accounts are pending or ongoing`() {
        assertTrue(stats(pending = 1uL, ongoing = 0uL).toStartSummary().hasWorkPending)
        assertTrue(stats(pending = 0uL, ongoing = 1uL).toStartSummary().hasWorkPending)
    }

    @Test
    fun `there is no work pending when every account is settled`() {
        // Given - completed, failed and disabled accounts are all nothing left to drive.
        val stats = SyncOrchestartorStartStats(
            completed = 2uL,
            pending = 0uL,
            disabled = 1uL,
            failed = 1uL,
            ongoing = 0uL,
            total = 4uL
        )

        // Then
        assertFalse(stats.toStartSummary().hasWorkPending)
    }

    @Test
    fun `progress carries the active account and the account counts`() {
        // Given
        val event = SyncOrchestratorEvent.Progress(
            progress(activeId = "user-1", completedUsers = 1uL, userCount = 3uL)
        )

        // When
        val activity = event.toIndexingActivity()

        // Then
        assertEquals(
            ContentIndexingActivity.Progress(UserId("user-1"), completedUsers = 1, userCount = 3),
            activity
        )
    }

    @Test
    fun `progress tolerates no active account`() {
        // Given - the orchestrator reports no active id while it is between accounts.
        val event = SyncOrchestratorEvent.Progress(progress(activeId = null))

        // When
        val activity = event.toIndexingActivity() as ContentIndexingActivity.Progress

        // Then
        assertNull(activity.activeUserId)
    }

    @Test
    fun `progress treats a blank active account as none`() {
        // Given
        val event = SyncOrchestratorEvent.Progress(progress(activeId = ""))

        // When
        val activity = event.toIndexingActivity() as ContentIndexingActivity.Progress

        // Then
        assertNull(activity.activeUserId)
    }

    @Test
    fun `forward mode carries the account that finished its backfill`() {
        assertEquals(
            ContentIndexingActivity.ForwardMode(UserId("user-2")),
            SyncOrchestratorEvent.ForwardModeEntered("user-2").toIndexingActivity()
        )
    }

    @Test
    fun `waiting on users maps to the domain equivalent`() {
        assertEquals(
            ContentIndexingActivity.WaitingOnUsers,
            SyncOrchestratorEvent.WaitingOnUsers.toIndexingActivity()
        )
    }

    @Test
    fun `stopped and completed both mean the orchestrator has nothing left to drive`() {
        assertEquals(ContentIndexingActivity.Stopped, SyncOrchestratorEvent.Stopped.toIndexingActivity())
        // Completed is never actually published by the SDK, so it folds into Stopped.
        assertEquals(ContentIndexingActivity.Stopped, SyncOrchestratorEvent.Completed.toIndexingActivity())
    }

    @Test
    fun `an orchestrator failure carries its reason`() {
        assertEquals(
            ContentIndexingActivity.Failed("boom"),
            SyncOrchestratorEvent.Failure("boom").toIndexingActivity()
        )
    }

    @Test
    fun `a single account failing is not an orchestrator level event`() {
        // Given - Rust records it and moves to the next candidate.
        val event = SyncOrchestratorEvent.UserFailure("user-3", "boom")

        // Then
        assertNull(event.toIndexingActivity())
    }

    @Test
    fun `started is ignored so it cannot be mistaken for progress`() {
        assertNull(SyncOrchestratorEvent.Started.toIndexingActivity())
    }

    private fun stats(pending: ULong, ongoing: ULong) = SyncOrchestartorStartStats(
        completed = 0uL,
        pending = pending,
        disabled = 0uL,
        failed = 0uL,
        ongoing = ongoing,
        total = pending + ongoing
    )

    private fun progress(
        activeId: String?,
        completedUsers: ULong = 0uL,
        userCount: ULong = 1uL
    ) = SyncOrchestratorProgress(
        activeId = activeId,
        userCount = userCount,
        completedUsers = completedUsers,
        total = 100uL,
        processed = 10uL,
        percentage = 10.0
    )
}
